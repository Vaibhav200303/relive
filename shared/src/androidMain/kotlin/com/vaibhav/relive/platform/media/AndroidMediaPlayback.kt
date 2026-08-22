package com.vaibhav.relive.platform.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.ui.components.timeline.WaveformView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.Volatile

@Composable
actual fun RelivedImage(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val key = ref.value + ":viewer"
    val cached = AndroidImageTileCache.get(key)
    val bitmap by produceState<Bitmap?>(initialValue = cached, key1 = key) {
        if (value != null) return@produceState
        val b = withContext(Dispatchers.Default) {
            decodeOriented(path, targetLongEdgePx = 2048)
        }
        if (b != null) {
            AndroidImageTileCache.put(key, b)
            value = b
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = "Photo",
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(modifier = modifier.background(Color(0x22000000)))
    }
}

@Composable
actual fun RelivedVideo(
    ref: MediaStorageRef,
    mediaStore: MediaStore,
    modifier: Modifier,
    posterFallbackPath: String?,
) {
    val path = mediaStore.resolveAbsolutePath(ref)
    var player: MediaPlayer? by remember(path) { mutableStateOf(null) }
    var playing by remember(path) { mutableStateOf(false) }
    // Rotation-aware natural size drives the inner surface aspect so the
    // decoded frame is never stretched to the outer bounds. Paused thumbnail
    // and playing surface share this same inner box, so preview size does
    // not jump on Play.
    val natural = rememberVideoNaturalSizeFor(ref, mediaStore)
    val aspect = natural?.let {
        if (it.widthPx > 0 && it.heightPx > 0) it.widthPx.toFloat() / it.heightPx.toFloat() else null
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        val inner = if (aspect != null) {
            // matchHeightConstraintsFirst for portrait so the inner box
            // never overflows the composer's height ceiling.
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .aspectRatio(aspect, matchHeightConstraintsFirst = aspect < 1f)
        } else Modifier.fillMaxSize()
        Box(modifier = inner) {
            AndroidView(
                factory = { ctx ->
                    val surface = SurfaceView(ctx)
                    surface.holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            val mp = MediaPlayer().apply {
                                setDataSource(path)
                                setDisplay(holder)
                                setOnPreparedListener { /* wait for user tap */ }
                                prepareAsync()
                            }
                            player = mp
                        }
                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) {}
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            player?.release(); player = null
                        }
                    })
                    surface
                },
                modifier = Modifier.fillMaxSize().clickable {
                    val mp = player ?: return@clickable
                    if (mp.isPlaying) { mp.pause(); playing = false }
                    else { mp.start(); playing = true }
                },
            )
            // Poster overlay so the ready surface never renders as a bare
            // black rectangle before first playback. Two layered thumbnails
            // (path-keyed cache): the pre-processing source poster below,
            // painted instantly from the composer's already-warm cache to
            // avoid a Processing→Ready black flash; the processed video's
            // own first frame above, extracted on a background dispatcher
            // and replacing the fallback once ready. Both are hidden while
            // the player is actively playing so the decoded frames show
            // through. Non-composer callers pass posterFallbackPath = null
            // and get no poster overlay (existing behavior preserved).
            if (!playing && posterFallbackPath != null) {
                VideoSourceThumbnail(
                    sourcePath = posterFallbackPath,
                    modifier = Modifier.fillMaxSize(),
                )
                VideoSourceThumbnail(
                    sourcePath = path,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (!playing) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) { Text("▶", color = Color.White) }
        }
    }
    DisposableEffect(path) {
        onDispose { player?.release(); player = null }
    }
}

