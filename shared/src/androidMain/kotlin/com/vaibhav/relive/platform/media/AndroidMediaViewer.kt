package com.vaibhav.relive.platform.media

import android.media.MediaPlayer
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
import kotlinx.coroutines.delay

/**
 * Full-screen audio viewer (ADR-0019 §8). Same visual identity as the
 * timeline tile — bounded centered waveform, generous black negative
 * space — but at viewer scale. Lazy MediaPlayer, released on dispose,
 * participates in [ActivePlayback] so only one media session is active.
 */
@Composable
actual fun RelivedAudioViewer(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val durationMs = rememberMediaDurationMs(ref, mediaStore) ?: 0L
    val envelope = rememberWaveformFor(ref, mediaStore, WaveformProcessor.bucketsFor(durationMs))
    val holder = remember(path) { ViewerAudioHolder() }
    var playing by remember(path) { mutableStateOf(false) }
    var posMs by remember(path) { mutableStateOf(0) }
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
            posMs = holder.currentPositionMs()
            if (!holder.isPlaying()) {
                playing = false
                ActivePlayback.release(stopSelf)
                break
            }
            delay(60)
        }
    }
    val progress = WaveformProcessor.progressFraction(posMs.toLong(), durationMs)
    val label = formatViewerTime(if (playing) posMs.toLong() else durationMs) +
        " / " + formatViewerTime(durationMs)
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
                        if (holder.togglePlay(path)) {
                            val nowPlaying = holder.isPlaying()
                            playing = nowPlaying
                            if (nowPlaying) ActivePlayback.claim(stopSelf)
                            else { posMs = holder.currentPositionMs(); ActivePlayback.release(stopSelf) }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { Text(if (playing) "❚❚" else "▶", color = Color.White) }
            Text(label, color = Color(0xFFEFECE5))
        }
    }
}

/** Lazy audio player for the viewer; same shape as the tile holder. */
private class ViewerAudioHolder {
    private var player: MediaPlayer? = null

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
            release(); false
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

private fun formatViewerTime(ms: Long): String {
    val total = (ms / 1000L).toInt()
    val m = total / 60
    val s = total % 60
    val ss = if (s < 10) "0$s" else s.toString()
    return "$m:$ss"
}
