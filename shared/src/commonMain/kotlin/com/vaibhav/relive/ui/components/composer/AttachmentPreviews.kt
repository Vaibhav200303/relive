package com.vaibhav.relive.ui.components.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedAudio
import com.vaibhav.relive.platform.media.RelivedImage
import com.vaibhav.relive.platform.media.RelivedVideo
import com.vaibhav.relive.presentation.composer.DraftAttachment
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Vertical stack of draft attachments shown above Add Media. Each carries its
 * own remove `×`.
 */
@Composable
internal fun DraftAttachmentColumn(
    attachments: List<DraftAttachment>,
    mediaStore: MediaStore,
    onRemove: (MediaStorageRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
    ) {
        attachments.forEach { att ->
            DraftAttachmentTile(att, mediaStore, onRemove = { onRemove(att.storageRef) })
        }
    }
}

@Composable
private fun DraftAttachmentTile(
    attachment: DraftAttachment,
    mediaStore: MediaStore,
    onRemove: () -> Unit,
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
    ) {
        when (attachment.type) {
            MediaType.Image -> RelivedImage(
                ref = attachment.storageRef,
                mediaStore = mediaStore,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            MediaType.Video -> RelivedVideo(
                ref = attachment.storageRef,
                mediaStore = mediaStore,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            MediaType.Audio -> RelivedAudio(
                ref = attachment.storageRef,
                mediaStore = mediaStore,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        RemoveAttachmentButton(
            onRemove = onRemove,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
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
