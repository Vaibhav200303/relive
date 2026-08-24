package com.vaibhav.relive.ui.components.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.NaturalSizePx
import com.vaibhav.relive.platform.media.RelivedAudio
import com.vaibhav.relive.platform.media.RelivedImage
import com.vaibhav.relive.platform.media.RelivedVideo
import com.vaibhav.relive.platform.media.VideoSourceThumbnail
import com.vaibhav.relive.platform.media.rememberImageNaturalSizeFor
import com.vaibhav.relive.platform.media.rememberVideoNaturalSizeFor
import com.vaibhav.relive.platform.media.rememberVideoNaturalSizeForPath
import com.vaibhav.relive.presentation.composer.DraftAttachment
import com.vaibhav.relive.presentation.composer.DraftMediaStatus
import com.vaibhav.relive.ui.media.computeAdaptiveMediaPreviewSize
import com.vaibhav.relive.ui.media.fallbackAdaptivePreviewSize
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Vertical stack of composer draft attachments. Each preview shrink-wraps
 * around the media's own aspect ratio, bounded by the available column
 * width and [ReliveMediaDimensions.composerPreviewMaxHeight]. A hairline
 * outline hugs the resulting media box; the processing overlay uses
 * matchParentSize so its spinner remains centered regardless of shape.
 * Timeline / gallery / viewer sizing is intentionally not shared with this
 * layout (composer-specific presentation).
 */
@Composable
internal fun DraftAttachmentColumn(
    attachments: List<DraftAttachment>,
    mediaStore: MediaStore,
    onRemove: (String) -> Unit,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
    ) {
        attachments.forEach { att ->
            DraftAttachmentTile(
                attachment = att,
                mediaStore = mediaStore,
                onRemove = { onRemove(att.draftId) },
                onRetry = { onRetry(att.draftId) },
            )
        }
    }
}

