package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
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
import com.vaibhav.relive.presentation.date.ProfileSinceFormatter
import com.vaibhav.relive.ui.components.MediaToCardSurfaceFade
import com.vaibhav.relive.ui.components.reliveCardOuterBorder
import com.vaibhav.relive.ui.components.timeline.TimelineCreationDialog
import com.vaibhav.relive.ui.components.composer.PlusGlyph
import com.vaibhav.relive.ui.components.navigation.ReliveWordmarkAppBar
import com.vaibhav.relive.ui.theme.ReliveTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun TimelineHomeScreen(
    viewModel: TimelineHomeViewModel,
    mediaStore: MediaStore,
    listState: LazyListState,
    onOpenTimeline: (Timeline) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val creation by viewModel.creationState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.navigation.collect(onOpenTimeline)
    }

    Column(
        modifier = Modifier
            .background(ReliveTheme.colors.bgCanvas)
            .fillMaxWidth(),
    ) {
        TimelineHomeHeader(onCreateTimeline = viewModel::showTimelineCreation, onOpenProfile = onOpenProfile)
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
private fun TimelineHomeHeader(onCreateTimeline: () -> Unit, onOpenProfile: () -> Unit) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    ReliveWordmarkAppBar {
        IconButton(
            onClick = onOpenProfile,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(dims.minTouchTarget)
                .semantics { contentDescription = "Open Profile" },
        ) {
            ProfileAffordanceGlyph()
        }
        IconButton(
            onClick = onCreateTimeline,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(dims.minTouchTarget)
                .semantics { contentDescription = "Create timeline" },
        ) {
            PlusGlyph(
                size = dims.timelineHome.createTimelineGlyphSize,
                color = colors.accent,
                strokeWidth = dims.stroke.iconBold,
            )
        }
    }
}

@Composable
private fun ProfileAffordanceGlyph() {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    androidx.compose.foundation.Canvas(Modifier.size(dims.icon.lg)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = dims.stroke.icon.toPx())
        drawCircle(colors.textSecondary, radius = size.minDimension * 0.2f, center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.32f), style = stroke)
        drawArc(
            color = colors.textSecondary,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.36f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.48f),
            style = stroke,
        )
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
        item(key = "your-timeline-heading") {
            Text("YOUR TIMELINE", style = ReliveTheme.typography.title, color = ReliveTheme.colors.accentMuted)
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
internal fun TimelineHomeCard(
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
    val cardShape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surfaceCard)
            .reliveCardOuterBorder(cardShape)
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
            Text(summary.name, style = ReliveTheme.typography.title, color = colors.textPrimary, maxLines = 2)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${summary.momentCount} ${if (summary.momentCount == 1L) "moment" else "moments"}",
                    style = ReliveTheme.typography.subtitle,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                summary.createdAt?.let { createdAt ->
                    Text(
                        text = "Created ${ProfileSinceFormatter.format(createdAt)}",
                        style = ReliveTheme.typography.tag,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }
            }
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
            .padding(horizontal = dims.spacing.md)
            .height(mediaHeight)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
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
        MediaToCardSurfaceFade(modifier = Modifier.align(Alignment.BottomCenter))
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
