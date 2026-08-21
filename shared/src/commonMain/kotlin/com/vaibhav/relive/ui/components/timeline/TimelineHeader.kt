package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Reference-matching app header: menu on the left, centered serif italic wordmark,
 * search on the right. Buttons occupy the min touch target; glyphs sit inside.
 */
@Composable
fun TimelineHeader(
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .semantics { contentDescription = "Settings" },
            ) {
                GearGlyph(size = dims.icon.lg, color = colors.textSecondary, strokeWidth = dims.stroke.icon)
            }
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .semantics { contentDescription = "Search" },
            ) {
                SearchGlyph(size = dims.icon.md, color = colors.textSecondary, strokeWidth = dims.stroke.icon)
            }
        }
        Text(
            text = "Relive",
            style = type.wordmark,
            color = colors.accent,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
