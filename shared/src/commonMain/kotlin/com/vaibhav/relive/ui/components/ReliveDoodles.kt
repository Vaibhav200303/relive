package com.vaibhav.relive.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Line-art doodles for empty states and onboarding — the one place personality illustration
 * earns its keep, where there is otherwise nothing to show. They are drawn as strokes in the
 * theme's accent color so they belong to every palette, and deliberately kept as accents: never
 * scattered across populated screens (see the app visual direction).
 */
object ReliveDoodles {

    /** An open journal with a few written lines and a small spark — "your story starts here". */
    @Composable
    fun OpenJournal(
        modifier: Modifier = Modifier,
        size: Dp = 96.dp,
        color: Color = ReliveTheme.colors.accentMuted,
    ) {
        val stroke = ReliveTheme.dimensions.stroke.iconBold
        Canvas(modifier = modifier.size(size)) {
            val s = Stroke(width = stroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val w = this.size.width
            val h = this.size.height
            val spineX = w * 0.5f
            val topY = h * 0.40f
            val bottomY = h * 0.86f
            val pageDrop = h * 0.10f

            // Left page: outer spine down, out to the left edge, back up with a soft top curve.
            drawPath(
                Path().apply {
                    moveTo(spineX, topY)
                    lineTo(spineX, bottomY)
                    lineTo(w * 0.12f, bottomY - pageDrop)
                    lineTo(w * 0.12f, topY - pageDrop * 0.4f)
                    quadraticBezierTo(w * 0.30f, topY - pageDrop, spineX, topY)
                },
                color = color,
                style = s,
            )
            // Right page mirrors the left.
            drawPath(
                Path().apply {
                    moveTo(spineX, topY)
                    lineTo(spineX, bottomY)
                    lineTo(w * 0.88f, bottomY - pageDrop)
                    lineTo(w * 0.88f, topY - pageDrop * 0.4f)
                    quadraticBezierTo(w * 0.70f, topY - pageDrop, spineX, topY)
                },
                color = color,
                style = s,
            )
            // Two short written lines on each page.
            val lineColor = color.copy(alpha = 0.7f)
            val ls = Stroke(width = stroke.toPx() * 0.8f, cap = StrokeCap.Round)
            listOf(0.56f, 0.66f).forEach { fy ->
                drawLine(lineColor, Offset(w * 0.20f, h * fy), Offset(w * 0.42f, h * (fy - 0.02f)), ls.width, StrokeCap.Round)
                drawLine(lineColor, Offset(w * 0.58f, h * (fy - 0.02f)), Offset(w * 0.80f, h * fy), ls.width, StrokeCap.Round)
            }
            // A small four-point spark rising from the page.
            spark(center = Offset(w * 0.74f, h * 0.20f), radius = w * 0.09f, color = color, strokePx = stroke.toPx())
        }
    }

    /** A framed photo with a horizon and sun — "the moments you keep will live here". */
    @Composable
    fun FramedMemory(
        modifier: Modifier = Modifier,
        size: Dp = 96.dp,
        color: Color = ReliveTheme.colors.accentMuted,
    ) {
        val stroke = ReliveTheme.dimensions.stroke.iconBold
        Canvas(modifier = modifier.size(size)) {
            val s = Stroke(width = stroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val w = this.size.width
            val h = this.size.height
            val left = w * 0.20f
            val top = h * 0.22f
            val frameW = w * 0.60f
            val frameH = h * 0.56f

            // Outer polaroid-style frame with a wider caption strip at the bottom.
            drawPath(
                Path().apply {
                    moveTo(left, top)
                    lineTo(left + frameW, top)
                    lineTo(left + frameW, top + frameH)
                    lineTo(left, top + frameH)
                    close()
                },
                color = color,
                style = s,
            )
            val photoBottom = top + frameH * 0.66f
            drawLine(color, Offset(left, photoBottom), Offset(left + frameW, photoBottom), s.width, StrokeCap.Round)

            // Horizon hill inside the photo window.
            drawPath(
                Path().apply {
                    moveTo(left, photoBottom)
                    quadraticBezierTo(left + frameW * 0.35f, photoBottom - frameH * 0.34f, left + frameW * 0.62f, photoBottom - frameH * 0.10f)
                    quadraticBezierTo(left + frameW * 0.80f, photoBottom - frameH * 0.02f, left + frameW, photoBottom - frameH * 0.16f)
                },
                color = color.copy(alpha = 0.75f),
                style = Stroke(width = stroke.toPx() * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Sun.
            drawCircle(color = color, radius = w * 0.055f, center = Offset(left + frameW * 0.72f, top + frameH * 0.22f), style = Stroke(stroke.toPx() * 0.85f))
            // A small spark by the corner for warmth.
            spark(center = Offset(left + frameW + w * 0.06f, top - h * 0.04f), radius = w * 0.07f, color = color, strokePx = stroke.toPx())
        }
    }

    /** A simple four-point sparkle centered at [center]. */
    private fun DrawScope.spark(center: Offset, radius: Float, color: Color, strokePx: Float) {
        val s = Stroke(width = strokePx * 0.85f, cap = StrokeCap.Round)
        drawLine(color, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), s.width, StrokeCap.Round)
        drawLine(color, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), s.width, StrokeCap.Round)
    }
}