@Composable
actual fun RelivedAudio(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val player = remember(path) {
        MediaPlayer().apply {
            try { setDataSource(path); prepare() } catch (_: Throwable) { /* leave in error state */ }
        }
    }
    var playing by remember(path) { mutableStateOf(false) }
    var pos by remember(path) { mutableStateOf(0) }
    val duration = remember(path) {
        try { player.duration } catch (_: Throwable) { 0 }
    }
    DisposableEffect(path) { onDispose { player.release() } }
    LaunchedEffect(playing, path) {
        while (playing) {
            pos = try { player.currentPosition } catch (_: Throwable) { 0 }
            delay(200)
        }
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x11000000))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF6F4E37))
                .clickable {
                    try {
                        if (player.isPlaying) { player.pause(); playing = false }
                        else { player.start(); playing = true }
                    } catch (_: Throwable) {}
                },
            contentAlignment = Alignment.Center,
        ) { Text(if (playing) "❚❚" else "▶", color = Color.White) }
        Text(formatTime(pos) + " / " + formatTime(duration))
    }
}

@Composable
actual fun RelivedImageTile(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val key = ref.value
    val cached = AndroidImageTileCache.get(key)
    val bitmap by produceState<Bitmap?>(initialValue = cached, key1 = key) {
        if (value != null) return@produceState
        val b = withContext(Dispatchers.Default) { decodeOriented(path, targetLongEdgePx = 1024) }
        if (b != null) {
            AndroidImageTileCache.put(key, b)
            value = b
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = "Photo",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier.background(Color(0x22000000)))
    }
}

@Composable
actual fun RelivedVideoTile(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val key = ref.value
    val cached = AndroidVideoThumbnailCache.get(key)
    val frame by produceState<Bitmap?>(initialValue = cached, key1 = key) {
        if (value != null) return@produceState
        val bmp = withContext(Dispatchers.Default) { extractVideoFrame(path) }
        if (bmp != null) {
            AndroidVideoThumbnailCache.put(key, bmp)
            value = bmp
        }
    }
    Box(
        modifier = modifier
            .background(Color.Black)
            .semantics { contentDescription = "Video" },
        contentAlignment = Alignment.Center,
    ) {
        val f = frame
        if (f != null) {
            Image(
                bitmap = f.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center,
        ) { Text("▶", color = Color.White) }
    }
}

@Composable
actual fun RelivedAudioTile(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val durationMs = rememberMediaDurationMs(ref, mediaStore) ?: 0L
    val envelope = rememberWaveformFor(ref, mediaStore, WaveformProcessor.bucketsFor(durationMs))
    val holder = remember(path) { AndroidAudioTileHolder() }
    var playing by remember(path) { mutableStateOf(false) }
    var pos by remember(path) { mutableStateOf(0) }
    val stopSelf: () -> Unit = remember(path) {
        {
            holder.pauseIfPlaying()
            playing = false
        }
    }
    DisposableEffect(path) {
        onDispose {
            ActivePlayback.release(stopSelf)
            holder.release()
        }
    }
    LaunchedEffect(playing, path) {
        while (playing) {
            pos = holder.currentPositionMs()
            if (!holder.isPlaying()) {
                playing = false
                ActivePlayback.release(stopSelf)
                break
            }
            delay(60)
        }
    }
    val progress = WaveformProcessor.progressFraction(pos.toLong(), durationMs)
    val label = if (playing) formatTime(pos) else formatTime(durationMs.toInt())
    Box(modifier = modifier.background(Color(0xFF0A0A0A))) {
        WaveformView(
            envelope = envelope,
            progress = progress,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(64.dp)
                .align(Alignment.Center)
                .padding(vertical = 12.dp),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable {
                        if (holder.togglePlay(path)) {
                            val nowPlaying = holder.isPlaying()
                            playing = nowPlaying
                            if (nowPlaying) ActivePlayback.claim(stopSelf)
                            else { pos = holder.currentPositionMs(); ActivePlayback.release(stopSelf) }
                        }
                    }
                    .semantics { contentDescription = if (playing) "Pause audio" else "Play audio" },
                contentAlignment = Alignment.Center,
            ) { Text(if (playing) "❚❚" else "▶", color = Color.White) }
            Text(label, color = Color(0xFFEFECE5))
        }
    }
}

