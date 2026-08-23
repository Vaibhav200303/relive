package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.policy.EditWindow
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.media.ActivePlayback
import com.vaibhav.relive.platform.media.MediaProcessor
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.rememberMediaPickerHandle
import com.vaibhav.relive.platform.permission.MicPermissionResult
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.composer.MomentComposerState
import com.vaibhav.relive.presentation.composer.MomentComposerViewModel
import com.vaibhav.relive.presentation.composer.ComposerOverlay
import com.vaibhav.relive.presentation.composer.SaveState
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import com.vaibhav.relive.presentation.timeline.TimelineMomentsState
import com.vaibhav.relive.presentation.timeline.TimelineMode
import com.vaibhav.relive.presentation.timeline.TimelineScreenState
import com.vaibhav.relive.presentation.timeline.TimelineViewModel
import com.vaibhav.relive.presentation.timeline.toMoment
import com.vaibhav.relive.presentation.viewer.TimelineMediaNavState
import com.vaibhav.relive.presentation.viewer.closeGallery
import com.vaibhav.relive.presentation.viewer.closeViewer
import com.vaibhav.relive.presentation.viewer.openFromCollage
import com.vaibhav.relive.presentation.viewer.openFromGallery
import com.vaibhav.relive.ui.components.composer.CollapsedComposerMarker
import com.vaibhav.relive.ui.components.composer.ComposerOverlayHost
import com.vaibhav.relive.ui.components.composer.MediaPickerDriver
import com.vaibhav.relive.ui.components.composer.MomentComposer
import com.vaibhav.relive.ui.components.timeline.DiscardTimelineDraftDialog
import com.vaibhav.relive.ui.components.timeline.EmptyCustomTimelinePlaceholder
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.components.timeline.TimelineCreationDialog
import com.vaibhav.relive.ui.components.timeline.TimelineHeader
import com.vaibhav.relive.ui.components.timeline.SystemCollectionHeader
import com.vaibhav.relive.ui.components.timeline.TimelineSelector
import com.vaibhav.relive.ui.components.viewer.MediaViewer
import com.vaibhav.relive.ui.components.viewer.MomentMediaGallery
import com.vaibhav.relive.ui.theme.ReliveTheme

private fun timelineViewModelCanEdit(moment: MomentPresentation, clock: Clock): Boolean =
    EditWindow.isEditable(moment.toMoment(), clock)

internal fun cleanupForgottenAttachments(moment: com.vaibhav.relive.domain.model.Moment, mediaStore: MediaStore) {
    moment.attachments.forEach { attachment -> runCatching { mediaStore.delete(attachment.storageRef) } }
}

