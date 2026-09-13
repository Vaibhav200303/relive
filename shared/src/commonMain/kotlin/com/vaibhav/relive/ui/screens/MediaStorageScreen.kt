package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaibhav.relive.domain.model.ArchiveInsights
import com.vaibhav.relive.domain.model.ArchiveMediaCategory
import com.vaibhav.relive.domain.model.ArchiveMediaCategorySummary
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.profile.MediaStorageState
import com.vaibhav.relive.presentation.profile.MediaStorageViewModel
import com.vaibhav.relive.presentation.profile.formatByteSize
import com.vaibhav.relive.ui.components.MediaStorageSkeleton
import com.vaibhav.relive.ui.components.ReliveSkeletonContent
import com.vaibhav.relive.ui.components.composer.ImageGlyph
import com.vaibhav.relive.ui.components.composer.MicGlyph
import com.vaibhav.relive.ui.components.composer.VideoGlyph
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun MediaStorageScreen(viewModel: MediaStorageViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    ReliveBackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(Unit) { viewModel.loadOnEntry() }

    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        ProfilePageHeader("Media & Storage", onBack)
        ReliveSkeletonContent(
            isLoading = state == MediaStorageState.Loading,
            skeleton = {
                MediaStorageSkeleton(
                    Modifier.semantics { contentDescription = "Loading archive storage" },
                )
            },
        ) {
            when (val current = state) {
                MediaStorageState.Loading -> Unit
                is MediaStorageState.Loaded -> ArchiveInsightsContent(current.insights)
                MediaStorageState.Error -> ArchiveInsightsError(viewModel::refresh)
            }
        }
    }
}

@Composable
private fun ArchiveInsightsError(onRetry: () -> Unit) {
    val d = ReliveTheme.dimensions
    Column(
        Modifier.fillMaxWidth().padding(d.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(d.spacing.md),
    ) {
        Text(
            "Couldn’t load archive storage.",
            style = ReliveTheme.typography.title,
            color = ReliveTheme.colors.textPrimary,
        )
        Text(
            "Your memories are unchanged. Try again when you’re ready.",
            style = ReliveTheme.typography.body,
            color = ReliveTheme.colors.textSecondary,
        )
        TextButton(onClick = onRetry) {
            Text("Try again", style = ReliveTheme.typography.action, color = ReliveTheme.colors.accent)
        }
    }
}

@Composable
private fun ArchiveInsightsContent(insights: ArchiveInsights) {
    val d = ReliveTheme.dimensions
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(d.spacing.lg),
    ) {
        Text(
            "Your memories, stored on this device.",
            style = ReliveTheme.typography.subtitle,
            color = ReliveTheme.colors.textSecondary,
        )
        ArchiveHero(insights)
        StorageBreakdownCard(insights)
        ArchiveCountCard(insights)
        if (insights.unavailableFileCount > 0L) {
            val text = unavailableFilesText(insights.unavailableFileCount)
            Text(
                text,
                Modifier.padding(horizontal = d.spacing.lg).semantics { contentDescription = text },
                color = ReliveTheme.colors.textMuted,
                style = ReliveTheme.typography.caption,
            )
        }
        ArchivePrivacyCard()
        Spacer(Modifier.height(d.spacing.huge))
    }
}

@Composable
private fun ArchiveHero(insights: ArchiveInsights) {
    val d = ReliveTheme.dimensions
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radii.largeIncreased),
        color = ReliveTheme.colors.surfaceCard.copy(alpha = 0.94f),
        shadowElevation = 1.dp,
    ) {
        BoxWithConstraints(Modifier.padding(d.spacing.xl)) {
            val stacked = maxWidth < 330.dp || LocalDensity.current.fontScale > 1.3f
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(d.spacing.lg)) {
                    ArchiveHeroCopy(insights, Modifier.fillMaxWidth())
                    if (insights.totalBytes > 0L) {
                        StorageRing(insights.totalBytes, Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArchiveHeroCopy(insights, Modifier.weight(1f))
                    if (insights.totalBytes > 0L) StorageRing(insights.totalBytes)
                }
            }
        }
    }
}

@Composable
private fun ArchiveHeroCopy(insights: ArchiveInsights, modifier: Modifier) {
    val d = ReliveTheme.dimensions
    Column(modifier, verticalArrangement = Arrangement.spacedBy(d.spacing.xs)) {
        Text("YOUR RELIVE ARCHIVE", style = ReliveTheme.typography.eyebrow, color = ReliveTheme.colors.textSecondary)
        Text(
            formatByteSize(insights.totalBytes),
            style = ReliveTheme.typography.dateLarge,
            color = ReliveTheme.colors.textPrimary,
        )
        Text("Total media stored", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textSecondary)
        Spacer(Modifier.height(d.spacing.xs))
        Text(
            "${pluralize(insights.momentCount, "moment")} • ${pluralize(insights.attachmentCount, "file")}",
            style = ReliveTheme.typography.caption,
            color = ReliveTheme.colors.textSecondary,
        )
        if (insights.totalBytes == 0L && insights.momentCount == 0L && insights.attachmentCount == 0L) {
            Text(
                "Your archive is just getting started.",
                style = ReliveTheme.typography.caption,
                color = ReliveTheme.colors.textMuted,
            )
        }
    }
}

