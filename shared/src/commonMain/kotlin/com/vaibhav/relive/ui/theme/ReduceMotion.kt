package com.vaibhav.relive.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Cross-platform accessibility gate for reduced-motion. When the host OS signals that the
 * user prefers reduced or disabled animation, this returns `true` so every transition can
 * degrade to a subtle fade via [spec]. Wired into the theme's [LocalReliveReduceMotion] so
 * any composable can read [ReliveTheme.reduceMotion] without re-querying the platform.
 */
@Composable
expect fun rememberReducedMotion(): Boolean

internal val LocalReliveReduceMotion = staticCompositionLocalOf { false }

/**
 * Return [full] when the user is fine with motion; return [reduced] when the OS has signaled
 * reduced motion. Every transition in later phases must route through this helper so a
 * reduced-motion user always gets a plain fade instead of a slide/scale/morph.
 *
 * The default [reduced] spec is the M3 short fade (`tween(short3, easing = standard)`), which
 * matches the plan's "subtle fade" rule. Override only when a call site needs a different
 * fade duration — never to add motion back on top of reduced.
 */
fun <T> ReliveMotion.spec(
    reduceMotion: Boolean,
    full: FiniteAnimationSpec<T>,
    reduced: FiniteAnimationSpec<T> = tween(
        durationMillis = durations.short3,
        easing = easings.standard,
    ),
): FiniteAnimationSpec<T> = if (reduceMotion) reduced else full
