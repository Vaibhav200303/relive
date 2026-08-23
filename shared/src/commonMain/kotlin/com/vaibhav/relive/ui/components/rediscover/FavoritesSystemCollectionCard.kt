package com.vaibhav.relive.ui.components.rediscover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.domain.model.FavoritesCollectionSummary
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.ui.theme.ReliveGeneratedCover
import com.vaibhav.relive.ui.components.timeline.HeartGlyph
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun FavoritesSystemCollectionCard(
    summary: FavoritesCollectionSummary,
    mediaStore: MediaStore,
    onOpen: () -> Unit,
    onDebugLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    val shape = RoundedCornerShape(dims.radii.md)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ReliveTheme.colors.surfaceCard)
            .border(dims.stroke.hairline, ReliveTheme.colors.border, shape)
            .combinedClickable(onClick = onOpen, onLongClick = onDebugLongPress)
            .semantics { contentDescription = "Open saved moments" },
    ) {
        FavoritesMediaPreview(summary.previewAttachments, summary.momentCount, mediaStore)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeartGlyph(
                size = dims.icon.lg,
                color = ReliveTheme.colors.accent,
                strokeWidth = dims.stroke.icon,
                filled = true,
            )
            Text(
                text = "${summary.momentCount} ${if (summary.momentCount == 1L) "moment" else "moments"}",
                style = ReliveTheme.typography.subtitle,
                color = ReliveTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun FavoritesMediaPreview(
    attachments: List<MediaAttachment>,
    momentCount: Long,
    mediaStore: MediaStore,
) {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.timelineHome.customMediaHeight)
            .background(ReliveTheme.colors.surfaceCardTranslucent),
    ) {
        when (attachments.size) {
            0 -> ReliveGeneratedCover("timeline-favorites", Modifier.matchParentSize())
            1 -> FavoritesPreviewTile(attachments.single(), mediaStore, Modifier.matchParentSize())
            2 -> Row(Modifier.matchParentSize()) {
                FavoritesPreviewTile(attachments[0], mediaStore, Modifier.weight(1f))
                Box(Modifier.size(dims.media.collageGap).background(ReliveTheme.colors.accent))
                FavoritesPreviewTile(attachments[1], mediaStore, Modifier.weight(1f))
            }
            else -> Row(Modifier.matchParentSize()) {
                FavoritesPreviewTile(attachments[0], mediaStore, Modifier.weight(1.4f))
                Box(Modifier.size(dims.media.collageGap).background(ReliveTheme.colors.accent))
                Column(Modifier.weight(1f)) {
                    FavoritesPreviewTile(attachments[1], mediaStore, Modifier.weight(1f).fillMaxWidth())
                    Box(Modifier.height(dims.media.collageGap).fillMaxWidth().background(ReliveTheme.colors.accent))
                    Row(Modifier.weight(1f)) {
                        FavoritesPreviewTile(attachments[2], mediaStore, Modifier.weight(1f))
                        if (attachments.size == 4) {
                            Box(Modifier.size(dims.media.collageGap).background(ReliveTheme.colors.accent))
                            FavoritesPreviewTile(attachments[3], mediaStore, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.timelineHome.mediaFadeHeight)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, ReliveTheme.colors.surfaceCard))),
        )
    }
}

@Composable
private fun FavoritesPreviewTile(attachment: MediaAttachment, mediaStore: MediaStore, modifier: Modifier) {
    when (attachment.type) {
        MediaType.Image -> RelivedImageTile(attachment.storageRef, mediaStore, modifier)
        MediaType.Video -> RelivedVideoTile(attachment.storageRef, mediaStore, modifier)
        MediaType.Audio -> Unit
    }
}