@Composable
private fun StorageRing(totalBytes: Long, modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    Box(
        modifier
            .size(120.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${formatByteSize(totalBytes)} used" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            drawCircle(
                colors.borderMuted,
                radius = (size.minDimension - stroke) / 2f,
                style = Stroke(stroke),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(colors.accentMuted, colors.accent, colors.accentMuted),
                    center = center,
                ),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatByteSize(totalBytes),
                style = ReliveTheme.typography.action.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Text("Used", style = ReliveTheme.typography.tag, color = colors.textSecondary)
        }
    }
}

@Composable
private fun StorageBreakdownCard(insights: ArchiveInsights) {
    val items = buildList {
        add(CategoryItem("Photos", insights.photo, ArchiveMediaCategory.Photo))
        add(CategoryItem("Videos", insights.video, ArchiveMediaCategory.Video))
        add(CategoryItem("Audio", insights.audio, ArchiveMediaCategory.Audio))
        if (insights.other.attachmentCount > 0L) add(CategoryItem("Other", insights.other, ArchiveMediaCategory.Other))
    }
    ArchiveSectionCard("Storage breakdown", "See how your archive uses space.") {
        items.forEach { ArchiveCategoryCard(it, insights.totalBytes) }
    }
}

@Composable
private fun ArchiveCategoryCard(item: CategoryItem, totalBytes: Long) {
    val d = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val percentage = archivePercentage(item.summary.bytes, totalBytes)
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription =
                "${item.label}, ${pluralize(item.summary.attachmentCount, "file")}, " +
                    "${formatByteSize(item.summary.bytes)}, $percentage percent"
        },
        shape = RoundedCornerShape(d.radii.medium),
        color = colors.surfaceCardTranslucent,
    ) {
        Column(Modifier.padding(d.spacing.sm), verticalArrangement = Arrangement.spacedBy(d.spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(item.category, 46.dp)
                Spacer(Modifier.width(d.spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.label,
                        style = ReliveTheme.typography.action.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                    )
                    Text(
                        pluralize(item.summary.attachmentCount, "file"),
                        style = ReliveTheme.typography.caption,
                        color = colors.textSecondary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatByteSize(item.summary.bytes),
                        style = ReliveTheme.typography.action.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                    )
                    Text("$percentage%", style = ReliveTheme.typography.caption, color = colors.textSecondary)
                }
            }
            StorageProgress(item.summary.bytes, totalBytes, item.category.color())
        }
    }
}

