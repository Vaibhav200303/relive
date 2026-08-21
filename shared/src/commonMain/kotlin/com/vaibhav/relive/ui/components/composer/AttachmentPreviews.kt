package com.vaibhav.relive.ui.components.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedAudio
import com.vaibhav.relive.platform.media.RelivedImage
import com.vaibhav.relive.platform.media.RelivedVideo
import com.vaibhav.relive.presentation.composer.DraftAttachment
import com.vaibhav.relive.presentation.composer.DraftMediaStatus
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Vertical stack of draft attachments shown above Add Media. Each carries its
 * own remove `×`. While a slot is processing, the tile keeps the same
 * geometry but overlays a compact circular spinner; a failed slot exposes
 * Retry and Remove instead.
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
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.radii.md))
            .background(colors.surfaceCard)
            .border(
                width = dims.stroke.hairline,
                color = colors.borderMuted,
                shape = RoundedCornerShape(dims.radii.md),
            )
            .padding(dims.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        TileMediaSlot(attachment, mediaStore)
        when (attachment.status) {
            DraftMediaStatus.Pending, DraftMediaStatus.Processing -> {
                ProcessingOverlay()
            }
            is DraftMediaStatus.Failed -> {
                FailedOverlay(onRetry = onRetry, onRemove = onRemove)
            }
            is DraftMediaStatus.Ready -> Unit
        }
        // Remove control: always available so the user can bail out of a
        // stuck processing tile without waiting.
        if (attachment.status !is DraftMediaStatus.Failed) {
            RemoveAttachmentButton(
                onRemove = onRemove,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun TileMediaSlot(attachment: DraftAttachment, mediaStore: MediaStore) {
    val status = attachment.status
    if (status is DraftMediaStatus.Ready) {
        when (attachment.type) {
            MediaType.Image -> RelivedImage(
                ref = status.storageRef,
                mediaStore = mediaStore,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            MediaType.Video -> RelivedVideo(
                ref = status.storageRef,
                mediaStore = mediaStore,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            MediaType.Audio -> RelivedAudio(
                ref = status.storageRef,
                mediaStore = mediaStore,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        // Placeholder geometry mirrors the final tile so nothing jumps when
        // processing completes. Audio tiles are shorter than photo/video.
        val height = if (attachment.type == MediaType.Audio) 72.dp else 180.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(ReliveTheme.dimensions.radii.sm))
                .background(Color.Black),
        )
    }
}

@Composable
private fun BoxScope.ProcessingOverlay() {
    val colors = ReliveTheme.colors
    // matchParentSize locks the overlay to the tile's measured bounds without
    // participating in Box measurement, so the spinner's center coincides
    // with the visible tile center regardless of thumbnail aspect ratio.
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
    Text(
        text = label,
        style = type.action,
        color = colors.textOnAccent,
        modifier = Modifier
            .clip(RoundedCornerShape(dims.radii.sm))
            .background(Color(0x33FFFFFF))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.xs)
            .semantics { contentDescription = "$label media" },
    )
}

@Composable
private fun RemoveAttachmentButton(onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    IconButton(
        onClick = onRemove,
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
