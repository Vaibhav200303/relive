package com.vaibhav.relive.ui.components.timeline

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * M3 Expressive container transform: a timeline's card on Timeline Home <-> that timeline's detail
 * screen. The same modifier goes on the card and on the detail screen's container, keyed by the
 * timeline, so tapping one card cannot morph into another (ADR-0063).
 *
 * Fade-through variant, matching the quick-capture transform this app already uses: the source
 * fades out quickly while the target fades in across the window where the emphasized bounds
 * actually move, so the growing container is always carried by visible content, while the bounds
 * interpolate across the whole duration on the emphasized curve.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.timelineCardSharedBounds(
    timelineId: TimelineId,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    reduceMotion: Boolean,
): Modifier = cardContainerSharedBounds(
    containerKey = timelineCardContainerKey(timelineId),
    sharedScope = sharedScope,
    animatedScope = animatedScope,
    reduceMotion = reduceMotion,
    // Clipped to the animated bounds with the card's own corners — without the clip the detail
    // screen is laid out full-size and never cropped to the morphing container, so the bounds
    // animation is invisible and the open reads as a plain cross-fade (ADR-0063).
    restingCornerRadiusPx = with(LocalDensity.current) {
        ReliveTheme.dimensions.radii.largeIncreased.toPx()
    },
)

/**
 * The same container transform for a Rediscover collection card on Home and the read-only
 * collection screen it opens (ADR-0065). Permitted by the rule ADR-0060 established: the card's
 * cover *is* the destination's cover now, so the morphing container carries a genuinely
 * continuous image. Keyed by the collection, so one card can only ever morph into its own screen.
 *
 * Both halves keep [ContentScale.None]: content is laid out at its own size and cropped by the
 * animating bounds. Scaling the card half with the container was tried and reverted — the
 * resizeMode is direction-agnostic, so on close the card (title label and all) faded in
 * magnified over the shrinking screen. With the covers now generated gradients, the retimed
 * cross-fade alone carries the open.
 */
@Composable
fun Modifier.rediscoverCardSharedBounds(
    collectionKey: String,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    reduceMotion: Boolean,
): Modifier = cardContainerSharedBounds(
    containerKey = "rediscover-collection-container-$collectionKey",
    sharedScope = sharedScope,
    animatedScope = animatedScope,
    reduceMotion = reduceMotion,
    restingCornerRadiusPx = with(LocalDensity.current) {
        ReliveTheme.dimensions.rediscover.cardOuterRadius.toPx()
    },
    restingWidthPx = with(LocalDensity.current) {
        ReliveTheme.dimensions.rediscover.compactCardWidth.toPx()
    },
)

/**
 * Clips the morphing overlay to the animated bounds with a corner radius that relaxes from the
 * card's resting radius to square as the container approaches full width — and tightens back on
 * the way down. A fixed-radius clip holds the card's corners for the whole transition, so the
 * long emphasized settle shows a full-screen surface with visibly rounded corners that pop square
 * the instant the overlay ends; interpolating the radius is what lets the container genuinely
 * finish *becoming* the screen (and the screen the card).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private class MorphingCornersOverlayClip(
    private val restingRadiusPx: Float,
    private val restingWidthPx: Float,
    private val expandedWidthPx: Float,
) : SharedTransitionScope.OverlayClip {
    private val path = Path()

    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path {
        val range = (expandedWidthPx - restingWidthPx).coerceAtLeast(1f)
        val towardCard = ((expandedWidthPx - bounds.width) / range).coerceIn(0f, 1f)
        path.rewind()
        path.addRoundRect(RoundRect(bounds, CornerRadius(restingRadiusPx * towardCard)))
        return path
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.cardContainerSharedBounds(
    containerKey: String,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    reduceMotion: Boolean,
    restingCornerRadiusPx: Float? = null,
    restingWidthPx: Float? = null,
): Modifier {
    if (reduceMotion) return this
    val motion = ReliveTheme.motion
    val totalMs = motion.durations.long2
    // The emphasized curve covers nearly all of its distance in the first ~30% of [totalMs], so
    // the cross-fade must live inside that window: the source drops out fast and the target is
    // substantially opaque while the bounds are actually moving. The old half-duration fades with
    // a delayed enter left the growing container carried by nothing on the way open — the morph
    // read as a teleport followed by a fade-in arrival.
    val transform = BoundsTransform { _, _ ->
        tween(durationMillis = totalMs, easing = motion.easings.emphasized)
    }
    val exitSpec = tween<Float>(
        durationMillis = motion.durations.short3,
        easing = motion.easings.standardAccelerate,
    )
    val enterSpec = tween<Float>(
        durationMillis = motion.durations.medium1,
        easing = motion.easings.emphasizedDecelerate,
    )
    val expandedWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()
    val overlayClip = restingCornerRadiusPx?.let { radius ->
        MorphingCornersOverlayClip(
            restingRadiusPx = radius,
            // With no exact card width, anchor the ramp at half the window: any card at least
            // that wide keeps its full resting radius on the first frame (the timeline grid's
            // two-up cards are just about that), and the endpoints — card radius at rest,
            // square at full width — hold either way.
            restingWidthPx = restingWidthPx ?: (expandedWidthPx * 0.5f),
            expandedWidthPx = expandedWidthPx,
        )
    }
    return with(sharedScope) {
        // The detail screen is laid out at its own full size and cropped to the morphing
        // container, rather than squashed into the card's aspect ratio on the first frame.
        val resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
            contentScale = ContentScale.None,
            alignment = Alignment.TopCenter,
        )
        if (overlayClip != null) {
            this@cardContainerSharedBounds.sharedBounds(
                sharedContentState = rememberSharedContentState(containerKey),
                animatedVisibilityScope = animatedScope,
                enter = fadeIn(enterSpec),
                exit = fadeOut(exitSpec),
                boundsTransform = transform,
                resizeMode = resizeMode,
                clipInOverlayDuringTransition = overlayClip,
            )
        } else {
            this@cardContainerSharedBounds.sharedBounds(
                sharedContentState = rememberSharedContentState(containerKey),
                animatedVisibilityScope = animatedScope,
                enter = fadeIn(enterSpec),
                exit = fadeOut(exitSpec),
                boundsTransform = transform,
                resizeMode = resizeMode,
            )
        }
    }
}

private fun timelineCardContainerKey(timelineId: TimelineId): String =
    "timeline-card-container-${timelineId.value}"
