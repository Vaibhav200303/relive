package com.vaibhav.relive.ui.components.composer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.CameraCaptureSurface
import com.vaibhav.relive.platform.media.MediaPickerHandle
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RawMedia
import com.vaibhav.relive.presentation.composer.ComposerOverlay
import com.vaibhav.relive.presentation.composer.PendingMediaAction
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.components.ReliveBottomSheet

/**
 * Presents the composer's active overlay — camera surface or library
 * choice sheet — and delivers results back to the view-model. Consumed as a
 * sibling of the timeline `LazyColumn` in the timeline screen.
 */
@Composable
internal fun ComposerOverlayHost(
    overlay: ComposerOverlay,
    mediaStore: MediaStore,
    onCaptured: (RawMedia) -> Unit,
    onDismiss: () -> Unit,
    onPick: (MediaType) -> Unit,
    onOpenLibraryFromCamera: () -> Unit,
) {
    when (overlay) {
        ComposerOverlay.None -> Unit
        ComposerOverlay.Camera -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                CameraCaptureSurface(
                    mediaStore = mediaStore,
                    onCaptured = { raw ->
                        onCaptured(raw)
                        onDismiss()
                    },
                    onCancel = onDismiss,
                    // In-camera Gallery hands off to the composer's Library
                    // sheet. openLibraryChoice() replaces the Camera overlay
                    // atomically so the user never sees a bare timeline flash.
                    onOpenGallery = onOpenLibraryFromCamera,
                )
            }
        }
        ComposerOverlay.LibraryChoice -> Unit
    }
    LibraryChoiceSheet(
        visible = overlay == ComposerOverlay.LibraryChoice,
        onPickImage = { onPick(MediaType.Image) },
        onPickVideo = { onPick(MediaType.Video) },
        onPickAudio = { onPick(MediaType.Audio) },
        onDismiss = onDismiss,
    )
}

/**
 * Drives pending picker actions: whenever the VM sets a [PendingMediaAction],
 * the handle is invoked once and the result forwarded, then the action is
 * cleared. Empty results (user cancellation) simply clear.
 */
@Composable
internal fun MediaPickerDriver(
    pending: PendingMediaAction?,
    handle: MediaPickerHandle,
    onResult: (List<RawMedia>) -> Unit,
    onClear: () -> Unit,
) {
    LaunchedEffect(pending) {
        val action = pending ?: return@LaunchedEffect
        val result = when (action) {
            PendingMediaAction.PickImage -> handle.pickImage()
            PendingMediaAction.PickVideo -> handle.pickVideo()
            PendingMediaAction.PickAudio -> handle.pickAudio()
        }
        onResult(result)
        onClear()
    }
}

@Composable
private fun LibraryChoiceSheet(
    visible: Boolean,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onPickAudio: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    ReliveBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = dims.spacing.xl)
                .padding(bottom = dims.spacing.xl)
                .semantics { contentDescription = "Choose media source" },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.spacing.xxl),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(dims.spacing.huge)
                        .height(dims.spacing.xs)
                        .background(colors.textMuted.copy(alpha = 0.55f), ReliveTheme.shapes.pill),
                )
            }
            Text("Choose from library", style = type.title, color = colors.textPrimary)
            Spacer(Modifier.height(dims.spacing.xs))
            Text(
                "Pick the type of media you want to add",
                style = type.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(dims.spacing.lg))
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.md)) {
                LibraryOption(
                    label = "Photo",
                    supportingText = "Choose one or more photos",
                    kind = LibraryOptionKind.Photo,
                    onClick = onPickImage,
                )
                LibraryOption(
                    label = "Video",
                    supportingText = "Choose one or more videos",
                    kind = LibraryOptionKind.Video,
                    onClick = onPickVideo,
                )
                LibraryOption(
                    label = "Audio",
                    supportingText = "Choose one or more audio files",
                    kind = LibraryOptionKind.Audio,
                    onClick = onPickAudio,
                )
            }
        }
    }
}

@Composable
private fun LibraryOption(
    label: String,
    supportingText: String,
    kind: LibraryOptionKind,
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    val tone = when (kind) {
        LibraryOptionKind.Photo -> lerp(colors.spark, colors.surfaceOverlay, 0.42f)
        LibraryOptionKind.Video -> lerp(colors.accent, colors.surfaceOverlay, 0.68f)
        LibraryOptionKind.Audio -> lerp(colors.accentMuted, colors.surfaceOverlay, 0.72f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .shadow(
                6.dp,
                RoundedCornerShape(dims.radii.largeIncreased),
                ambientColor = colors.shadow.copy(alpha = 0.18f),
            )
            .clickable(
                role = Role.Button,
                onClickLabel = "Choose $label",
                onClick = {
                    haptics.perform(ReliveHapticCue.Action)
                    onClick()
                },
            ),
        shape = RoundedCornerShape(dims.radii.largeIncreased),
        color = lerp(colors.surfaceCard, tone, 0.18f),
        contentColor = colors.textPrimary,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LibraryOptionWatermark(
                kind = kind,
                color = tone,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 42.dp)
                    .size(width = 84.dp, height = 54.dp)
                    .alpha(0.38f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.spacing.lg, vertical = dims.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(dims.radii.medium),
                    color = tone,
                    contentColor = colors.textPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = kind.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colors.textPrimary,
                        )
                    }
                }
                Spacer(Modifier.width(dims.spacing.lg))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        style = type.title.copy(fontSize = type.body.fontSize),
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(supportingText, style = type.caption, color = colors.textSecondary)
                }
                Spacer(Modifier.width(dims.spacing.sm))
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = ReliveTheme.shapes.pill,
                    color = colors.textPrimary.copy(alpha = 0.05f),
                ) {
                    Icon(
                        imageVector = ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.padding(dims.spacing.sm),
                        tint = colors.textPrimary,
                    )
                }
            }
        }
    }
}

