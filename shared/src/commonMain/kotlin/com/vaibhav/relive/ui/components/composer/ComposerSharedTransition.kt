package com.vaibhav.relive.ui.components.composer

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
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * M3 container transform: global "New" FAB <-> quick-capture destination
 * (All Timeline screen). Same modifier attached to source and target.
 *
 * Fade-through variant (matches Views MaterialContainerTransform FADE_MODE_THROUGH):
 * source fully fades out over first half, target fully fades in over second half,
 * so neither content is visible enlarging inside the morphing container. Bounds
 * interpolate over the full duration with emphasized easing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.quickCaptureSharedBounds(
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
        this@quickCaptureSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(QUICK_CAPTURE_CONTAINER_KEY),
            animatedVisibilityScope = animatedScope,
            enter = fadeIn(enterSpec),
            exit = fadeOut(exitSpec),
            boundsTransform = transform,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.None,
                alignment = Alignment.BottomEnd,
            ),
        )
    }
}

private const val QUICK_CAPTURE_CONTAINER_KEY = "quick-capture-container"
