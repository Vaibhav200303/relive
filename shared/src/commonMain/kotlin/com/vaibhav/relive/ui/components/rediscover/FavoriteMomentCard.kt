package com.vaibhav.relive.ui.components.rediscover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.vaibhav.relive.domain.model.FavoriteMomentPreview
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.presentation.cardcover.firstVisualPreviewAttachment
import com.vaibhav.relive.presentation.date.EditorialDateFormatter
import com.vaibhav.relive.presentation.date.EditorialTimeFormatter
import com.vaibhav.relive.ui.components.MediaToCardSurfaceFade
import com.vaibhav.relive.ui.components.reliveCardOuterBorder
import com.vaibhav.relive.ui.components.timeline.HeartGlyph
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun FavoriteMomentCard(
    moment: FavoriteMomentPreview,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
    showFavoriteIndicator: Boolean = true,
    semanticDescription: String = "Open favorite moment",
    onOpen: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val shape = RoundedCornerShape(dims.rediscover.cardOuterRadius)
    val lead = moment.attachments.firstVisualPreviewAttachment()
    Column(
        modifier = modifier
            .height(dims.rediscover.favoriteShelfCardHeight)
            .clip(shape)
            .background(colors.surfaceCard)
            .reliveCardOuterBorder(shape)
            .clickable(onClick = onOpen)
            .semantics { contentDescription = semanticDescription },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.rediscover.compactMediaHeight)
        ) {
            if (lead?.type == MediaType.Image || lead?.type == MediaType.Video) {
                FavoriteMomentLead(
                    attachment = lead,
                    attachmentCount = moment.attachments.size,
                    mediaStore = mediaStore,
                )
            } else {
                com.vaibhav.relive.ui.theme.ReliveGeneratedCover(
                    stableKey = moment.id.value,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(dims.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
            ) {
                val title = moment.title.ifBlank {
                    moment.content.ifBlank { "A saved memory" }
                }
                Text(
                    text = title,
                    style = ReliveTheme.typography.action,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${EditorialDateFormatter.format(moment.createdAt)} • ${EditorialTimeFormatter.format(moment.createdAt)}",
                    style = ReliveTheme.typography.eyebrow,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showFavoriteIndicator) Box(modifier = Modifier.padding(start = dims.spacing.md)) {
                HeartGlyph(
                    size = dims.icon.md,
                    color = colors.accent,
                    strokeWidth = dims.stroke.icon,
                    filled = true,
                )
            }
        }
    }
}

@Composable
private fun FavoriteMomentLead(
    attachment: MediaAttachment,
    attachmentCount: Int,
    mediaStore: MediaStore,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(),
    ) {
        when (attachment.type) {
            MediaType.Image -> RelivedImageTile(attachment.storageRef, mediaStore, Modifier.matchParentSize())
            MediaType.Video -> RelivedVideoTile(attachment.storageRef, mediaStore, Modifier.matchParentSize())
            MediaType.Audio -> Unit
        }
        MediaToCardSurfaceFade(modifier = Modifier.align(Alignment.BottomCenter))
        if (attachmentCount > 1) {
            Text(
                text = "+${attachmentCount - 1}",
                style = ReliveTheme.typography.eyebrow,
                color = colors.textOnAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(colors.accentMuted, RoundedCornerShape(dims.radii.sm))
                    .padding(horizontal = dims.spacing.sm, vertical = dims.spacing.xs),
            )
        }
    }
}
