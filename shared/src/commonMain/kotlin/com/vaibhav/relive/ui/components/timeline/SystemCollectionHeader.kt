package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun SystemCollectionHeader(title: String, onBack: () -> Unit) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(dims.minTouchTarget)
                .semantics { contentDescription = "Back" },
        ) {
            BackGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
        }
        Text(
            text = title,
            style = ReliveTheme.typography.title,
            color = colors.textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