@Composable
private fun StorageProgress(bytes: Long, totalBytes: Long, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(ReliveTheme.colors.borderMuted),
    ) {
        if (bytes > 0L && totalBytes > 0L) {
            Box(
                Modifier
                    .fillMaxWidth(archiveMeasuredFraction(bytes, totalBytes))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun ArchiveCountCard(insights: ArchiveInsights) {
    val items = listOf(
        CategoryItem("Photos", insights.photo, ArchiveMediaCategory.Photo),
        CategoryItem("Videos", insights.video, ArchiveMediaCategory.Video),
        CategoryItem("Audio", insights.audio, ArchiveMediaCategory.Audio),
    )
    ArchiveSectionCard("Your archive", "A quick count of everything you’ve saved.") {
        if (LocalDensity.current.fontScale > 1.3f) {
            items.forEach { ArchiveCountTile(it, Modifier.fillMaxWidth()) }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.sm),
            ) {
                items.forEach { ArchiveCountTile(it, Modifier.weight(1f)) }
            }
        }
        if (insights.other.attachmentCount > 0L) {
            ArchiveCountTile(CategoryItem("Other", insights.other, ArchiveMediaCategory.Other), Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ArchiveCountTile(item: CategoryItem, modifier: Modifier) {
    val d = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Surface(
        modifier = modifier.heightIn(min = 64.dp).semantics(mergeDescendants = true) {
            contentDescription = "${item.label}, ${item.summary.attachmentCount}"
        },
        shape = RoundedCornerShape(d.radii.medium),
        color = colors.surfaceCardTranslucent,
    ) {
        Row(Modifier.padding(d.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(item.category, 38.dp)
            Spacer(Modifier.width(d.spacing.sm))
            Column {
                Text(
                    item.summary.attachmentCount.toString(),
                    style = ReliveTheme.typography.action.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )
                Text(item.label, style = ReliveTheme.typography.tag, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ArchiveSectionCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val d = ReliveTheme.dimensions
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radii.largeIncreased),
        color = ReliveTheme.colors.surfaceCard.copy(alpha = 0.88f),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(d.spacing.lg), verticalArrangement = Arrangement.spacedBy(d.spacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = ReliveTheme.typography.title.copy(fontSize = 18.sp, lineHeight = 23.sp),
                    color = ReliveTheme.colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(subtitle, style = ReliveTheme.typography.caption, color = ReliveTheme.colors.textSecondary)
            }
            content()
        }
    }
}

@Composable
private fun CategoryBadge(category: ArchiveMediaCategory, size: Dp) {
    val d = ReliveTheme.dimensions
    val color = category.color()
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(d.radii.medium),
        color = color.copy(alpha = 0.12f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (category) {
                ArchiveMediaCategory.Photo -> ImageGlyph(d.icon.md, color, d.stroke.icon)
                ArchiveMediaCategory.Video -> VideoGlyph(d.icon.md, color, d.stroke.icon)
                ArchiveMediaCategory.Audio -> MicGlyph(d.icon.md, color, d.stroke.icon)
                ArchiveMediaCategory.Other -> Icon(
                    ProfileIcons.Archive,
                    null,
                    Modifier.size(d.icon.md),
                    tint = color,
                )
            }
        }
    }
}

@Composable
private fun ArchivePrivacyCard() {
    val d = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radii.largeIncreased),
        color = colors.tint.copy(alpha = 0.58f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = d.spacing.lg, top = d.spacing.lg, bottom = d.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ProfileIcons.Security, null, Modifier.size(28.dp), tint = colors.accentMuted)
            Spacer(Modifier.width(d.spacing.lg))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.spacing.xs)) {
                Text(
                    "Your memories stay yours",
                    style = ReliveTheme.typography.action.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Your memories are stored locally on this device and are never uploaded without your permission.",
                    style = ReliveTheme.typography.caption,
                    color = colors.textSecondary,
                )
            }
            if (LocalDensity.current.fontScale <= 1.3f) {
                MemoryLeaves(Modifier.padding(start = d.spacing.sm).width(70.dp).height(84.dp))
            } else {
                Spacer(Modifier.width(d.spacing.lg))
            }
        }
    }
}

@Composable
private fun MemoryLeaves(modifier: Modifier) {
    val color = ReliveTheme.colors.accentMuted
    Canvas(modifier) {
        val stem = Path().apply {
            moveTo(size.width * 0.78f, size.height)
            cubicTo(
                size.width * 0.72f,
                size.height * 0.68f,
                size.width * 0.42f,
                size.height * 0.52f,
                size.width * 0.18f,
                size.height * 0.18f,
            )
        }
        drawPath(stem, color.copy(alpha = 0.28f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        drawLeaf(
            color.copy(alpha = 0.11f),
            Offset(size.width * 0.18f, size.height * 0.18f),
            Offset(size.width * 0.48f, size.height * 0.50f),
            size.width * 0.24f,
        )
        drawLeaf(
            color.copy(alpha = 0.10f),
            Offset(size.width * 0.84f, size.height * 0.33f),
            Offset(size.width * 0.62f, size.height * 0.65f),
            size.width * 0.22f,
        )
        drawLeaf(
            color.copy(alpha = 0.13f),
            Offset(size.width * 0.25f, size.height * 0.68f),
            Offset(size.width * 0.74f, size.height * 0.94f),
            size.width * 0.18f,
        )
    }
}

private fun DrawScope.drawLeaf(color: Color, tip: Offset, base: Offset, width: Float) {
    val dx = base.x - tip.x
    val dy = base.y - tip.y
    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val normal = Offset(-dy / length * width, dx / length * width)
    val middle = Offset((tip.x + base.x) / 2f, (tip.y + base.y) / 2f)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        quadraticTo(middle.x + normal.x, middle.y + normal.y, base.x, base.y)
        quadraticTo(middle.x - normal.x, middle.y - normal.y, tip.x, tip.y)
        close()
    }
    drawPath(path, color)
}

@Composable
private fun ArchiveMediaCategory.color(): Color = when (this) {
    ArchiveMediaCategory.Photo -> ReliveTheme.colors.accentMuted
    ArchiveMediaCategory.Video -> ReliveTheme.colors.accent
    ArchiveMediaCategory.Audio -> ReliveTheme.colors.surfaceAudio
    ArchiveMediaCategory.Other -> ReliveTheme.colors.border
}

private data class CategoryItem(
    val label: String,
    val summary: ArchiveMediaCategorySummary,
    val category: ArchiveMediaCategory,
)

private fun archiveMeasuredFraction(bytes: Long, totalBytes: Long): Float =
    if (bytes > 0L && totalBytes > 0L) {
        (bytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f

internal fun archivePercentage(bytes: Long, totalBytes: Long): Int =
    if (bytes > 0L && totalBytes > 0L) {
        ((bytes.toDouble() / totalBytes.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    } else 0

private fun pluralize(count: Long, singular: String): String =
    "$count $singular${if (count == 1L) "" else "s"}"

private fun unavailableFilesText(count: Long): String =
    "${pluralize(count, "file").replaceFirstChar { it.uppercase() }} couldn’t be measured."
