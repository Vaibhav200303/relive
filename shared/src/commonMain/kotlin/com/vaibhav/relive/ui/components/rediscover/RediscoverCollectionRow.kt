package com.vaibhav.relive.ui.components.rediscover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.presentation.timeline.SystemCollectionCover
import com.vaibhav.relive.ui.theme.ReliveCoverLabelScrim
import com.vaibhav.relive.ui.theme.ReliveGeneratedCover
import com.vaibhav.relive.ui.theme.ReliveTheme
import kotlin.random.Random

/** Stable card keys, shared with the navigation host that keys the container transform on them. */
const val REDISCOVER_CARD_FAVOURITES = "favourites"
const val REDISCOVER_CARD_ON_THIS_DAY = "on-this-day"
const val REDISCOVER_CARD_FROM_YOUR_PAST = "from-your-past"
const val REDISCOVER_CARD_ALL_PHOTOS = "all-photos"

/**
 * One shuffle per app launch, mixed into every cover key (ADR-0064). Which accent-derived
 * gradient a collection card wears is redrawn each session, but holds still within one so covers
 * don't churn as cards scroll in and out of composition.
 */
private val sessionCoverShuffle: String = Random.nextInt().toString()

private fun sessionCoverKey(coverSeed: String): String = "$coverSeed-$sessionCoverShuffle"

/**
 * The cover a collection wears this session: always the generated accent-derived gradient chosen
 * by the session-shuffled seed — never a member's own media, so collection cards read as a
 * considered set rather than a lottery of whatever was saved last. One function serves the Home
 * card and the collection screen the card opens, so the two surfaces carry the same gradient and
 * the container transform between them is continuous (ADR-0065).
 */
fun resolvedRediscoverCollectionCover(coverSeed: String): SystemCollectionCover =
    SystemCollectionCover.Generated(sessionCoverKey(coverSeed))

/** Renders a resolved collection cover; the fallback for every surface that shows one. */
@Composable
fun SystemCollectionCoverImage(
    cover: SystemCollectionCover,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
) {
    when (cover) {
        is SystemCollectionCover.Generated -> ReliveGeneratedCover(cover.coverKey, modifier)
        is SystemCollectionCover.Media -> when (cover.type) {
            MediaType.Image -> RelivedImageTile(cover.storageRef, mediaStore, modifier)
            MediaType.Video -> RelivedVideoTile(cover.storageRef, mediaStore, modifier)
            MediaType.Audio -> Unit
        }
    }
}

/**
 * Tap targets for the Rediscover cards, registered by the carousel and consulted by Home's
 * transparent backdrop-window item. The window owns the hit test while the feed sheet is at rest
 * over the row, so a tap there never reaches the cards themselves — the window forwards it here
 * instead, against each card's *visible* (masked) bounds in root coordinates.
 */
@Stable
class RediscoverRowHitTester {
    private val targets = mutableStateMapOf<String, Pair<Rect, () -> Unit>>()

    fun register(key: String, visibleBounds: Rect, onOpen: () -> Unit) {
        targets[key] = visibleBounds to onOpen
    }

    fun unregister(key: String) {
        targets.remove(key)
    }

    /** Opens the card under [positionInRoot], if any; true when a card took the tap. */
    fun openAt(positionInRoot: Offset): Boolean {
        val hit = targets.values.firstOrNull { (bounds, _) -> bounds.contains(positionInRoot) }
        hit?.second?.invoke()
        return hit != null
    }
}

/**
 * One card in the Home surface's Rediscover row.
 *
 * [coverSeed] seeds the card's generated-gradient pick. The seed is mixed with the per-launch
 * shuffle above, so every collection reads as a considered card, freshly dealt each session.
 */
