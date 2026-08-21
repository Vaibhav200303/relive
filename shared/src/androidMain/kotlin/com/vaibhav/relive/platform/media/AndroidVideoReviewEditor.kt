package com.vaibhav.relive.platform.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Video review with a lightweight in-place editor: thumbnail strip + trim
 * handles at the top, mute + metadata row below the strip, main preview with
 * centered Play/Pause overlay in the middle. Purely UI — trim/mute intents
 * are read via callbacks and applied once by the media processor on Confirm.
 *
 * The composable owns:
 *  - the framework [VideoView] and its [MediaPlayer] handle (for volume + seek)
 *  - asynchronously extracted thumbnails
 *  - probed duration and byte size of the source file
 *
 * State that must survive to Confirm ([VideoReviewEditState]) is hoisted to the
 * caller. Overlay Play/Pause icon state ([VideoReviewPlayback]) is also
 * hoisted, since Retake/Confirm need to reset it.
 */
@Composable
internal fun VideoReviewEditor(
    filePath: String,
    edit: VideoReviewEditState,
    playback: VideoReviewPlayback,
    onEditChange: (VideoReviewEditState) -> Unit,
    onSingleTap: () -> Unit,
    onTapPlay: () -> Unit,
    onTapPause: () -> Unit,
    onOverlayTimeout: () -> Unit,
    onPlaybackReachedTrimEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val editLatest by rememberUpdatedState(edit)
    val onEditChangeLatest by rememberUpdatedState(onEditChange)
    val onReachedEnd by rememberUpdatedState(onPlaybackReachedTrimEnd)

    // Probed source metadata. Duration is required to size the trim range;
    // size is displayed to the user as a truthful pre-processing figure.
    var probedDurationMs by remember(filePath) { mutableStateOf(0L) }
    var probedSizeBytes by remember(filePath) { mutableStateOf(0L) }
    LaunchedEffect(filePath) {
        val probed = withContext(Dispatchers.IO) { probeSource(filePath) }
        probedDurationMs = probed.first
        probedSizeBytes = probed.second
    }
    // Rebase edit state onto the freshly probed duration exactly once per
    // capture. Downstream trim/pos calculations depend on a non-zero duration.
    LaunchedEffect(filePath, probedDurationMs) {
        if (probedDurationMs > 0L && editLatest.durationMs != probedDurationMs) {
            onEditChangeLatest(editLatest.withDuration(probedDurationMs))
        }
    }

    // Asynchronously extract a small, uniformly sampled thumbnail set. The
    // strip renders whatever is present so early frames appear as they arrive.
    var thumbnails by remember(filePath) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    LaunchedEffect(filePath, probedDurationMs) {
        if (probedDurationMs <= 0L) return@LaunchedEffect
        val count = thumbnailCountFor(probedDurationMs)
        val frames = withContext(Dispatchers.IO) {
            extractThumbnails(filePath, probedDurationMs, count)
        }
        thumbnails = frames
    }

    // MediaPlayer handle for volume + seek control. Bound in the prepared
    // callback below. Cleared on dispose so we never send commands to a
    // released player.
    var mediaPlayer by remember(filePath) { mutableStateOf<MediaPlayer?>(null) }
    val videoView = remember(filePath) {
        VideoView(context).apply {
            setVideoURI(Uri.fromFile(File(filePath)))
            setOnPreparedListener { mp ->
                mediaPlayer = mp
                val vol = if (editLatest.isMuted) 0f else 1f
                try { mp.setVolume(vol, vol) } catch (_: Throwable) {}
                mp.setOnCompletionListener { onReachedEnd() }
                // Show the first frame of the trim range behind the Play icon
                // without starting playback.
                try { seekTo(editLatest.trimStartMs.toInt()) } catch (_: Throwable) {}
            }
        }
    }

    // Push mute changes through to the live MediaPlayer whenever isMuted flips.
    LaunchedEffect(filePath, edit.isMuted, mediaPlayer) {
        val vol = if (edit.isMuted) 0f else 1f
        try { mediaPlayer?.setVolume(vol, vol) } catch (_: Throwable) {}
    }

    // Playback loop. When isPlaying flips true, seek to the current position
    // (already snapped into the trim range by pressPlay()) then start, and
    // poll ~10x/sec so we can stop cleanly at trimEnd.
    LaunchedEffect(filePath, playback.isPlaying) {
        if (playback.isPlaying) {
            try { videoView.seekTo(editLatest.playbackPositionMs.toInt()) } catch (_: Throwable) {}
            try { videoView.start() } catch (_: Throwable) {}
            while (playback.isPlaying) {
                val pos = try { videoView.currentPosition.toLong() } catch (_: Throwable) { editLatest.trimStartMs }
                if (pos >= editLatest.trimEndMs) {
                    try { videoView.pause() } catch (_: Throwable) {}
                    try { videoView.seekTo(editLatest.trimStartMs.toInt()) } catch (_: Throwable) {}
                    onReachedEnd()
                    break
                }
                onEditChangeLatest(editLatest.updatePlaybackPosition(pos))
                delay(100)
            }
        } else {
            try { if (videoView.isPlaying) videoView.pause() } catch (_: Throwable) {}
        }
    }

    // When the user moves handles while paused, clamp the visible frame to
    // the new trim range. Only fires while paused so we don't fight the
    // playback loop's own seeks.
    LaunchedEffect(filePath, edit.playbackPositionMs, playback.isPlaying) {
        if (!playback.isPlaying) {
            try { videoView.seekTo(edit.playbackPositionMs.toInt()) } catch (_: Throwable) {}
        }
    }

    // Auto-hide transient Pause overlay after a short timeout (matches the
    // existing playback overlay behavior for consistency).
    LaunchedEffect(playback.overlay, playback.isPlaying) {
        if (playback.overlay == VideoReviewOverlay.Pause && playback.isPlaying) {
            delay(PAUSE_OVERLAY_TIMEOUT_MS)
            onOverlayTimeout()
        }
    }

    DisposableEffect(videoView) {
        onDispose {
            try { if (videoView.isPlaying) videoView.pause() } catch (_: Throwable) {}
            try { videoView.stopPlayback() } catch (_: Throwable) {}
            mediaPlayer = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Editor header: thumbnail strip + trim handles.
        TrimStrip(
            edit = edit,
            thumbnails = thumbnails,
            onTrimStart = { onEditChangeLatest(editLatest.setTrimStart(it)) },
            onTrimEnd = { onEditChangeLatest(editLatest.setTrimEnd(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        // Mute icon + compact duration / size metadata row.
        MetaRow(
            isMuted = edit.isMuted,
            durationMs = if (edit.durationMs > 0L) edit.selectedDurationMs else 0L,
            sizeBytes = probedSizeBytes,
            onToggleMute = { onEditChangeLatest(editLatest.toggleMute()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
        )

        // Video preview area — takes the remaining space in the editor. Tap
        // to reveal Pause overlay while playing; centered Play overlay when
        // paused. Same idiom as the pre-editor surface.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(filePath) { detectTapGestures(onTap = { onSingleTap() }) },
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(factory = { videoView }, modifier = Modifier.fillMaxSize())
            when (playback.overlay) {
                VideoReviewOverlay.Play -> PlayOverlay(onClick = onTapPlay)
                VideoReviewOverlay.Pause -> PauseOverlay(onClick = onTapPause)
                VideoReviewOverlay.Hidden -> Unit
            }
        }
    }
}

// ---- trim strip ----------------------------------------------------------

private val STRIP_HEIGHT: Dp = 52.dp
private val HANDLE_HIT: Dp = 28.dp
private val HANDLE_VISUAL: Dp = 8.dp
private val BORDER: Dp = 2.dp

@Composable
private fun TrimStrip(
    edit: VideoReviewEditState,
    thumbnails: List<ImageBitmap>,
    onTrimStart: (Long) -> Unit,
    onTrimEnd: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.height(STRIP_HEIGHT)) {
        val stripWidthDp = maxWidth
        val density = LocalDensity.current
        val stripWidthPx = with(density) { stripWidthDp.toPx() }
        val duration = edit.durationMs
        // Guard against divide-by-zero before we know the real duration.
        fun msFromPx(px: Float): Long =
            if (duration <= 0L || stripWidthPx <= 0f) 0L
            else ((px / stripWidthPx) * duration).toLong()
        fun pxFromMs(ms: Long): Float =
            if (duration <= 0L) 0f else (ms.toFloat() / duration.toFloat()) * stripWidthPx

        // Thumbnail background — evenly distributed frames beneath the trim
        // box. The strip renders zero-width until any frames are available.
        Row(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
            if (thumbnails.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2A2A2A)))
            } else {
                thumbnails.forEach { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        val startPxState = rememberUpdatedState(pxFromMs(edit.trimStartMs))
        val endPxState = rememberUpdatedState(pxFromMs(edit.trimEndMs))

        // Dim the two rejected regions outside the current selection.
        val dim = Color(0xB3000000)
        val leftPxDp = with(density) { startPxState.value.toDp() }
        val rightPxDp = with(density) { endPxState.value.toDp() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(leftPxDp)
                .background(dim),
        )
        Box(
            modifier = Modifier
                .offset(x = rightPxDp)
                .fillMaxHeight()
                .width((stripWidthDp - rightPxDp).coerceAtLeast(0.dp))
                .background(dim),
        )

        // Selection border.
        val selectionWidthDp = (rightPxDp - leftPxDp).coerceAtLeast(0.dp)
        Canvas(
            modifier = Modifier
                .offset(x = leftPxDp)
                .width(selectionWidthDp)
                .fillMaxHeight(),
        ) {
            val stroke = with(density) { BORDER.toPx() }
            drawRect(
                color = Color(0xFFEDE9C8),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height),
                style = Stroke(width = stroke),
            )
        }

        // Left handle — draggable vertical bar. Fat touch target (HANDLE_HIT)
        // centered on the visible handle so the user can grab either side.
        val leftHitStart = leftPxDp - HANDLE_HIT / 2
        Box(
            modifier = Modifier
                .offset(x = leftHitStart)
                .width(HANDLE_HIT)
                .fillMaxHeight()
                .pointerInput(duration, stripWidthPx) {
                    if (duration <= 0L) return@pointerInput
                    detectDragGestures { change, delta ->
                        change.consume()
                        val nextPx = (startPxState.value + delta.x)
                            .coerceIn(0f, endPxState.value)
                        onTrimStart(msFromPx(nextPx))
                    }
                }
                .semantics { contentDescription = "Trim start handle" },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(HANDLE_VISUAL)
                    .fillMaxHeight()
                    .background(Color(0xFFEDE9C8), RoundedCornerShape(4.dp)),
            )
        }

        // Right handle.
        val rightHitStart = rightPxDp - HANDLE_HIT / 2
        Box(
            modifier = Modifier
                .offset(x = rightHitStart)
                .width(HANDLE_HIT)
                .fillMaxHeight()
                .pointerInput(duration, stripWidthPx) {
                    if (duration <= 0L) return@pointerInput
                    detectDragGestures { change, delta ->
                        change.consume()
                        val nextPx = (endPxState.value + delta.x)
                            .coerceIn(startPxState.value, stripWidthPx)
                        onTrimEnd(msFromPx(nextPx))
                    }
                }
                .semantics { contentDescription = "Trim end handle" },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(HANDLE_VISUAL)
                    .fillMaxHeight()
                    .background(Color(0xFFEDE9C8), RoundedCornerShape(4.dp)),
            )
        }
    }
}

// ---- meta row ------------------------------------------------------------

@Composable
private fun MetaRow(
    isMuted: Boolean,
    durationMs: Long,
    sizeBytes: Long,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SpeakerButton(isMuted = isMuted, onClick = onToggleMute)
        val label = buildString {
            append(formatDurationClock(durationMs))
            if (sizeBytes > 0L) {
                append("  •  ")
                append(formatFileSize(sizeBytes))
            }
        }
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun SpeakerButton(isMuted: Boolean, onClick: () -> Unit) {
    val desc = if (isMuted) "Unmute video" else "Mute video"
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x80000000))
            .clickable(onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        SpeakerGlyph(size = 18.dp, color = Color.White, muted = isMuted)
    }
}

@Composable
private fun SpeakerGlyph(size: Dp, color: Color, muted: Boolean) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = px * 0.08f
        // Speaker cone: small square + trapezoid horn.
        val body = Path().apply {
            moveTo(px * 0.15f, px * 0.38f)
            lineTo(px * 0.32f, px * 0.38f)
            lineTo(px * 0.48f, px * 0.22f)
            lineTo(px * 0.48f, px * 0.78f)
            lineTo(px * 0.32f, px * 0.62f)
            lineTo(px * 0.15f, px * 0.62f)
            close()
        }
        drawPath(body, color = color)
        if (muted) {
            // Material `volume_off`-style single diagonal slash across the
            // whole glyph: top-right to bottom-left, passing through the
            // icon center. Rounded caps match the horn silhouette weight.
            drawLine(
                color = color,
                start = Offset(px * 0.85f, px * 0.15f),
                end = Offset(px * 0.15f, px * 0.85f),
                strokeWidth = sw * 1.6f,
                cap = StrokeCap.Round,
            )
        } else {
            // Two sound arcs to the right of the horn.
            drawArcStroke(px, color, sw, radiusFrac = 0.14f)
            drawArcStroke(px, color, sw, radiusFrac = 0.22f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArcStroke(
    px: Float,
    color: Color,
    strokeWidth: Float,
    radiusFrac: Float,
) {
    val cx = px * 0.56f
    val cy = px * 0.50f
    val r = px * radiusFrac
    drawArc(
        color = color,
        startAngle = -55f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = strokeWidth),
    )
}

// ---- play / pause overlays (moved from AndroidCameraCapture) -------------

@Composable
private fun PlayOverlay(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(42.dp))
            .background(Color(0x99000000))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Play video" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val px = size.width
            val path = Path().apply {
                moveTo(px * 0.30f, px * 0.20f)
                lineTo(px * 0.80f, px * 0.50f)
                lineTo(px * 0.30f, px * 0.80f)
                close()
            }
            drawPath(path, color = Color.White)
        }
    }
}

