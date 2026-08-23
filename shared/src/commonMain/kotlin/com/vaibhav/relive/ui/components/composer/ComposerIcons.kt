package com.vaibhav.relive.ui.components.composer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

@Composable
internal fun PlusGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val cx = px / 2f
        val cy = px / 2f
        val arm = px * 0.34f
        drawLine(color = color, start = Offset(cx - arm, cy), end = Offset(cx + arm, cy), strokeWidth = sw)
        drawLine(color = color, start = Offset(cx, cy - arm), end = Offset(cx, cy + arm), strokeWidth = sw)
    }
}

@Composable
internal fun CloseGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val inset = px * 0.22f
        drawLine(color = color, start = Offset(inset, inset), end = Offset(px - inset, px - inset), strokeWidth = sw)
        drawLine(color = color, start = Offset(px - inset, inset), end = Offset(inset, px - inset), strokeWidth = sw)
    }
}

@Composable
internal fun PinGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val cx = px / 2f
        drawCircle(color = color, radius = px * 0.16f, center = Offset(cx, px * 0.42f), style = Stroke(width = sw))
        drawCircle(color = color, radius = px * 0.36f, center = Offset(cx, px * 0.42f), style = Stroke(width = sw))
    }
}

@Composable
internal fun ImageGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val inset = px * 0.14f
        drawRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(px - inset * 2, px - inset * 2),
            style = Stroke(width = sw),
        )
        drawCircle(color = color, radius = px * 0.06f, center = Offset(px * 0.36f, px * 0.36f), style = Stroke(width = sw))
        // Mountain-ish diagonal line.
        drawLine(
            color = color,
            start = Offset(inset, px - inset),
            end = Offset(px * 0.55f, px * 0.5f),
            strokeWidth = sw,
        )
        drawLine(
            color = color,
            start = Offset(px * 0.55f, px * 0.5f),
            end = Offset(px - inset, px - inset),
            strokeWidth = sw,
        )
    }
}

@Composable
internal fun VideoGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val inset = px * 0.16f
        drawRect(
            color = color,
            topLeft = Offset(inset, px * 0.28f),
            size = androidx.compose.ui.geometry.Size(px * 0.5f, px * 0.44f),
            style = Stroke(width = sw),
        )
        // Play notch on the right.
        drawLine(
            color = color,
            start = Offset(px * 0.68f, px * 0.36f),
            end = Offset(px - inset, px * 0.28f),
            strokeWidth = sw,
        )
        drawLine(
            color = color,
            start = Offset(px * 0.68f, px * 0.64f),
            end = Offset(px - inset, px * 0.72f),
            strokeWidth = sw,
        )
        drawLine(
            color = color,
            start = Offset(px - inset, px * 0.28f),
            end = Offset(px - inset, px * 0.72f),
            strokeWidth = sw,
        )
    }
}

@Composable
internal fun CameraGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(px * 0.08f, px * 0.27f),
            size = Size(px * 0.84f, px * 0.62f),
            cornerRadius = CornerRadius(px * 0.1f),
            style = Stroke(width = sw),
        )
        drawLine(color, Offset(px * 0.31f, px * 0.27f), Offset(px * 0.4f, px * 0.14f), sw)
        drawLine(color, Offset(px * 0.4f, px * 0.14f), Offset(px * 0.64f, px * 0.14f), sw)
        drawLine(color, Offset(px * 0.64f, px * 0.14f), Offset(px * 0.73f, px * 0.27f), sw)
        drawCircle(
            color = color,
            radius = px * 0.18f,
            center = Offset(px * 0.52f, px * 0.58f),
            style = Stroke(width = sw),
        )
    }
}

@Composable
internal fun GalleryGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val corner = CornerRadius(px * 0.08f)
        drawRoundRect(
            color = color,
            topLeft = Offset(px * 0.24f, px * 0.1f),
            size = Size(px * 0.66f, px * 0.66f),
            cornerRadius = corner,
            style = Stroke(width = sw),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(px * 0.1f, px * 0.24f),
            size = Size(px * 0.66f, px * 0.66f),
            cornerRadius = corner,
            style = Stroke(width = sw),
        )
        drawCircle(
            color = color,
            radius = px * 0.055f,
            center = Offset(px * 0.32f, px * 0.44f),
            style = Stroke(width = sw),
        )
        drawLine(color, Offset(px * 0.16f, px * 0.82f), Offset(px * 0.43f, px * 0.57f), sw)
        drawLine(color, Offset(px * 0.43f, px * 0.57f), Offset(px * 0.7f, px * 0.82f), sw)
    }
}

@Composable
internal fun MicGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val cx = px / 2f
        // Capsule body.
        val bodyW = px * 0.22f
        val bodyH = px * 0.44f
        val top = px * 0.14f
        drawLine(color = color, start = Offset(cx - bodyW / 2, top + bodyW / 2), end = Offset(cx - bodyW / 2, top + bodyH - bodyW / 2), strokeWidth = sw)
        drawLine(color = color, start = Offset(cx + bodyW / 2, top + bodyW / 2), end = Offset(cx + bodyW / 2, top + bodyH - bodyW / 2), strokeWidth = sw)
        drawCircle(color = color, radius = bodyW / 2, center = Offset(cx, top + bodyW / 2), style = Stroke(width = sw))
        drawCircle(color = color, radius = bodyW / 2, center = Offset(cx, top + bodyH - bodyW / 2), style = Stroke(width = sw))
        // Stand.
        drawLine(color = color, start = Offset(cx, top + bodyH + px * 0.06f), end = Offset(cx, px * 0.86f), strokeWidth = sw)
        drawLine(color = color, start = Offset(cx - px * 0.14f, px * 0.86f), end = Offset(cx + px * 0.14f, px * 0.86f), strokeWidth = sw)
    }
}
