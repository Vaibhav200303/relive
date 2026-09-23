package com.vaibhav.relive.ui.components.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun DownloadGlyph(modifier: Modifier = Modifier, color: Color = ViewerActionInk) {
    Canvas(modifier) {
        val stroke = 2.dp.toPx()
        drawLine(color, center.copy(y = size.height * .18f), center.copy(y = size.height * .66f), stroke, StrokeCap.Round)
        drawLine(color, center.copy(y = size.height * .66f), center.copy(x = size.width * .31f, y = size.height * .48f), stroke, StrokeCap.Round)
        drawLine(color, center.copy(y = size.height * .66f), center.copy(x = size.width * .69f, y = size.height * .48f), stroke, StrokeCap.Round)
        drawLine(color, center.copy(x = size.width * .22f, y = size.height * .82f), center.copy(x = size.width * .78f, y = size.height * .82f), stroke, StrokeCap.Round)
    }
}

@Composable
internal fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(30.dp).background(if (selected) ViewerActionInk else Color(0xB3FFFFFF), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            if (selected) {
                drawLine(Color.White, size.run { androidx.compose.ui.geometry.Offset(width * .2f, height * .52f) }, size.run { androidx.compose.ui.geometry.Offset(width * .43f, height * .75f) }, 2.5.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, size.run { androidx.compose.ui.geometry.Offset(width * .43f, height * .75f) }, size.run { androidx.compose.ui.geometry.Offset(width * .82f, height * .28f) }, 2.5.dp.toPx(), StrokeCap.Round)
            } else {
                drawCircle(Color(0xFF49454F), style = Stroke(1.5.dp.toPx()))
            }
        }
    }
}

internal val ViewerActionInk = Color(0xFF23202B)
