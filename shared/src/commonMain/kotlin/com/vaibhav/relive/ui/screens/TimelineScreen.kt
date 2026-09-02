package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.animateFloatingActionButton
import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.policy.EditWindow
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.repository.AppearanceRepository
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
import com.vaibhav.relive.platform.share.IncomingSharePayload
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
import com.vaibhav.relive.presentation.timeline.resolveMomentContextualActionAvailability
import com.vaibhav.relive.presentation.timeline.timelineThemeDestinationOrNull
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
import com.vaibhav.relive.ui.components.ReliveSnackbarHost
import com.vaibhav.relive.ui.components.ReliveAlertDialog
import com.vaibhav.relive.ui.components.timeline.EmptyCustomTimelinePlaceholder
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.components.timeline.TimelineMediaSharedTransition
import com.vaibhav.relive.ui.components.timeline.sharedTransitionKey
import com.vaibhav.relive.ui.components.timeline.TimelineHeader
import com.vaibhav.relive.ui.components.timeline.TimelineWallpaperSurface
import com.vaibhav.relive.ui.components.timeline.TimelineCoverHero
import com.vaibhav.relive.ui.components.timeline.AllTimelineCoverHero
import com.vaibhav.relive.ui.components.timeline.TimelineMomentActionHeader
import com.vaibhav.relive.ui.components.timeline.DateNavigationPicker
import com.vaibhav.relive.ui.components.timeline.DiscardTimelineDraftDialog
import com.vaibhav.relive.ui.components.timeline.DownGlyph
import com.vaibhav.relive.ui.components.timeline.SystemCollectionHeader
import com.vaibhav.relive.ui.components.viewer.MediaViewer
import com.vaibhav.relive.ui.components.viewer.MomentMediaGallery
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.reliveSequentialSlideFade
import com.vaibhav.relive.ui.theme.spec
import com.vaibhav.relive.presentation.cardcover.allTimelineCollageBucket
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private fun timelineViewModelCanEdit(moment: MomentPresentation, clock: Clock): Boolean =
    EditWindow.isEditable(moment.toMoment(), clock)

private fun allTimelineCoverAttachments(moments: List<MomentPresentation>): List<MediaAttachment> =
    moments.flatMap { moment ->
        moment.attachments.mapIndexed { index, attachment ->
            MediaAttachment(
                // Match the persisted attachment identity used by the All card's
                // deterministic cover selection, so both surfaces choose the same tile.
                id = MediaAttachmentId(attachment.id.ifBlank { attachment.storageRef.value }),
                type = attachment.type,
                storageRef = attachment.storageRef,
                sortIndex = index,
            )
        }
    }

private fun allTimelineCoverCandidates(
    attachments: List<MediaAttachment>,
    bucket: Long,
): List<MediaAttachment> = attachments
    .asSequence()
    .filter { it.type == MediaType.Image || it.type == MediaType.Video }
    .sortedWith(compareBy<MediaAttachment>({ allCoverCandidateScore(it.id.value, bucket) }, { it.id.value }))
    .take(9)
    .toList()