data class RediscoverCollectionCardModel(
    val key: String,
    val title: String,
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
 * the invitation to browse. Each card is one full-bleed cover (ADR-0064); the title is overlaid
 * on the focal card only, over a scrim that dims the cover just while that label is shown, so
 * masked items read as pure imagery.
 *
 * [state] is hoisted because on Home the feed's transparent window sits over this row and owns the
 * hit test at rest — the window proxies its horizontal drags into the same state and its taps into
 * [hitTester] (see the window item in `HomeScreen`). When that window moves away in the expanded
 * state, this row deliberately drives the same state through the same direct [scrollable] path.
 * Material's pager still lays out and masks the cards, but does not install a second gesture path
 * that can constrain or consume the release velocity. [cardContainerModifier] carries the
 * container-transform bounds for the card being opened (ADR-0065), supplied by the navigation
 * host; the default leaves cards with no shared element.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RediscoverCollectionRow(
    cards: List<RediscoverCollectionCardModel>,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
    state: CarouselState = rememberCarouselState(itemCount = { cards.size }),
    flingBehavior: TargetedFlingBehavior = CarouselDefaults.multiBrowseFlingBehavior(state),
    hitTester: RediscoverRowHitTester? = null,
    cardContainerModifier: @Composable (RediscoverCollectionCardModel) -> Modifier = { Modifier },
) {
    if (cards.isEmpty()) return
    val dims = ReliveTheme.dimensions
    val reverseDirection = ScrollableDefaults.reverseDirection(
        LocalLayoutDirection.current,
        Orientation.Horizontal,
        false,
    )
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = dims.rediscover.compactCardWidth,
        modifier = modifier
            .fillMaxWidth()
            .height(dims.rediscover.compactCardHeight)
            .scrollable(
                state = state,
                orientation = Orientation.Horizontal,
                reverseDirection = reverseDirection,
                flingBehavior = flingBehavior,
            ),
        itemSpacing = dims.spacing.md,
        flingBehavior = flingBehavior,
        userScrollEnabled = false,
        contentPadding = PaddingValues(horizontal = dims.spacing.xl),
    ) { index ->
        RediscoverCollectionCard(cards[index], mediaStore, hitTester, cardContainerModifier)
    }
}

@Composable
private fun CarouselItemScope.RediscoverCollectionCard(
    card: RediscoverCollectionCardModel,
    mediaStore: MediaStore,
    hitTester: RediscoverRowHitTester?,
    cardContainerModifier: @Composable (RediscoverCollectionCardModel) -> Modifier,
) {
    val dims = ReliveTheme.dimensions
    val shape = RoundedCornerShape(dims.rediscover.cardOuterRadius)
    if (hitTester != null) {
        DisposableEffect(hitTester, card.key) {
            onDispose { hitTester.unregister(card.key) }
        }
    }
    Box(
        modifier = cardContainerModifier(card)
            .fillMaxSize()
            .maskClip(shape)
            .clickable(onClick = card.onOpen)
            .onGloballyPositioned { coordinates ->
                if (hitTester != null) {
                    val mask = carouselItemDrawInfo.maskRect
                    hitTester.register(
                        key = card.key,
                        visibleBounds = Rect(
                            offset = coordinates.positionInRoot() + Offset(mask.left, mask.top),
                            size = mask.size,
                        ),
                        onOpen = card.onOpen,
                    )
                }
            }
            .semantics { contentDescription = "Open ${card.title}" },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(ReliveTheme.colors.surfaceCard)
                .border(1.dp, Color.White.copy(alpha = 0.16f), shape),
        ) {
            SystemCollectionCoverImage(
                cover = resolvedRediscoverCollectionCover(card.coverSeed),
                mediaStore = mediaStore,
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .graphicsLayer {
                        val info = carouselItemDrawInfo
                        val range = info.maxSize - info.minSize
                        val grown = if (range > 0f) {
                            ((info.size - info.minSize) / range).coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                        alpha = ((grown - 0.8f) / 0.2f).coerceIn(0f, 1f)
                    }
                    .background(ReliveCoverLabelScrim, shape),
            ) {
                Text(
                    text = card.title,
                    style = ReliveTheme.typography.title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .graphicsLayer { translationX = carouselItemDrawInfo.maskRect.left }
                        .padding(dims.spacing.lg),
                )
            }
        }
    }
}
