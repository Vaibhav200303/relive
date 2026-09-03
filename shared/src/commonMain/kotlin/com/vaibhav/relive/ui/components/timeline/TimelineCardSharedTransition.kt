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
import androidx.compose.ui.layout.ContentScale
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * M3 Expressive container transform: a timeline's card on Timeline Home <-> that timeline's detail
 * screen. The same modifier goes on the card and on the detail screen's container, keyed by the
 * timeline, so tapping one card cannot morph into another (ADR-0063).
 *
 * Fade-through variant, matching the quick-capture transform this app already uses: the source
 * fades out over the first half and the target fades in over the second, so neither is seen
 * enlarging inside the morphing container, while the bounds interpolate across the whole duration
 * on the emphasized curve.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.timelineCardSharedBounds(
    timelineId: TimelineId,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    reduceMotion: Boolean,
): Modifier {
    if (reduceMotion) return this
    val motion = ReliveTheme.motion
    val totalMs = motion.durations.long2
    val fadeMs = motion.durations.medium2
    val enterDelayMs = totalMs - fadeMs - fadeMs / 3
    val transform = BoundsTransform { _, _ ->
        tween(durationMillis = totalMs, easing = motion.easings.emphasized)
    }
    val exitSpec = tween<Float>(
        durationMillis = fadeMs,
        easing = motion.easings.emphasizedAccelerate,
    )
    val enterSpec = tween<Float>(
        durationMillis = fadeMs,
        delayMillis = enterDelayMs,
        easing = motion.easings.emphasizedDecelerate,
    )
    return with(sharedScope) {
        this@timelineCardSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(timelineCardContainerKey(timelineId)),
            animatedVisibilityScope = animatedScope,
            enter = fadeIn(enterSpec),
            exit = fadeOut(exitSpec),
            boundsTransform = transform,
            // The detail screen is laid out at its own full size and cropped to the morphing
            // container, rather than squashed into the card's aspect ratio on the first frame.
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.None,
                alignment = Alignment.TopCenter,
            ),
        )
    }
}

private fun timelineCardContainerKey(timelineId: TimelineId): String =
    "timeline-card-container-${timelineId.value}"