@Composable
fun TimelineScreen(
    momentRepository: MomentRepository,
    timelineRepository: TimelineRepository,
    rediscoverRepository: RediscoverRepository,
    clock: Clock,
    idGenerator: IdGenerator,
    mediaStore: MediaStore,
    mediaProcessor: MediaProcessor,
    initialTimeline: CurrentTimeline = CurrentTimeline.All,
    mode: TimelineMode = TimelineMode.Editable,
    selectedMomentId: MomentId? = null,
    onBackToTimelineHome: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val timelineViewModel = remember(
        momentRepository,
        timelineRepository,
        rediscoverRepository,
        clock,
        idGenerator,
        scope,
        initialTimeline,
        mode,
    ) {
        TimelineViewModel(
            momentRepository = momentRepository,
            timelineRepository = timelineRepository,
            rediscoverRepository = rediscoverRepository,
            clock = clock,
            idGenerator = idGenerator,
            scope = scope,
            initialTimeline = initialTimeline,
            mode = mode,
        )
    }
    val composerViewModel = remember(
        momentRepository,
        clock,
        idGenerator,
        scope,
        mediaStore,
        mediaProcessor,
    ) {
        MomentComposerViewModel(
            momentRepository = momentRepository,
            clock = clock,
            idGenerator = idGenerator,
            scope = scope,
            mediaStore = mediaStore,
            mediaProcessor = mediaProcessor,
        )
    }
    val timelineState by timelineViewModel.state.collectAsState()
    val composerState by composerViewModel.state.collectAsState()

    ReliveBackHandler(enabled = onBackToTimelineHome != null) { onBackToTimelineHome?.invoke() }

    var navState by remember { mutableStateOf(TimelineMediaNavState.Idle) }
    var isComposerExpanded by remember { mutableStateOf(false) }
    var pendingTimelineSwitch by remember { mutableStateOf<CurrentTimeline?>(null) }
    var wasSaving by remember { mutableStateOf(false) }
    var wasEditingWhenSaving by remember { mutableStateOf(false) }
    var momentToForget by remember { mutableStateOf<MomentPresentation?>(null) }
    var editorBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(composerState.isEditing) {
        if (!composerState.isEditing) editorBounds = null
    }

    LaunchedEffect(composerState.saveState) {
        val nowSaving = composerState.saveState is SaveState.Saving
        if (wasSaving && composerState.saveState is SaveState.Idle) {
            if (!wasEditingWhenSaving) isComposerExpanded = false
        }
        if (nowSaving) wasEditingWhenSaving = composerState.isEditing
        wasSaving = nowSaving
    }

    fun completeTimelineSwitch(target: CurrentTimeline) {
        ActivePlayback.stopActive()
        navState = TimelineMediaNavState.Idle
        composerViewModel.reset()
        composerViewModel.prepareForTimeline(target)
        isComposerExpanded = false
        timelineViewModel.selectTimeline(target)
    }

    fun requestTimelineSwitch(target: CurrentTimeline) {
        if (target == timelineState.currentTimeline || composerState.isSaving) return
        if (composerState.hasUserDraft) {
            pendingTimelineSwitch = target
        } else {
            completeTimelineSwitch(target)
        }
    }

    val pickerHandle = rememberMediaPickerHandle(mediaStore)
    MediaPickerDriver(
        pending = composerState.pendingMediaAction,
        handle = pickerHandle,
        onResult = composerViewModel::processRawBatch,
        onClear = composerViewModel::clearPendingMediaAction,
    )

    val outsideTapSaveEnabled = mode.allowsMutations && composerState.canSaveActiveEdit &&
        composerState.overlay == ComposerOverlay.None &&
        composerState.pendingMediaAction == null &&
        !composerState.pendingMicPermissionRequest &&
        momentToForget == null &&
        pendingTimelineSwitch == null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(editorBounds, outsideTapSaveEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        ?: return@awaitEachGesture
                    val wasTap = up.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis &&
                        (up.position - down.position).getDistance() <= viewConfiguration.touchSlop
                    if (wasTap && outsideTapSaveEnabled && isTapOutsideEditor(editorBounds, up.position)) {
                        composerViewModel.keepMoment()
                    }
                }
            },
    ) {
        TimelineContent(
            timelineState = timelineState,
            composerState = composerState,
            mode = mode,
            selectedMomentId = selectedMomentId,
            clock = clock,
            mediaStore = mediaStore,
            onSelectTimeline = ::requestTimelineSwitch,
            onAddTimeline = timelineViewModel::showTimelineCreation,
            onToggleFavorite = timelineViewModel::setFavorite,
            onEditMoment = { moment ->
                if (mode.allowsMutations && timelineViewModel.canEditOrForget(moment.toMoment()) &&
                    composerViewModel.beginEdit(moment.toMoment())
                ) {
                    ActivePlayback.stopActive()
                }
            },
            onForgetMoment = { moment -> momentToForget = moment },
            onOpenMedia = { list, index ->
                ActivePlayback.stopActive()
                navState = navState.openFromCollage(list, index)
            },
            onMenuClick = { },
            onSearchClick = { },
            onBack = onBackToTimelineHome,
            onTitleChange = composerViewModel::updateTitle,
            onContentChange = composerViewModel::updateContent,
            onPendingTagChange = composerViewModel::updatePendingTagInput,
            onCommitPendingTag = composerViewModel::commitPendingTag,
            onRemoveTag = composerViewModel::removeTag,
            onToggleTimelineAssignment = composerViewModel::toggleTimelineAssignment,
            onToggleAddMedia = composerViewModel::toggleAddMedia,
            onMicTap = composerViewModel::requestMicPermission,
            onCameraTap = composerViewModel::openCamera,
            onLibraryTap = composerViewModel::openLibraryChoice,
            onStopRecording = composerViewModel::stopRecording,
            onCancelRecording = composerViewModel::cancelRecording,
            onRemoveAttachment = { draftId ->
                if (composerState.isEditing) ActivePlayback.stopActive()
                composerViewModel.removeAttachment(draftId)
            },
            onRetryAttachment = composerViewModel::retryAttachment,
            onReset = {
                composerViewModel.reset()
                isComposerExpanded = false
            },
            onKeepMoment = composerViewModel::keepMoment,
            isComposerExpanded = isComposerExpanded,
            onExpandComposer = {
                if (!composerState.hasUserDraft) {
                    composerViewModel.prepareForTimeline(timelineState.currentTimeline)
                    isComposerExpanded = true
                }
            },
            onMicPermissionResult = composerViewModel::onMicPermissionResult,
            onDismissMicPermissionMessage = composerViewModel::dismissMicPermissionMessage,
            onOpenAppSettings = composerViewModel::openAppSettings,
            onEditorBoundsChanged = { editorBounds = it },
        )

        ComposerOverlayHost(
            overlay = composerState.overlay,
            mediaStore = mediaStore,
            onCaptured = composerViewModel::processRaw,
            onDismiss = composerViewModel::dismissOverlay,
            onPick = composerViewModel::requestPick,
            onOpenLibraryFromCamera = composerViewModel::openLibraryChoice,
        )

        val gallery = navState.gallery
        val viewer = navState.viewer
        if (gallery != null) {
            MomentMediaGallery(
                state = gallery,
                mediaStore = mediaStore,
                onOpenItem = { index ->
                    ActivePlayback.stopActive()
                    navState = navState.openFromGallery(index)
                },
                onClose = {
                    ActivePlayback.stopActive()
                    navState = navState.closeGallery()
                },
                backEnabled = viewer == null,
            )
        }
        if (viewer != null) {
            MediaViewer(
                state = viewer,
                mediaStore = mediaStore,
                onIndexChange = { index -> navState = navState.copy(viewer = viewer.withCurrent(index)) },
                onClose = {
                    ActivePlayback.stopActive()
                    navState = navState.closeViewer()
                },
            )
        }
    }

    if (mode.allowsMutations) {
        TimelineCreationDialog(
            state = timelineState.creation,
            onNameChange = timelineViewModel::updateTimelineName,
            onCreate = timelineViewModel::createTimeline,
            onDismiss = timelineViewModel::dismissTimelineCreation,
        )
    }

    pendingTimelineSwitch?.let { target ->
        DiscardTimelineDraftDialog(
            onDiscard = {
                pendingTimelineSwitch = null
                completeTimelineSwitch(target)
            },
            onKeepEditing = { pendingTimelineSwitch = null },
        )
    }

    if (mode.allowsMutations) momentToForget?.let { moment ->
        AlertDialog(
            onDismissRequest = { momentToForget = null },
            title = { Text("Forget this moment?") },
            text = { Text("This permanently removes it from Relive.") },
            dismissButton = { TextButton(onClick = { momentToForget = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    ActivePlayback.stopActive()
                    timelineViewModel.forget(
                        moment = moment.toMoment(),
                        onDeleted = { deleted ->
                            cleanupForgottenAttachments(deleted, mediaStore)
                            momentToForget = null
                        },
                        onFailure = { momentToForget = null },
                    )
                }) { Text("Forget") }
            },
        )
    }
}

