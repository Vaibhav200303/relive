package com.vaibhav.relive.ui.components.rediscover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.ui.theme.ReliveGeneratedCover
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * One card in the Home surface's Rediscover row.
 *
 * [coverSeed] chooses the deterministic generated cover when a collection has no visual media, so
 * an empty collection still reads as a considered card rather than a hole.
 */
data class RediscoverCollectionCardModel(
    val key: String,
    val title: String,
    val subtitle: String,
    val coverAttachments: List<MediaAttachment>,
    val coverSeed: String,
    val onOpen: () -> Unit,
)

/**
 * The horizontally scrollable collection row that sits under `Relive your memories` on Home
 * (ADR-0061). It is a row inside the Home surface, not a destination: none of its cards is an
 * entry point into the All moments feed, which is already on this surface directly below.
 *
 * The row deliberately bleeds past the screen edge — the partially visible trailing card is the
 * affordance that says it scrolls.
 */
@Composable
fun RediscoverCollectionRow(
    cards: List<RediscoverCollectionCardModel>,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return
    val dims = ReliveTheme.dimensions
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = dims.spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
    ) {
        items(cards, key = { it.key }) { card ->
            RediscoverCollectionCard(
                card = card,
                mediaStore = mediaStore,
                modifier = Modifier.width(dims.rediscover.compactCardWidth),
            )
        }
    }
}

@Composable
private fun RediscoverCollectionCard(
    card: RediscoverCollectionCardModel,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    val shape = RoundedCornerShape(dims.rediscover.cardOuterRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(ReliveTheme.colors.surfaceCard)
            .clickable(onClick = card.onOpen)
            .semantics { contentDescription = "Open ${card.title}" },
    ) {
        CollectionCover(card.coverAttachments, card.coverSeed, mediaStore)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dims.rediscover.compactInfoAreaHeight)
                .padding(dims.spacing.lg),
        ) {
            Text(
                text = card.title,
                style = ReliveTheme.typography.title,
                color = ReliveTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.subtitle,
                style = ReliveTheme.typography.caption,
                color = ReliveTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CollectionCover(
    attachments: List<MediaAttachment>,
    coverSeed: String,
    mediaStore: MediaStore,
) {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.rediscover.compactMediaHeight)
            .background(ReliveTheme.colors.surfaceCardTranslucent),
    ) {
        when (attachments.size) {
            0 -> ReliveGeneratedCover(coverSeed, Modifier.matchParentSize())
            1 -> CoverTile(attachments.single(), mediaStore, Modifier.matchParentSize())
            2 -> Row(Modifier.matchParentSize()) {
                CoverTile(attachments[0], mediaStore, Modifier.weight(1f))
                Box(Modifier.size(dims.media.collageGap).background(ReliveTheme.colors.accent))
                CoverTile(attachments[1], mediaStore, Modifier.weight(1f))
            }
            else -> Row(Modifier.matchParentSize()) {
                CoverTile(attachments[0], mediaStore, Modifier.weight(1.4f))
                Box(Modifier.size(dims.media.collageGap).background(ReliveTheme.colors.accent))
                Column(Modifier.weight(1f)) {
                    CoverTile(attachments[1], mediaStore, Modifier.weight(1f).fillMaxWidth())
                    Box(
                        Modifier.height(dims.media.collageGap).fillMaxWidth()
                            .background(ReliveTheme.colors.accent),
                    )
                    Row(Modifier.weight(1f)) {
                        CoverTile(attachments[2], mediaStore, Modifier.weight(1f))
                        if (attachments.size >= 4) {
                            Box(
                                Modifier.size(dims.media.collageGap)
                                    .background(ReliveTheme.colors.accent),
                            )
                            CoverTile(attachments[3], mediaStore, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverTile(attachment: MediaAttachment, mediaStore: MediaStore, modifier: Modifier) {
    when (attachment.type) {
        MediaType.Image -> RelivedImageTile(attachment.storageRef, mediaStore, modifier)
        MediaType.Video -> RelivedVideoTile(attachment.storageRef, mediaStore, modifier)
        MediaType.Audio -> Unit
    }
}
