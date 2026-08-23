package com.vaibhav.relive.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaibhav.relive.ui.theme.ReliveTheme

/** Shared top-level identity bar for Relive roots. */
@Composable
fun ReliveWordmarkAppBar(
    modifier: Modifier = Modifier,
    action: @Composable BoxScope.() -> Unit = {},
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
        Text(
            text = "Relive",
            style = ReliveTheme.typography.wordmark,
            color = colors.accent,
            modifier = Modifier.align(Alignment.Center),
        )
        action()
    }
}
