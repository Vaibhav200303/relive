package com.vaibhav.relive.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp

/** Semantic colors for the abstract landscape used when a custom timeline has no cover photo. */
data class ReliveTimelineThumbnailColors(
    val skyStart: Color,
    val skyMiddle: Color,
    val skyEnd: Color,
    val sun: Color,
    val distantMountain: Color,
    val foreground: Color,
)

/**
 * Derives the fallback landscape from the active canvas instead of treating an empty timeline as
 * a neutral media placeholder. This makes the cover feel like part of its surrounding palette.
 */
fun timelineThumbnailColorsFor(colors: ReliveColors): ReliveTimelineThumbnailColors =
    ReliveTimelineThumbnailColors(
        skyStart = lerp(colors.bgCanvasGlow, colors.accent, 0.30f),
        skyMiddle = lerp(colors.tint, colors.accent, 0.36f),
        skyEnd = lerp(colors.tint, colors.spark, 0.40f),
        sun = lerp(Color.White, colors.spark, 0.42f),
        distantMountain = lerp(colors.accent, colors.shadow, 0.50f),
        foreground = lerp(colors.bgCanvas, colors.shadow, 0.58f),
    )

/** A theme-aware abstract landscape with the same visual language as the onboarding timeline card. */
@Composable
fun ReliveTimelineThumbnail(modifier: Modifier = Modifier) {
    val colors = timelineThumbnailColorsFor(ReliveTheme.colors)
    Canvas(modifier) {
        drawRect(Brush.linearGradient(listOf(colors.skyStart, colors.skyMiddle, colors.skyEnd)))
        drawCircle(
            color = colors.sun.copy(alpha = 0.72f),
            radius = size.minDimension * 0.12f,
            center = Offset(size.width * 0.78f, size.height * 0.24f),
        )
        val distantMountain = Path().apply {
            moveTo(0f, size.height * 0.74f)
            lineTo(size.width * 0.27f, size.height * 0.38f)
            lineTo(size.width * 0.48f, size.height * 0.67f)
            lineTo(size.width * 0.66f, size.height * 0.44f)
            lineTo(size.width, size.height * 0.76f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(distantMountain, colors.distantMountain.copy(alpha = 0.74f))
        val foreground = Path().apply {
            moveTo(0f, size.height * 0.82f)
            cubicTo(
                size.width * 0.24f,
                size.height * 0.68f,
                size.width * 0.44f,
                size.height * 0.96f,
                size.width * 0.65f,
                size.height * 0.78f,
            )
            cubicTo(
                size.width * 0.82f,
                size.height * 0.65f,
                size.width * 0.90f,
                size.height * 0.84f,
                size.width,
                size.height * 0.72f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(foreground, colors.foreground.copy(alpha = 0.90f))
    }
}
