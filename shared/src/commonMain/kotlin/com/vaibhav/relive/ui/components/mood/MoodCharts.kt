package com.vaibhav.relive.ui.components.mood

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MoodInsights
import com.vaibhav.relive.presentation.insights.MOOD_WEEKDAY_LABELS
import com.vaibhav.relive.presentation.insights.moodMonthLabel
import com.vaibhav.relive.ui.theme.ReliveTheme

/** One plotted position: an axis [label], an optional score, and an optional face to ride the curve. */
internal data class MoodChartPoint(
    val label: String,
    val score: Float?,
    val face: MomentFeeling?,
)

private val ChartHeight = 200.dp
private val ChartFaceSize = 28.dp
private val AxisLabelWidth = 44.dp
private val AxisLabelAreaHeight = 28.dp

/**
 * `Weekly mood` (PRODUCT_SPEC §10A.4): the current week's per-day averages as a soft
 * filled curve with a face riding it on every day that has felt Moments.
 */
@Composable
fun WeeklyMoodPanel(
    insights: MoodInsights,
    todayIndex: Int,
    modifier: Modifier = Modifier,
) {
    MoodChartPanel(title = "Weekly mood", modifier = modifier) {
        MoodAreaChart(
            points = insights.weekDays.mapIndexed { index, day ->
                MoodChartPoint(
                    label = MOOD_WEEKDAY_LABELS.getOrElse(index) { "" },
                    score = day.averageScore,
                    face = day.verdict,
                )
            },
            showFaceAxis = false,
            highlightIndex = todayIndex,
        )
    }
}

/**
 * `Mood over time`: six monthly averages under the same curve treatment, with the three
 * faces standing as the left axis instead of riding the line.
 */
@Composable
fun MoodOverTimePanel(
    insights: MoodInsights,
    modifier: Modifier = Modifier,
) {
    MoodChartPanel(title = "Mood over time", modifier = modifier) {
        MoodAreaChart(
            points = insights.months.map { month ->
                MoodChartPoint(
                    label = moodMonthLabel(month.month),
                    score = month.averageScore,
                    face = null,
                )
            },
            showFaceAxis = true,
            highlightIndex = insights.months.lastIndex,
        )
    }
}

@Composable
private fun MoodChartPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val shape = RoundedCornerShape(dims.radii.large)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceCard)
            .border(BorderStroke(dims.stroke.hairline, colors.borderMuted), shape)
            .padding(
                start = dims.spacing.md,
                end = dims.spacing.md,
                top = dims.spacing.md,
                bottom = dims.spacing.sm,
            ),
    ) {
        Text(
            text = title,
            style = type.title,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = dims.spacing.xs),
        )
        Spacer(Modifier.height(dims.spacing.xs))
        content()
    }
}

/**
 * The shared curve. One scale places the fill, the line, every face and every label:
 * scores run 1..3 bottom to top, and positions are interpolated across days or months
 * with no felt Moments rather than being drawn at a fabricated zero.
 */
