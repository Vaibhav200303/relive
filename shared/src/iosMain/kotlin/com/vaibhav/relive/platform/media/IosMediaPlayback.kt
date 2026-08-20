package com.vaibhav.relive.platform.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaStorageRef
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.CoreGraphics.CGRect
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
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

private fun formatSec(sec: Double): String {
    val total = sec.toInt()
    val m = total / 60
    val s = total % 60
    return "$m:${if (s < 10) "0$s" else s.toString()}"
}