actual fun readImageAspectRatio(ref: MediaStorageRef, mediaStore: MediaStore): Float? {
    val path = mediaStore.resolveAbsolutePath(ref)
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        val w = opts.outWidth
        val h = opts.outHeight
        if (w <= 0 || h <= 0) null
        else {
            val (ow, oh) = orientedDimensions(path, w, h)
            ow.toFloat() / oh.toFloat()
        }
    } catch (_: Throwable) {
        null
    }
}

/**
 * Pixel dimensions of the image at [path] after applying its EXIF
 * orientation (90/270 rotations swap width and height). Falls back to
 * [rawW]/[rawH] on any EXIF failure.
 */
private fun orientedDimensions(path: String, rawW: Int, rawH: Int): Pair<Int, Int> = try {
    val exif = ExifInterface(path)
    val o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    if (o == ExifInterface.ORIENTATION_ROTATE_90 ||
        o == ExifInterface.ORIENTATION_ROTATE_270 ||
        o == ExifInterface.ORIENTATION_TRANSPOSE ||
        o == ExifInterface.ORIENTATION_TRANSVERSE
    ) rawH to rawW else rawW to rawH
} catch (_: Throwable) {
    rawW to rawH
}

actual fun extractWaveform(
    ref: MediaStorageRef,
    mediaStore: MediaStore,
    targetBuckets: Int,
): FloatArray? {
    val path = mediaStore.resolveAbsolutePath(ref)
    return try { decodePcmEnvelope(path, targetBuckets) } catch (_: Throwable) { null }
}

/**
 * Decode the first audio track of the file at [path] with MediaExtractor +
 * MediaCodec, feeding decoded 16-bit PCM into [WaveformProcessor.envelope]
 * incrementally so raw PCM is never fully retained. Releases codec and
 * extractor in a finally block on every path.
 *
 * Returns null on codec-config failure so the tile falls back to a flat
 * baseline (ADR-0019 §12).
 */
private fun decodePcmEnvelope(path: String, targetBuckets: Int): FloatArray? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
        extractor.setDataSource(path)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) { trackIndex = i; format = f; break }
        }
        if (trackIndex < 0 || format == null) return null
        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        // Two-pass would need to buffer all PCM; instead do a single pass
        // and downsample streamingly into fixed buckets by output-sample
        // index. We do not know total sample count up-front, so pick bucket
        // width by decoded-sample position vs an estimate from duration and
        // sample-rate; if the estimate is off, we grow buckets on demand.
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1) else 1
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION))
            format.getLong(MediaFormat.KEY_DURATION) else 0L
        val estFrames = if (durationUs > 0L) (durationUs * sampleRate / 1_000_000L) else 0L
        val buckets = targetBuckets.coerceAtLeast(1)
        val peaks = IntArray(buckets)
        var frameIndex = 0L
        // If we do not know estFrames, size buckets to a lower bound and
        // extend proportionally at end.
        val plannedFrames = if (estFrames > 0L) estFrames else Long.MAX_VALUE / 4

        val info = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false
        val timeoutUs = 10_000L
        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inIdx = codec.dequeueInputBuffer(timeoutUs)
                if (inIdx >= 0) {
                    val inBuf = codec.getInputBuffer(inIdx)!!
                    val sz = extractor.readSampleData(inBuf, 0)
                    if (sz < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        val ts = extractor.sampleTime
                        codec.queueInputBuffer(inIdx, 0, sz, ts, 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
            if (outIdx >= 0) {
                if (info.size > 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)!!
                    outBuf.position(info.offset)
                    outBuf.limit(info.offset + info.size)
                    val sb = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val n = sb.remaining()
                    var i = 0
                    while (i < n) {
                        // Fold channels: max abs across channels at this frame.
                        var frameMax = 0
                        var c = 0
                        while (c < channels && i < n) {
                            val v = sb.get(i).toInt()
                            val a = if (v < 0) -v else v
                            if (a > frameMax) frameMax = a
                            c++; i++
                        }
                        val b = if (plannedFrames > 0)
                            ((frameIndex * buckets) / plannedFrames).toInt().coerceIn(0, buckets - 1)
                        else 0
                        if (frameMax > peaks[b]) peaks[b] = frameMax
                        frameIndex++
                    }
                }
                codec.releaseOutputBuffer(outIdx, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEOS = true
            }
        }
        if (frameIndex == 0L) return null
        val out = FloatArray(buckets)
        for (i in 0 until buckets) out[i] = (peaks[i] / 32767f).coerceIn(0f, 1f)
        return out
    } catch (_: Throwable) {
        return null
    } finally {
        try { codec?.stop() } catch (_: Throwable) {}
        try { codec?.release() } catch (_: Throwable) {}
        try { extractor.release() } catch (_: Throwable) {}
    }
}

