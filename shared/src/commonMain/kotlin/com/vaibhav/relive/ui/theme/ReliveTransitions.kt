package com.vaibhav.relive.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/** Tokenized M3 lateral settle used by pagers after the drag leaves the user's finger. */
fun ReliveMotion.reliveLateralPagerSnapSpec(
    reduceMotion: Boolean,
): FiniteAnimationSpec<Float> = spec(
    reduceMotion = reduceMotion,
    full = tween(
        durationMillis = durations.medium2,
        easing = easings.standard,
    ),
)

/**
 * Shared M3 forward/backward transition for hierarchical destinations.
 *
 * Forward navigation enters from the right while the previous destination exits a short
 * distance to the left; backward navigation mirrors that direction. Reduced motion keeps
 * the same fade timing while removing the lateral movement.
 */
fun <S> AnimatedContentTransitionScope<S>.reliveForwardBackward(
    motion: ReliveMotion,
    reduceMotion: Boolean,
    movingForward: Boolean,
): ContentTransform {
    val enterSpec = motion.spec<Float>(
        reduceMotion = reduceMotion,
        full = tween(
            durationMillis = motion.durations.medium4,
            easing = motion.easings.emphasizedDecelerate,
        ),
    )
    val exitSpec = motion.spec<Float>(
        reduceMotion = reduceMotion,
        full = tween(
            durationMillis = motion.durations.short4,
            easing = motion.easings.emphasizedAccelerate,
        ),
    )
    val enterSlideSpec = motion.spec<IntOffset>(
        reduceMotion = reduceMotion,
        full = tween(
            durationMillis = motion.durations.medium4,
            easing = motion.easings.emphasizedDecelerate,
        ),
    )
    val exitSlideSpec = motion.spec<IntOffset>(
        reduceMotion = reduceMotion,
        full = tween(
            durationMillis = motion.durations.short4,
            easing = motion.easings.emphasizedAccelerate,
        ),
    )
    val enter = fadeIn(animationSpec = enterSpec) + if (reduceMotion) {
        EnterTransition.None
    } else {
        slideInHorizontally(animationSpec = enterSlideSpec) { width ->
            if (movingForward) width / 5 else -width / 5
        }
    }
    val exit = fadeOut(animationSpec = exitSpec) + if (reduceMotion) {
        ExitTransition.None
    } else {
        slideOutHorizontally(animationSpec = exitSlideSpec) { width ->
            if (movingForward) -width / 5 else width / 5
        }
    }
    return enter togetherWith exit
}