@Composable
private fun DraftAttachmentTile(
    attachment: DraftAttachment,
    mediaStore: MediaStore,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    // Audio keeps the approved waveform surface at full column width — the
    // waveform's own container carries its identity, so no adaptive sizing
    // or outer boundary is applied here (spec §10).
    if (attachment.type == MediaType.Audio) {
        AudioAttachmentTile(attachment, mediaStore, onRemove, onRetry)
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        AdaptiveMediaTile(
            attachment = attachment,
            mediaStore = mediaStore,
            availableWidth = availableWidth,
            onRemove = onRemove,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun AdaptiveMediaTile(
    attachment: DraftAttachment,
    mediaStore: MediaStore,
    availableWidth: Dp,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val density = LocalDensity.current

    val status = attachment.status
    val ready = status as? DraftMediaStatus.Ready
    val readyRef = ready?.storageRef

    // Images: prefer dimensions the processor already stamped; fall back
    // to a platform probe only if missing.
    // Videos: always probe — the processor records raw track WxH without
    // applying rotation metadata, so a portrait clip would arrive here as
    // a landscape ratio. rememberVideoNaturalSizeFor returns rotation-
    // corrected dimensions matching what the player will render, so the
    // outer preview Box matches the actual frame aspect.
    val probedImage: NaturalSizePx? =
        if (readyRef != null && attachment.type == MediaType.Image &&
            (ready.widthPx == null || ready.heightPx == null)
        ) rememberImageNaturalSizeFor(readyRef, mediaStore) else null
    val probedVideo: NaturalSizePx? =
        if (readyRef != null && attachment.type == MediaType.Video)
            rememberVideoNaturalSizeFor(readyRef, mediaStore) else null
    // Pre-Ready video: probe the source file directly so the Processing
    // placeholder shrink-wraps to the SAME bounds the ready preview will use.
    // The lightweight metadata probe reads WxH + rotation only — no full
    // decode — and runs off the main thread. Aspect ratio is preserved by
    // the processor, so the placeholder's future layout matches the ready
    // video's.
    val probedSourceVideo: NaturalSizePx? =
        if (readyRef == null && attachment.type == MediaType.Video)
            rememberVideoNaturalSizeForPath(attachment.sourcePath) else null

    val naturalSize: NaturalSizePx? = when {
        attachment.type == MediaType.Video && probedVideo != null -> probedVideo
        attachment.type == MediaType.Video && probedSourceVideo != null -> probedSourceVideo
        ready?.widthPx != null && ready.heightPx != null ->
            NaturalSizePx(ready.widthPx, ready.heightPx)
        probedImage != null -> probedImage
        probedVideo != null -> probedVideo
        else -> null
    }

    val maxHeight = dims.media.composerPreviewMaxHeight
    val displaySize: DpSize = computeAdaptiveSize(
        natural = naturalSize,
        maxWidth = availableWidth,
        maxHeight = maxHeight,
        density = density,
        fallbackAspect = dims.media.composerPlaceholderFallbackAspect,
        fallbackHeight = dims.media.composerPlaceholderFallbackHeight,
        hasReadyRef = readyRef != null,
    )

    Box(
        // .size (not .fillMaxWidth) so the boundary hugs the media exactly.
        modifier = Modifier
            .size(displaySize.width, displaySize.height)
            .clip(RoundedCornerShape(dims.radii.md))
            .border(
                width = dims.stroke.hairline,
                color = colors.borderMuted,
                shape = RoundedCornerShape(dims.radii.md),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (readyRef != null) {
            when (attachment.type) {
                MediaType.Image -> RelivedImage(
                    ref = readyRef,
                    mediaStore = mediaStore,
                    modifier = Modifier.size(displaySize.width, displaySize.height),
                )
                MediaType.Video -> RelivedVideo(
                    ref = readyRef,
                    mediaStore = mediaStore,
                    modifier = Modifier.size(displaySize.width, displaySize.height),
                    // Instant poster: source thumbnail was already extracted
                    // and cached during the Processing state; passing the
                    // source path lets the ready surface reuse that cached
                    // frame with no black flash while the ready file's own
                    // thumbnail extracts. Kept for pause/idle states too.
                    posterFallbackPath = attachment.sourcePath,
                )
                MediaType.Audio -> Unit
            }
        } else {
            // Restrained placeholder at the same computed bounds. Video
            // gets an immediate frame from the ORIGINAL picked file so the
            // user always sees what they selected while transcoding runs;
            // the ProcessingOverlay's translucent black darkens it and the
            // spinner stays centered. Image/audio keep the plain black
            // placeholder (unchanged behavior).
            Box(
                modifier = Modifier
                    .size(displaySize.width, displaySize.height)
                    .background(Color.Black),
            ) {
                if (attachment.type == MediaType.Video) {
                    VideoSourceThumbnail(
                        sourcePath = attachment.sourcePath,
                        modifier = Modifier.size(displaySize.width, displaySize.height),
                    )
                }
            }
        }

        when (attachment.status) {
            DraftMediaStatus.Pending, DraftMediaStatus.Processing -> ProcessingOverlay()
            is DraftMediaStatus.Failed -> FailedOverlay(onRetry = onRetry, onRemove = onRemove)
            is DraftMediaStatus.Ready -> Unit
        }
        if (attachment.status !is DraftMediaStatus.Failed) {
            RemoveAttachmentButton(
                onRemove = onRemove,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/**
 * Adaptive sizing algorithm (composer-only).
 *
 *   Rule A: displayWidth <= maxWidth, displayHeight <= maxHeight.
 *   Rule B: aspect ratio preserved — no stretch, no crop.
 *   Rule C: if the media's natural dp-equivalent size already fits inside
 *           both bounds, shrink-wrap around it (never enlarge to fill).
 *   Rule D: maxWidth/maxHeight are ceilings, not targets. This function
 *           returns explicit computed dimensions used with `Modifier.size`.
 *
 * When natural dimensions aren't yet known, returns a restrained
 * fallback box using the theme placeholder aspect / height so the
 * spinner has a stable centered target until real bounds arrive.
 */
private fun computeAdaptiveSize(
    natural: NaturalSizePx?,
    maxWidth: Dp,
    maxHeight: Dp,
    density: Density,
    fallbackAspect: Float,
    fallbackHeight: Dp,
    @Suppress("UNUSED_PARAMETER") hasReadyRef: Boolean,
): DpSize {
    if (natural == null) {
        return fallbackAdaptivePreviewSize(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            fallbackAspect = fallbackAspect,
            fallbackHeight = fallbackHeight,
        )
    }
    return computeAdaptiveMediaPreviewSize(natural, maxWidth, maxHeight, density)
}

@Composable
private fun AudioAttachmentTile(
    attachment: DraftAttachment,
    mediaStore: MediaStore,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val status = attachment.status
        if (status is DraftMediaStatus.Ready) {
            RelivedAudio(
                ref = status.storageRef,
                mediaStore = mediaStore,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(dims.radii.sm))
                    .background(Color.Black),
            )
        }
        when (attachment.status) {
            DraftMediaStatus.Pending, DraftMediaStatus.Processing -> ProcessingOverlay()
            is DraftMediaStatus.Failed -> FailedOverlay(onRetry = onRetry, onRemove = onRemove)
            is DraftMediaStatus.Ready -> Unit
        }
        if (attachment.status !is DraftMediaStatus.Failed) {
            RemoveAttachmentButton(
                onRemove = onRemove,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun BoxScope.ProcessingOverlay() {
    val colors = ReliveTheme.colors
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color(0x66000000))
            .semantics { contentDescription = "Processing media" },
    ) {
        CircularProgressIndicator(
            color = colors.textOnAccent,
            strokeWidth = 2.dp,
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.Center),
        )
    }
}

@Composable
private fun BoxScope.FailedOverlay(onRetry: () -> Unit, onRemove: () -> Unit) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color(0x99000000))
            .semantics { contentDescription = "Media failed to process" },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            Text(
                text = "Couldn't process",
                style = type.subtitle,
                color = colors.textOnAccent,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(dims.spacing.md)) {
                FailedActionButton(label = "Retry", onClick = onRetry)
                FailedActionButton(label = "Remove", onClick = onRemove)
            }
        }
    }
}

@Composable
private fun FailedActionButton(label: String, onClick: () -> Unit) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    TextButton(
        onClick = {
            haptics.perform(ReliveHapticCue.Action)
            onClick()
        },
        colors = ButtonDefaults.textButtonColors(contentColor = colors.textOnAccent),
        modifier = Modifier
            .heightIn(min = dims.minTouchTarget)
            .clip(RoundedCornerShape(dims.radii.sm))
            .background(Color(0x33FFFFFF))
            .semantics { contentDescription = "$label media" },
    ) {
        Text(text = label, style = type.action)
    }
}

@Composable
private fun RemoveAttachmentButton(onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    IconButton(
        onClick = {
            haptics.perform(ReliveHapticCue.Action)
            onRemove()
        },
        modifier = modifier
            .size(dims.minTouchTarget)
            .semantics { contentDescription = "Remove attachment" },
    ) {
        Box(
            modifier = Modifier
                .size(dims.icon.lg)
                .clip(RoundedCornerShape(50))
                .background(colors.bgCanvas)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            CloseGlyph(size = dims.icon.md, color = colors.textPrimary, strokeWidth = dims.stroke.icon)
        }
    }
}

@Composable
internal fun MediaErrorText(message: String?) {
    if (message.isNullOrBlank()) return
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Text(
        text = message,
        style = type.subtitle,
        color = colors.textSecondary,
        modifier = Modifier.padding(bottom = dims.spacing.sm),
    )
    Spacer(Modifier.height(dims.spacing.xs))
}