/** Mirrors TimelineHome.sq's bounded All-card candidate order. */
private fun allCoverCandidateScore(id: String, bucket: Long): Long {
    fun codeAt(index: Int): Long = id.getOrNull(index)?.code?.toLong() ?: 0L
    val length = id.length.toLong()
    return (
        codeAt(0) * 1_000_003L +
            codeAt(1) * 1_000_033L +
            codeAt(3) * 1_000_037L +
            codeAt(id.length - 2) * 1_000_039L +
            codeAt(id.length - 1) * 1_000_081L +
            length * 1_000_099L +
            bucket * (codeAt(id.length - 2).coerceAtLeast(1L) + codeAt(id.length - 1).coerceAtLeast(1L) + length) * 2_654_435_761L
        ) % 2_147_483_647L
}

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
    appearanceRepository: AppearanceRepository,
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
    incomingShare: IncomingSharePayload? = null,
    onIncomingShareApplied: ((String) -> Unit)? = null,
    onBackToTimelineHome: (() -> Unit)? = null,
    onOpenTimelineTheme: (() -> Unit)? = null,
    behaviorPreferences: BehaviorPreferences = BehaviorPreferences(),
    onComposerExpandedChanged: ((Boolean) -> Unit)? = null,
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
    val appearancePreferences by appearanceRepository.preferences.collectAsState()
    val displayedTimelineState = if (timelineState.currentTimeline == CurrentTimeline.All) {
        timelineState.copy(appearance = appearancePreferences.allTimelineAppearance)
    } else {
        timelineState
    }
    val composerState by composerViewModel.state.collectAsState()

    val leaveTimeline: () -> Unit = {
        composerViewModel.preserveDraft()
        onBackToTimelineHome?.invoke()
        Unit
    }
    ReliveBackHandler(enabled = onBackToTimelineHome != null) {
        if (timelineState.momentActions.selectedMomentId != null) {
            timelineViewModel.clearMomentActionSelection()
        } else {
            leaveTimeline()
        }
    }

    var navState by remember { mutableStateOf(TimelineMediaNavState.Idle) }
    var isComposerExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(isComposerExpanded) {
        onComposerExpandedChanged?.invoke(isComposerExpanded)
    }
    var composerOpenIntentConsumed by remember(initialTimeline, openComposerOnEnter) {
        mutableStateOf(false)
    }
    var wasSaving by remember { mutableStateOf(false) }
    var wasEditingWhenSaving by remember { mutableStateOf(false) }
    var momentToForget by remember { mutableStateOf<MomentPresentation?>(null) }
    var editorBounds by remember { mutableStateOf<Rect?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var discardConfirmation by remember { mutableStateOf(ComposerDiscardConfirmationState()) }
    var showCoverPicker by remember { mutableStateOf(false) }
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
    LaunchedEffect(openComposerOnEnter, incomingShare?.requestId, timelineState.currentTimeline, composerDestinationSettled, isTimelineEmpty) {
        if (mode.allowsMutations && shouldExpandComposerOnEnter(
                requested = openComposerOnEnter && !composerOpenIntentConsumed,
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
            incomingShare?.let { request ->
                composerViewModel.applyIncomingShare(request)
                onIncomingShareApplied?.invoke(request.requestId)
            }
            isComposerExpanded = true
            composerOpenIntentConsumed = true
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

    @OptIn(ExperimentalSharedTransitionApi::class)
    SharedTransitionLayout(
        modifier = Modifier.fillMaxSize(),
    ) {
    val sharedTransitionScope = this
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val viewer = navState.viewer
    val gallery = navState.gallery
    val heroAttachment = viewer?.takeIf { it.attachments.size == 1 }?.current
    val galleryHeroAttachment = gallery?.heroAttachment
    var activeHeroAttachmentId by remember { mutableStateOf<String?>(null) }
    var activeGalleryHeroAttachmentId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(heroAttachment?.sharedTransitionKey()) {
        heroAttachment?.let { activeHeroAttachmentId = it.sharedTransitionKey() }
    }
    LaunchedEffect(galleryHeroAttachment?.sharedTransitionKey()) {
        galleryHeroAttachment?.let { activeGalleryHeroAttachmentId = it.sharedTransitionKey() }
    }
    LaunchedEffect(viewer, activeHeroAttachmentId, motion.durations.long2) {
        if (viewer == null && activeHeroAttachmentId != null) {
            delay(motion.durations.long2.toLong())
            if (navState.viewer == null) activeHeroAttachmentId = null
        }
    }
    LaunchedEffect(gallery, activeGalleryHeroAttachmentId, motion.durations.long2) {
        if (gallery == null && activeGalleryHeroAttachmentId != null) {
            delay(motion.durations.long2.toLong())
            if (navState.gallery == null) activeGalleryHeroAttachmentId = null
        }
    }
    val heroAttachmentId = heroAttachment?.sharedTransitionKey() ?: activeHeroAttachmentId
    val galleryHeroAttachmentId = galleryHeroAttachment?.sharedTransitionKey() ?: activeGalleryHeroAttachmentId
    val heroBoundsTransform = remember(motion, reduceMotion) {
        BoundsTransform { _, _ ->
            motion.spec(
                reduceMotion = reduceMotion,
                full = tween(
                    durationMillis = motion.durations.long2,
                    easing = motion.easings.emphasized,
                ),
            )
        }
    }
    val mediaSharedTransition = TimelineMediaSharedTransition(
        scope = sharedTransitionScope,
        activeAttachmentId = heroAttachmentId,
        viewerVisible = heroAttachment != null,
        activeGalleryAttachmentId = galleryHeroAttachmentId,
        galleryVisible = galleryHeroAttachment != null,
        reduceMotion = reduceMotion,
        boundsTransform = heroBoundsTransform,
    )
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
            timelineState = displayedTimelineState,
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
                    timelineViewModel.clearMomentActionSelection()
                }
            },
            onForgetMoment = { moment -> momentToForget = moment },
            onShowMomentActions = { moment ->
                timelineViewModel.selectMomentForActions(moment.id) {
                    scope.launch { snackbarHostState.showSnackbar("Could not load timeline assignments.") }
                }
            },
            onExitMomentActions = timelineViewModel::clearMomentActionSelection,
            onShowTimelineAssignmentPicker = timelineViewModel::showTimelineAssignmentPicker,
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
            onMicPermissionResult = composerViewModel::onMicPermissionResult,
            onDismissMicPermissionMessage = composerViewModel::dismissMicPermissionMessage,
            onOpenAppSettings = composerViewModel::openAppSettings,
            onEditorBoundsChanged = { editorBounds = it },
            onJumpToDate = { showDatePicker = true },
            onChangeTheme = if (mode.allowsMutations && timelineState.currentTimeline.timelineThemeDestinationOrNull() != null) onOpenTimelineTheme else null,
            onUpdateCover = { showCoverPicker = true },
            dateNavigationTargetId = timelineState.dateNavigation?.momentId,
            onDateNavigationHandled = timelineViewModel::consumeDateNavigation,
            snackbarHostState = snackbarHostState,
            momentVisibility = resolveTimelineMomentVisibility(mode, behaviorPreferences),
            sharedTransition = mediaSharedTransition,
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

        if (showCoverPicker) {
            val custom = (timelineState.currentTimeline as? CurrentTimeline.Custom)?.let { current ->
                timelineState.customTimelines.firstOrNull { it.id == current.id }
            }
            TimelineCoverDialog(
                hasCover = custom?.coverPhotoRef != null,
                onDismiss = { showCoverPicker = false },
                onChoose = {
                    showCoverPicker = false
                    scope.launch {
                        val raw = pickerHandle.pickImage().firstOrNull() ?: return@launch
                        runCatching { mediaProcessor.process(raw) }.onSuccess { processed ->
                            timelineViewModel.updateCurrentTimelineCover(processed.storageRef) { ok ->
                                if (ok) custom?.coverPhotoRef?.let(mediaStore::delete) else mediaStore.delete(processed.storageRef)
                            }
                        }
                    }
                },
                onClear = {
                    val old = custom?.coverPhotoRef
                    timelineViewModel.updateCurrentTimelineCover(null) { ok -> if (ok) old?.let(mediaStore::delete) }
                    showCoverPicker = false
                },
            )
        }

        if (timelineState.momentActions.isAssignmentPickerVisible) {
            TimelineAssignmentDialog(
                timelines = timelineState.customTimelines,
                assignedTimelineIds = timelineState.momentActions.assignedTimelineIds,
                isAssigning = timelineState.momentActions.isAssigning,
                onDismiss = timelineViewModel::dismissTimelineAssignmentPicker,
                onSelect = { timeline ->
                    timelineViewModel.addSelectedMomentToTimeline(timeline.id) { succeeded ->
                        haptics.perform(if (succeeded) ReliveHapticCue.Confirm else ReliveHapticCue.Reject)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (succeeded) "Added to ${timeline.name}." else "Could not add to timeline.",
                            )
                        }
                    }
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

        AnimatedContent(
            targetState = gallery,
            contentKey = { it != null },
            transitionSpec = {
                val fadeSpec = motion.spec<Float>(
                    reduceMotion = reduceMotion,
                    full = tween(
                        durationMillis = motion.durations.long2,
                        easing = motion.easings.emphasized,
                    ),
                )
                fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)
            },
            label = "moment-media-gallery-container",
        ) { galleryState ->
            galleryState?.let { openGallery ->
                MomentMediaGallery(
                    state = openGallery,
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
                    wallpaper = displayedTimelineState.appearance.wallpaper,
                    sharedTransition = mediaSharedTransition,
                )
            }
        }
        AnimatedContent(
            targetState = viewer,
            contentKey = { it != null },
            transitionSpec = {
                val fadeSpec = motion.spec<Float>(
                    reduceMotion = reduceMotion,
                    full = tween(
                        durationMillis = motion.durations.medium4,
                        easing = motion.easings.emphasizedDecelerate,
                    ),
                )
                fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)
            },
            label = "media-viewer-chrome",
        ) { viewerState ->
            viewerState?.let { openViewer ->
                MediaViewer(
                    state = openViewer,
                    mediaStore = mediaStore,
                    onIndexChange = { index -> navState = navState.copy(viewer = openViewer.withCurrent(index)) },
                    onClose = {
                        ActivePlayback.stopActive()
                        navState = navState.closeViewer()
                    },
                    wallpaper = displayedTimelineState.appearance.wallpaper,
                    sharedTransition = mediaSharedTransition,
                )
            }
        }
    }
    }

    if (mode.allowsMutations) momentToForget?.let { moment ->
        ReliveAlertDialog(
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
                                timelineViewModel.clearMomentActionSelection()
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
private fun TimelineAssignmentDialog(
    timelines: List<Timeline.Custom>,
    assignedTimelineIds: Set<TimelineId>,
    isAssigning: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Timeline.Custom) -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val scrollState = rememberScrollState()
    ReliveAlertDialog(
        onDismissRequest = { if (!isAssigning) onDismiss() },
        shape = RoundedCornerShape(dims.radii.dialog),
        containerColor = colors.surfaceOverlay,
        title = {
            Text(
                text = "Add to timeline",
                style = ReliveTheme.typography.title,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dims.spacing.huge * 6)
                    .verticalScroll(scrollState),
            ) {
                if (isAssigning) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(dims.icon.md),
                        color = colors.accent,
                        strokeWidth = dims.stroke.icon,
                    )
                }
                timelines.forEach { timeline ->
                    val isAssigned = timeline.id in assignedTimelineIds
                    TextButton(
                        onClick = { onSelect(timeline) },
                        enabled = !isAssigned && !isAssigning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = dims.minTouchTarget),
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textPrimary),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(timeline.name, style = ReliveTheme.typography.body)
                                if (isAssigned) {
                                    Text(
                                        text = "Already added",
                                        style = ReliveTheme.typography.subtitle,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                            Checkbox(
                                checked = isAssigned,
                                onCheckedChange = null,
                                enabled = isAssigned,
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isAssigning) { Text("Cancel") }
        },
        confirmButton = {},
    )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    onShowMomentActions: (MomentPresentation) -> Unit,
    onExitMomentActions: () -> Unit,
    onShowTimelineAssignmentPicker: () -> Unit,
    onOpenMedia: (List<MomentAttachmentPresentation>, Int) -> Unit,
    sharedTransition: TimelineMediaSharedTransition?,
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
    onJumpToDate: () -> Unit,
    onChangeTheme: (() -> Unit)?,
    onUpdateCover: () -> Unit,
    dateNavigationTargetId: MomentId?,
    onDateNavigationHandled: () -> Unit,
    snackbarHostState: SnackbarHostState,
    momentVisibility: TimelineMomentVisibility,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val moments: List<MomentPresentation> = when (val state = timelineState.moments) {
        TimelineMomentsState.Loading, TimelineMomentsState.Empty -> emptyList()
        is TimelineMomentsState.Loaded -> state.moments
    }
    val selectedActionMoment = timelineState.momentActions.selectedMomentId?.let { selectedId ->
        moments.firstOrNull { it.id == selectedId }
    }
    val actionAvailability = selectedActionMoment?.let { moment ->
        resolveMomentContextualActionAvailability(
            mode = mode,
            currentTimeline = timelineState.currentTimeline,
            isWithinEditWindow = timelineViewModelCanEdit(moment, clock),
            hasCustomTimelines = timelineState.customTimelines.isNotEmpty(),
        )
    }
    val isContextualActionMode = actionAvailability?.canEnter == true
    val hasTimelineCoverHero = timelineState.currentTimeline is CurrentTimeline.Custom ||
        timelineState.currentTimeline == CurrentTimeline.All
    var coverStretchPx by remember { mutableFloatStateOf(0f) }
    val maxCoverStretchPx = with(LocalDensity.current) { (dims.spacing.huge * 3).toPx() }
    val maxCoverPullDeltaPx = with(LocalDensity.current) { dims.spacing.xl.toPx() }
    val coverStretchConnection = remember(hasTimelineCoverHero, maxCoverStretchPx, maxCoverPullDeltaPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Let the LazyColumn consume all normal upward movement first. Only its
                // unconsumed boundary motion grows the cover in onPostScroll, so a fast
                // scroll transitions into the elastic state without a visible stop.
                if (!hasTimelineCoverHero || available.y >= 0f || coverStretchPx <= 0f) {
                    return Offset.Zero
                }
                val consumedY = min(-available.y, coverStretchPx)
                coverStretchPx -= consumedY
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // A non-zero `available` value is only delivered after the list itself
                // has reached its boundary. Rely on it directly so one fast gesture
                // transitions into the elastic cover without waiting for list-state
                // observation to catch up on the following frame.
                if (!hasTimelineCoverHero || source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                val pullDelta = (available.y * 0.7f).coerceAtMost(maxCoverPullDeltaPx)
                coverStretchPx = (coverStretchPx + pullDelta).coerceAtMost(maxCoverStretchPx)
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (coverStretchPx > 0f) {
                    animate(
                        initialValue = coverStretchPx,
                        targetValue = 0f,
                        animationSpec = tween(motion.durations.slowMillis, easing = motion.easings.standard),
                    ) { value, _ -> coverStretchPx = value }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (coverStretchPx > 0f) {
                    animate(
                        initialValue = coverStretchPx,
                        targetValue = 0f,
                        animationSpec = tween(motion.durations.slowMillis, easing = motion.easings.standard),
                    ) { value, _ -> coverStretchPx = value }
                }
                return Velocity.Zero
            }
        }
    }

    TimelineWallpaperSurface(
        wallpaper = timelineState.appearance.wallpaper,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(coverStretchConnection),
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = isContextualActionMode,
            transitionSpec = {
                reliveSequentialSlideFade(
                    motion = motion,
                    reduceMotion = reduceMotion,
                    enterFromRight = targetState,
                )
            },
            label = "moment selection app bar",
        ) { inActionMode ->
            if (inActionMode && selectedActionMoment != null && actionAvailability != null) {
                TimelineMomentActionHeader(
                    showEdit = actionAvailability.canEdit,
                    showAddToTimeline = actionAvailability.canAddToTimeline,
                    addToTimelineEnabled = !timelineState.momentActions.isLoadingAssignments &&
                        !timelineState.momentActions.hasAssignmentLoadFailed,
                    showForget = actionAvailability.canForget,
                    onExit = onExitMomentActions,
                    onEdit = { onEditMoment(selectedActionMoment) },
                    onAddToTimeline = onShowTimelineAssignmentPicker,
                    onForget = { onForgetMoment(selectedActionMoment) },
                )
            } else if (mode is TimelineMode.ReadOnlySystemCollection) {
                SystemCollectionHeader(title = mode.title, onBack = onBack ?: {})
            } else if (timelineState.currentTimeline == CurrentTimeline.All) {
                val collageBucket = allTimelineCollageBucket(clock.now())
                AllTimelineCoverHero(
                    attachments = allTimelineCoverCandidates(allTimelineCoverAttachments(moments), collageBucket),
                    collageBucket = collageBucket,
                    mediaStore = mediaStore,
                    onBack = onBack,
                    onJumpToDate = onJumpToDate,
                    onChangeTheme = onChangeTheme ?: {},
                    stretchPx = coverStretchPx,
                )
            } else if (timelineState.currentTimeline is CurrentTimeline.Custom) {
                val custom = timelineState.customTimelines.firstOrNull { it.id == (timelineState.currentTimeline as CurrentTimeline.Custom).id }
                if (custom != null) TimelineCoverHero(
                    name = custom.name,
                    coverPhotoRef = custom.coverPhotoRef,
                    mediaStore = mediaStore,
                    onBack = onBack,
                    onJumpToDate = onJumpToDate,
                    onChangeTheme = onChangeTheme ?: {},
                    onUpdateCover = onUpdateCover,
                    stretchPx = coverStretchPx,
                )
            } else {
                TimelineHeader(
                    onBack = onBack,
                    onJumpToDate = onJumpToDate,
                    onChangeTheme = onChangeTheme,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dims.timeline.horizontalPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            val customName = (timelineState.currentTimeline as? CurrentTimeline.Custom)?.let { current ->
                timelineState.customTimelines.firstOrNull { it.id == current.id }?.name
            }

            key(timelineState.currentTimeline) {
                val listState = rememberLazyListState()
                var lastSeenCount by remember { mutableIntStateOf(-1) }
                var isProgrammaticScroll by remember { mutableStateOf(false) }
                var lastListPosition by remember { mutableStateOf<TimelineListPosition?>(null) }
                var returnToBottomState by remember { mutableStateOf(TimelineReturnToBottomState()) }
                val autoScrollController = remember { TimelineAutoScrollController() }
                val listScope = rememberCoroutineScope()

                LaunchedEffect(listState.isScrollInProgress) {
                    if (!listState.isScrollInProgress && coverStretchPx > 0f) {
                        animate(
                            initialValue = coverStretchPx,
                            targetValue = 0f,
                            animationSpec = tween(motion.durations.slowMillis, easing = motion.easings.standard),
                        ) { value, _ -> coverStretchPx = value }
                    }
                }

                LaunchedEffect(listState) {
                    snapshotFlow {
                        TimelineListPosition(
                            index = listState.firstVisibleItemIndex,
                            scrollOffset = listState.firstVisibleItemScrollOffset,
                        ) to listState.canScrollForward
                    }.collect { (position, canScrollForward) ->
                        val previous = lastListPosition
                        if (previous != null) {
                            returnToBottomState = returnToBottomState.onPositionChanged(
                                isProgrammaticScroll = isProgrammaticScroll,
                                movedTowardOlderMoments = position.movedTowardOlderThan(previous),
                                canScrollForward = canScrollForward,
                            )
                        }
                        lastListPosition = position
                    }
                }
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
                    isProgrammaticScroll = true
                    try {
                        if (dateNavigationTargetId != null) {
                            listState.animateScrollToItem(target)
                        } else if (lastSeenCount == -1) {
                            listState.scrollToItem(target)
                        } else if (moments.size > lastSeenCount) {
                            listState.animateScrollToItem(target)
                        }
                    } finally {
                        isProgrammaticScroll = false
                    }
                    lastSeenCount = moments.size
                    if (dateNavigationTargetId != null || timelineState.dateNavigation != null) {
                        onDateNavigationHandled()
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(returnToBottomState.isAutoScrolling) {
                            awaitEachGesture {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                if (!returnToBottomState.isAutoScrolling) {
                                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                    return@awaitEachGesture
                                }

                                autoScrollController.cancel()
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                        },
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
                                sharedTransition = sharedTransition,
                                canEditOrForget = mode.allowsMutations && timelineViewModelCanEdit(moment, clock),
                                onEdit = { onEditMoment(moment) },
                                onForget = { onForgetMoment(moment) },
                                onShowContextualActions = if (
                                    resolveMomentContextualActionAvailability(
                                        mode = mode,
                                        currentTimeline = timelineState.currentTimeline,
                                        isWithinEditWindow = timelineViewModelCanEdit(moment, clock),
                                        hasCustomTimelines = timelineState.customTimelines.isNotEmpty(),
                                    ).canEnter
                                ) {
                                    { onShowMomentActions(moment) }
                                } else {
                                    null
                                },
                                isContextuallySelected = selectedActionMoment?.id == moment.id,
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
                val showReturnToBottom = returnToBottomState.isVisible(listState.canScrollForward)
                SmallFloatingActionButton(
                    onClick = {
                        if (!autoScrollController.isRunning && listState.canScrollForward) {
                            returnToBottomState = returnToBottomState.onAutoScrollStarted()
                            autoScrollController.start(
                                scope = listScope,
                                scroll = {
                                    isProgrammaticScroll = true
                                    while (listState.canScrollForward) {
                                        val viewportHeight = listState.layoutInfo
                                            .let { it.viewportEndOffset - it.viewportStartOffset }
                                        if (viewportHeight <= 0) break
                                        val consumed = listState.animateScrollBy(
                                            value = viewportHeight.toFloat(),
                                            animationSpec = tween(
                                                durationMillis = motion.durations.timelineReturnMillis,
                                                easing = LinearEasing,
                                            ),
                                        )
                                        if (consumed < viewportHeight) break
                                    }
                                },
                                onStopped = {
                                    isProgrammaticScroll = false
                                    returnToBottomState = returnToBottomState.onAutoScrollStopped(
                                        canScrollForward = listState.canScrollForward,
                                    )
                                },
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = dims.spacing.lg)
                        .size(dims.minTouchTarget)
                        .animateFloatingActionButton(
                            visible = showReturnToBottom,
                            alignment = Alignment.BottomCenter,
                        )
                        .semantics { contentDescription = "Scroll to newest moment" },
                    shape = RoundedCornerShape(dims.radii.pill),
                    containerColor = colors.surfaceFloating,
                    contentColor = colors.accent,
                    elevation = FloatingActionButtonDefaults.elevation(),
                ) {
                    DownGlyph(
                        size = dims.icon.lg,
                        color = colors.accent,
                        strokeWidth = dims.stroke.iconBold,
                    )
                }
                ReliveSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = if (showReturnToBottom) {
                                dims.minTouchTarget + dims.spacing.lg
                            } else {
                                dims.spacing.none
                            },
                        ),
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
    }
}

@Composable
private fun TimelineCoverDialog(
    hasCover: Boolean,
    onDismiss: () -> Unit,
    onChoose: () -> Unit,
    onClear: () -> Unit,
) {
    ReliveAlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(ReliveTheme.dimensions.radii.dialog),
        containerColor = ReliveTheme.colors.surfaceOverlay,
        title = { Text("Timeline cover", style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary) },
        text = { Text(if (hasCover) "Replace or remove this local cover photo." else "Choose a cover photo for this timeline.", style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Row {
                if (hasCover) TextButton(onClick = onClear) { Text("None") }
                TextButton(onClick = onChoose) { Text(if (hasCover) "Replace" else "Choose") }
            }
        },
    )
}
