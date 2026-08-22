package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun EmptyTimelinePlaceholder(modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dims.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
        ) {
            Text(
                text = "Your timeline is waiting.",
                style = type.title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "The moments you keep will live here.",
                style = type.subtitle,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun EmptyCustomTimelinePlaceholder(
    timelineName: String,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dims.timeline.contentInset,
                top = dims.spacing.huge,
                end = dims.spacing.sm,
                bottom = dims.spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        Text(
            text = "$timelineName is waiting for its first Moment.",
            style = type.title,
            color = colors.textPrimary,
        )
        Text(
            text = "Use the + below to begin this chapter.",
            style = type.subtitle,
            color = colors.textSecondary,
        )
    }
}
