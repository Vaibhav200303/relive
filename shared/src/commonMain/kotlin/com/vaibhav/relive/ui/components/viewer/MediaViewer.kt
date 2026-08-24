package com.vaibhav.relive.ui.components.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.ActivePlayback
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedAudioViewer
import com.vaibhav.relive.platform.media.RelivedImage
import com.vaibhav.relive.platform.media.RelivedVideo
import com.vaibhav.relive.platform.media.rememberImageAspectRatioFor
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.presentation.viewer.MediaViewerState

/**
 * Full-screen media viewer (ADR-0019 §5-8). One unified surface for image,
 * video, and audio pages. HorizontalPager for swipe navigation between all
 * attachments of a Moment; image pages support pinch/pan/double-tap zoom
 * and disable pager scroll while zoomed. Only the currently visible page
 * allocates playback resources — off-screen pages render nothing. System
 * Back closes the viewer (or resets zoom first, if a zoomed image is
 * showing).
 */
@Composable
fun MediaViewer(
    state: MediaViewerState,
    mediaStore: MediaStore,
    onIndexChange: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = state.initialIndex,
        pageCount = { state.attachments.size },
    )
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var resetZoomRequest by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            ActivePlayback.stopActive()
            onIndexChange(page)
            currentZoom = 1f
            resetZoomRequest += 1f
        }
    }

    ReliveBackHandler(enabled = true, onBack = {
        if (ZoomableImageMath.isZoomed(currentZoom)) {
            resetZoomRequest += 1f; currentZoom = 1f
        } else onClose()
    })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !ZoomableImageMath.isZoomed(currentZoom),
            modifier = Modifier.fillMaxSize(),
            key = { i -> state.attachments[i].storageRef.value + ":" + i },
        ) { page ->
            val att = state.attachments[page]
            val isActive = page == pagerState.currentPage
            MediaViewerPage(
                attachment = att,
                index = page,
                total = state.attachments.size,
                mediaStore = mediaStore,
                isActive = isActive,
                onZoomChange = { z -> if (isActive) currentZoom = z },
                resetZoomKey = if (isActive) resetZoomRequest else 0f,
            )
        }

        TopBar(
            index = pagerState.currentPage,
            total = state.attachments.size,
            onClose = onClose,
        )
    }
}

@Composable
private fun MediaViewerPage(
    attachment: MomentAttachmentPresentation,
    index: Int,
    total: Int,
    mediaStore: MediaStore,
    isActive: Boolean,
    onZoomChange: (Float) -> Unit,
    resetZoomKey: Float,
) {
    val label = when (attachment.type) {
        MediaType.Image -> "Image ${index + 1} of $total"
        MediaType.Video -> "Video ${index + 1} of $total"
        MediaType.Audio -> "Audio ${index + 1} of $total"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        when (attachment.type) {
            MediaType.Image -> {
                val aspect = rememberImageAspectRatioFor(attachment.storageRef, mediaStore)
                ZoomableImage(
                    imageAspect = aspect,
                    content = { modifier ->
                        RelivedImage(attachment.storageRef, mediaStore, modifier)
                    },
                    onZoomChange = onZoomChange,
                    resetKey = resetZoomKey,
                )
            }
            MediaType.Video -> if (isActive) {
                RelivedVideo(attachment.storageRef, mediaStore, Modifier.fillMaxSize())
            }
            MediaType.Audio -> if (isActive) {
                RelivedAudioViewer(attachment.storageRef, mediaStore, Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * Pinch/pan/double-tap zoom for a single image. Fits the image into the
 * maximum available viewport preserving [imageAspect], then applies scale
 * and translation via a single graphicsLayer. Pager scroll is gated by the
 * caller when scale > 1 so panning does not trigger page swipe until the
 * image resets to 1x (ADR-0019 §12). While [imageAspect] is null (aspect
 * still probing), we render the raw image with Fit against the viewport so
 * the first paint still uses the maximum area — the aspect fallback here
 * only affects zoom clamping.
 */
@Composable
private fun ZoomableImage(
    imageAspect: Float?,
    content: @Composable (Modifier) -> Unit,
    onZoomChange: (Float) -> Unit,
    resetKey: Float,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(resetKey) {
        scale = 1f; offsetX = 0f; offsetY = 0f; onZoomChange(1f)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val viewportW = with(density) { maxWidth.toPx() }
        val viewportH = with(density) { maxHeight.toPx() }
        val aspect = imageAspect ?: (if (viewportH > 0f) viewportW / viewportH else 1f)
        val (fittedW, fittedH) = ZoomableImageMath.fittedSize(viewportW, viewportH, aspect)
        val fittedWDp = with(density) { fittedW.toDp() }
        val fittedHDp = with(density) { fittedH.toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(fittedW, fittedH) {
                    detectTapGestures(onDoubleTap = {
                        val next = ZoomableImageMath.doubleTapTarget(scale)
                        scale = next
                        val (nx, ny) = ZoomableImageMath.clampTranslation(
                            next, viewportW, viewportH, fittedW, fittedH, offsetX, offsetY,
                        )
                        offsetX = nx; offsetY = ny
                        onZoomChange(next)
                    })
                }
                .pointerInput(fittedW, fittedH) {
                    // Only consume drag events as pan when a real transform is
                    // active: either the image is already zoomed, or a second
                    // finger has landed (pinch). Single-finger drags at fitted
                    // scale must fall through to the HorizontalPager so swipe
                    // pages between attachments (ADR-0019 §5).
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var transforming = ZoomableImageMath.isZoomed(scale)
                        while (true) {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            if (canceled) break
                            val activePointers = event.changes.count { it.pressed }
                            if (activePointers >= 2) transforming = true
                            if (transforming) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                if (zoom != 1f || pan != androidx.compose.ui.geometry.Offset.Zero) {
                                    val nextScale = ZoomableImageMath.clampScale(scale * zoom)
                                    scale = nextScale
                                    val (nx, ny) = ZoomableImageMath.clampTranslation(
                                        nextScale, viewportW, viewportH, fittedW, fittedH,
                                        offsetX + pan.x, offsetY + pan.y,
                                    )
                                    offsetX = nx; offsetY = ny
                                    onZoomChange(nextScale)
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val childModifier = if (fittedW > 0f && fittedH > 0f) {
                Modifier
                    .size(width = fittedWDp, height = fittedHDp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    )
            } else {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    )
            }
            content(childModifier)
        }
    }
}

@Composable
private fun TopBar(index: Int, total: Int, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Close media viewer" },
        ) { Text("✕", color = Color.White) }
        if (total > 1) {
            Text("${index + 1} / $total", color = Color(0xFFEFECE5))
        }
        Box(modifier = Modifier.size(48.dp))
    }
}
