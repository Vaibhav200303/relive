package com.vaibhav.relive.platform.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.media.ExifInterface
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android implementation. Never modifies the source; on failure removes any
 * partial output. Video normalization uses Media3 Transformer (H.264/AAC,
 * downscale-only). Image normalization uses `BitmapFactory` + `ExifInterface`
 * to correct orientation and downscale to ~1920 px long edge, then encodes
 * JPEG at ~82% quality. Audio is passed through: the recorder already writes
 * AAC/M4A at the desired bitrate.
 */
class AndroidMediaProcessor(
    private val context: Context,
    private val store: AndroidMediaStore,
) : MediaProcessor {

    override suspend fun process(raw: RawMedia): ProcessedMedia = withContext(Dispatchers.IO) {
        try {
            val out = when (raw.type) {
                MediaType.Image -> processImage(raw)
                MediaType.Video -> processVideo(raw)
                MediaType.Audio -> processAudio(raw)
            }
            out
        } finally {
            if (raw.ownedByRelive) runCatching { File(raw.sourcePath).delete() }
        }
    }

    // ---- image ---------------------------------------------------------

    private fun processImage(raw: RawMedia): ProcessedMedia {
        val srcFile = File(raw.sourcePath)
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(srcFile.absolutePath, opts)
        val maxEdge = maxOf(opts.outWidth, opts.outHeight)
        val sample = if (maxEdge > TARGET_LONG_EDGE) {
            var s = 1
            while (maxEdge / (s * 2) >= TARGET_LONG_EDGE) s *= 2
            s
        } else 1
        val decoded = BitmapFactory.decodeFile(
            srcFile.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: throw IllegalStateException("Failed to decode image")

        val oriented = applyExifOrientation(decoded, srcFile)
        val scaled = downscaleIfNeeded(oriented)

        val destRef = store.allocateKey(MediaType.Image)
        val destFile = File(store.resolveAbsolutePath(destRef))
        destFile.parentFile?.mkdirs()
        try {
            FileOutputStream(destFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
            }
        } catch (t: Throwable) {
            destFile.delete()
            throw t
        } finally {
            if (scaled !== oriented) scaled.recycle()
            if (oriented !== decoded) oriented.recycle()
            decoded.recycle()
        }
        return ProcessedMedia(
            type = MediaType.Image,
            storageRef = destRef,
            widthPx = scaled.width,
            heightPx = scaled.height,
        )
    }

    private fun applyExifOrientation(bitmap: Bitmap, file: File): Bitmap {
        val exif = try { ExifInterface(file.absolutePath) } catch (_: Throwable) { return bitmap }
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun downscaleIfNeeded(bitmap: Bitmap): Bitmap {
        val long = maxOf(bitmap.width, bitmap.height)
        if (long <= TARGET_LONG_EDGE) return bitmap
        val scale = TARGET_LONG_EDGE.toFloat() / long
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    // ---- video ---------------------------------------------------------

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private suspend fun processVideo(raw: RawMedia): ProcessedMedia {
        val destRef = store.allocateKey(MediaType.Video)
        val destFile = File(store.resolveAbsolutePath(destRef))
        destFile.parentFile?.mkdirs()

        val uri = File(raw.sourcePath).toURI().toString()
        // Apply optional trim range as MediaItem clipping so Transformer only
        // demuxes/decodes the selected interval — cheaper and exact.
        val itemBuilder = MediaItem.Builder().setUri(uri)
        val start = raw.trimStartMs
        val end = raw.trimEndMs
        if (start != null && end != null && end > start) {
            itemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(start.coerceAtLeast(0L))
                    .setEndPositionMs(end)
                    .build(),
            )
        }
        val mediaItem = itemBuilder.build()
        val videoEffects: List<Effect> = listOf(Presentation.createForHeight(TARGET_VIDEO_SHORT_EDGE))
        val edited = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), videoEffects))
            .apply { if (raw.muteAudio) setRemoveAudio(true) }
            .build()
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.post {
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(androidx.media3.common.MimeTypes.VIDEO_H264)
                        .setAudioMimeType(androidx.media3.common.MimeTypes.AUDIO_AAC)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, result: ExportResult) {
                                if (cont.isActive) cont.resume(Unit)
                            }
                            override fun onError(
                                composition: Composition,
                                result: ExportResult,
                                exception: ExportException,
                            ) {
                                if (cont.isActive) cont.resumeWithException(exception)
                            }
                        })
                        .build()
                    transformer.start(edited, destFile.absolutePath)
                    cont.invokeOnCancellation { handler.post { transformer.cancel() } }
                }
            }
        } catch (t: Throwable) {
            destFile.delete()
            throw t
        }

        val (w, h, duration) = probeVideo(destFile)
        return ProcessedMedia(
            type = MediaType.Video,
            storageRef = destRef,
            widthPx = w,
            heightPx = h,
            durationMs = duration,
        )
    }

    private fun probeVideo(f: File): Triple<Int?, Int?, Long?> {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(f.absolutePath)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            Triple(w, h, d)
        } catch (_: Throwable) {
            Triple(null, null, null)
        } finally {
            r.release()
        }
    }

    // ---- audio ---------------------------------------------------------

    private fun processAudio(raw: RawMedia): ProcessedMedia {
        // Recorder-produced audio is already AAC/M4A in the store's tmp area
        // if the recorder wrote directly there. Otherwise copy the source into
        // an allocated audio key without re-encoding — the source is either
        // the user's library file (AAC-family in practice for phone audio) or
        // an already-normalized recording.
        val src = File(raw.sourcePath)
        val destRef = store.allocateKey(MediaType.Audio)
        val destFile = File(store.resolveAbsolutePath(destRef))
        destFile.parentFile?.mkdirs()
        try {
            src.inputStream().use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
        } catch (t: Throwable) {
            destFile.delete()
            throw t
        }
        val duration = try {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(destFile.absolutePath)
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            }
        } catch (_: Throwable) { null }
        return ProcessedMedia(type = MediaType.Audio, storageRef = destRef, durationMs = duration)
    }

    private inline fun <R> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> R): R {
        try { return block(this) } finally { release() }
    }

    companion object {
        private const val TARGET_LONG_EDGE = 1920
        private const val JPEG_QUALITY = 82
        private const val TARGET_VIDEO_SHORT_EDGE = 720
    }
}
