package com.vaibhav.relive.ui.components.composer

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.spec

/** Coordinates the persistent New control and its composer destination. */
@OptIn(ExperimentalSharedTransitionApi::class)
class ComposerSharedTransition(
    private val scope: SharedTransitionScope,
    private val composerVisible: Boolean,
    private val reduceMotion: Boolean,
) {
    @Composable
    fun sourceModifier(): Modifier = sharedModifier(visible = !composerVisible)

    @Composable
    fun targetModifier(): Modifier = sharedModifier(visible = composerVisible)

    @Composable
    private fun sharedModifier(visible: Boolean): Modifier {
        if (reduceMotion) return Modifier
        val motion = ReliveTheme.motion
        val transform = BoundsTransform { _, _ ->
            motion.spec(
                reduceMotion = reduceMotion,
                full = tween(
                    durationMillis = motion.durations.medium4,
                    easing = motion.easings.emphasized,
                ),
            )
        }
        return with(scope) {
            Modifier.sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState("new-moment-composer"),
                visible = visible,
                boundsTransform = transform,
            ).clip(if (visible) ReliveTheme.shapes.sheet else ReliveTheme.shapes.pill)
        }
    }
}
