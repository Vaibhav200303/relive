package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.domain.model.ArchiveInsights
import com.vaibhav.relive.domain.model.ArchiveMediaCategory
import com.vaibhav.relive.domain.model.ArchiveMediaCategorySummary
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.profile.MediaStorageState
import com.vaibhav.relive.presentation.profile.MediaStorageViewModel
import com.vaibhav.relive.presentation.profile.formatByteSize
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun MediaStorageScreen(
    viewModel: MediaStorageViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    ReliveBackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(Unit) { viewModel.loadOnEntry() }

    Column(
        modifier = Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas),
    ) {
        MediaStorageHeader(onBack)
        when (val current = state) {
            MediaStorageState.Loading -> LoadingArchiveInsights()
            is MediaStorageState.Loaded -> ArchiveInsightsContent(current.insights)
            MediaStorageState.Error -> ArchiveInsightsError(onRetry = viewModel::refresh)
        }
    }
}

@Composable
private fun MediaStorageHeader(onBack: () -> Unit) {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(dims.minTouchTarget)
                .align(Alignment.CenterStart)
                .semantics { contentDescription = "Back to Profile" },
        ) {
            BackGlyph(dims.icon.lg, ReliveTheme.colors.textSecondary, dims.stroke.icon)
        }
        Text(
            text = "Media & Storage",
            style = ReliveTheme.typography.action,
            color = ReliveTheme.colors.textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun LoadingArchiveInsights() {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier.fillMaxSize().semantics { contentDescription = "Loading archive storage" },
        contentAlignment = Alignment.TopCenter,
    ) {
        CircularProgressIndicator(
            color = ReliveTheme.colors.accent,
            modifier = Modifier.padding(top = dims.spacing.huge),
        )
    }
}

@Composable
private fun ArchiveInsightsError(onRetry: () -> Unit) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier.fillMaxWidth().padding(dims.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
    ) {
        Text("Couldn’t load archive storage.", style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
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
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.xl),
    ) {
        Text(
            "Your memories, stored on this device.",
            style = ReliveTheme.typography.subtitle,
            color = ReliveTheme.colors.textSecondary,
        )
        ArchiveHero(insights)
        ArchiveSection("STORAGE BREAKDOWN") {
            ArchiveCategoryRow("Photos", insights.photo, insights.totalBytes, ArchiveMediaCategory.Photo)
            ArchiveCategoryRow("Videos", insights.video, insights.totalBytes, ArchiveMediaCategory.Video)
            ArchiveCategoryRow("Audio", insights.audio, insights.totalBytes, ArchiveMediaCategory.Audio)
            if (insights.other.attachmentCount > 0L) {
                ArchiveCategoryRow("Other", insights.other, insights.totalBytes, ArchiveMediaCategory.Other)
            }
        }
        ArchiveSection("YOUR ARCHIVE") {
            ArchiveCountRow("Photos", insights.photo.attachmentCount)
            ArchiveCountRow("Videos", insights.video.attachmentCount)
            ArchiveCountRow("Audio", insights.audio.attachmentCount)
            if (insights.other.attachmentCount > 0L) ArchiveCountRow("Other", insights.other.attachmentCount)
        }
        if (insights.unavailableFileCount > 0L) {
            Text(
                text = unavailableFilesText(insights.unavailableFileCount),
                style = ReliveTheme.typography.subtitle,
                color = ReliveTheme.colors.textMuted,
                modifier = Modifier.semantics { contentDescription = unavailableFilesText(insights.unavailableFileCount) },
            )
        }
        HorizontalDivider(color = ReliveTheme.colors.borderMuted, thickness = dims.stroke.hairline)
        Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
            Text(
                "Your memories stay yours",
                style = ReliveTheme.typography.title,
                color = ReliveTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Your memories are stored locally on this device.",
                style = ReliveTheme.typography.body,
                color = ReliveTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = dims.spacing.huge),
            )
        }
    }
}

