package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.vaibhav.relive.presentation.cardcover.cardCoverStableKey
import com.vaibhav.relive.presentation.date.TimelineCreatedDateFormatter
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeContent
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeViewModel
import com.vaibhav.relive.ui.components.MediaToCardSurfaceFade
import com.vaibhav.relive.ui.components.AllTimelineCollage
import com.vaibhav.relive.ui.components.composer.PlusGlyph
import com.vaibhav.relive.ui.components.navigation.ReliveWordmarkAppBar
import com.vaibhav.relive.ui.components.reliveCardOuterBorder
import com.vaibhav.relive.ui.components.timeline.TimelineCreationDialog
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.presentation.cardcover.resolveAllTimelineCollage

@Composable
fun TimelineHomeScreen(
    viewModel: TimelineHomeViewModel,
    mediaStore: MediaStore,
    listState: LazyListState,
    onOpenTimeline: (Timeline) -> Unit,
    onOpenProfile: () -> Unit,
    onCreateMoment: (() -> Unit)? = null,
    navigationToolbarExpanded: Boolean = true,
    onNavigationToolbarExpand: () -> Unit = {},
    onNavigationToolbarCollapse: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val creation by viewModel.creationState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.navigation.collect(onOpenTimeline)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .background(ReliveTheme.colors.bgCanvas)
                .fillMaxWidth(),
        ) {
            TimelineHomeHeader(onCreateTimeline = viewModel::showTimelineCreation, onOpenProfile = onOpenProfile)
            TimelineHomeSearchBar(
                query = state.query,
                onQueryChange = viewModel::updateSearchQuery,
            )
            when (val content = state.content) {
                TimelineHomeContent.Loading -> TimelineHomeLoading()
                is TimelineHomeContent.Loaded -> TimelineHomeContent(
                    hasCustomTimelines = state.customSummaries.isNotEmpty(),
                    summaries = state.visibleCustomSummaries,
                    query = state.query,
                    mediaStore = mediaStore,
                    listState = listState,
                    onOpenTimeline = viewModel::selectTimeline,
                    reserveQuickCaptureSpace = onCreateMoment != null,
                    navigationToolbarExpanded = navigationToolbarExpanded,
                    onNavigationToolbarExpand = onNavigationToolbarExpand,
                    onNavigationToolbarCollapse = onNavigationToolbarCollapse,
                )
            }
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
private fun TimelineHomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val shape = RoundedCornerShape(dims.radii.pill)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.sm)
            .height(dims.search.containerHeight)
            .clip(shape)
            .background(colors.surfaceCard)
            .border(dims.stroke.hairline, colors.borderMuted, shape)
            .padding(horizontal = dims.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
    ) {
        TimelineSearchGlyph()
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Search timelines" },
            textStyle = ReliveTheme.typography.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search timelines...",
                            style = ReliveTheme.typography.body,
                            color = colors.textMuted,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun TimelineSearchGlyph() {
    val dims = ReliveTheme.dimensions
    val color = ReliveTheme.colors.textPrimary
    Canvas(
        modifier = Modifier
            .padding(start = dims.spacing.md, end = dims.spacing.sm)
            .size(dims.icon.lg),
    ) {
        val strokeWidth = dims.stroke.icon.toPx()
        val radius = size.minDimension * 0.28f
        val center = Offset(size.width * 0.43f, size.height * 0.43f)
        drawCircle(color = color, radius = radius, center = center, style = Stroke(strokeWidth))
        drawLine(
            color = color,
            start = center + Offset(radius * 0.7f, radius * 0.7f),
            end = Offset(size.width * 0.86f, size.height * 0.86f),
            strokeWidth = strokeWidth,
        )
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
    hasCustomTimelines: Boolean,
    summaries: List<TimelineHomeSummary>,
    query: String,
    mediaStore: MediaStore,
    listState: LazyListState,
    onOpenTimeline: (Timeline) -> Unit,
    reserveQuickCaptureSpace: Boolean,
    navigationToolbarExpanded: Boolean,
    onNavigationToolbarExpand: () -> Unit,
    onNavigationToolbarCollapse: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val bottomPadding = if (reserveQuickCaptureSpace) dims.spacing.huge * 2 else dims.spacing.huge
    LazyColumn(
        state = listState,
        modifier = Modifier.floatingToolbarNestedScroll(
            expanded = navigationToolbarExpanded,
            onExpand = onNavigationToolbarExpand,
            onCollapse = onNavigationToolbarCollapse,
        ),
        contentPadding = PaddingValues(
            start = dims.spacing.xl,
            top = dims.spacing.md,
            end = dims.spacing.xl,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.xl),
    ) {
        item(key = "your-timeline-heading") {
            Text("YOUR TIMELINE", style = ReliveTheme.typography.title, color = ReliveTheme.colors.accentMuted)
        }
        if (summaries.isEmpty()) {
            if (query.isNotBlank() || hasCustomTimelines) {
                item(key = "no-matching-custom-timelines") {
                    Text(
                        text = "No timelines found.",
                        style = ReliveTheme.typography.subtitle,
                        color = ReliveTheme.colors.textSecondary,
                    )
                }
            } else {
                item(key = "empty-custom-timelines") { TimelineHomeEmptyCustomState() }
            }
        } else {
            items(summaries, key = { (it.timeline as Timeline.Custom).id.value }) { summary ->
                TimelineHomeCard(summary, mediaStore, onClick = { onOpenTimeline(summary.timeline) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun Modifier.floatingToolbarNestedScroll(
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
): Modifier = with(FloatingToolbarDefaults) {
    floatingToolbarVerticalNestedScroll(
        expanded = expanded,
        onExpand = onExpand,
        onCollapse = onCollapse,
    )
}

@Composable
internal fun TimelineHomeCard(
    summary: TimelineHomeSummary,
    mediaStore: MediaStore,
    allCollageBucket: Long = 0L,
    allCollageCandidates: List<MediaAttachment>? = null,
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
            allCollageBucket = allCollageBucket,
            allCollageCandidates = allCollageCandidates,
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
                        text = TimelineCreatedDateFormatter.format(createdAt),
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
    allCollageBucket: Long,
    allCollageCandidates: List<MediaAttachment>?,
) {
    val dims = ReliveTheme.dimensions
    val attachments = summary.previewAttachments
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(mediaHeight)
            .background(ReliveTheme.colors.surfaceCardTranslucent),
    ) {
        if (summary.timeline == Timeline.All) {
            val selection = resolveAllTimelineCollage(
                available = allCollageCandidates ?: attachments,
                bucket = allCollageBucket,
            )
            if (selection.attachments.isEmpty()) {
                com.vaibhav.relive.ui.theme.ReliveGeneratedCover(
                    stableKey = summary.timeline.cardCoverStableKey(),
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                AllTimelineCollage(
                    attachments = selection.attachments,
                    layout = requireNotNull(selection.layout),
                    mediaStore = mediaStore,
                    modifier = Modifier.matchParentSize(),
                )
            }
        } else when (attachments.size) {
            0 -> com.vaibhav.relive.ui.theme.ReliveGeneratedCover(
                stableKey = summary.timeline.cardCoverStableKey(),
                modifier = Modifier.matchParentSize(),
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
private fun PreviewTile(attachment: MediaAttachment, mediaStore: MediaStore, modifier: Modifier) {
    when (attachment.type) {
        MediaType.Image -> RelivedImageTile(attachment.storageRef, mediaStore, modifier)
        MediaType.Video -> RelivedVideoTile(attachment.storageRef, mediaStore, modifier)
        MediaType.Audio -> Unit
    }
}