actual fun readImageNaturalSize(ref: MediaStorageRef, mediaStore: MediaStore): NaturalSizePx? {
    val path = mediaStore.resolveAbsolutePath(ref)
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        val w = opts.outWidth
        val h = opts.outHeight
        if (w <= 0 || h <= 0) null
        else {
            val (ow, oh) = orientedDimensions(path, w, h)
            NaturalSizePx(ow, oh)
        }
    } catch (_: Throwable) {
        null
    }
}

actual fun readVideoNaturalSize(ref: MediaStorageRef, mediaStore: MediaStore): NaturalSizePx? =
    readVideoNaturalSizeFromPath(mediaStore.resolveAbsolutePath(ref))

actual fun readVideoNaturalSizeFromPath(path: String): NaturalSizePx? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (w <= 0 || h <= 0) null
        else if (rot == 90 || rot == 270) NaturalSizePx(h, w)
        else NaturalSizePx(w, h)
    } catch (_: Throwable) {
        null
    } finally {
        try { retriever.release() } catch (_: Throwable) {}
    }
}

actual fun readMediaDurationMs(ref: MediaStorageRef, mediaStore: MediaStore): Long? {
    val path = mediaStore.resolveAbsolutePath(ref)
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (_: Throwable) {
        null
    } finally {
        try { retriever.release() } catch (_: Throwable) {}
    }
}

/**
 * Lazy Android audio player used by the timeline audio tile. Never
 * constructs a [MediaPlayer] until the user taps Play; releases on scroll
 * off or Compose disposal.
 */
private class AndroidAudioTileHolder {
    private var player: MediaPlayer? = null

    /** Returns true if the state may have changed and the caller should re-read [isPlaying]. */
    fun togglePlay(path: String): Boolean {
        val existing = player
        return try {
            if (existing == null) {
                val mp = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    start()
                }
                player = mp
                true
            } else if (existing.isPlaying) {
                existing.pause()
                true
            } else {
                existing.start()
                true
            }
        } catch (_: Throwable) {
            release()
            false
        }
    }

    fun isPlaying(): Boolean = try { player?.isPlaying == true } catch (_: Throwable) { false }

    fun pauseIfPlaying() {
        try { if (player?.isPlaying == true) player?.pause() } catch (_: Throwable) {}
    }

    fun currentPositionMs(): Int = try { player?.currentPosition ?: 0 } catch (_: Throwable) { 0 }

    fun release() {
        try { player?.release() } catch (_: Throwable) {}
        player = null
    }
}

/**
 * Bounded per-process Bitmap cache for downsampled timeline image tiles.
 * Copy-on-write to keep read/get paths lock-free during scrolling.
 */
private object AndroidImageTileCache {
    private const val CAPACITY = 96
    @Volatile private var map: Map<String, Bitmap> = emptyMap()
    fun get(key: String): Bitmap? = map[key]
    fun put(key: String, value: Bitmap) {
        val cur = map
        map = if (cur.size < CAPACITY) cur + (key to value)
        else cur.entries.drop(cur.size - CAPACITY + 1).associate { it.key to it.value } + (key to value)
    }
}

