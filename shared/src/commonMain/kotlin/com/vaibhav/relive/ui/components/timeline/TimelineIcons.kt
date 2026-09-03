package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Thin outlined settings gear. Eight radial teeth around an outer ring with a
 * small centered inner ring. Uses a single stroke width so the visual weight
 * matches the search glyph.
 */
@Composable
internal fun GearGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val cx = px / 2f
        val cy = px / 2f
        val bodyRadius = px * 0.28f
        val toothInner = bodyRadius
        val toothOuter = px * 0.44f
        val toothHalfWidth = px * 0.055f
        val innerRadius = px * 0.11f

        val toothCount = 8
        for (i in 0 until toothCount) {
            val angle = (kotlin.math.PI * 2.0 * i / toothCount).toFloat()
            val cosA = kotlin.math.cos(angle)
            val sinA = kotlin.math.sin(angle)
            // Perpendicular offset for tooth flanks.
            val perpX = -sinA
            val perpY = cosA
            val innerL = Offset(
                cx + cosA * toothInner + perpX * toothHalfWidth,
                cy + sinA * toothInner + perpY * toothHalfWidth,
            )
            val outerL = Offset(
                cx + cosA * toothOuter + perpX * toothHalfWidth,
                cy + sinA * toothOuter + perpY * toothHalfWidth,
            )
            val innerR = Offset(
                cx + cosA * toothInner - perpX * toothHalfWidth,
                cy + sinA * toothInner - perpY * toothHalfWidth,
            )
            val outerR = Offset(
                cx + cosA * toothOuter - perpX * toothHalfWidth,
                cy + sinA * toothOuter - perpY * toothHalfWidth,
            )
            drawLine(color = color, start = innerL, end = outerL, strokeWidth = sw)
            drawLine(color = color, start = innerR, end = outerR, strokeWidth = sw)
            drawLine(color = color, start = outerL, end = outerR, strokeWidth = sw)
        }

        drawCircle(
            color = color,
            radius = bodyRadius,
            center = Offset(cx, cy),
            style = Stroke(width = sw),
        )
        drawCircle(
            color = color,
            radius = innerRadius,
            center = Offset(cx, cy),
            style = Stroke(width = sw),
        )
    }
}

@Composable
internal fun SearchGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val radius = px * 0.32f
        val cx = px * 0.42f
        val cy = px * 0.42f
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = sw),
        )
        val handleStart = Offset(cx + radius * 0.7071f, cy + radius * 0.7071f)
        val handleEnd = Offset(px * 0.92f, px * 0.92f)
        drawLine(
            color = color,
            start = handleStart,
            end = handleEnd,
            strokeWidth = sw,
        )
    }
}

@Composable
fun BackGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val center = px / 2f
        val stroke = strokeWidth.toPx()
        drawLine(color, Offset(px * 0.80f, center), Offset(px * 0.22f, center), stroke)
        drawLine(color, Offset(px * 0.22f, center), Offset(px * 0.48f, px * 0.24f), stroke)
        drawLine(color, Offset(px * 0.22f, center), Offset(px * 0.48f, px * 0.76f), stroke)
    }
}

@Composable
internal fun CalendarGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val stroke = strokeWidth.toPx()
        val left = px * 0.16f
        val right = px * 0.84f
        val top = px * 0.25f
        val bottom = px * 0.84f
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(px * 0.08f),
            style = Stroke(stroke),
        )
        drawLine(color, Offset(left, px * 0.43f), Offset(right, px * 0.43f), stroke)
        drawLine(color, Offset(px * 0.34f, px * 0.12f), Offset(px * 0.34f, px * 0.33f), stroke)
        drawLine(color, Offset(px * 0.66f, px * 0.12f), Offset(px * 0.66f, px * 0.33f), stroke)
        drawCircle(color, px * 0.045f, Offset(px * 0.36f, px * 0.61f))
        drawCircle(color, px * 0.045f, Offset(px * 0.64f, px * 0.61f))
    }
}

@Composable
internal fun ForwardGlyph(size: Dp, color: Color, strokeWidth: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val px = size.toPx()
        val center = px / 2f
        val stroke = strokeWidth.toPx()
        drawLine(color, Offset(px * 0.20f, center), Offset(px * 0.78f, center), stroke)
        drawLine(color, Offset(px * 0.78f, center), Offset(px * 0.52f, px * 0.24f), stroke)
        drawLine(color, Offset(px * 0.78f, center), Offset(px * 0.52f, px * 0.76f), stroke)
    }
}

@Composable
internal fun UpGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val center = px / 2f
        val stroke = strokeWidth.toPx()
        drawLine(color, Offset(center, px * 0.80f), Offset(center, px * 0.18f), stroke)
        drawLine(color, Offset(center, px * 0.18f), Offset(px * 0.24f, px * 0.44f), stroke)
        drawLine(color, Offset(center, px * 0.18f), Offset(px * 0.76f, px * 0.44f), stroke)
    }
}

@Composable
internal fun DownGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val center = px / 2f
        val stroke = strokeWidth.toPx()
        drawLine(color, Offset(center, px * 0.18f), Offset(center, px * 0.80f), stroke)
        drawLine(color, Offset(center, px * 0.80f), Offset(px * 0.24f, px * 0.54f), stroke)
        drawLine(color, Offset(center, px * 0.80f), Offset(px * 0.76f, px * 0.54f), stroke)
    }
}

/**
 * Understated heart glyph. Draws an outline when [filled] is false and a filled
 * silhouette when true. Reference is favor of a subtle line-weight heart, not a
 * flashy filled control (see docs/ui-reference).
 */
@Composable
internal fun HeartGlyph(
    size: Dp,
    color: Color,
    strokeWidth: Dp,
    filled: Boolean,
) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val path = heartPath(Rect(offset = Offset.Zero, size = Size(px, px)))
        if (filled) {
            drawPath(path = path, color = color)
        } else {
            drawPath(path = path, color = color, style = Stroke(width = strokeWidth.toPx()))
        }
    }
}

private fun heartPath(bounds: Rect): Path {
    val w = bounds.width
    val h = bounds.height
    val left = bounds.left
    val top = bounds.top
    // Two-cubic heart shape sized to bounds.
    return Path().apply {
        moveTo(left + w * 0.5f, top + h * 0.88f)
        cubicTo(
            left + w * -0.05f, top + h * 0.60f,
            left + w * 0.10f, top + h * 0.05f,
            left + w * 0.5f, top + h * 0.30f,
        )
        cubicTo(
            left + w * 0.90f, top + h * 0.05f,
            left + w * 1.05f, top + h * 0.60f,
            left + w * 0.5f, top + h * 0.88f,
        )
        close()
    }
}