@Composable
private fun MoodAreaChart(
    points: List<MoodChartPoint>,
    showFaceAxis: Boolean,
    highlightIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions

    val padStart = if (showFaceAxis) 36.dp else 12.dp
    val padEnd = 12.dp
    val padTop = 14.dp
    // Reserve a distinct x-axis strip so a Low face never enters the day/month labels.
    val padBottom = AxisLabelAreaHeight + ChartFaceSize / 2 + 6.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight),
    ) {
        val width = maxWidth
        val height = maxHeight
        val plotWidth = width - padStart - padEnd
        val plotHeight = height - padTop - padBottom
        val lastIndex = (points.size - 1).coerceAtLeast(1)

        fun xOf(index: Int): Dp = padStart + plotWidth * (index.toFloat() / lastIndex)
        fun yOf(score: Float): Dp = padTop + plotHeight * ((3f - score) / 2f)

        val plotted = points.withIndex().filter { it.value.score != null }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val startPx = padStart.toPx()
            val endPx = size.width - padEnd.toPx()
            val topPx = padTop.toPx()
            val bottomPx = size.height - padBottom.toPx()
            fun px(index: Int): Float =
                startPx + (endPx - startPx) * (index.toFloat() / lastIndex)
            fun py(score: Float): Float = topPx + (bottomPx - topPx) * ((3f - score) / 2f)

            // Score guides at Low / Good / Great, so the curve's height is readable.
            for (score in 1..3) {
                drawLine(
                    color = colors.borderMuted,
                    start = Offset(startPx, py(score.toFloat())),
                    end = Offset(endPx, py(score.toFloat())),
                    strokeWidth = dims.stroke.hairline.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 10f)),
                )
            }

            if (plotted.size >= 2) {
                val nodes = plotted.map { (index, point) ->
                    Offset(px(index), py(point.score!!))
                }
                val line = Path().apply {
                    moveTo(nodes.first().x, nodes.first().y)
                    for (i in 0 until nodes.size - 1) {
                        val previous = nodes[(i - 1).coerceAtLeast(0)]
                        val current = nodes[i]
                        val next = nodes[i + 1]
                        val following = nodes[(i + 2).coerceAtMost(nodes.size - 1)]
                        // Catmull-Rom control points: a calm curve that still passes
                        // exactly through every real measurement.
                        cubicTo(
                            current.x + (next.x - previous.x) / 6f,
                            current.y + (next.y - previous.y) / 6f,
                            next.x - (following.x - current.x) / 6f,
                            next.y - (following.y - current.y) / 6f,
                            next.x,
                            next.y,
                        )
                    }
                }
                val floor = bottomPx + 8f
                val area = Path().apply {
                    addPath(line)
                    lineTo(nodes.last().x, floor)
                    lineTo(nodes.first().x, floor)
                    close()
                }
                drawPath(
                    path = area,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            FeelingFaceColors.chartFill.copy(alpha = 0.32f),
                            FeelingFaceColors.chartFill.copy(alpha = 0.03f),
                        ),
                        startY = topPx,
                        endY = floor,
                    ),
                )
                drawPath(
                    path = line,
                    color = FeelingFaceColors.chartStroke.copy(alpha = 0.65f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
                )
            } else if (plotted.size == 1) {
                val (index, point) = plotted.first()
                drawCircle(
                    color = FeelingFaceColors.chartStroke.copy(alpha = 0.5f),
                    radius = 3.dp.toPx(),
                    center = Offset(px(index), py(point.score!!)),
                )
            }

            // The emphasized axis mark under today / the current month.
            if (highlightIndex in points.indices) {
                val markWidth = 18.dp.toPx()
                drawRoundRect(
                    color = FeelingFaceColors.chartFill,
                    topLeft = Offset(px(highlightIndex) - markWidth / 2f, size.height - 5.dp.toPx()),
                    size = Size(markWidth, 2.4.dp.toPx()),
                    cornerRadius = CornerRadius(1.2.dp.toPx()),
                )
            }
        }

        if (showFaceAxis) {
            listOf(MomentFeeling.Great, MomentFeeling.Good, MomentFeeling.Low).forEach { feeling ->
                FeelingFace(
                    feeling = feeling,
                    size = ChartFaceSize,
                    modifier = Modifier.offset(
                        x = padStart / 2 - ChartFaceSize / 2,
                        y = yOf(feeling.score.toFloat()) - ChartFaceSize / 2,
                    ),
                )
            }
        }

        plotted.forEach { (index, point) ->
            val face = point.face ?: return@forEach
            if (showFaceAxis) return@forEach
            FeelingFace(
                feeling = face,
                size = ChartFaceSize,
                modifier = Modifier.offset(
                    x = xOf(index) - ChartFaceSize / 2,
                    y = yOf(point.score!!) - ChartFaceSize / 2,
                ),
            )
        }

        points.forEachIndexed { index, point ->
            val isHighlighted = index == highlightIndex
            Text(
                text = point.label,
                style = type.eyebrow,
                color = if (isHighlighted) FeelingFaceColors.chartFill else colors.accentMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(
                        x = xOf(index) - AxisLabelWidth / 2,
                        y = height - AxisLabelAreaHeight,
                    )
                    .width(AxisLabelWidth),
            )
        }

        if (plotted.isEmpty()) {
            Text(
                text = "Nothing felt yet",
                style = type.subtitle,
                color = colors.textMuted,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** `How the days split`: felt-Moment counts per feeling across the last 28 days. */
@Composable
fun MoodSplitRows(
    insights: MoodInsights,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val order = listOf(MomentFeeling.Great, MomentFeeling.Good, MomentFeeling.Low)
    val maximum = order.maxOf { insights.splitCounts[it] ?: 0 }.coerceAtLeast(1)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        order.forEach { feeling ->
            val count = insights.splitCounts[feeling] ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                FeelingFace(feeling = feeling, size = 20.dp)
                Spacer(Modifier.width(dims.spacing.md))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(dims.spacing.md)
                        .clip(RoundedCornerShape(dims.radii.pill))
                        .background(colors.surfaceCard),
                ) {
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(count.toFloat() / maximum)
                                .height(dims.spacing.md)
                                .clip(RoundedCornerShape(dims.radii.pill))
                                .background(FeelingFaceColors.split(feeling)),
                        )
                    }
                }
                Spacer(Modifier.width(dims.spacing.md))
                Text(
                    text = count.toString(),
                    style = type.caption,
                    color = colors.textSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(dims.spacing.xl),
                )
            }
        }
    }
}

/** The Moments/streak pair for the same 28-day window. */
@Composable
fun MoodCountTiles(
    insights: MoodInsights,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        MoodTile(
            label = "MOMENTS",
            value = insights.momentCount.toString(),
            suffix = null,
            modifier = Modifier.weight(1f),
        )
        MoodTile(
            label = "STREAK",
            value = insights.streakDays.toString(),
            suffix = if (insights.streakDays == 1) "day" else "days",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MoodTile(
    label: String,
    value: String,
    suffix: String?,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(dims.radii.large))
            .background(colors.surfaceCard)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        Text(text = label, style = type.eyebrow, color = colors.accentMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = type.dateLarge, color = colors.textPrimary)
            if (suffix != null) {
                Spacer(Modifier.width(dims.spacing.xs))
                Text(
                    text = suffix,
                    style = type.caption,
                    color = colors.accentMuted,
                    modifier = Modifier.padding(bottom = dims.spacing.xs),
                )
            }
        }
    }
}