/**
 * Bounded per-process Bitmap cache for video thumbnails. Smaller cap
 * because video frame Bitmaps at source resolution are heavier than
 * downsampled photos.
 */
private object AndroidVideoThumbnailCache {
    private const val CAPACITY = 64
    @Volatile private var map: Map<String, Bitmap> = emptyMap()
    fun get(key: String): Bitmap? = map[key]
    fun put(key: String, value: Bitmap) {
        val cur = map
        map = if (cur.size < CAPACITY) cur + (key to value)
        else cur.entries.drop(cur.size - CAPACITY + 1).associate { it.key to it.value } + (key to value)
    }
}

/**
 * Separate cache for thumbnails extracted from a pre-processing source
 * file (composer placeholder). Keyed by absolute path so it does not
 * collide with the ref-keyed processed-video thumbnail cache above.
 */
private object AndroidSourceVideoThumbnailCache {
    private const val CAPACITY = 32
    @Volatile private var map: Map<String, Bitmap> = emptyMap()
    fun get(key: String): Bitmap? = map[key]
    fun put(key: String, value: Bitmap) {
        val cur = map
        map = if (cur.size < CAPACITY) cur + (key to value)
        else cur.entries.drop(cur.size - CAPACITY + 1).associate { it.key to it.value } + (key to value)
    }
}

@Composable
actual fun VideoSourceThumbnail(sourcePath: String?, modifier: Modifier) {
    if (sourcePath == null) {
        Box(modifier = modifier)
        return
    }
    val cached = AndroidSourceVideoThumbnailCache.get(sourcePath)
    val frame by produceState<Bitmap?>(initialValue = cached, key1 = sourcePath) {
        if (value != null) return@produceState
        val bmp = withContext(Dispatchers.Default) { extractVideoFrame(sourcePath) }
        if (bmp != null) {
            AndroidSourceVideoThumbnailCache.put(sourcePath, bmp)
            value = bmp
        }
    }
    val f = frame
    if (f != null) {
        Image(
            bitmap = f.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier)
    }
}

/**
 * Decodes an image at [path] downsampled so its longest edge is at most
 * ~[targetLongEdgePx]. Uses a bounds-only probe first, then an
 * `inSampleSize`-guided decode. Returns null on any failure.
 *
 * The optimized Relive copy caps the long edge at ~1920 px; timeline grid
 * tiles need far fewer pixels, so a 1024 target avoids decoding the full
 * bitmap into memory just for a small cell.
 */
/**
 * Decodes at [path] downsampled to at most [targetLongEdgePx] and applies
 * the EXIF orientation so callers can render the bitmap as-is. Portrait
 * photos captured on a rotated sensor otherwise appear sideways and waste
 * viewer area (ADR-0019 §5). Returns null on any failure.
 */
private fun decodeOriented(path: String, targetLongEdgePx: Int): Bitmap? {
    val raw = decodeDownsampled(path, targetLongEdgePx) ?: return null
    return try {
        val exif = ExifInterface(path)
        val orient = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
        )
        val matrix = Matrix()
        when (orient) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return raw
        }
        val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (rotated !== raw) raw.recycle()
        rotated
    } catch (_: Throwable) {
        raw
    }
}

private fun decodeDownsampled(path: String, targetLongEdgePx: Int = 1024): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) {
        null
    } else {
        var sample = 1
        val longEdge = if (w >= h) w else h
        while (longEdge / (sample * 2) >= targetLongEdgePx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(path, opts)
    }
} catch (_: Throwable) {
    null
}

private fun extractVideoFrame(path: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (_: Throwable) {
        null
    } finally {
        try { retriever.release() } catch (_: Throwable) {}
    }
}

private fun formatTime(ms: Int): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    val ss = if (s < 10) "0$s" else s.toString()
    return "$m:$ss"
}
