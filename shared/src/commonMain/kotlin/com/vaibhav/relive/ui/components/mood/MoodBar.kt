package com.vaibhav.relive.ui.components.mood

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MoodWeekSummary
import com.vaibhav.relive.presentation.insights.moodVerdictLabel
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * The week-over-week mood bar (PRODUCT_SPEC §10A.3). Two cells split by a dashed rule,
 * each carrying its label, its face and its one-word verdict.
 *
 * The bar is the fixed anchor of the Mood insights interaction: it is revealed inside
 * Home's expanded backdrop and must hold its position when [isInsightsOpen] flips, so
 * opening insights only lifts it — it never moves, resizes its content, or reflows.
 */
@Composable
fun MoodBar(
    lastWeek: MoodWeekSummary?,
    thisWeek: MoodWeekSummary?,
    isInsightsOpen: Boolean,
    onToggleInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val shape = RoundedCornerShape(dims.radii.xl)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isInsightsOpen) dims.timelineHome.cardElevation else 0.dp,
                shape = shape,
                ambientColor = colors.shadow,
                spotColor = colors.shadow,
            )
            .clip(shape)
            .background(colors.surfaceCard)
            .border(BorderStroke(dims.stroke.hairline, colors.borderMuted), shape)
            .clickable(onClick = onToggleInsights)
            .semantics {
                contentDescription = if (isInsightsOpen) {
                    "Close mood insights"
                } else {
                    "Open mood insights"
                }
            }
            // Wrap to the cells' own height so the dashed divider's fillMaxHeight resolves to
            // the content, not the full-screen constraint the backdrop Column hands down.
            .height(IntrinsicSize.Min)
            .padding(vertical = dims.spacing.md, horizontal = dims.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoodBarCell(
            label = "LAST WEEK",
            summary = lastWeek,
            animationDelayMillis = 0,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .width(dims.stroke.hairline)
                .fillMaxHeight()
                .heightIn(min = MoodBarCellHeight)
                .drawBehind {
                    drawLine(
                        color = colors.borderMuted,
                        start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                        strokeWidth = size.width,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
                    )
                },
        )
        MoodBarCell(
            label = "THIS WEEK",
            summary = thisWeek,
            animationDelayMillis = 700,
            modifier = Modifier.weight(1f),
        )
    }
}

private val MoodBarCellHeight = 92.dp
private val MoodBarFaceSize = 44.dp

@Composable
private fun MoodBarCell(
    label: String,
    summary: MoodWeekSummary?,
    animationDelayMillis: Int,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier.heightIn(min = MoodBarCellHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = type.eyebrow,
            color = colors.accentMuted,
        )
        Spacer(Modifier.height(dims.spacing.xs))
        if (summary == null) {
            // A week with nothing felt says so quietly rather than inventing a face.
            Box(
                modifier = Modifier
                    .height(MoodBarFaceSize)
                    .semantics { contentDescription = "No moments felt" },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "—", style = type.display, color = colors.textMuted)
            }
        } else {
            FeelingFace(
                feeling = summary.verdict,
                size = MoodBarFaceSize,
                animated = true,
                animationDelayMillis = animationDelayMillis,
            )
        }
        Spacer(Modifier.height(dims.spacing.xs))
        Text(
            text = summary?.let { moodVerdictLabel(it.verdict) } ?: "No moments",
            style = type.subtitle,
            color = if (summary == null) colors.textMuted else colors.accent,
        )
    }
}
