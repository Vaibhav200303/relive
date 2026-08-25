package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.policy.EditWindow
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.ThemeReference
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
import com.vaibhav.relive.presentation.composer.TimelineComposerDraftStore
import com.vaibhav.relive.presentation.composer.MomentSaveOutcome
import com.vaibhav.relive.presentation.composer.ComposerOverlay
import com.vaibhav.relive.presentation.composer.SaveState
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import com.vaibhav.relive.presentation.timeline.TimelineMomentsState
import com.vaibhav.relive.presentation.timeline.TimelineMode
import com.vaibhav.relive.presentation.timeline.TimelineMomentVisibility
import com.vaibhav.relive.presentation.timeline.resolveTimelineMomentVisibility
import com.vaibhav.relive.presentation.timeline.TimelineScreenState
import com.vaibhav.relive.presentation.timeline.TimelineViewModel
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.presentation.timeline.toMoment
import com.vaibhav.relive.presentation.navigation.shouldExpandComposerOnEnter
import com.vaibhav.relive.presentation.viewer.TimelineMediaNavState
import com.vaibhav.relive.presentation.viewer.closeGallery
import com.vaibhav.relive.presentation.viewer.closeViewer
import com.vaibhav.relive.presentation.viewer.openFromCollage
import com.vaibhav.relive.presentation.viewer.openFromGallery
import com.vaibhav.relive.ui.components.composer.CollapsedComposerMarker
import com.vaibhav.relive.ui.components.composer.ComposerOverlayHost
import com.vaibhav.relive.ui.components.composer.MediaPickerDriver
import com.vaibhav.relive.ui.components.composer.MomentComposer
import com.vaibhav.relive.ui.components.timeline.EmptyCustomTimelinePlaceholder
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.components.timeline.TimelineHeader
import com.vaibhav.relive.ui.components.timeline.DateNavigationPicker
import com.vaibhav.relive.ui.components.timeline.DiscardTimelineDraftDialog
import com.vaibhav.relive.ui.components.timeline.SystemCollectionHeader
import com.vaibhav.relive.ui.components.viewer.MediaViewer
import com.vaibhav.relive.ui.components.viewer.MomentMediaGallery
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.components.settings.RelivePalettePicker
import com.vaibhav.relive.ui.theme.ReliveTheme
import kotlinx.coroutines.launch

private fun timelineViewModelCanEdit(moment: MomentPresentation, clock: Clock): Boolean =
    EditWindow.isEditable(moment.toMoment(), clock)

internal fun cleanupForgottenAttachments(moment: com.vaibhav.relive.domain.model.Moment, mediaStore: MediaStore) {
    moment.attachments.forEach { attachment -> runCatching { mediaStore.delete(attachment.storageRef) } }
}

internal sealed interface ComposerCloseAction {
    data object ResetAndCollapse : ComposerCloseAction
    data object ShowDiscardConfirmation : ComposerCloseAction
}

internal data class ComposerDiscardConfirmationState(val isVisible: Boolean = false) {
    fun onCloseRequested(
        hasUserDraft: Boolean,
        confirmBeforeDiscarding: Boolean,
    ): ComposerDiscardTransition =
        if (hasUserDraft && confirmBeforeDiscarding) {
            ComposerDiscardTransition(copy(isVisible = true), ComposerCloseAction.ShowDiscardConfirmation)
        } else {
            ComposerDiscardTransition(copy(isVisible = false), ComposerCloseAction.ResetAndCollapse)
        }

    fun onCancelled(): ComposerDiscardConfirmationState = copy(isVisible = false)

    fun onDiscarded(): ComposerDiscardTransition =
        ComposerDiscardTransition(copy(isVisible = false), ComposerCloseAction.ResetAndCollapse)
}

internal data class ComposerDiscardTransition(
    val state: ComposerDiscardConfirmationState,
    val action: ComposerCloseAction,
)

