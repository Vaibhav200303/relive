package com.vaibhav.relive.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Immutable

@Immutable
data class ReliveDurations(
    val fastMillis: Int = 120,
    val timelineReturnMillis: Int = 145,
    val standardMillis: Int = 240,
    val slowMillis: Int = 360,
)

@Immutable
data class ReliveEasings(
    val standard: Easing = FastOutSlowInEasing,
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
)

@Immutable
data class ReliveMotion(
    val durations: ReliveDurations = ReliveDurations(),
    val easings: ReliveEasings = ReliveEasings(),
)

val DefaultReliveMotion: ReliveMotion = ReliveMotion()
