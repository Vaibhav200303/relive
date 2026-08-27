package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.icons.TimelineActionIcons

/** Timeline detail header with optional back navigation and a centered wordmark. */
@Composable
fun TimelineHeader(
    onBack: (() -> Unit)? = null,
    onJumpToDate: (() -> Unit)? = null,
    onChangeTheme: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .align(Alignment.CenterStart)
                    .semantics { contentDescription = "Back to Timeline Home" },
            ) {
                BackGlyph(size = dims.icon.lg, color = colors.textSecondary, strokeWidth = dims.stroke.icon)
            }
        }
        Text(
            text = "Relive",
            style = type.wordmark,
            color = colors.accent,
            modifier = Modifier.align(Alignment.Center),
        )
        if (onJumpToDate != null || onChangeTheme != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onChangeTheme != null) {
                    IconButton(
                        onClick = onChangeTheme,
                        modifier = Modifier
                            .size(dims.minTouchTarget)
                            .semantics { contentDescription = "Change timeline theme" },
                    ) {
                        PaletteGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
                    }
                }
                if (onJumpToDate != null) {
                    IconButton(
                        onClick = onJumpToDate,
                        modifier = Modifier
                            .size(dims.minTouchTarget)
                            .semantics { contentDescription = "Jump to date" },
                    ) {
                        CalendarGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
                    }
                }
            }
        }
    }
}

/** A temporary All-timeline action bar for one long-pressed Moment. */
@Composable
fun TimelineMomentActionHeader(
    showEdit: Boolean,
    showAddToTimeline: Boolean,
    addToTimelineEnabled: Boolean,
    showForget: Boolean,
    onExit: () -> Unit,
    onEdit: () -> Unit,
    onAddToTimeline: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .size(dims.minTouchTarget)
                .align(Alignment.CenterStart)
                .semantics { contentDescription = "Exit moment selection" },
        ) {
            BackGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showEdit) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Edit moment" },
                ) {
                    Icon(TimelineActionIcons.Rename, contentDescription = null, tint = colors.textSecondary)
                }
            }
            if (showAddToTimeline) {
                IconButton(
                    onClick = onAddToTimeline,
                    enabled = addToTimelineEnabled,
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Add to timeline" },
                ) {
                    Icon(TimelineActionIcons.AddToTimeline, contentDescription = null, tint = colors.textSecondary)
                }
            }
            if (showForget) {
                IconButton(
                    onClick = onForget,
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Forget moment" },
                ) {
                    Icon(TimelineActionIcons.Delete, contentDescription = null, tint = colors.actionDestructive)
                }
            }
        }
    }
}

@Composable
private fun PaletteGlyph(
    size: androidx.compose.ui.unit.Dp,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
) {
    Canvas(Modifier.size(size)) {
        val stroke = Stroke(strokeWidth.toPx())
        drawCircle(color, radius = this.size.minDimension * 0.34f, style = stroke)
        listOf(
            Offset(this.size.width * 0.50f, this.size.height * 0.26f),
            Offset(this.size.width * 0.70f, this.size.height * 0.48f),
            Offset(this.size.width * 0.38f, this.size.height * 0.68f),
        ).forEach { center ->
            drawCircle(color, radius = this.size.minDimension * 0.035f, center = center)
        }
    }
}
