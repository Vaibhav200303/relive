package com.vaibhav.relive.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.ui.theme.ReliveTheme

enum class ReliveTopLevelDestination { Timelines, Rediscover, Search }

@Composable
fun ReliveBottomBar(
    selected: ReliveTopLevelDestination,
    onSelect: (ReliveTopLevelDestination) -> Unit,
) {
    val colors = ReliveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = ReliveTheme.dimensions.spacing.lg, vertical = ReliveTheme.dimensions.spacing.xs),
    ) {
        ReliveTopLevelDestination.entries.forEach { destination ->
            val label = when (destination) {
                ReliveTopLevelDestination.Timelines -> "Timelines"
                ReliveTopLevelDestination.Rediscover -> "Rediscover"
                ReliveTopLevelDestination.Search -> "Search"
            }
            val isSelected = selected == destination
            val tint = if (isSelected) colors.accent else colors.textMuted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ReliveTheme.dimensions.minTouchTarget)
                    .clickable { onSelect(destination) }
                    .semantics {
                        contentDescription = label
                        this.selected = isSelected
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DestinationGlyph(destination, tint)
                Text(label, style = ReliveTheme.typography.tag, color = tint)
            }
        }
    }
}

@Composable
private fun DestinationGlyph(destination: ReliveTopLevelDestination, tint: Color) {
    val dims = ReliveTheme.dimensions
    Canvas(Modifier.size(dims.icon.lg)) {
        val stroke = Stroke(width = dims.stroke.iconBold.toPx())
        val inset = size.width * 0.13f
        if (destination == ReliveTopLevelDestination.Timelines) {
            val cell = size.width * 0.28f
            listOf(0f, 1f).forEach { x ->
                listOf(0f, 1f).forEach { y ->
                    drawRect(tint, topLeft = androidx.compose.ui.geometry.Offset(inset + x * (cell + inset), inset + y * (cell + inset)), size = androidx.compose.ui.geometry.Size(cell, cell), style = stroke)
                }
            }
        } else if (destination == ReliveTopLevelDestination.Rediscover) {
            drawCircle(tint, radius = size.width * 0.34f, style = stroke)
            drawLine(tint, androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f), androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.27f), strokeWidth = stroke.width)
            drawLine(tint, androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f), androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.62f), strokeWidth = stroke.width)
        } else {
            val radius = size.width * 0.25f
            val center = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.42f)
            drawCircle(tint, radius = radius, center = center, style = stroke)
            drawLine(
                tint,
                start = center + androidx.compose.ui.geometry.Offset(radius * 0.7f, radius * 0.7f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height * 0.86f),
                strokeWidth = stroke.width,
            )
        }
    }
}
