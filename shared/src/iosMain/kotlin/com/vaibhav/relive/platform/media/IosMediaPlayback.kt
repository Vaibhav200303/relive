package com.vaibhav.relive.platform.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.ui.components.timeline.WaveformView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayer
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RelivedImage(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    UIKitView(
        factory = {
            val iv = UIImageView()
            iv.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
            val data = NSFileManager.defaultManager.contentsAtPath(path)
            iv.image = data?.let { UIImage.imageWithData(it) }
            iv
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RelivedVideo(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val player = remember(path) { AVPlayer(uRL = NSURL.fileURLWithPath(path)) }
    var playing by remember(path) { mutableStateOf(false) }
    DisposableEffect(path) { onDispose { player.pause() } }
    Box(modifier = modifier.background(Color.Black)) {
        UIKitView(
            factory = {
                val view = UIView()
                val layer = AVPlayerLayer.playerLayerWithPlayer(player)
                layer.frame = view.bounds
                view.layer.addSublayer(layer)
                view
            },
            modifier = Modifier.fillMaxSize().clickable {
                if (playing) { player.pause(); playing = false }
                else { player.play(); playing = true }
            },
        )
        if (!playing) {
            Box(
                modifier = Modifier.align(Alignment.Center).size(56.dp)
                    .clip(CircleShape).background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) { Text("▶", color = Color.White) }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RelivedAudio(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val player = remember(path) {
        AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(path), error = null).also { it.prepareToPlay() }
    }
    var playing by remember(path) { mutableStateOf(false) }
    DisposableEffect(path) { onDispose { player.stop() } }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x11000000))
            .padding(12.dp)
            .clickable {
                if (player.playing) { player.pause(); playing = false }
                else { player.play(); playing = true }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(if (playing) "❚❚  ${formatSec(player.currentTime)}" else "▶  ${formatSec(player.duration)}")
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RelivedImageTile(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val key = ref.value
    val cached = IosImageTileCache.get(key)
    val image by produceState<UIImage?>(initialValue = cached, key1 = key) {
        if (value != null) return@produceState
        val img = withContext(Dispatchers.Default) { loadUIImage(path) }
        if (img != null) {
            IosImageTileCache.put(key, img)
            value = img
        }
    }
    val current = image
    if (current != null) {
        UIKitView(
            factory = {
                val iv = UIImageView()
                iv.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
                iv.clipsToBounds = true
                iv.image = current
                iv
            },
            update = { it.image = current },
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.background(Color(0x22000000)))
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RelivedVideoTile(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val key = ref.value
    val cached = IosVideoThumbnailCache.get(key)
    val frame by produceState<UIImage?>(initialValue = cached, key1 = key) {
        if (value != null) return@produceState
        val img = withContext(Dispatchers.Default) { extractVideoFrame(path) }
        if (img != null) {
            IosVideoThumbnailCache.put(key, img)
            value = img
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
            UIKitView(
                factory = {
                    val iv = UIImageView()
                    iv.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
                    iv.clipsToBounds = true
                    iv.image = f
                    iv
                },
                update = { it.image = f },
                modifier = Modifier.fillMaxSize(),
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

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RelivedAudioTile(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val durationMs = rememberMediaDurationMs(ref, mediaStore) ?: 0L
    val envelope = rememberWaveformFor(ref, mediaStore, WaveformProcessor.bucketsFor(durationMs))
    val holder = remember(path) { IosAudioTileHolder() }
    var playing by remember(path) { mutableStateOf(false) }
    var posMs by remember(path) { mutableStateOf(0L) }
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
            posMs = (holder.currentTimeSec() * 1000.0).toLong()
            if (!holder.isPlaying()) {
                playing = false
                ActivePlayback.release(stopSelf)
                break
            }
            delay(60)
        }
    }
    val progress = WaveformProcessor.progressFraction(posMs, durationMs)
    val label = if (playing) formatSec(posMs / 1000.0) else formatSec(durationMs.toDouble() / 1000.0)
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
                        holder.togglePlay(path)
                        val nowPlaying = holder.isPlaying()
                        playing = nowPlaying
                        if (nowPlaying) ActivePlayback.claim(stopSelf)
                        else { posMs = (holder.currentTimeSec() * 1000.0).toLong(); ActivePlayback.release(stopSelf) }
                    }
                    .semantics { contentDescription = if (playing) "Pause audio" else "Play audio" },
                contentAlignment = Alignment.Center,
            ) { Text(if (playing) "❚❚" else "▶", color = Color.White) }
            Text(label, color = Color(0xFFEFECE5))
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun readImageAspectRatio(ref: MediaStorageRef, mediaStore: MediaStore): Float? {
    val path = mediaStore.resolveAbsolutePath(ref)
    val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
    val image = UIImage.imageWithData(data) ?: return null
    return image.size.useContents {
        if (width > 0.0 && height > 0.0) (width / height).toFloat() else null
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun extractWaveform(
    ref: MediaStorageRef,
    mediaStore: MediaStore,
    targetBuckets: Int,
): FloatArray? {
    val path = mediaStore.resolveAbsolutePath(ref)
    return try {
        val url = NSURL.fileURLWithPath(path)
        val file = AVAudioFile(forReading = url, error = null)
        val format = file.processingFormat
        val length = file.length
        if (length <= 0L) return null
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = length.toUInt())
            ?: return null
        val ok = try { file.readIntoBuffer(buffer, error = null) } catch (_: Throwable) { false }
        if (!ok) return null
        val chansPtr = buffer.floatChannelData ?: return null
        val ch0 = chansPtr[0] ?: return null
        val n = buffer.frameLength.toLong()
        if (n <= 0L) return null
        val buckets = targetBuckets.coerceAtLeast(1)
        val peaks = FloatArray(buckets)
        for (b in 0 until buckets) {
            val start = b.toLong() * n / buckets
            val endRaw = (b + 1).toLong() * n / buckets
            val end = (if (endRaw > start) endRaw else start + 1L).coerceAtMost(n)
            var peak = 0f
            var i = start
            while (i < end) {
                val v = ch0[i]
                val a = if (v < 0f) -v else v
                if (a > peak) peak = a
                i++
            }
            peaks[b] = peak.coerceIn(0f, 1f)
        }
        peaks
    } catch (_: Throwable) {
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun readMediaDurationMs(ref: MediaStorageRef, mediaStore: MediaStore): Long? {
    val path = mediaStore.resolveAbsolutePath(ref)
    val asset = AVAsset.assetWithURL(NSURL.fileURLWithPath(path))
    val seconds = CMTimeGetSeconds(asset.duration)
    if (seconds.isNaN() || seconds <= 0.0) return null
    return (seconds * 1000.0).toLong()
}

/**
 * Lazy iOS audio player. Never allocates AVAudioPlayer until Play tapped.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosAudioTileHolder {
    private var player: AVAudioPlayer? = null

    fun togglePlay(path: String) {
        val existing = player
        if (existing == null) {
            val mp = AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(path), error = null)
            mp.prepareToPlay()
            mp.play()
            player = mp
        } else if (existing.playing) {
            existing.pause()
        } else {
            existing.play()
        }
    }

    fun isPlaying(): Boolean = player?.playing == true

    fun currentTimeSec(): Double = player?.currentTime ?: 0.0

    fun pauseIfPlaying() {
        val p = player ?: return
        if (p.playing) p.pause()
    }

    fun release() {
        player?.stop()
        player = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun loadUIImage(path: String): UIImage? {
    val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
    return UIImage.imageWithData(data)
}

@OptIn(ExperimentalForeignApi::class)
private fun extractVideoFrame(path: String): UIImage? = try {
    val asset = AVAsset.assetWithURL(NSURL.fileURLWithPath(path))
    val gen = AVAssetImageGenerator(asset).apply { appliesPreferredTrackTransform = true }
    val cg = gen.copyCGImageAtTime(CMTimeMake(0, 1), null, null)
    cg?.let { UIImage.imageWithCGImage(it) }
} catch (_: Throwable) {
    null
}

/** Bounded per-process UIImage cache for timeline image tiles. */
private object IosImageTileCache {
    private const val CAPACITY = 96
    @Volatile private var map: Map<String, UIImage> = emptyMap()
    fun get(key: String): UIImage? = map[key]
    fun put(key: String, value: UIImage) {
        val cur = map
        map = if (cur.size < CAPACITY) cur + (key to value)
        else cur.entries.drop(cur.size - CAPACITY + 1).associate { it.key to it.value } + (key to value)
    }
}

/** Bounded per-process UIImage cache for video thumbnails. */
private object IosVideoThumbnailCache {
    private const val CAPACITY = 64
    @Volatile private var map: Map<String, UIImage> = emptyMap()
    fun get(key: String): UIImage? = map[key]
    fun put(key: String, value: UIImage) {
        val cur = map
        map = if (cur.size < CAPACITY) cur + (key to value)
        else cur.entries.drop(cur.size - CAPACITY + 1).associate { it.key to it.value } + (key to value)
    }
}

private fun formatSec(sec: Double): String {
    val total = sec.toInt()
    val m = total / 60
    val s = total % 60
    return "$m:${if (s < 10) "0$s" else s.toString()}"
}