@Composable
private fun SystemCollectionEmptyState() {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dims.spacing.huge, horizontal = dims.timeline.contentInset),
    ) {
        Text(
            text = "No favorite moments yet.",
            style = ReliveTheme.typography.title,
            color = ReliveTheme.colors.textPrimary,
        )
        Text(
            text = "Moments you favorite will appear here.",
            style = ReliveTheme.typography.subtitle,
            color = ReliveTheme.colors.textSecondary,
            modifier = Modifier.padding(top = dims.spacing.sm),
        )
    }
}

@Composable
private fun TimelineContent(
    timelineState: TimelineScreenState,
    composerState: MomentComposerState,
    mode: TimelineMode,
    selectedMomentId: MomentId?,
    clock: Clock,
    mediaStore: MediaStore,
    onSelectTimeline: (CurrentTimeline) -> Unit,
    onAddTimeline: () -> Unit,
    onToggleFavorite: (MomentId, Boolean) -> Unit,
    onEditMoment: (MomentPresentation) -> Unit,
    onForgetMoment: (MomentPresentation) -> Unit,
    onOpenMedia: (List<MomentAttachmentPresentation>, Int) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onBack: (() -> Unit)?,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onPendingTagChange: (String) -> Unit,
    onCommitPendingTag: () -> Unit,
    onRemoveTag: (Tag) -> Unit,
    onToggleTimelineAssignment: (TimelineId) -> Unit,
    onToggleAddMedia: () -> Unit,
    onMicTap: () -> Unit,
    onCameraTap: () -> Unit,
    onLibraryTap: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onRetryAttachment: (String) -> Unit,
    onReset: () -> Unit,
    onKeepMoment: () -> Unit,
    onMicPermissionResult: (MicPermissionResult) -> Unit,
    onDismissMicPermissionMessage: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEditorBoundsChanged: (Rect) -> Unit,
    isComposerExpanded: Boolean,
    onExpandComposer: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas),
    ) {
        if (mode is TimelineMode.ReadOnlySystemCollection) {
            SystemCollectionHeader(title = mode.title, onBack = onBack ?: {})
        } else {
            TimelineHeader(onMenuClick = onMenuClick, onSearchClick = onSearchClick, onBack = onBack)
            TimelineSelector(
                timelines = timelineState.customTimelines,
                selected = timelineState.currentTimeline,
                enabled = !composerState.isSaving,
                onSelect = onSelectTimeline,
                onAdd = onAddTimeline,
                modifier = Modifier.padding(bottom = dims.spacing.sm),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dims.timeline.horizontalPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            val moments: List<MomentPresentation> = when (val state = timelineState.moments) {
                TimelineMomentsState.Loading, TimelineMomentsState.Empty -> emptyList()
                is TimelineMomentsState.Loaded -> state.moments
            }
            val customName = (timelineState.currentTimeline as? CurrentTimeline.Custom)?.let { current ->
                timelineState.customTimelines.firstOrNull { it.id == current.id }?.name
            }

            val railInset = remember(dims.timeline.contentInset, dims.timeline.railWidth) {
                (dims.timeline.contentInset - dims.timeline.railWidth) / 2
            }
            Box(
                modifier = Modifier
                    .offset(x = railInset)
                    .width(dims.timeline.railWidth)
                    .fillMaxHeight()
                    .background(colors.borderMuted)
                    .align(Alignment.TopStart),
            )

            key(timelineState.currentTimeline) {
                val listState = rememberLazyListState()
                var lastSeenCount by remember { mutableIntStateOf(-1) }
                LaunchedEffect(moments, selectedMomentId) {
                    val selectedIndex = selectedMomentId?.let { selected ->
                        moments.indexOfFirst { it.id == selected }.takeIf { it >= 0 }
                    }
                    val target = selectedIndex ?: when {
                        customName != null && moments.isEmpty() -> 0
                        mode.allowsMutations -> moments.size
                        else -> moments.lastIndex.coerceAtLeast(0)
                    }
                    if (lastSeenCount == -1) {
                        listState.scrollToItem(target)
                    } else if (moments.size > lastSeenCount) {
                        listState.animateScrollToItem(target)
                    }
                    lastSeenCount = moments.size
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = dims.spacing.huge),
                ) {
                    items(items = moments, key = { it.id.value }) { moment ->
                        if (composerState.editingMoment?.id == moment.id) {
                            MomentComposer(
                                state = composerState,
                                customTimelines = emptyList(),
                                clock = clock,
                                mediaStore = mediaStore,
                                onTitleChange = onTitleChange,
                                onContentChange = onContentChange,
                                onPendingTagChange = onPendingTagChange,
                                onCommitPendingTag = onCommitPendingTag,
                                onRemoveTag = onRemoveTag,
                                onToggleTimelineAssignment = onToggleTimelineAssignment,
                                onToggleAddMedia = onToggleAddMedia,
                                onMicTap = onMicTap,
                                onCameraTap = onCameraTap,
                                onLibraryTap = onLibraryTap,
                                onStopRecording = onStopRecording,
                                onCancelRecording = onCancelRecording,
                                onRemoveAttachment = onRemoveAttachment,
                                onRetryAttachment = onRetryAttachment,
                                onReset = onReset,
                                onKeepMoment = onKeepMoment,
                                onMicPermissionResult = onMicPermissionResult,
                                onDismissMicPermissionMessage = onDismissMicPermissionMessage,
                                onOpenAppSettings = onOpenAppSettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { onEditorBoundsChanged(it.boundsInRoot()) },
                            )
                        } else {
                            MomentCard(
                                moment = moment,
                                mediaStore = mediaStore,
                                onToggleFavorite = if (mode.allowsMutations) {
                                    { value -> onToggleFavorite(moment.id, value) }
                                } else {
                                    null
                                },
                                onOpenMedia = onOpenMedia,
                                canEditOrForget = mode.allowsMutations && timelineViewModelCanEdit(moment, clock),
                                onEdit = { onEditMoment(moment) },
                                onForget = { onForgetMoment(moment) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (mode is TimelineMode.ReadOnlySystemCollection && timelineState.moments == TimelineMomentsState.Empty) {
                        item(key = "system-collection-empty") {
                            SystemCollectionEmptyState()
                        }
                    } else if (customName != null && timelineState.moments == TimelineMomentsState.Empty) {
                        item(key = "custom-empty") {
                            EmptyCustomTimelinePlaceholder(timelineName = customName)
                        }
                    }
                    if (mode.allowsMutations) item(key = "composer") {
                        AnimatedContent(
                            targetState = isComposerExpanded,
                            transitionSpec = {
                                val expandMs = 320
                                val collapseMs = 260
                                val enter = expandVertically(
                                    animationSpec = tween(expandMs, easing = FastOutSlowInEasing),
                                    expandFrom = Alignment.Top,
                                ) + fadeIn(animationSpec = tween(expandMs, easing = FastOutSlowInEasing))
                                val exit = shrinkVertically(
                                    animationSpec = tween(collapseMs, easing = FastOutSlowInEasing),
                                    shrinkTowards = Alignment.Top,
                                ) + fadeOut(animationSpec = tween(collapseMs, easing = FastOutSlowInEasing))
                                enter togetherWith exit
                            },
                            label = "composer-expand",
                            modifier = Modifier.fillMaxWidth(),
                        ) { expanded ->
                            if (expanded) {
                                MomentComposer(
                                    state = composerState,
                                    customTimelines = timelineState.customTimelines,
                                    clock = clock,
                                    mediaStore = mediaStore,
                                    onTitleChange = onTitleChange,
                                    onContentChange = onContentChange,
                                    onPendingTagChange = onPendingTagChange,
                                    onCommitPendingTag = onCommitPendingTag,
                                    onRemoveTag = onRemoveTag,
                                    onToggleTimelineAssignment = onToggleTimelineAssignment,
                                    onToggleAddMedia = onToggleAddMedia,
                                    onMicTap = onMicTap,
                                    onCameraTap = onCameraTap,
                                    onLibraryTap = onLibraryTap,
                                    onStopRecording = onStopRecording,
                                    onCancelRecording = onCancelRecording,
                                    onRemoveAttachment = onRemoveAttachment,
                                    onRetryAttachment = onRetryAttachment,
                                    onReset = onReset,
                                    onKeepMoment = onKeepMoment,
                                    onMicPermissionResult = onMicPermissionResult,
                                    onDismissMicPermissionMessage = onDismissMicPermissionMessage,
                                    onOpenAppSettings = onOpenAppSettings,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                CollapsedComposerMarker(
                                    onExpand = onExpandComposer,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