private enum class LibraryOptionKind(val icon: ImageVector) {
    Photo(PhotoLibraryIcon),
    Video(VideoLibraryIcon),
    Audio(AudioLibraryIcon),
}

@Composable
private fun LibraryOptionWatermark(kind: LibraryOptionKind, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (kind) {
            LibraryOptionKind.Photo -> {
                rotate(-15f, pivot = center) {
                    drawRoundRect(
                        color,
                        topLeft = Offset(20f, 12f),
                        size = Size(size.width * .56f, size.height * .72f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
                        style = Stroke(6f),
                    )
                    drawRoundRect(
                        color,
                        topLeft = Offset(size.width * .38f, 4f),
                        size = Size(size.width * .50f, size.height * .72f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
                        style = Stroke(6f),
                    )
                    drawCircle(color, radius = 7f, center = Offset(size.width * .66f, size.height * .29f))
                    val mountains = Path().apply {
                        moveTo(size.width * .43f, size.height * .61f)
                        lineTo(size.width * .57f, size.height * .43f)
                        lineTo(size.width * .67f, size.height * .54f)
                        lineTo(size.width * .76f, size.height * .46f)
                        lineTo(size.width * .86f, size.height * .63f)
                    }
                    drawPath(mountains, color, style = Stroke(6f, cap = StrokeCap.Round))
                }
            }

            LibraryOptionKind.Video -> {
                rotate(-12f, pivot = center) {
                    drawRoundRect(
                        color,
                        topLeft = Offset(14f, 12f),
                        size = Size(size.width * .68f, size.height * .70f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                        style = Stroke(6f),
                    )
                    drawCircle(color, radius = 12f, center = Offset(size.width * .46f, size.height * .47f))
                    val play = Path().apply {
                        moveTo(size.width * .43f, size.height * .38f)
                        lineTo(size.width * .55f, size.height * .47f)
                        lineTo(size.width * .43f, size.height * .56f)
                        close()
                    }
                    drawPath(play, color = color.copy(alpha = 0.75f))
                }
            }

            LibraryOptionKind.Audio -> {
                val heights = listOf(.22f, .48f, .72f, .92f, .62f, .38f, .76f, .52f, .30f)
                heights.forEachIndexed { index, fraction ->
                    val x = size.width * (.10f + index * .095f)
                    drawLine(
                        color,
                        Offset(x, center.y - size.height * fraction / 2),
                        Offset(x, center.y + size.height * fraction / 2),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

private val ChevronRight = ImageVector.Builder("ChevronRight", 24.dp, 24.dp, 24f, 24f).apply {
    path(
        fill = null,
        stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
    ) {
        moveTo(9f, 6f)
        lineTo(15f, 12f)
        lineTo(9f, 18f)
    }
}.build()

private val PhotoLibraryIcon = ImageVector.Builder("PhotoLibrary", 24.dp, 24.dp, 24f, 24f).apply {
    path(
        fill = null,
        stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
    ) {
        moveTo(4f, 3.5f)
        lineTo(20f, 3.5f)
        lineTo(21.5f, 5f)
        lineTo(21.5f, 19f)
        lineTo(20f, 20.5f)
        lineTo(4f, 20.5f)
        lineTo(2.5f, 19f)
        lineTo(2.5f, 5f)
        close()
        moveTo(5f, 17f)
        lineTo(9.2f, 11.5f)
        lineTo(12.3f, 15f)
        lineTo(15.1f, 12f)
        lineTo(19f, 17f)
    }
    path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
        moveTo(9f, 7.5f)
        curveTo(9f, 9.5f, 6f, 9.5f, 6f, 7.5f)
        curveTo(6f, 5.5f, 9f, 5.5f, 9f, 7.5f)
        close()
    }
}.build()

private val VideoLibraryIcon = ImageVector.Builder("VideoLibrary", 24.dp, 24.dp, 24f, 24f).apply {
    path(
        fill = null,
        stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
    ) {
        moveTo(4f, 5f)
        lineTo(15f, 5f)
        lineTo(17f, 7f)
        lineTo(17f, 17f)
        lineTo(15f, 19f)
        lineTo(4f, 19f)
        lineTo(2f, 17f)
        lineTo(2f, 7f)
        close()
        moveTo(17f, 10f)
        lineTo(22f, 7.5f)
        lineTo(22f, 16.5f)
        lineTo(17f, 14f)
    }
}.build()

private val AudioLibraryIcon = ImageVector.Builder("AudioLibrary", 24.dp, 24.dp, 24f, 24f).apply {
    path(
        fill = null,
        stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(3f, 9f)
        lineTo(3f, 15f)
        moveTo(7.5f, 5f)
        lineTo(7.5f, 19f)
        moveTo(12f, 2.5f)
        lineTo(12f, 21.5f)
        moveTo(16.5f, 6f)
        lineTo(16.5f, 18f)
        moveTo(21f, 9.5f)
        lineTo(21f, 14.5f)
    }
}.build()