@Composable
private fun ArchiveHero(insights: ArchiveInsights) {
    val dims = ReliveTheme.dimensions
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.radii.lg),
        color = ReliveTheme.colors.surfaceCard,
        contentColor = ReliveTheme.colors.textPrimary,
        border = androidx.compose.foundation.BorderStroke(dims.stroke.hairline, ReliveTheme.colors.borderMuted),
    ) {
        Column(
            modifier = Modifier.padding(dims.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
        ) {
            Text("YOUR RELIVE ARCHIVE", style = ReliveTheme.typography.eyebrow, color = ReliveTheme.colors.accentMuted)
            Text(formatByteSize(insights.totalBytes), style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
            Text("Total media stored", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textSecondary)
            if (insights.totalBytes > 0L) ArchiveComposition(insights)
            if (insights.momentCount == 0L && insights.attachmentCount == 0L) {
                Text("Your archive is just getting started.", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textMuted)
            }
            Text(
                text = "${pluralize(insights.momentCount, "moment")} • ${pluralize(insights.attachmentCount, "file")}",
                style = ReliveTheme.typography.body,
                color = ReliveTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ArchiveComposition(insights: ArchiveInsights) {
    val entries = listOf(
        ArchiveMediaCategory.Photo to insights.photo,
        ArchiveMediaCategory.Video to insights.video,
        ArchiveMediaCategory.Audio to insights.audio,
        ArchiveMediaCategory.Other to insights.other,
    ).filter { it.second.bytes > 0L }
    val description = entries.joinToString(", ") { (category, summary) ->
        "${category.label()} ${formatByteSize(summary.bytes)}"
    }
    val visualWeights = archiveVisualWeights(
        bytes = entries.map { it.second.bytes },
        totalBytes = insights.totalBytes,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ReliveTheme.dimensions.spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "Storage composition: $description" },
    ) {
        entries.forEachIndexed { index, (category, _) ->
            Box(
                modifier = Modifier
                    .weight(visualWeights[index])
                    .height(ReliveTheme.dimensions.spacing.sm)
                    .background(category.color()),
            )
        }
    }
}

@Composable
private fun ArchiveSection(title: String, content: @Composable () -> Unit) {
    val dims = ReliveTheme.dimensions
    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.md)) {
        Text(title, style = ReliveTheme.typography.eyebrow, color = ReliveTheme.colors.accentMuted, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun ArchiveCategoryRow(
    label: String,
    summary: ArchiveMediaCategorySummary,
    totalBytes: Long,
    category: ArchiveMediaCategory,
) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "$label, ${formatByteSize(summary.bytes)}"
        },
        verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = ReliveTheme.typography.body, color = ReliveTheme.colors.textPrimary, modifier = Modifier.weight(1f))
            Text(formatByteSize(summary.bytes), style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
        }
        Box(modifier = Modifier.fillMaxWidth().height(dims.spacing.xs).background(ReliveTheme.colors.surfaceCardTranslucent)) {
            if (summary.bytes > 0L && totalBytes > 0L) {
                Box(
                    modifier = Modifier
                    .fillMaxWidth(archiveMeasuredFraction(summary.bytes, totalBytes))
                        .height(dims.spacing.xs)
                        .background(category.color()),
                )
            }
        }
    }
}

@Composable
private fun ArchiveCountRow(label: String, count: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ReliveTheme.dimensions.minTouchTarget)
            .semantics(mergeDescendants = true) { contentDescription = "$label, $count" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = ReliveTheme.typography.body, color = ReliveTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        Text(count.toString(), style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
    }
}

@Composable
private fun ArchiveMediaCategory.color() = when (this) {
    ArchiveMediaCategory.Photo -> ReliveTheme.colors.accentMuted
    ArchiveMediaCategory.Video -> ReliveTheme.colors.accent
    ArchiveMediaCategory.Audio -> ReliveTheme.colors.surfaceAudio
    ArchiveMediaCategory.Other -> ReliveTheme.colors.border
}

private const val MinimumArchiveVisualWeight = 0.02f

internal fun archiveVisualWeights(
    bytes: List<Long>,
    totalBytes: Long,
    minimumWeight: Float = MinimumArchiveVisualWeight,
): List<Float> {
    require(minimumWeight >= 0f) { "Minimum visual weight must not be negative" }
    if (bytes.isEmpty() || totalBytes <= 0L) return bytes.map { 0f }

    val positiveCount = bytes.count { it > 0L }
    if (positiveCount == 0) return bytes.map { 0f }

    val minimumTotal = minimumWeight * positiveCount
    val measuredSpace = (1f - minimumTotal).coerceAtLeast(0f)
    val weights = bytes.map { byteCount ->
        if (byteCount <= 0L) {
            0f
        } else {
            minimumWeight + measuredSpace * (byteCount.toDouble() / totalBytes.toDouble()).toFloat()
        }
    }
    val weightTotal = weights.sum()
    return if (weightTotal > 0f) weights.map { it / weightTotal } else weights
}

private fun archiveMeasuredFraction(bytes: Long, totalBytes: Long): Float =
    if (bytes > 0L && totalBytes > 0L) {
        (bytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

private fun ArchiveMediaCategory.label(): String = when (this) {
    ArchiveMediaCategory.Photo -> "Photos"
    ArchiveMediaCategory.Video -> "Videos"
    ArchiveMediaCategory.Audio -> "Audio"
    ArchiveMediaCategory.Other -> "Other"
}

private fun pluralize(count: Long, singular: String): String =
    "$count $singular${if (count == 1L) "" else "s"}"

private fun unavailableFilesText(count: Long): String =
    "${pluralize(count, "file").replaceFirstChar { it.uppercase() }} couldn’t be measured."
