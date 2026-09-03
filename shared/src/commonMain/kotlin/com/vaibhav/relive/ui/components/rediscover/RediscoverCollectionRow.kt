package com.vaibhav.relive.ui.components.rediscover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
 * Laid out as a Material 3 multi-browse carousel: the leading card is shown at full size and
 * trailing cards compress into masked medium/small items, which is both the scroll affordance and
 * the invitation to browse. Labels fade out as a card shrinks so masked items read as imagery, not
 * clipped text.
 *
 * [state] is hoisted because on Home the feed's transparent window sits over this row and owns the
 * hit test at rest — the window proxies its horizontal drags into the same state (see the window
 * item in `HomeScreen`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RediscoverCollectionRow(
    cards: List<RediscoverCollectionCardModel>,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
    state: CarouselState = rememberCarouselState(itemCount = { cards.size }),
) {
    if (cards.isEmpty()) return
    val dims = ReliveTheme.dimensions
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = dims.rediscover.compactCardWidth,
        modifier = modifier
            .fillMaxWidth()
            .height(dims.rediscover.compactMediaHeight + dims.rediscover.compactInfoAreaHeight),
        itemSpacing = dims.spacing.md,
        contentPadding = PaddingValues(horizontal = dims.spacing.xl),
    ) { index ->
        RediscoverCollectionCard(cards[index], mediaStore)
    }
}

@Composable
private fun CarouselItemScope.RediscoverCollectionCard(
    card: RediscoverCollectionCardModel,
    mediaStore: MediaStore,
) {
    val dims = ReliveTheme.dimensions
    val shape = RoundedCornerShape(dims.rediscover.cardOuterRadius)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .maskClip(shape)
            .background(ReliveTheme.colors.surfaceCard)
            .clickable(onClick = card.onOpen)
            .semantics { contentDescription = "Open ${card.title}" },
    ) {
        CollectionCover(card.coverAttachments, card.coverSeed, mediaStore)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(dims.spacing.lg)
                .graphicsLayer {
                    val info = carouselItemDrawInfo
                    // The mask is centred in the item, so pin the label block to its visible left
                    // edge instead of letting letters get sliced mid-glyph.
                    translationX = info.maskRect.left
                    val range = info.maxSize - info.minSize
                    val grown = if (range > 0f) {
                        ((info.size - info.minSize) / range).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    // Labels belong to the focal card only: gone on medium/small masks, fading in
                    // as a card grows toward the large slot.
                    alpha = ((grown - 0.8f) / 0.2f).coerceIn(0f, 1f)
                },
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
