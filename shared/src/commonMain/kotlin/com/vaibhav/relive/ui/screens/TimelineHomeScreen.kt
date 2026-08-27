package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.presentation.cardcover.cardCoverStableKey
import com.vaibhav.relive.presentation.date.TimelineCreatedDateFormatter
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeContent
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeNavigation
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeViewModel
import com.vaibhav.relive.presentation.timeline.TimelineCreationOutcome
import com.vaibhav.relive.ui.components.AllTimelineCollage
import com.vaibhav.relive.ui.components.composer.PlusGlyph
import com.vaibhav.relive.ui.components.navigation.ReliveWordmarkAppBar
import com.vaibhav.relive.ui.components.timeline.TimelineCreationDialog
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.ReliveOpacity
import com.vaibhav.relive.ui.theme.toReliveThemeId
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.icons.TimelineActionIcons
import com.vaibhav.relive.presentation.cardcover.resolveAllTimelineCollage

@Composable
fun TimelineHomeScreen(
    viewModel: TimelineHomeViewModel,
    mediaStore: MediaStore,
    listState: LazyListState,
    onOpenTimeline: (TimelineHomeNavigation) -> Unit,
    onOpenProfile: () -> Unit,
    profilePhoto: MediaStorageRef? = null,
    onCreateMoment: (() -> Unit)? = null,
    navigationToolbarExpanded: Boolean = true,
    onNavigationToolbarExpand: () -> Unit = {},
    onNavigationToolbarCollapse: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val creation by viewModel.creationState.collectAsState()
    val haptics = rememberReliveHaptics()
    var selectedTimelines by remember { mutableStateOf<Set<Timeline.Custom>>(emptySet()) }
    var timelineToRename by remember { mutableStateOf<Timeline.Custom?>(null) }
    var timelinesToDelete by remember { mutableStateOf<List<Timeline.Custom>>(emptyList()) }
    LaunchedEffect(viewModel) {
        viewModel.navigation.collect(onOpenTimeline)
    }
    LaunchedEffect(viewModel) {
        viewModel.creationOutcomes.collect { outcome ->
            haptics.perform(
                when (outcome) {
                    TimelineCreationOutcome.Succeeded -> ReliveHapticCue.Confirm
                    TimelineCreationOutcome.Rejected -> ReliveHapticCue.Reject
                },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReliveTheme.colors.bgCanvas),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ReliveTheme.colors.bgCanvas),
        ) {
            TimelineHomeHeader(
                selectedTimelineCount = selectedTimelines.size,
                onExitSelection = { selectedTimelines = emptySet() },
                onCreateTimeline = {
                    haptics.perform(ReliveHapticCue.Action)
                    viewModel.showTimelineCreation()
                },
                onOpenProfile = onOpenProfile,
                profilePhoto = profilePhoto,
                mediaStore = mediaStore,
                onRenameSelectedTimeline = {
                    selectedTimelines.singleOrNull()?.let { timeline ->
                        selectedTimelines = emptySet()
                        timelineToRename = timeline
                    }
                },
                onDeleteSelectedTimeline = {
                    if (selectedTimelines.isNotEmpty()) {
                        timelinesToDelete = selectedTimelines.toList()
                        selectedTimelines = emptySet()
                    }
                },
            )
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
                    onOpenTimeline = { navigation -> viewModel.selectTimeline(navigation.timeline) },
                    onShowTimelineOptions = { timeline ->
                        haptics.perform(ReliveHapticCue.Context)
                        selectedTimelines = setOf(timeline)
                    },
                    selectedTimelineIds = selectedTimelines.mapTo(mutableSetOf()) { it.id },
                    onToggleTimelineSelection = { timeline ->
                        selectedTimelines = if (timeline in selectedTimelines) {
                            selectedTimelines - timeline
                        } else {
                            selectedTimelines + timeline
                        }
                    },
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
    timelineToRename?.let { timeline ->
        RenameTimelineDialog(
            timeline = timeline,
            onDismiss = { timelineToRename = null },
            onRename = { newName, onResult ->
                viewModel.renameTimeline(timeline, newName) { renamed ->
                    if (renamed) {
                        haptics.perform(ReliveHapticCue.Confirm)
                        timelineToRename = null
                    } else {
                        haptics.perform(ReliveHapticCue.Reject)
                    }
                    onResult(renamed)
                }
            },
        )
    }
    if (timelinesToDelete.isNotEmpty()) {
        DeleteTimelineDialog(
            timelines = timelinesToDelete,
            onDismiss = { timelinesToDelete = emptyList() },
            onDelete = {
                viewModel.deleteTimelines(timelinesToDelete) { deleted ->
                    if (deleted) {
                        haptics.perform(ReliveHapticCue.Confirm)
                        timelinesToDelete = emptyList()
                    } else {
                        haptics.perform(ReliveHapticCue.Reject)
                    }
                }
            },
        )
    }
}

@Composable
private fun RenameTimelineDialog(
    timeline: Timeline.Custom,
    onDismiss: () -> Unit,
    onRename: (String, (Boolean) -> Unit) -> Unit,
) {
    val colors = ReliveTheme.colors
    var name by remember(timeline.id) { mutableStateOf(timeline.name) }
    var saveFailed by remember(timeline.id) { mutableStateOf(false) }
    val isNameValid = name.trim().isNotEmpty() && name.trim().length <= Timeline.Custom.MAX_NAME_LENGTH
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(ReliveTheme.dimensions.radii.dialog),
        containerColor = colors.surfaceOverlay,
        title = { Text("Rename timeline", style = ReliveTheme.typography.title, color = colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { value ->
                    name = value
                    saveFailed = false
                },
                singleLine = true,
                label = { Text("Timeline name") },
                isError = saveFailed,
                supportingText = {
                    Text(
                        if (saveFailed) "Could not rename timeline." else "${name.length}/${Timeline.Custom.MAX_NAME_LENGTH}",
                        style = ReliveTheme.typography.subtitle,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = { onRename(name.trim()) { renamed -> saveFailed = !renamed } },
                enabled = isNameValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.textOnAccent,
                ),
            ) { Text("Rename") }
        },
    )
}

@Composable
private fun DeleteTimelineDialog(
    timelines: List<Timeline.Custom>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = ReliveTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(ReliveTheme.dimensions.radii.dialog),
        containerColor = colors.surfaceOverlay,
        title = { Text(if (timelines.size == 1) "Delete this timeline?" else "Delete these timelines?", style = ReliveTheme.typography.title, color = colors.textPrimary) },
        text = {
            Text(
                "Only ${timelines.size} ${if (timelines.size == 1) "timeline" else "timelines"} and their assignments will be removed. Their Moments stay safely in Relive.",
                style = ReliveTheme.typography.body,
                color = colors.textSecondary,
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.actionDestructive,
                    contentColor = colors.textOnDestructive,
                ),
            ) { Text("Delete timeline") }
        },
    )
}

