package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.vaibhav.relive.ui.components.ReliveDoodles
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun EmptyTimelinePlaceholder(modifier: Modifier = Modifier) {
    EditorialEmptyTimelineState(
        title = "Your timeline is waiting.",
        message = "The moments you keep will live here.",
        modifier = modifier,
    )
}

@Composable
fun EmptyCustomTimelinePlaceholder(
    timelineName: String,
    modifier: Modifier = Modifier,
) {
    EditorialEmptyTimelineState(
        title = "$timelineName is waiting for its first Moment.",
        message = "Use the + below to begin this chapter.",
        doodle = { ReliveDoodles.FramedMemory() },
        modifier = modifier,
    )
}

@Composable
fun EmptyCustomTimelinesPlaceholder(modifier: Modifier = Modifier) {
    EditorialEmptyTimelineState(
        title = "A new chapter can begin whenever you are ready.",
        message = "Use + to create your first timeline.",
        modifier = modifier,
    )
}

@Composable
private fun EditorialEmptyTimelineState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    doodle: @Composable () -> Unit = { ReliveDoodles.OpenJournal() },
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dims.timeline.coverHeroHeight)
            .padding(vertical = dims.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceCardTranslucent, RoundedCornerShape(dims.radii.largeIncreased))
                .border(
                    width = dims.stroke.cardOuter,
                    color = colors.borderMuted,
                    shape = RoundedCornerShape(dims.radii.largeIncreased),
                )
                .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
        ) {
            doodle()
            Text(
                text = title,
                style = ReliveTheme.typography.title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = ReliveTheme.typography.subtitle,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
