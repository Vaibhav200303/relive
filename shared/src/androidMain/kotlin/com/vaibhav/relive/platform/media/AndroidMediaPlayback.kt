package com.vaibhav.relive.platform.media

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vaibhav.relive.domain.model.MediaStorageRef
import kotlinx.coroutines.delay

@Composable
actual fun RelivedImage(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    val bitmap = remember(path) {
        try { BitmapFactory.decodeFile(path) } catch (_: Throwable) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Photo",
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(modifier = modifier.background(Color(0x22000000)))
    }
}

@Composable
actual fun RelivedVideo(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier) {
    val path = mediaStore.resolveAbsolutePath(ref)
    var player: MediaPlayer? by remember(path) { mutableStateOf(null) }
    var playing by remember(path) { mutableStateOf(false) }

    Box(modifier = modifier.background(Color.Black)) {
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
        if (!playing) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
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

private fun formatTime(ms: Int): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    val ss = if (s < 10) "0$s" else s.toString()
    return "$m:$ss"
}