@Composable
private fun TimelineHomeHeader(
    selectedTimelineCount: Int,
    onExitSelection: () -> Unit,
    onCreateTimeline: () -> Unit,
    onOpenProfile: () -> Unit,
    profilePhoto: MediaStorageRef?,
    mediaStore: MediaStore,
    onRenameSelectedTimeline: () -> Unit,
    onDeleteSelectedTimeline: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    AnimatedContent(
        targetState = selectedTimelineCount > 0,
        transitionSpec = {
            (fadeIn(tween(motion.durations.fastMillis, easing = motion.easings.emphasized)) +
                slideInHorizontally(tween(motion.durations.fastMillis, easing = motion.easings.emphasized)) { it / 8 }) togetherWith
                (fadeOut(tween(motion.durations.fastMillis, easing = motion.easings.emphasized)) +
                    slideOutHorizontally(tween(motion.durations.fastMillis, easing = motion.easings.emphasized)) { -it / 8 })
        },
        label = "timeline selection app bar",
    ) { isSelectionMode ->
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgHeader)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
            ) {
                IconButton(
                    onClick = onExitSelection,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Exit timeline selection" },
                ) {
                    BackGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
                }
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    if (selectedTimelineCount == 1) {
                        IconButton(
                            onClick = onRenameSelectedTimeline,
                            modifier = Modifier
                                .size(dims.minTouchTarget)
                                .semantics { contentDescription = "Rename timeline" },
                        ) {
                            Icon(TimelineActionIcons.Rename, contentDescription = null, tint = colors.textSecondary)
                        }
                    }
                    IconButton(
                        onClick = onDeleteSelectedTimeline,
                        modifier = Modifier
                            .size(dims.minTouchTarget)
                            .semantics { contentDescription = "Delete timeline" },
                    ) {
                        Icon(TimelineActionIcons.Delete, contentDescription = null, tint = colors.actionDestructive)
                    }
                }
            }
        } else {
            ReliveWordmarkAppBar {
                IconButton(
                    onClick = onOpenProfile,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Open Profile" },
                ) {
                    ProfileAffordanceGlyph(profilePhoto, mediaStore)
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
private fun ProfileAffordanceGlyph(photo: MediaStorageRef?, mediaStore: MediaStore) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    if (photo != null) {
        com.vaibhav.relive.platform.media.RelivedImageTile(photo, mediaStore, Modifier.size(dims.icon.lg).clip(RoundedCornerShape(dims.radii.pill)))
    } else Box(
        modifier = Modifier
            .size(dims.icon.lg)
            .clip(RoundedCornerShape(dims.radii.pill))
            .background(colors.surfaceCard),
        contentAlignment = Alignment.Center,
    ) {
        Icon(ProfileIcons.Person, contentDescription = null, modifier = Modifier.size(dims.icon.md), tint = colors.textSecondary)
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
    onOpenTimeline: (TimelineHomeNavigation) -> Unit,
    onShowTimelineOptions: (Timeline.Custom) -> Unit,
    selectedTimelineIds: Set<com.vaibhav.relive.domain.model.TimelineId>,
    onToggleTimelineSelection: (Timeline.Custom) -> Unit,
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
                TimelineHomeCard(
                    summary,
                    mediaStore,
                    onClick = {
                        val timeline = summary.timeline as Timeline.Custom
                        if (selectedTimelineIds.isEmpty()) onOpenTimeline(TimelineHomeNavigation(timeline))
                        else onToggleTimelineSelection(timeline)
                    },
                    onLongClick = { onShowTimelineOptions(summary.timeline as Timeline.Custom) },
                    isSelected = (summary.timeline as Timeline.Custom).id in selectedTimelineIds,
                )
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
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    showDraftIndicator: Boolean = false,
) {
    val override = (summary.timeline as? Timeline.Custom)?.theme
    val themeId = override?.toReliveThemeId() ?: ReliveTheme.tokens.id
    val darkMode = ReliveTheme.isDark
    val content: @Composable () -> Unit = {
        TimelineHomeCardContent(
            summary = summary,
            mediaStore = mediaStore,
            allCollageBucket = allCollageBucket,
            allCollageCandidates = allCollageCandidates,
            onClick = onClick,
            onLongClick = onLongClick,
            isSelected = isSelected,
            showDraftIndicator = showDraftIndicator,
        )
    }
    com.vaibhav.relive.ui.theme.ReliveTheme(
        themeId = themeId,
        darkMode = darkMode,
        content = content,
    )
}

@Composable
private fun TimelineHomeCardContent(
    summary: TimelineHomeSummary,
    mediaStore: MediaStore,
    allCollageBucket: Long,
    allCollageCandidates: List<MediaAttachment>?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    isSelected: Boolean,
    showDraftIndicator: Boolean,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val cardColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent.copy(alpha = ReliveOpacity.Low) else colors.surfaceCard,
        animationSpec = tween(motion.durations.fastMillis, easing = motion.easings.emphasized),
        label = "timeline card selection",
    )
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
            .background(cardColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        TimelineHomeMediaPreview(
            summary = summary,
            mediaStore = mediaStore,
            mediaHeight = mediaHeight,
            allCollageBucket = allCollageBucket,
            allCollageCandidates = allCollageCandidates,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dims.timelineHome.infoAreaMinHeight)
                .padding(dims.spacing.lg),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(summary.name, style = ReliveTheme.typography.title, color = colors.textPrimary, maxLines = 2)
            }
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
                if (showDraftIndicator) {
                    Text(
                        text = "DRAFT",
                        style = ReliveTheme.typography.tag,
                        color = colors.accentMuted,
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
internal fun TimelineHomeMediaPreview(
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
