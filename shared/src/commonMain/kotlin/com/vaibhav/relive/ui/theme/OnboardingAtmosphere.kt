package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The deliberately small visual system used by the first-launch story. It is separate from a
 * person's selected Relive palette: onboarding is a single cinematic introduction, while the
 * archive itself continues to use the selected app theme.
 */
@Immutable
data class OnboardingAtmosphere(
    val upper: Color,
    val middle: Color,
    val lower: Color,
)

object ReliveOnboardingColors {
    val darkBase = Color(0xFF0B090B)
    val darkPlum = Color(0xFF160D14)
    val textPrimary = Color.White
    val textSecondary = Color(0xFFB7ADB4)
    val softWhiteSurface = Color(0xFFFAF7F8)
    val controlBorder = Color(0x26FFFFFF)
    val controlShadow = Color(0x590B090B)
    val quietProgress = Color(0x66FFFFFF)
    val captureChrome = Color(0xFFFFE5F0)
    val timelineChrome = Color(0xFFF0E5FF)
    val photoControlTop = Color(0xFFF09ABD)
    val photoControlBottom = Color(0xFFC95B88)
    val noteControlTop = Color(0xFFC49AF0)
    val noteControlBottom = Color(0xFF865BC4)
    val videoControlTop = Color(0xFF9B91F2)
    val videoControlBottom = Color(0xFF5D5AC8)
    val audioControlTop = Color(0xFFF2B28F)
    val audioControlBottom = Color(0xFFD07769)

    val capture = OnboardingAtmosphere(
        upper = Color(0xFFE789AC),
        middle = Color(0xFFB94D77),
        lower = Color(0xFF65263E),
    )
    val timeline = OnboardingAtmosphere(
        upper = Color(0xFFBE83D8),
        middle = Color(0xFF7650BE),
        lower = Color(0xFF3E275F),
    )
    val customTimeline = OnboardingAtmosphere(
        upper = Color(0xFFB98CE0),
        middle = Color(0xFF65458A),
        lower = Color(0xFF2A1B3D),
    )
    val privacy = OnboardingAtmosphere(
        upper = Color(0xFFB87386),
        middle = Color(0xFF74364D),
        lower = Color(0xFF351A28),
    )
    val ready = OnboardingAtmosphere(
        upper = Color(0xFFD4868F),
        middle = Color(0xFF8A4556),
        lower = Color(0xFF42212C),
    )
}

/** A full-screen, boundary-free stage that settles naturally into the persistent dark shell. */
fun onboardingAtmosphereBrush(progress: Float): Brush {
    val chapter = progress.coerceIn(0f, 4f)
    val capture = ReliveOnboardingColors.capture
    val timeline = ReliveOnboardingColors.timeline
    val customTimeline = ReliveOnboardingColors.customTimeline
    val privacy = ReliveOnboardingColors.privacy
    val ready = ReliveOnboardingColors.ready
    val from = when {
        chapter <= 1f -> capture
        chapter <= 2f -> timeline
        chapter <= 3f -> customTimeline
        else -> privacy
    }
    val to = when {
        chapter <= 1f -> timeline
        chapter <= 2f -> customTimeline
        chapter <= 3f -> privacy
        else -> ready
    }
    val t = when {
        chapter <= 1f -> chapter
        chapter <= 2f -> chapter - 1f
        chapter <= 3f -> chapter - 2f
        else -> chapter - 3f
    }
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to lerp(from.upper, to.upper, t),
            0.26f to lerp(from.middle, to.middle, t),
            0.52f to lerp(from.lower, to.lower, t),
            0.70f to lerp(from.lower, to.lower, t),
            0.86f to ReliveOnboardingColors.darkPlum,
            1.00f to ReliveOnboardingColors.darkBase,
        ),
    )
}

fun onboardingAtmosphereCurveColor(progress: Float): Color {
    val chapter = progress.coerceIn(0f, 4f)
    return when {
        chapter <= 1f -> lerp(ReliveOnboardingColors.capture.upper, ReliveOnboardingColors.timeline.upper, chapter)
        chapter <= 2f -> lerp(
            ReliveOnboardingColors.timeline.upper,
            ReliveOnboardingColors.customTimeline.upper,
            chapter - 1f,
        )
        chapter <= 3f -> lerp(
            ReliveOnboardingColors.customTimeline.upper,
            ReliveOnboardingColors.privacy.upper,
            chapter - 2f,
        )
        else -> lerp(
            ReliveOnboardingColors.privacy.upper,
            ReliveOnboardingColors.ready.upper,
            chapter - 3f,
        )
    }
}

fun onboardingAtmosphereChromeColor(progress: Float): Color = lerp(
    ReliveOnboardingColors.captureChrome,
    ReliveOnboardingColors.timelineChrome,
    progress.coerceIn(0f, 1f),
)
