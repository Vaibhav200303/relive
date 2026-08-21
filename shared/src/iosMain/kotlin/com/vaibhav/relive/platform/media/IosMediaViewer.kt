package com.vaibhav.relive.platform.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.ui.components.timeline.WaveformView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RelivedAudioViewer(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val durationMs = rememberMediaDurationMs(ref, mediaStore) ?: 0L
    val envelope = rememberWaveformFor(ref, mediaStore, WaveformProcessor.bucketsFor(durationMs))
    val holder = remember(path) { IosViewerAudioHolder() }
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
    val label = formatViewerSec(if (playing) posMs / 1000.0 else durationMs / 1000.0) +
        " / " + formatViewerSec(durationMs / 1000.0)
    Box(
        modifier = modifier
            .background(Color.Black)
            .semantics { contentDescription = if (playing) "Pause audio" else "Play audio" },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WaveformView(
                envelope = envelope,
                progress = progress,
                preferredSegments = 21,
                segmentWidthPx = 6f,
                segmentGapPx = 8f,
                minSegmentHeightPx = 6f,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(140.dp)
                    .padding(vertical = 12.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6F4E37))
                    .clickable {
                        holder.togglePlay(path)
                        val nowPlaying = holder.isPlaying()
                        playing = nowPlaying
                        if (nowPlaying) ActivePlayback.claim(stopSelf)
                        else { posMs = (holder.currentTimeSec() * 1000.0).toLong(); ActivePlayback.release(stopSelf) }
                    },
                contentAlignment = Alignment.Center,
            ) { Text(if (playing) "❚❚" else "▶", color = Color.White) }
            Text(label, color = Color(0xFFEFECE5))
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosViewerAudioHolder {
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
    fun pauseIfPlaying() { val p = player ?: return; if (p.playing) p.pause() }
    fun release() { player?.stop(); player = null }
}

private fun formatViewerSec(sec: Double): String {
    val total = sec.toInt()
    val m = total / 60
    val s = total % 60
    return "$m:${if (s < 10) "0$s" else s.toString()}"
}
