package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetExportPreset1280x720
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.duration
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.CoreMedia.CMTimeGetSeconds
import kotlin.coroutines.resume

/**
 * iOS implementation. Video normalization uses `AVAssetExportSession` with
 * the 720p H.264 preset producing an MP4. Image normalization decodes via
 * `UIImage` (respecting embedded orientation), downscales to a max long edge
 * of 1920 px, then encodes JPEG at ~82% quality. Audio is passed through.
 *
 * On any failure the processor deletes any partial output before rethrowing.
 * The user's external source file is never touched.
 */
@OptIn(ExperimentalForeignApi::class)
class IosMediaProcessor(private val store: IosMediaStore) : MediaProcessor {

    override suspend fun process(raw: RawMedia): ProcessedMedia {
        return try {
            when (raw.type) {
                MediaType.Image -> processImage(raw)
                MediaType.Video -> processVideo(raw)
                MediaType.Audio -> processAudio(raw)
            }
        } finally {
            if (raw.ownedByRelive) {
                NSFileManager.defaultManager.removeItemAtPath(raw.sourcePath, error = null)
            }
        }
    }

    private fun processImage(raw: RawMedia): ProcessedMedia {
        val data = NSFileManager.defaultManager.contentsAtPath(raw.sourcePath)
            ?: error("Cannot read image")
        val src = UIImage.imageWithData(data)
            ?: error("Cannot decode image")
        val scaled = scaleIfNeeded(src, TARGET_LONG_EDGE.toDouble())
        val jpeg = UIImageJPEGRepresentation(scaled, JPEG_QUALITY)
            ?: error("Cannot encode JPEG")
        val destRef = store.allocateKey(MediaType.Image)
        val destPath = store.resolveAbsolutePath(destRef)
        return try {
            NSFileManager.defaultManager.createFileAtPath(destPath, contents = jpeg, attributes = null)
            ProcessedMedia(
                type = MediaType.Image,
                storageRef = destRef,
                widthPx = scaled.size.useContents { width.toInt() },
                heightPx = scaled.size.useContents { height.toInt() },
            )
        } catch (t: Throwable) {
            NSFileManager.defaultManager.removeItemAtPath(destPath, error = null)
            throw t
        }
    }

    private fun scaleIfNeeded(image: UIImage, maxEdge: Double): UIImage {
        val (w, h) = image.size.useContents { width to height }
        val long = maxOf(w, h)
        if (long <= maxEdge) return image
        val scale = maxEdge / long
        val nw = (w * scale)
        val nh = (h * scale)
        val newSize = platform.CoreGraphics.CGSizeMake(nw, nh)
        platform.UIKit.UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        image.drawInRect(platform.CoreGraphics.CGRectMake(0.0, 0.0, nw, nh))
        val out = platform.UIKit.UIGraphicsGetImageFromCurrentImageContext()
        platform.UIKit.UIGraphicsEndImageContext()
        return out ?: image
    }

    private suspend fun processVideo(raw: RawMedia): ProcessedMedia {
        val srcUrl = NSURL.fileURLWithPath(raw.sourcePath)
        val asset = AVURLAsset.URLAssetWithURL(srcUrl, options = null)
        val destRef = store.allocateKey(MediaType.Video)
        val destPath = store.resolveAbsolutePath(destRef)
        val destUrl = NSURL.fileURLWithPath(destPath)
        // Overwrite guard.
        NSFileManager.defaultManager.removeItemAtPath(destPath, error = null)

        val session = AVAssetExportSession(asset = asset, presetName = AVAssetExportPreset1280x720)
            ?: error("Cannot create export session")
        session.outputURL = destUrl
        session.outputFileType = AVFileTypeMPEG4
        session.shouldOptimizeForNetworkUse = true

        val status = suspendCancellableCoroutine<platform.AVFoundation.AVAssetExportSessionStatus> { cont ->
            session.exportAsynchronouslyWithCompletionHandler {
                if (cont.isActive) cont.resume(session.status)
            }
            cont.invokeOnCancellation { session.cancelExport() }
        }
        if (status != AVAssetExportSessionStatusCompleted) {
            NSFileManager.defaultManager.removeItemAtPath(destPath, error = null)
            error("Export failed: ${session.error?.localizedDescription}")
        }
        val durationMs = (CMTimeGetSeconds(asset.duration) * 1000.0).toLong()
        return ProcessedMedia(
            type = MediaType.Video,
            storageRef = destRef,
            durationMs = durationMs,
        )
    }

    private fun processAudio(raw: RawMedia): ProcessedMedia {
        val destRef = store.allocateKey(MediaType.Audio)
        val destPath = store.resolveAbsolutePath(destRef)
        val data = NSFileManager.defaultManager.contentsAtPath(raw.sourcePath) ?: error("Cannot read audio")
        return try {
            NSFileManager.defaultManager.createFileAtPath(destPath, contents = data, attributes = null)
            val asset = AVURLAsset.URLAssetWithURL(NSURL.fileURLWithPath(destPath), options = null)
            val durationMs = (CMTimeGetSeconds(asset.duration) * 1000.0).toLong()
            ProcessedMedia(type = MediaType.Audio, storageRef = destRef, durationMs = durationMs)
        } catch (t: Throwable) {
            NSFileManager.defaultManager.removeItemAtPath(destPath, error = null)
            throw t
        }
    }

    private companion object {
        const val TARGET_LONG_EDGE = 1920
        const val JPEG_QUALITY = 0.82
    }
}
