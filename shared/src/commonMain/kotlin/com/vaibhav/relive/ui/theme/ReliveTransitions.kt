package com.vaibhav.relive.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.Alignment

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

/**
 * M3 enter/exit transition for components that remain within the current screen.
 *
 * Full-motion components expand from their contextual edge; this deliberately avoids scale
 * and z-axis motion. Reduced motion is always the shared short, standard fade.
 */
fun ReliveMotion.reliveInContextVerticalEnter(
    reduceMotion: Boolean,
    expandFrom: Alignment.Vertical = Alignment.Bottom,
): EnterTransition {
    val fullSpec = tween<IntSize>(
        durationMillis = durations.medium4,
        easing = easings.emphasizedDecelerate,
    )
    return if (reduceMotion) fadeIn(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.medium4, easing = easings.emphasizedDecelerate),
    )) else expandVertically(
        expandFrom = expandFrom,
        animationSpec = spec(reduceMotion, fullSpec),
    )
}

fun ReliveMotion.reliveInContextVerticalExit(
    reduceMotion: Boolean,
    shrinkTowards: Alignment.Vertical = Alignment.Bottom,
): ExitTransition {
    val fullSpec = tween<IntSize>(
        durationMillis = durations.short4,
        easing = easings.emphasizedAccelerate,
    )
    return if (reduceMotion) fadeOut(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
    )) else shrinkVertically(
        shrinkTowards = shrinkTowards,
        animationSpec = spec(reduceMotion, fullSpec),
    )
}

/** Horizontal in-context variant for anchored menus. */
fun ReliveMotion.reliveInContextHorizontalEnter(
    reduceMotion: Boolean,
    expandFrom: Alignment.Horizontal = Alignment.Start,
): EnterTransition = if (reduceMotion) {
    fadeIn(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.medium4, easing = easings.emphasizedDecelerate),
    ))
} else {
    expandHorizontally(
        expandFrom = expandFrom,
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.medium4, easing = easings.emphasizedDecelerate),
        ),
    )
}

fun ReliveMotion.reliveInContextHorizontalExit(
    reduceMotion: Boolean,
    shrinkTowards: Alignment.Horizontal = Alignment.Start,
): ExitTransition = if (reduceMotion) {
    fadeOut(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
    ))
} else {
    shrinkHorizontally(
        shrinkTowards = shrinkTowards,
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
        ),
    )
}

/**
 * Composer sheets cross the screen boundary, so they slide from/to the bottom. Full motion
 * intentionally has no fade; reduced motion is the shared short, standard fade only.
 */
fun ReliveMotion.reliveBottomSheetEnter(reduceMotion: Boolean): EnterTransition = if (reduceMotion) {
    fadeIn(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.medium4, easing = easings.emphasizedDecelerate),
    ))
} else {
    slideInVertically(
        initialOffsetY = { it },
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.medium4, easing = easings.emphasizedDecelerate),
        ),
    )
}

fun ReliveMotion.reliveBottomSheetExit(reduceMotion: Boolean): ExitTransition = if (reduceMotion) {
    fadeOut(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
    ))
} else {
    slideOutVertically(
        targetOffsetY = { it },
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
        ),
    )
}

/**
 * Scroll-driven top app bars cross the top screen boundary. Full motion combines that
 * boundary slide with a vertical expand/shrink; reduced motion is a plain fade.
 */
fun ReliveMotion.reliveScrollAppBarEnter(reduceMotion: Boolean): EnterTransition = if (reduceMotion) {
    fadeIn(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.medium2, easing = easings.emphasizedDecelerate),
    ))
} else {
    slideInVertically(
        initialOffsetY = { -it },
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.medium2, easing = easings.emphasizedDecelerate),
        ),
    ) + expandVertically(
        expandFrom = Alignment.Top,
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.medium2, easing = easings.emphasizedDecelerate),
        ),
    )
}

fun ReliveMotion.reliveScrollAppBarExit(reduceMotion: Boolean): ExitTransition = if (reduceMotion) {
    fadeOut(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
    ))
} else {
    slideOutVertically(
        targetOffsetY = { -it },
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
        ),
    ) + shrinkVertically(
        shrinkTowards = Alignment.Top,
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
        ),
    )
}

/** Bottom floating controls mirror [reliveScrollAppBarEnter] from the lower screen boundary. */
fun ReliveMotion.reliveScrollFloatingControlsEnter(reduceMotion: Boolean): EnterTransition = if (reduceMotion) {
    fadeIn(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.medium2, easing = easings.emphasizedDecelerate),
    ))
} else {
    slideInVertically(
        initialOffsetY = { it },
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.medium2, easing = easings.emphasizedDecelerate),
        ),
    ) + expandVertically(
        expandFrom = Alignment.Bottom,
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.medium2, easing = easings.emphasizedDecelerate),
        ),
    )
}

fun ReliveMotion.reliveScrollFloatingControlsExit(reduceMotion: Boolean): ExitTransition = if (reduceMotion) {
    fadeOut(animationSpec = spec(
        reduceMotion = true,
        full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
    ))
} else {
    slideOutVertically(
        targetOffsetY = { it },
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
        ),
    ) + shrinkVertically(
        shrinkTowards = Alignment.Bottom,
        animationSpec = spec(
            reduceMotion = false,
            full = tween(durationMillis = durations.short4, easing = easings.emphasizedAccelerate),
        ),
    )
}
