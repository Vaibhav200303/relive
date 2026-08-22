package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.media.ActivePlayback
import com.vaibhav.relive.platform.media.MediaProcessor
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.rememberMediaPickerHandle
import com.vaibhav.relive.platform.permission.MicPermissionResult
import com.vaibhav.relive.presentation.composer.MomentComposerState
import com.vaibhav.relive.presentation.composer.MomentComposerViewModel
import com.vaibhav.relive.presentation.timeline.AllTimelineUiState
import com.vaibhav.relive.presentation.timeline.AllTimelineViewModel
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import com.vaibhav.relive.presentation.viewer.TimelineMediaNavState
import com.vaibhav.relive.presentation.viewer.closeGallery
import com.vaibhav.relive.presentation.viewer.closeViewer
import com.vaibhav.relive.presentation.viewer.openFromCollage
import com.vaibhav.relive.presentation.viewer.openFromGallery
import com.vaibhav.relive.presentation.composer.SaveState
import com.vaibhav.relive.ui.components.composer.CollapsedComposerMarker
import com.vaibhav.relive.ui.components.composer.ComposerOverlayHost
import com.vaibhav.relive.ui.components.viewer.MediaViewer
import com.vaibhav.relive.ui.components.viewer.MomentMediaGallery
import com.vaibhav.relive.ui.components.composer.MediaPickerDriver
import com.vaibhav.relive.ui.components.composer.MomentComposer
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.components.timeline.TimelineHeader
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun AllTimelineScreen(
    momentRepository: MomentRepository,
    clock: Clock,
    idGenerator: IdGenerator,
    mediaStore: MediaStore,
    mediaProcessor: MediaProcessor,
) {
    val scope = rememberCoroutineScope()
    val timelineViewModel: AllTimelineViewModel = remember(momentRepository, scope) {
        AllTimelineViewModel(momentRepository, scope)
    }
    val composerViewModel: MomentComposerViewModel = remember(momentRepository, clock, idGenerator, scope, mediaStore, mediaProcessor) {
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

    var navState by remember { mutableStateOf(TimelineMediaNavState.Idle) }
    var isComposerExpanded by remember { mutableStateOf(false) }
    var wasSaving by remember { mutableStateOf(false) }
    LaunchedEffect(composerState.saveState) {
        val nowSaving = composerState.saveState is SaveState.Saving
        if (wasSaving && composerState.saveState is SaveState.Idle) {
            isComposerExpanded = false
        }
        wasSaving = nowSaving
    }

    val pickerHandle = rememberMediaPickerHandle(mediaStore)

    MediaPickerDriver(
        pending = composerState.pendingMediaAction,
        handle = pickerHandle,
        onResult = composerViewModel::processRawBatch,
        onClear = composerViewModel::clearPendingMediaAction,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AllTimelineContent(
            timelineState = timelineState,
            composerState = composerState,
            clock = clock,
            mediaStore = mediaStore,
            onToggleFavorite = timelineViewModel::setFavorite,
            onOpenMedia = { list, idx ->
                ActivePlayback.stopActive()
                navState = navState.openFromCollage(list, idx)
            },
            onMenuClick = { },
            onSearchClick = { },
            onTitleChange = composerViewModel::updateTitle,
            onContentChange = composerViewModel::updateContent,
            onPendingTagChange = composerViewModel::updatePendingTagInput,
            onCommitPendingTag = composerViewModel::commitPendingTag,
            onRemoveTag = composerViewModel::removeTag,
            onToggleAddMedia = composerViewModel::toggleAddMedia,
            onMicTap = composerViewModel::requestMicPermission,
            onCameraTap = composerViewModel::openCamera,
            onLibraryTap = composerViewModel::openLibraryChoice,
            onStopRecording = composerViewModel::stopRecording,
            onCancelRecording = composerViewModel::cancelRecording,
            onRemoveAttachment = composerViewModel::removeAttachment,
            onRetryAttachment = composerViewModel::retryAttachment,
            onReset = {
                composerViewModel.reset()
                isComposerExpanded = false
            },
            onKeepMoment = composerViewModel::keepMoment,
            isComposerExpanded = isComposerExpanded,
            onExpandComposer = { isComposerExpanded = true },
            onMicPermissionResult = composerViewModel::onMicPermissionResult,
            onDismissMicPermissionMessage = composerViewModel::dismissMicPermissionMessage,
            onOpenAppSettings = composerViewModel::openAppSettings,
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
                onOpenItem = { idx ->
                    ActivePlayback.stopActive()
                    navState = navState.openFromGallery(idx)
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
                onIndexChange = { idx -> navState = navState.copy(viewer = viewer.withCurrent(idx)) },
                onClose = {
                    ActivePlayback.stopActive()
                    navState = navState.closeViewer()
                },
            )
        }
    }
}

@Composable
fun AllTimelineContent(
    timelineState: AllTimelineUiState,
    composerState: MomentComposerState,
    clock: Clock,
    mediaStore: MediaStore,
    onToggleFavorite: (MomentId, Boolean) -> Unit,
    onOpenMedia: (List<MomentAttachmentPresentation>, Int) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onPendingTagChange: (String) -> Unit,
    onCommitPendingTag: () -> Unit,
    onRemoveTag: (com.vaibhav.relive.domain.model.Tag) -> Unit,
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
        TimelineHeader(onMenuClick = onMenuClick, onSearchClick = onSearchClick)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dims.timeline.horizontalPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            val moments: List<MomentPresentation> = when (timelineState) {
                AllTimelineUiState.Loading -> emptyList()
                AllTimelineUiState.Empty -> emptyList()
                is AllTimelineUiState.Loaded -> timelineState.moments
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
            val listState = rememberLazyListState()
            var lastSeenCount by remember { mutableIntStateOf(-1) }
            LaunchedEffect(moments.size) {
                val target = moments.size
                if (lastSeenCount == -1) {
                    listState.scrollToItem(target)
                } else if (target > lastSeenCount) {
                    listState.animateScrollToItem(target)
                }
                lastSeenCount = target
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = dims.spacing.huge),
            ) {
                items(items = moments, key = { it.id.value }) { moment ->
                    MomentCard(
                        moment = moment,
                        mediaStore = mediaStore,
                        onToggleFavorite = { newValue -> onToggleFavorite(moment.id, newValue) },
                        onOpenMedia = onOpenMedia,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "composer") {
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
                                clock = clock,
                                mediaStore = mediaStore,
                                onTitleChange = onTitleChange,
                                onContentChange = onContentChange,
                                onPendingTagChange = onPendingTagChange,
                                onCommitPendingTag = onCommitPendingTag,
                                onRemoveTag = onRemoveTag,
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
