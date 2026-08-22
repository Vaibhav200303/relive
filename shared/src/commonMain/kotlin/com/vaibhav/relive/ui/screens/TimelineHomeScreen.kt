package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeContent
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeViewModel
import com.vaibhav.relive.presentation.timelinehome.emptyPreviewPlaceholderText
import com.vaibhav.relive.ui.components.timeline.TimelineCreationDialog
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun TimelineHomeScreen(
    viewModel: TimelineHomeViewModel,
    mediaStore: MediaStore,
    listState: LazyListState,
    onOpenTimeline: (Timeline) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val creation by viewModel.creationState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.navigation.collect(onOpenTimeline)
    }

    Column(
        modifier = Modifier
            .background(ReliveTheme.colors.bgCanvas)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        TimelineHomeHeader(onCreateTimeline = viewModel::showTimelineCreation)
        when (val content = state.content) {
            TimelineHomeContent.Loading -> TimelineHomeLoading()
            is TimelineHomeContent.Loaded -> TimelineHomeContent(
                summaries = content.summaries,
                mediaStore = mediaStore,
                listState = listState,
                onOpenTimeline = viewModel::selectTimeline,
            )
        }
    }
    TimelineCreationDialog(
        state = creation,
        onNameChange = viewModel::updateTimelineName,
        onCreate = viewModel::createTimeline,
        onDismiss = viewModel::dismissTimelineCreation,
    )
}

@Composable
private fun TimelineHomeHeader(onCreateTimeline: () -> Unit) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        Text(
            text = "Relive",
            style = ReliveTheme.typography.wordmark,
            color = colors.accent,
            modifier = Modifier.align(Alignment.Center),
        )
        IconButton(
            onClick = onCreateTimeline,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(dims.minTouchTarget)
                .semantics { contentDescription = "Create timeline" },
        ) {
            Text(text = "+", style = ReliveTheme.typography.title, color = colors.accent)
        }
    }
}

@Composable
private fun TimelineHomeLoading() {
    Box(modifier = Modifier.fillMaxWidth().padding(ReliveTheme.dimensions.spacing.xl))
}

@Composable
private fun TimelineHomeContent(
    summaries: List<TimelineHomeSummary>,
    mediaStore: MediaStore,
    listState: LazyListState,
    onOpenTimeline: (Timeline) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val all = summaries.firstOrNull { it.timeline == Timeline.All }
    val custom = summaries.filter { it.timeline is Timeline.Custom }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = dims.spacing.xl,
            top = dims.spacing.md,
            end = dims.spacing.xl,
            bottom = dims.spacing.huge,
        ),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.xl),
    ) {
        all?.let { summary ->
            item(key = "all") {
                TimelineHomeCard(summary, mediaStore, onClick = { onOpenTimeline(summary.timeline) })
            }
        }
        item(key = "your-timeline-heading") {
            Text("YOUR TIMELINE", style = ReliveTheme.typography.eyebrow, color = ReliveTheme.colors.accentMuted)
        }
        if (custom.isEmpty()) {
            item(key = "empty-custom-timelines") { TimelineHomeEmptyCustomState() }
        } else {
            items(custom, key = { (it.timeline as Timeline.Custom).id.value }) { summary ->
                TimelineHomeCard(summary, mediaStore, onClick = { onOpenTimeline(summary.timeline) })
            }
        }
    }
}

@Composable
private fun TimelineHomeCard(
    summary: TimelineHomeSummary,
    mediaStore: MediaStore,
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val mediaHeight = if (summary.timeline == Timeline.All) {
        dims.timelineHome.allMediaHeight
    } else {
        dims.timelineHome.customMediaHeight
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(dims.radii.md))
            .background(colors.surfaceCard)
            .border(dims.stroke.hairline, colors.border, androidx.compose.foundation.shape.RoundedCornerShape(dims.radii.md))
            .clickable(onClick = onClick)
    ) {
        TimelineHomeMediaPreview(
            summary = summary,
            mediaStore = mediaStore,
            mediaHeight = mediaHeight,
        )
        Column(
            modifier = Modifier.padding(dims.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
        ) {
            Text(summary.name, style = ReliveTheme.typography.title, color = colors.textPrimary)
            Text(
                text = "${summary.momentCount} ${if (summary.momentCount == 1L) "moment" else "moments"}",
                style = ReliveTheme.typography.subtitle,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun TimelineHomeEmptyCustomState() {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = dims.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
    ) {
        Text("A new chapter can begin whenever you are ready.", style = ReliveTheme.typography.title, color = colors.textPrimary)
        Text("Use + to create your first timeline.", style = ReliveTheme.typography.subtitle, color = colors.textSecondary)
    }
}

@Composable
private fun TimelineHomeMediaPreview(
    summary: TimelineHomeSummary,
    mediaStore: MediaStore,
    mediaHeight: androidx.compose.ui.unit.Dp,
) {
    val dims = ReliveTheme.dimensions
    val attachments = summary.previewAttachments
    val placeholderText = summary.emptyPreviewPlaceholderText()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(mediaHeight)
            .background(ReliveTheme.colors.surfaceCardTranslucent),
    ) {
        when (attachments.size) {
            0 -> TimelineHomeEditorialPlaceholder(
                text = placeholderText.orEmpty(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = dims.spacing.lg),
            )
            1 -> PreviewTile(attachments.single(), mediaStore, Modifier.matchParentSize())
            2 -> Row(Modifier.matchParentSize()) {
                PreviewTile(attachments[0], mediaStore, Modifier.weight(1f))
                Box(Modifier.size(dims.media.collageGap).background(ReliveTheme.colors.accent))
                PreviewTile(attachments[1], mediaStore, Modifier.weight(1f))
            }
            else -> Row(Modifier.matchParentSize()) {
                PreviewTile(attachments[0], mediaStore, Modifier.weight(1.4f))
                Box(Modifier.size(dims.media.collageGap).background(ReliveTheme.colors.accent))
                Column(Modifier.weight(1f)) {
                    PreviewTile(attachments[1], mediaStore, Modifier.weight(1f).fillMaxWidth())
                    Box(Modifier.height(dims.media.collageGap).fillMaxWidth().background(ReliveTheme.colors.accent))
                    Row(Modifier.weight(1f)) {
                        PreviewTile(attachments[2], mediaStore, Modifier.weight(1f))
                        if (attachments.size == 4) {
                            Box(Modifier.size(dims.media.collageGap).background(ReliveTheme.colors.accent))
                            PreviewTile(attachments[3], mediaStore, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.timelineHome.mediaFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, ReliveTheme.colors.surfaceCard),
                    ),
                ),
        )
    }
}

@Composable
private fun TimelineHomeEditorialPlaceholder(text: String, modifier: Modifier) {
    val colors = ReliveTheme.colors
    Text(
        text = text,
        style = ReliveTheme.typography.title,
        color = colors.textPrimary,
        modifier = modifier,
    )
}

@Composable
private fun PreviewTile(attachment: MediaAttachment, mediaStore: MediaStore, modifier: Modifier) {
    when (attachment.type) {
        MediaType.Image -> RelivedImageTile(attachment.storageRef, mediaStore, modifier)
        MediaType.Video -> RelivedVideoTile(attachment.storageRef, mediaStore, modifier)
        MediaType.Audio -> Unit
    }
}
