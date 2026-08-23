package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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

/** Timeline detail header with optional back navigation and a centered wordmark. */
@Composable
fun TimelineHeader(
    onBack: (() -> Unit)? = null,
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
    }
}
