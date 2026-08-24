package com.vaibhav.relive.ui.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.IconButton
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveOpacity
import com.vaibhav.relive.ui.theme.ReliveDimensions

enum class ReliveTopLevelDestination { Timelines, Rediscover, Search }

internal data class FloatingToolbarDestinations(
    val leading: List<ReliveTopLevelDestination>,
    val active: ReliveTopLevelDestination,
    val trailing: List<ReliveTopLevelDestination>,
)

internal fun floatingToolbarDestinations(
    selected: ReliveTopLevelDestination,
): FloatingToolbarDestinations {
    val destinations = ReliveTopLevelDestination.entries
    val selectedIndex = destinations.indexOf(selected)
    return FloatingToolbarDestinations(
        leading = destinations.take(selectedIndex),
        active = selected,
        trailing = destinations.drop(selectedIndex + 1),
    )
}

@Immutable
internal data class FloatingToolbarLayout(
    val navigationExpandedWidth: Dp,
    val newExpandedWidth: Dp,
)

internal fun floatingToolbarLayout(
    availableWidth: Dp,
    dimensions: ReliveDimensions,
): FloatingToolbarLayout {
    val floating = dimensions.floatingToolbar
    val rowWidth = availableWidth - dimensions.spacing.lg * 2
    val navigationMinimum = dimensions.minTouchTarget * ReliveTopLevelDestination.entries.size +
        dimensions.spacing.xs * 2
    val newMinimum = floating.compactWidth
    val availableForNew = rowWidth - floating.controlGap - navigationMinimum
    val newExpandedWidth = availableForNew.coerceIn(newMinimum, floating.newExpandedWidth)
    return FloatingToolbarLayout(
        navigationExpandedWidth = (rowWidth - floating.controlGap - newExpandedWidth)
            .coerceAtLeast(navigationMinimum),
        newExpandedWidth = newExpandedWidth,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReliveFloatingBottomControls(
    selected: ReliveTopLevelDestination,
    expanded: Boolean,
    onSelect: (ReliveTopLevelDestination) -> Unit,
    onCreateMoment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = floatingToolbarLayout(maxWidth, dims)
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(
                    start = dims.spacing.lg,
                    end = dims.spacing.lg,
                    bottom = dims.spacing.lg,
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReliveFloatingNavigationToolbar(
                selected = selected,
                expanded = expanded,
                onSelect = onSelect,
                expandedWidth = layout.navigationExpandedWidth,
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(dims.floatingToolbar.controlGap))
            GlobalNewMomentButton(
                onClick = onCreateMoment,
                expanded = expanded,
                expandedWidth = layout.newExpandedWidth,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReliveFloatingNavigationToolbar(
    selected: ReliveTopLevelDestination,
    expanded: Boolean,
    onSelect: (ReliveTopLevelDestination) -> Unit,
    expandedWidth: Dp,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val toolbarWidth by animateDpAsState(
        targetValue = if (expanded) expandedWidth else dims.floatingToolbar.compactWidth,
        animationSpec = tween(
            durationMillis = ReliveTheme.motion.durations.standardMillis,
            easing = ReliveTheme.motion.easings.standard,
        ),
        label = "navigation toolbar width",
    )
    HorizontalFloatingToolbar(
        expanded = expanded,
        modifier = Modifier
            .width(toolbarWidth)
            .height(dims.floatingToolbar.height),
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = colors.surfaceFloating,
            toolbarContentColor = colors.accent,
        ),
        contentPadding = PaddingValues(dims.spacing.xs),
        shape = RoundedCornerShape(dims.radii.pill),
    ) {
        FloatingNavigationContent(
            selected = selected,
            expanded = expanded,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun FloatingNavigationContent(
    selected: ReliveTopLevelDestination,
    expanded: Boolean,
    onSelect: (ReliveTopLevelDestination) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val selectedIndex = ReliveTopLevelDestination.entries.indexOf(selected)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.minTouchTarget),
    ) {
        val actionWidth = if (expanded) maxWidth / ReliveTopLevelDestination.entries.size else maxWidth
        val indicatorOffset by animateDpAsState(
            targetValue = if (expanded) actionWidth * selectedIndex else 0.dp,
            animationSpec = tween(
                durationMillis = ReliveTheme.motion.durations.standardMillis,
                easing = ReliveTheme.motion.easings.standard,
            ),
            label = "selected destination indicator",
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = indicatorOffset)
                .width(actionWidth)
                .height(dims.floatingToolbar.indicatorHeight)
                .clip(RoundedCornerShape(dims.radii.pill))
                .background(ReliveTheme.colors.accent.copy(alpha = ReliveOpacity.Low)),
        )
        if (expanded) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ReliveTopLevelDestination.entries.forEach { destination ->
                    DestinationAction(
                        destination = destination,
                        selected = selected,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            DestinationAction(
                destination = selected,
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DestinationAction(
    destination: ReliveTopLevelDestination,
    selected: ReliveTopLevelDestination,
    onSelect: (ReliveTopLevelDestination) -> Unit,
    modifier: Modifier,
) {
    val isSelected = selected == destination
    val tint = if (isSelected) ReliveTheme.colors.accent else ReliveTheme.colors.textMuted
    val haptics = rememberReliveHaptics()
    IconButton(
        onClick = {
            if (!isSelected) haptics.perform(ReliveHapticCue.Selection)
            onSelect(destination)
        },
        modifier = modifier
            .semantics {
                contentDescription = destination.label
                this.selected = isSelected
            },
    ) {
        DestinationGlyph(destination, tint)
    }
}

private val ReliveTopLevelDestination.label: String
    get() = when (this) {
        ReliveTopLevelDestination.Timelines -> "Timelines"
        ReliveTopLevelDestination.Rediscover -> "Rediscover"
        ReliveTopLevelDestination.Search -> "Search"
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