@Composable
private fun PauseOverlay(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(42.dp))
            .background(Color(0x99000000))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Pause video" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            val px = size.width
            val barW = px * 0.18f
            val gap = px * 0.14f
            val leftX = (px - (barW * 2 + gap)) / 2
            drawRect(
                color = Color.White,
                topLeft = Offset(leftX, px * 0.20f),
                size = Size(barW, px * 0.60f),
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(leftX + barW + gap, px * 0.20f),
                size = Size(barW, px * 0.60f),
            )
        }
    }
}

private const val PAUSE_OVERLAY_TIMEOUT_MS: Long = 1800L

// ---- native probing ------------------------------------------------------

private fun probeSource(filePath: String): Pair<Long, Long> {
    val file = File(filePath)
    val size = try { if (file.exists()) file.length() else 0L } catch (_: Throwable) { 0L }
    val duration = try {
        MediaMetadataRetriever().use { r ->
            r.setDataSource(filePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        }
    } catch (_: Throwable) { 0L }
    return duration to size
}

/**
 * Decide how many thumbnails to sample. Bounded so we never allocate a wall
 * of tiny bitmaps for very long clips — the strip is a compact overview, not
 * a scrubbing timeline.
 */
private fun thumbnailCountFor(durationMs: Long): Int {
    if (durationMs <= 1_500L) return 4
    if (durationMs <= 5_000L) return 6
    if (durationMs <= 15_000L) return 8
    return 10
}

private fun extractThumbnails(filePath: String, durationMs: Long, count: Int): List<ImageBitmap> {
    if (count <= 0 || durationMs <= 0L) return emptyList()
    val out = ArrayList<ImageBitmap>(count)
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(filePath)
        for (i in 0 until count) {
            // Sample at the midpoint of each equal slice so first/last frames
            // are not edge frames that might be blank.
            val t = ((i.toFloat() + 0.5f) / count) * durationMs
            val bmp: Bitmap? = try {
                retriever.getFrameAtTime(
                    (t * 1000).toLong(),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                )
            } catch (_: Throwable) { null }
            if (bmp != null) {
                out += bmp.asImageBitmap()
            }
        }
    } catch (_: Throwable) {
        // best-effort; return whatever we already collected
    } finally {
        try { retriever.release() } catch (_: Throwable) {}
    }
    return out
}

private inline fun <R> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> R): R {
    try { return block(this) } finally { release() }
}