@Composable
fun TimelineScreen(
    momentRepository: MomentRepository,
    timelineRepository: TimelineRepository,
    rediscoverRepository: RediscoverRepository,
    clock: Clock,
    idGenerator: IdGenerator,
    mediaStore: MediaStore,
    mediaProcessor: MediaProcessor,
    draftStore: TimelineComposerDraftStore? = null,
    initialTimeline: CurrentTimeline = CurrentTimeline.All,
    mode: TimelineMode = TimelineMode.Editable,
    selectedMomentId: MomentId? = null,
    openComposerOnEnter: Boolean = false,
    onComposerOpenIntentConsumed: (() -> Unit)? = null,
    onBackToTimelineHome: (() -> Unit)? = null,
    globalTheme: ThemeReference = ThemeReference.WarmJournal,
    behaviorPreferences: BehaviorPreferences = BehaviorPreferences(),
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
            draftStore = draftStore,
        )
    }
    val timelineState by timelineViewModel.state.collectAsState()
    val composerState by composerViewModel.state.collectAsState()

    val leaveTimeline: () -> Unit = {
        composerViewModel.preserveDraft()
        onBackToTimelineHome?.invoke()
        Unit
    }
    ReliveBackHandler(enabled = onBackToTimelineHome != null) { leaveTimeline() }

    var navState by remember { mutableStateOf(TimelineMediaNavState.Idle) }
    var isComposerExpanded by remember { mutableStateOf(false) }
    var wasSaving by remember { mutableStateOf(false) }
    var wasEditingWhenSaving by remember { mutableStateOf(false) }
    var momentToForget by remember { mutableStateOf<MomentPresentation?>(null) }
    var editorBounds by remember { mutableStateOf<Rect?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var discardConfirmation by remember { mutableStateOf(ComposerDiscardConfirmationState()) }
    var requestComposerFocus by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showDeleteTimelineConfirmation by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberReliveHaptics()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val dismissKeyboardThen: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        action()
    }

    val composerDestinationSettled = timelineState.moments != TimelineMomentsState.Loading
    val isTimelineEmpty = timelineState.moments == TimelineMomentsState.Empty
    LaunchedEffect(openComposerOnEnter, timelineState.currentTimeline, composerDestinationSettled, isTimelineEmpty) {
        if (mode.allowsMutations && shouldExpandComposerOnEnter(
                requested = openComposerOnEnter,
                currentTimeline = timelineState.currentTimeline,
                isAlreadyExpanded = isComposerExpanded,
                isDestinationSettled = composerDestinationSettled,
                isTimelineEmpty = isTimelineEmpty,
            )
        ) {
            // Let the destination render once in its normal collapsed state so AnimatedContent
            // observes a real false -> true transition instead of entering already open.
            withFrameNanos { }
            if (isComposerExpanded) return@LaunchedEffect
            composerViewModel.prepareForTimeline(timelineState.currentTimeline)
            isComposerExpanded = true
            // Focus only after the expanding composer has entered composition.
            withFrameNanos { }
            requestComposerFocus = true
            // Clearing the route intent before this point cancels this effect during
            // recomposition, leaving All collapsed. Consume it only after expansion.
            if (openComposerOnEnter) onComposerOpenIntentConsumed?.invoke()
        }
    }

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
    LaunchedEffect(composerViewModel) {
        composerViewModel.saveOutcomes.collect { outcome ->
            haptics.perform(
                when (outcome) {
                    MomentSaveOutcome.Succeeded -> ReliveHapticCue.Confirm
                    MomentSaveOutcome.Rejected -> ReliveHapticCue.Reject
                },
            )
        }
    }
    LaunchedEffect(timelineState.dateNavigation) {
        timelineState.dateNavigation?.message?.let { snackbarHostState.showSnackbar(it) }
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
        momentToForget == null

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
            onBack = if (onBackToTimelineHome == null) null else leaveTimeline,
            onTitleChange = composerViewModel::updateTitle,
            onContentChange = composerViewModel::updateContent,
            onLocationChange = composerViewModel::updateManualLocation,
            onPendingTagChange = composerViewModel::updatePendingTagInput,
            onCommitPendingTag = composerViewModel::commitPendingTag,
            onRemoveTag = composerViewModel::removeTag,
            onToggleTimelineAssignment = composerViewModel::toggleTimelineAssignment,
            onToggleAddMedia = { dismissKeyboardThen(composerViewModel::toggleAddMedia) },
            onMicTap = { dismissKeyboardThen(composerViewModel::requestMicPermission) },
            onCameraTap = { dismissKeyboardThen(composerViewModel::openCamera) },
            onLibraryTap = { dismissKeyboardThen(composerViewModel::openLibraryChoice) },
            onStopRecording = composerViewModel::stopRecording,
            onCancelRecording = composerViewModel::cancelRecording,
            onRemoveAttachment = { draftId ->
                if (composerState.isEditing) ActivePlayback.stopActive()
                composerViewModel.removeAttachment(draftId)
            },
            onRetryAttachment = composerViewModel::retryAttachment,
            onReset = {
                val transition = discardConfirmation.onCloseRequested(
                    hasUserDraft = composerState.hasUserDraft,
                    confirmBeforeDiscarding = behaviorPreferences.confirmBeforeDiscarding,
                )
                discardConfirmation = transition.state
                if (transition.action == ComposerCloseAction.ResetAndCollapse) {
                    composerViewModel.reset()
                    isComposerExpanded = false
                }
            },
            onKeepMoment = composerViewModel::keepMoment,
            isComposerExpanded = isComposerExpanded,
            onExpandComposer = {
                if (!composerState.hasUserDraft) {
                    composerViewModel.prepareForTimeline(timelineState.currentTimeline)
                }
                isComposerExpanded = true
            },
            requestComposerFocus = requestComposerFocus,
            onComposerFocusHandled = { requestComposerFocus = false },
            onMicPermissionResult = composerViewModel::onMicPermissionResult,
            onDismissMicPermissionMessage = composerViewModel::dismissMicPermissionMessage,
            onOpenAppSettings = composerViewModel::openAppSettings,
            onEditorBoundsChanged = { editorBounds = it },
            onJumpToDate = { showDatePicker = true },
            onChangeTheme = if (
                mode.allowsMutations && timelineState.currentTimeline is CurrentTimeline.Custom
            ) {
                { showThemePicker = true }
            } else {
                null
            },
            dateNavigationTargetId = timelineState.dateNavigation?.momentId,
            onDateNavigationHandled = timelineViewModel::consumeDateNavigation,
            snackbarHostState = snackbarHostState,
            momentVisibility = resolveTimelineMomentVisibility(mode, behaviorPreferences),
        )

        if (showDatePicker) {
            DateNavigationPicker(
                initialDate = RediscoverCalendar.localDate(clock.now()),
                onDismiss = { showDatePicker = false },
                onDateSelected = { date ->
                    showDatePicker = false
                    timelineViewModel.jumpToDate(date)
                },
            )
        }

        if (showThemePicker) {
            val currentId = (timelineState.currentTimeline as? CurrentTimeline.Custom)?.id
            val selectedTheme = timelineState.customTimelines.firstOrNull { it.id == currentId }?.theme
            TimelineThemeDialog(
                selectedTheme = selectedTheme,
                globalTheme = globalTheme,
                onDismiss = { showThemePicker = false },
                onSelect = { theme ->
                    timelineViewModel.updateCurrentTimelineTheme(theme) { succeeded ->
                        if (succeeded) {
                            showThemePicker = false
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Could not save timeline theme.") }
                        }
                    }
                },
                onDeleteTimeline = { showThemePicker = false; showDeleteTimelineConfirmation = true },
            )
        }

        if (showDeleteTimelineConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteTimelineConfirmation = false },
                shape = RoundedCornerShape(ReliveTheme.dimensions.radii.dialog),
                containerColor = ReliveTheme.colors.surfaceOverlay,
                title = { Text("Delete this timeline?", style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary) },
                text = { Text("Only this custom timeline and its assignments will be removed. Its Moments stay safely in Relive.", style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary) },
                dismissButton = { TextButton(onClick = { showDeleteTimelineConfirmation = false }) { Text("Cancel") } },
                confirmButton = {
                    Button(
                        onClick = {
                            timelineViewModel.deleteCurrentCustomTimeline { deleted ->
                                showDeleteTimelineConfirmation = false
                                if (deleted) {
                                    haptics.perform(ReliveHapticCue.Confirm)
                                    leaveTimeline()
                                } else {
                                    haptics.perform(ReliveHapticCue.Reject)
                                    scope.launch { snackbarHostState.showSnackbar("Could not delete timeline.") }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ReliveTheme.colors.actionDestructive, contentColor = ReliveTheme.colors.textOnDestructive),
                    ) { Text("Delete timeline") }
                },
            )
        }

        ComposerOverlayHost(
            overlay = composerState.overlay,
            mediaStore = mediaStore,
            onCaptured = composerViewModel::processRaw,
            onDismiss = composerViewModel::dismissOverlay,
            onPick = { mediaType ->
                dismissKeyboardThen { composerViewModel.requestPick(mediaType) }
            },
            onOpenLibraryFromCamera = {
                dismissKeyboardThen(composerViewModel::openLibraryChoice)
            },
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

    if (mode.allowsMutations) momentToForget?.let { moment ->
        AlertDialog(
            onDismissRequest = { momentToForget = null },
            shape = RoundedCornerShape(ReliveTheme.dimensions.radii.dialog),
            containerColor = ReliveTheme.colors.surfaceOverlay,
            title = {
                Text(
                    "Forget this moment?",
                    style = ReliveTheme.typography.title,
                    color = ReliveTheme.colors.textPrimary,
                )
            },
            text = {
                Text(
                    "This permanently removes it from Relive.",
                    style = ReliveTheme.typography.body,
                    color = ReliveTheme.colors.textSecondary,
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { momentToForget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = ReliveTheme.colors.textSecondary),
                ) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ActivePlayback.stopActive()
                        timelineViewModel.forget(
                            moment = moment.toMoment(),
                            onDeleted = { deleted ->
                                cleanupForgottenAttachments(deleted, mediaStore)
                                haptics.perform(ReliveHapticCue.Confirm)
                                momentToForget = null
                            },
                            onFailure = {
                                haptics.perform(ReliveHapticCue.Reject)
                                momentToForget = null
                            },
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReliveTheme.colors.actionDestructive,
                        contentColor = ReliveTheme.colors.textOnDestructive,
                    ),
                ) { Text("Forget") }
            },
        )
    }

    if (mode.allowsMutations && discardConfirmation.isVisible) {
        DiscardTimelineDraftDialog(
            onDiscard = {
                val transition = discardConfirmation.onDiscarded()
                discardConfirmation = transition.state
                composerViewModel.reset()
                isComposerExpanded = false
            },
            onKeepEditing = {
                discardConfirmation = discardConfirmation.onCancelled()
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
    onToggleFavorite: (MomentId, Boolean) -> Unit,
    onEditMoment: (MomentPresentation) -> Unit,
    onForgetMoment: (MomentPresentation) -> Unit,
    onOpenMedia: (List<MomentAttachmentPresentation>, Int) -> Unit,
    onBack: (() -> Unit)?,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
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
    requestComposerFocus: Boolean,
    onComposerFocusHandled: () -> Unit,
    onJumpToDate: () -> Unit,
    onChangeTheme: (() -> Unit)?,
    dateNavigationTargetId: MomentId?,
    onDateNavigationHandled: () -> Unit,
    snackbarHostState: SnackbarHostState,
    momentVisibility: TimelineMomentVisibility,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas),
    ) {
        if (mode is TimelineMode.ReadOnlySystemCollection) {
            SystemCollectionHeader(title = mode.title, onBack = onBack ?: {})
        } else {
            TimelineHeader(
                onBack = onBack,
                onJumpToDate = onJumpToDate,
                onChangeTheme = onChangeTheme,
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

            key(timelineState.currentTimeline) {
                val listState = rememberLazyListState()
                var lastSeenCount by remember { mutableIntStateOf(-1) }
                LaunchedEffect(moments, selectedMomentId, dateNavigationTargetId) {
                    val targetId = dateNavigationTargetId ?: selectedMomentId
                    val selectedIndex = targetId?.let { selected ->
                        moments.indexOfFirst { it.id == selected }.takeIf { it >= 0 }
                    }
                    val target = selectedIndex ?: when {
                        customName != null && moments.isEmpty() -> 0
                        mode.allowsMutations -> moments.size
                        else -> moments.lastIndex.coerceAtLeast(0)
                    }
                    if (dateNavigationTargetId != null) {
                        listState.animateScrollToItem(target)
                    } else if (lastSeenCount == -1) {
                        listState.scrollToItem(target)
                    } else if (moments.size > lastSeenCount) {
                        listState.animateScrollToItem(target)
                    }
                    lastSeenCount = moments.size
                    if (dateNavigationTargetId != null || timelineState.dateNavigation != null) {
                        onDateNavigationHandled()
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = dims.spacing.huge),
                ) {
                    itemsIndexed(items = moments, key = { _, moment -> moment.id.value }) { index, moment ->
                        if (composerState.editingMoment?.id == moment.id) {
                            MomentComposer(
                                state = composerState,
                                customTimelines = emptyList(),
                                clock = clock,
                                mediaStore = mediaStore,
                                onTitleChange = onTitleChange,
                                onContentChange = onContentChange,
                                onLocationChange = onLocationChange,
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
                                hasPreviousMoment = index > 0,
                                showLocation = momentVisibility.showLocations,
                                showTags = momentVisibility.showTags,
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
                                val expandMs = motion.durations.slowMillis
                                val collapseMs = motion.durations.standardMillis
                                val enter = expandVertically(
                                    animationSpec = tween(expandMs, easing = motion.easings.standard),
                                    expandFrom = Alignment.Top,
                                ) + fadeIn(animationSpec = tween(expandMs, easing = motion.easings.standard))
                                val exit = shrinkVertically(
                                    animationSpec = tween(collapseMs, easing = motion.easings.standard),
                                    shrinkTowards = Alignment.Top,
                                ) + fadeOut(animationSpec = tween(collapseMs, easing = motion.easings.standard))
                                (enter togetherWith exit).using(
                                    SizeTransform(
                                        clip = false,
                                        sizeAnimationSpec = { _, _ ->
                                            tween(
                                                durationMillis = if (targetState) expandMs else collapseMs,
                                                easing = motion.easings.standard,
                                            )
                                        },
                                    ),
                                )
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
                                    onLocationChange = onLocationChange,
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
                                    requestInitialFocus = requestComposerFocus,
                                    onInitialFocusHandled = onComposerFocusHandled,
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
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(ReliveTheme.dimensions.radii.menu),
                    containerColor = ReliveTheme.colors.accent,
                    contentColor = ReliveTheme.colors.textOnAccent,
                    actionColor = ReliveTheme.colors.textOnAccent,
                    actionContentColor = ReliveTheme.colors.textOnAccent,
                    dismissActionContentColor = ReliveTheme.colors.textOnAccent,
                )
            }
        }
    }
}

@Composable
private fun TimelineThemeDialog(
    selectedTheme: ThemeReference?,
    globalTheme: ThemeReference,
    onDismiss: () -> Unit,
    onSelect: (ThemeReference?) -> Unit,
    onDeleteTimeline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(ReliveTheme.dimensions.radii.dialog),
        containerColor = ReliveTheme.colors.surfaceOverlay,
        title = {
            Text(
                "Timeline theme",
                style = ReliveTheme.typography.title,
                color = ReliveTheme.colors.textPrimary,
            )
        },
        text = {
            RelivePalettePicker(
                selectedTheme = selectedTheme,
                globalTheme = globalTheme,
                includeUseAppTheme = true,
                onSelect = onSelect,
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDeleteTimeline,
                colors = ButtonDefaults.textButtonColors(contentColor = ReliveTheme.colors.actionDestructive),
            ) { Text("Delete timeline") }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = ReliveTheme.colors.accent),
            ) { Text("Close") }
        },
    )
}
