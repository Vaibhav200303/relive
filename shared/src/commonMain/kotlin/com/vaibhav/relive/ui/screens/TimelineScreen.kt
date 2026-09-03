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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.ui.components.timeline.TimelineCoverControls
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.Dp
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
import kotlinx.coroutines.flow.distinctUntilChanged
import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.policy.EditWindow
import com.vaibhav.relive.domain.model.MomentFeeling
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
import com.vaibhav.relive.presentation.timeline.HOME_FEED_PREFETCH
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
import com.vaibhav.relive.ui.components.timeline.HomeFloatingHeaderActions
import com.vaibhav.relive.ui.components.timeline.TimelineHeader
import com.vaibhav.relive.ui.components.timeline.TimelineWallpaperSurface
import com.vaibhav.relive.ui.components.timeline.TimelineCoverHero
import com.vaibhav.relive.ui.components.timeline.AllTimelineCoverHero
import com.vaibhav.relive.ui.components.timeline.TimelineMomentActionHeader
import com.vaibhav.relive.ui.components.timeline.DateNavigationPicker
import com.vaibhav.relive.ui.components.timeline.DiscardTimelineDraftDialog
import com.vaibhav.relive.ui.components.timeline.DownGlyph
import com.vaibhav.relive.ui.components.timeline.UpGlyph
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
    /**
     * Renders this timeline as the All moments region of the unified Home surface (ADR-0061).
     *
     * Every Home-specific behaviour hangs off this flag and every one of them is off by default, so
     * custom timeline detail and the read-only system collections keep their existing ordering,
     * entry scroll, composer placement and chrome untouched. On Home the feed is newest-first, the
     * composer sits at the head of the feed, [homeHeader] items are emitted above it, and the
     * surface never scrolls itself on entry or after a save.
     */
    isHomeSurface: Boolean = false,
    /**
     * Incremented by Home's `+ New`. Each new value expands the existing inline composer in place;
     * a counter rather than a flag because Home is a persistent surface, so a latched boolean would
     * make every tap after the first a silent no-op.
     */
    expandComposerRequest: Int = 0,
    /** Invoked once a pending [expandComposerRequest] has been acted on, so the owner can clear it. */
    onExpandComposerRequestHandled: (() -> Unit)? = null,
    /** Items emitted above the composer on Home: the backdrop spacer and the section heading. */
    homeHeader: (LazyListScope.() -> Unit)? = null,
    /**
     * Drawn behind the scrolling list on Home: the welcome block and Rediscover row, plus the
     * opaque surface the All moments sheet rides on. Living outside the list is what lets the
     * timeline genuinely slide *over* the welcome area rather than scrolling in lockstep with it.
     */
    homeBackdrop: (@Composable () -> Unit)? = null,
    /** Supplied by Home, which owns the backdrop geometry that defines the two states. */
    isFocusedAllMoments: Boolean = false,
    /** How many items [homeHeader] emits. Every index computation offsets by this. */
    homeHeaderCount: Int = 0,
    /** Hoisted so Home's scroll position outlives the screen's own composition. */
    listState: LazyListState? = null,
    /** True while the welcome block and Rediscover row have scrolled above the viewport. */
    onFocusedAllMomentsChanged: ((Boolean) -> Unit)? = null,
    /** Leading app-bar slot for Home, which is a root and so carries no back action. */
    homeAppBarLeading: (@Composable () -> Unit)? = null,
    /** Applied to the feed itself, so Home can hang the floating-controls collapse off its scroll. */
    listModifier: Modifier = Modifier,
    /** Run before the head composer is seated, so Home can bring its feed back on screen first. */
    onExpandingComposer: (suspend () -> Unit)? = null,
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

    // Back on the Home surface (ADR-0061, PRODUCT_SPEC §2 "Back on Home"): clear a contextual
    // selection first, then collapse an expanded composer in place, and only then fall through to
    // the platform default. Home is a root, so it takes no back handler of its own beyond those
    // two — without this the composer stayed open and Back left the app. Collapsing preserves the
    // draft and shows no discard dialog; `×` remains the explicit discard path (§6.1). Neither
    // step scrolls: the surface stays exactly where it was in focused All moments.
    ReliveBackHandler(
        enabled = isHomeSurface &&
            (timelineState.momentActions.selectedMomentId != null || isComposerExpanded),
    ) {
        if (timelineState.momentActions.selectedMomentId != null) {
            timelineViewModel.clearMomentActionSelection()
        } else {
            composerViewModel.preserveDraft()
            isComposerExpanded = false
        }
    }
    var composerOpenIntentConsumed by remember(initialTimeline, openComposerOnEnter) {
        mutableStateOf(false)
    }
    var wasSaving by remember { mutableStateOf(false) }
    var wasEditingWhenSaving by remember { mutableStateOf(false) }
    // Armed by a successful new save, cleared by choosing, skipping, or the next save.
    var feelingPromptMomentId by remember { mutableStateOf<MomentId?>(null) }
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

    // Home has two ways to open the head composer — the `+ New` control and the inline `+` marker
    // on the rail — and they are the same action: expand the existing composer where it already
    // sits, with no navigation and no settled-frame handoff, since the composer is already composed
    // and collapsed in place so AnimatedContent still sees a real false -> true transition
    // (ADR-0061). Per ADR-0059 no focus is requested and no IME is opened. A preserved draft is
    // reopened as it stands rather than being prepared over.
    // One travel speed for the whole surface: the pace the return-to-top control already moves
    // at, so seating the composer reads as this surface moving rather than a jump cut.
    val composerSeatPaceMillis = ReliveTheme.motion.durations.short4
    val openHomeComposer: () -> Unit = {
        // Already open means this is a request to go back to it, not to start over: re-preparing
        // would throw away the date, timeline assignments and anything else already set on it.
        if (!isComposerExpanded && !composerState.hasUserDraft) {
            composerViewModel.prepareForTimeline(timelineState.currentTimeline)
        }
        isComposerExpanded = true
        scope.launch {
            onExpandingComposer?.invoke()
            // Travel to the composer rather than arrive at it. Whether it is just below the
            // welcome block or far above the current position, the surface covers the distance at
            // a steady pace and lands exactly on it.
            listState?.scrollToItemAtPace(
                targetIndex = homeHeaderCount,
                millisPerViewport = composerSeatPaceMillis,
            )
        }
    }
    LaunchedEffect(expandComposerRequest, composerDestinationSettled) {
        if (expandComposerRequest > 0 &&
            isHomeSurface &&
            mode.allowsMutations &&
            composerDestinationSettled
        ) {
            // Deliberately not gated on the composer being closed. Home is a persistent surface, so
            // `+ New` is just as often pressed from deep in the feed with the composer already open
            // above, and then the useful thing it can do is carry the person back up to it.
            openHomeComposer()
            // Cleared only after the composer is seated: resetting the counter restarts this
            // effect, and a restart mid-scroll would abandon the seat-in-view animation.
            onExpandComposerRequestHandled?.invoke()
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
                    is MomentSaveOutcome.Succeeded -> ReliveHapticCue.Confirm
                    MomentSaveOutcome.Rejected -> ReliveHapticCue.Reject
                },
            )
            // Only a first save reflects, and only where Moments can be mutated: an inline
            // edit never re-prompts, and a read-only collection never prompts (ADR-0064).
            feelingPromptMomentId = (outcome as? MomentSaveOutcome.Succeeded)
                ?.takeIf { it.isNewMoment && mode.allowsMutations }
                ?.momentId
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
            onExpandComposer = if (isHomeSurface) openHomeComposer else {
                {
                    if (!composerState.hasUserDraft) {
                        composerViewModel.prepareForTimeline(timelineState.currentTimeline)
                    }
                    isComposerExpanded = true
                }
            },
            onMicPermissionResult = composerViewModel::onMicPermissionResult,
            onDismissMicPermissionMessage = composerViewModel::dismissMicPermissionMessage,
            onOpenAppSettings = composerViewModel::openAppSettings,
            onEditorBoundsChanged = { editorBounds = it },
            feelingPromptMomentId = feelingPromptMomentId,
            onChooseFeeling = { momentId, feeling ->
                timelineViewModel.setFeeling(momentId, feeling)
                feelingPromptMomentId = null
            },
            onDismissFeelingPrompt = { feelingPromptMomentId = null },
            onJumpToDate = { showDatePicker = true },
            onChangeTheme = if (mode.allowsMutations && timelineState.currentTimeline.timelineThemeDestinationOrNull() != null) onOpenTimelineTheme else null,
            onUpdateCover = { showCoverPicker = true },
            dateNavigationTargetId = timelineState.dateNavigation?.momentId,
            onDateNavigationHandled = timelineViewModel::consumeDateNavigation,
            snackbarHostState = snackbarHostState,
            momentVisibility = resolveTimelineMomentVisibility(mode, behaviorPreferences),
            sharedTransition = mediaSharedTransition,
            isHomeSurface = isHomeSurface,
            homeHeader = homeHeader,
            homeBackdrop = homeBackdrop,
            isFocusedAllMoments = isFocusedAllMoments,
            homeHeaderCount = homeHeaderCount,
            listState = listState,
            // The window this pages belongs to the view model created here, so Home's paging is
            // wired from it rather than asked of every caller.
            onLoadOlderMoments = timelineViewModel::loadOlderMoments,
            hasOlderMoments = timelineState.hasOlderMoments,
            onFocusedAllMomentsChanged = onFocusedAllMomentsChanged,
            homeAppBarLeading = homeAppBarLeading,
            listModifier = listModifier,
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
    /** The Moment whose post-save reflection prompt is currently armed (PRODUCT_SPEC §10A.1). */
    feelingPromptMomentId: MomentId? = null,
    onChooseFeeling: ((MomentId, MomentFeeling) -> Unit)? = null,
    onDismissFeelingPrompt: (() -> Unit)? = null,
    onJumpToDate: () -> Unit,
    onChangeTheme: (() -> Unit)?,
    onUpdateCover: () -> Unit,
    dateNavigationTargetId: MomentId?,
    onDateNavigationHandled: () -> Unit,
    snackbarHostState: SnackbarHostState,
    momentVisibility: TimelineMomentVisibility,
    isHomeSurface: Boolean = false,
    homeHeader: (LazyListScope.() -> Unit)? = null,
    homeBackdrop: (@Composable () -> Unit)? = null,
    isFocusedAllMoments: Boolean = false,
    homeHeaderCount: Int = 0,
    listState: LazyListState? = null,
    onLoadOlderMoments: (() -> Unit)? = null,
    hasOlderMoments: Boolean = false,
    onFocusedAllMomentsChanged: ((Boolean) -> Unit)? = null,
    homeAppBarLeading: (@Composable () -> Unit)? = null,
    listModifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val density = LocalDensity.current
    val reduceMotion = ReliveTheme.reduceMotion
    val moments: List<MomentPresentation> = when (val state = timelineState.moments) {
        TimelineMomentsState.Loading, TimelineMomentsState.Empty -> emptyList()
        is TimelineMomentsState.Loaded -> state.moments
    }
    // Home's two states are a pure function of scroll offset: once the welcome block and the
    // Rediscover row have passed above the viewport, the timeline dominates (ADR-0061).
    if (isHomeSurface) {
        LaunchedEffect(isFocusedAllMoments) { onFocusedAllMomentsChanged?.invoke(isFocusedAllMoments) }
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
    // Custom timeline detail rides its cover photo the way Home rides its welcome block: the cover
    // becomes a backdrop drawn behind the feed, and the feed becomes a sheet that slides over it,
    // off the bottom edge, and back (ADR-0062). Home is excluded because it already is that
    // surface, with a backdrop of its own supplied by the caller.
    val isSlidingCoverSurface = !isHomeSurface &&
        timelineState.currentTimeline is CurrentTimeline.Custom
    // What both sliding-backdrop surfaces share: the feed runs newest-first with the composer at
    // its head, so the chronological end is a short scroll from the top of the surface rather than
    // the far end of history, and the return control points up rather than down.
    val isNewestFirst = isHomeSurface || isSlidingCoverSurface
    // The elastic cover stretch and the sliding backdrop both want the overscroll at the top of the
    // feed, so a surface has one or the other. Home renders no cover at all (ADR-0061); a custom
    // timeline now grows its cover to fill the viewport instead of stretching it (ADR-0062). That
    // leaves the All timeline's generated collage as the one hero that still springs back.
    val hasTimelineCoverHero = !isHomeSurface && !isSlidingCoverSurface &&
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
        // Home renders no app bar at all: the canvas runs to the top edge and its few controls
        // float over the surface (see the overlay below), so only the other timeline modes emit
        // an in-flow header that content stacks beneath.
        if (!isHomeSurface) AnimatedContent(
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
            if (isSlidingCoverSurface) {
                // This surface reserves no app-bar row: its cover is a backdrop inside the content
                // area below, and both the cover controls and the selection bar are pinned above
                // the sheet there. Keeping the selection bar out of the layout is what stops a
                // long-press from shunting the whole timeline down by an app bar's height.
                Unit
            } else if (inActionMode && selectedActionMoment != null && actionAvailability != null) {
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
            // Behind the list, so the timeline sheet slides over it rather than with it.
            if (isHomeSurface) homeBackdrop?.invoke()

            val customName = (timelineState.currentTimeline as? CurrentTimeline.Custom)?.let { current ->
                timelineState.customTimelines.firstOrNull { it.id == current.id }?.name
            }

            key(timelineState.currentTimeline) {
                val listState = listState ?: rememberLazyListState()

                // The sliding cover's own geometry. Unlike Home, whose welcome block has to be
                // measured, the cover rests at a fixed height, so the backdrop needs no
                // measurement pass and the sheet knows where it sits from the first frame.
                //
                // Two heights, not one. The cover *rests* at its full hero height, which is what
                // the expanded state grows past to fill the viewport. But the sheet comes to rest
                // below the pinned controls rather than at the top of the screen, so the distance
                // it actually travels — and therefore the height of the window reserving its space
                // — is the hero height less that inset.
                val coverControlsInset = coverControlsInset()
                val coverHeightPx = with(density) { dims.timeline.coverHeroHeight.roundToPx() }
                val coverTravelPx = with(density) {
                    (dims.timeline.coverHeroHeight - coverControlsInset).roundToPx()
                }.coerceAtLeast(0)
                val expansion = rememberBackdropExpansionState()
                LaunchedEffect(coverHeightPx) { expansion.backdropHeightPx = coverHeightPx }
                val expansionConnection = rememberBackdropExpansionConnection(expansion)
                val scrolledIntoCover by rememberScrolledIntoBackdrop(listState) { coverTravelPx }
                if (isSlidingCoverSurface) {
                    BackdropSettleEffect(
                        listState = listState,
                        backdropHeightPx = coverTravelPx,
                        scrolledIntoBackdrop = { scrolledIntoCover },
                    )
                    val custom = (timelineState.currentTimeline as? CurrentTimeline.Custom)?.let { current ->
                        timelineState.customTimelines.firstOrNull { it.id == current.id }
                    }
                    if (custom != null) {
                        TimelineCoverBackdrop(
                            name = custom.name,
                            coverPhotoRef = custom.coverPhotoRef,
                            mediaStore = mediaStore,
                            onUpdateCover = onUpdateCover.takeIf { mode.allowsMutations },
                            coverHeightPx = coverHeightPx,
                            coverTravelPx = coverTravelPx,
                            scrolledIntoCover = scrolledIntoCover,
                            expansionPx = expansion.expansionPx,
                            wallpaper = timelineState.appearance.wallpaper,
                            onViewportMeasured = { expansion.viewportHeightPx = it },
                        )
                    }
                }

                // Index of the first moment. On a sliding-backdrop surface the header items and the
                // head composer sit above the feed; everywhere else the feed still starts at zero.
                val headerItemCount = when {
                    isHomeSurface -> homeHeaderCount
                    isSlidingCoverSurface -> SLIDING_COVER_HEADER_ITEM_COUNT
                    else -> 0
                }
                val feedOffset = if (isNewestFirst) headerItemCount + 1 else 0
                // The composer is one item emitted at whichever end of the feed is the
                // chronological end: the head on Home (newest-first), the tail everywhere else.
                val composerItem: LazyListScope.() -> Unit = {
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
                                    // On Home the composer is the head of a newest-first feed, so
                                    // the rail leaves its marker downward toward the first moment.
                                    railContinuesBelow = isNewestFirst,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                CollapsedComposerMarker(
                                    onExpand = onExpandComposer,
                                    railContinuesBelow = isNewestFirst,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                var lastSeenCount by remember { mutableIntStateOf(-1) }
                var isProgrammaticScroll by remember { mutableStateOf(false) }
                var lastListPosition by remember { mutableStateOf<TimelineListPosition?>(null) }
                var returnToBottomState by remember { mutableStateOf(TimelineReturnToBottomState()) }
                var returnToTopState by remember { mutableStateOf(TimelineReturnToTopState()) }
                val autoScrollController = remember { TimelineAutoScrollController() }
                val listScope = rememberCoroutineScope()
                // The first item drawn on the sheet, which is where the return control goes back to:
                // on Home the `All moments` heading, on a custom timeline the composer at the head
                // of the feed. Both sit directly after the single backdrop-window item, so landing
                // there leaves the surface focused rather than pulling the backdrop into view.
                val sheetTopIndex = if (isSlidingCoverSurface) {
                    SLIDING_COVER_HEADER_ITEM_COUNT - 1
                } else {
                    (homeHeaderCount - 1).coerceAtLeast(0)
                }
                val canReturnToFeedTop by remember(listState, sheetTopIndex) {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > sheetTopIndex ||
                            (listState.firstVisibleItemIndex == sheetTopIndex &&
                                listState.firstVisibleItemScrollOffset > 0)
                    }
                }

                LaunchedEffect(listState.isScrollInProgress) {
                    if (!listState.isScrollInProgress && coverStretchPx > 0f) {
                        animate(
                            initialValue = coverStretchPx,
                            targetValue = 0f,
                            animationSpec = tween(motion.durations.slowMillis, easing = motion.easings.standard),
                        ) { value, _ -> coverStretchPx = value }
                    }
                }

                if (isHomeSurface && onLoadOlderMoments != null) {
                    // Grow the bounded window before the person reaches its oldest loaded moment,
                    // so paging is invisible. snapshotFlow + distinctUntilChanged means one request
                    // per threshold crossing rather than one per scroll frame.
                    LaunchedEffect(listState, moments.size, hasOlderMoments) {
                        snapshotFlow {
                            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            hasOlderMoments && last >= feedOffset + moments.size - HOME_FEED_PREFETCH
                        }
                            .distinctUntilChanged()
                            .collect { shouldLoad -> if (shouldLoad) onLoadOlderMoments() }
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
                            returnToTopState = returnToTopState.onPositionChanged(
                                isProgrammaticScroll = isProgrammaticScroll,
                                movedTowardTop = position.movedTowardTopOf(previous),
                                canReturnToTop = position.index > sheetTopIndex ||
                                    (position.index == sheetTopIndex && position.scrollOffset > 0),
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
                    val target = feedOffset + (
                        selectedIndex ?: when {
                            customName != null && moments.isEmpty() -> 0
                            mode.allowsMutations && !isNewestFirst -> moments.size
                            isNewestFirst -> 0
                            else -> moments.lastIndex.coerceAtLeast(0)
                        }
                        )
                    isProgrammaticScroll = true
                    try {
                        if (dateNavigationTargetId != null) {
                            // Calendar navigation is the one app-initiated scroll Home allows.
                            listState.animateScrollToItem(target)
                        } else if (isHomeSurface) {
                            // Home opens at the top of the surface and never scrolls itself: not on
                            // entry, and not when a moment is saved (ADR-0061). Keeping the offset
                            // is what makes the kept moment appear where the composer stood.
                            Unit
                        } else if (isSlidingCoverSurface && selectedIndex == null) {
                            // A custom timeline opens on its cover with the newest moment first and
                            // holds its offset after a save, under the same rule (ADR-0062). An
                            // explicitly selected Moment is still navigated to, which is why this
                            // defers rather than blocking every app-initiated scroll.
                            Unit
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
                // Home receives these already assembled by its caller, which also hangs the
                // floating-toolbar collapse off the same scroll. A custom timeline owns its
                // backdrop outright, so it builds them here.
                val feedModifier = if (!isSlidingCoverSurface) listModifier else listModifier
                    // The sheet rests below the pinned controls rather than at the top of the
                    // screen, so the feed is inset to match: its first pixel is the sheet's top
                    // edge in the focused state, and the strip above it stays the cover's.
                    .padding(top = coverControlsInset)
                    // The feed slides down past the bottom edge with its sheet, so what travels off
                    // must stop being drawn. Clipping the feed rather than the whole content area
                    // leaves the cover free to bleed to the screen edges.
                    .clipToBounds()
                    .nestedScroll(expansionConnection)
                    // The feed rides with the sheet it sits on, so the timeline leaves and re-enters
                    // by the bottom edge as one surface rather than standing still while its ground
                    // moves underneath it.
                    .graphicsLayer { translationY = expansion.expansionPx }
                LazyColumn(
                    state = listState,
                    modifier = feedModifier
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
                    if (isHomeSurface) {
                        homeHeader?.invoke(this)
                    } else if (isSlidingCoverSurface) {
                        // A transparent window onto the cover behind the list, reserving its space
                        // without scrolling it.
                        //
                        // It also carries the cover's tap. A scrolling list does not let touches
                        // fall through to what is drawn behind it, so the cover cannot be its own
                        // target while the sheet is on screen. The window is exactly as tall as the
                        // cover and its bottom edge is the sheet's top edge at every position, so
                        // the target is always precisely the part of the cover still showing.
                        item(key = "cover-backdrop-window") {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(dims.timeline.coverHeroHeight - coverControlsInset)
                                    .then(
                                        if (mode.allowsMutations) {
                                            Modifier
                                                .clickable(onClick = onUpdateCover)
                                                .semantics {
                                                    contentDescription = "Update cover photo"
                                                }
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                        // Breathing room at the head of the sheet, so the composer's plus marker
                        // does not sit hard against the sheet's own top edge. It is sheet content
                        // rather than list padding, which is what keeps the window above it aligned
                        // with the cover.
                        item(key = "cover-sheet-lead") {
                            Spacer(Modifier.height(dims.spacing.lg))
                        }
                    }
                    // On a newest-first feed the chronological end of the timeline is its head, so
                    // the composer is emitted before the moments rather than after them.
                    if (isNewestFirst) composerItem()
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
                                // On Home the composer sits above the newest moment, so the very
                                // first card still needs rail above its dot for the composer's
                                // rail to meet it. Elsewhere the first card starts the rail.
                                hasPreviousMoment = index > 0 || isNewestFirst,
                                showLocation = momentVisibility.showLocations,
                                showTags = momentVisibility.showTags,
                                showFeelingPrompt = feelingPromptMomentId == moment.id,
                                onChooseFeeling = if (mode.allowsMutations && onChooseFeeling != null) {
                                    { feeling -> onChooseFeeling(moment.id, feeling) }
                                } else {
                                    null
                                },
                                onDismissFeelingPrompt = onDismissFeelingPrompt,
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
                    if (!isNewestFirst) composerItem()
                }
                // A newest-first feed's chronological end is its head, so the place people scroll
                // back to is the top of the sheet and the affordance points up. Every other
                // timeline runs oldest-first and keeps its return-to-newest control pointing down.
                val showReturnToBottom = !isNewestFirst &&
                    returnToBottomState.isVisible(listState.canScrollForward)
                val showReturnToTop = isNewestFirst && returnToTopState.isVisible(canReturnToFeedTop)
                if (isNewestFirst) SmallFloatingActionButton(
                    onClick = {
                        if (canReturnToFeedTop) {
                            listScope.launch {
                                isProgrammaticScroll = true
                                try {
                                    listState.scrollToItemAtPace(
                                        targetIndex = sheetTopIndex,
                                        millisPerViewport = motion.durations.short4,
                                    )
                                } finally {
                                    isProgrammaticScroll = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Clear of the floating navigation bar and `+ New`, which Home carries at
                        // the bottom of this same surface. A custom timeline carries neither, so
                        // its control sits where every other timeline's does.
                        .padding(
                            bottom = if (isHomeSurface) {
                                dims.floatingToolbar.height + dims.spacing.lg + dims.spacing.md
                            } else {
                                dims.spacing.lg
                            },
                        )
                        .size(dims.minTouchTarget)
                        .animateFloatingActionButton(
                            visible = showReturnToTop,
                            alignment = Alignment.BottomCenter,
                        )
                        .semantics { contentDescription = "Scroll to the top of All moments" },
                    shape = RoundedCornerShape(dims.radii.pill),
                    containerColor = colors.surfaceFloating,
                    contentColor = colors.accent,
                    elevation = FloatingActionButtonDefaults.elevation(),
                ) {
                    UpGlyph(
                        size = dims.icon.lg,
                        color = colors.accent,
                        strokeWidth = dims.stroke.iconBold,
                    )
                } else SmallFloatingActionButton(
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

                if (isSlidingCoverSurface) {
                    // Pinned above the sheet: the timeline passes underneath these, never over
                    // them, which is the whole point of lifting them out of the cover. The
                    // selection bar takes the same slot, so a long-press swaps the controls in
                    // place rather than shifting the surface underneath them.
                    AnimatedContent(
                        targetState = isContextualActionMode,
                        transitionSpec = {
                            reliveSequentialSlideFade(
                                motion = motion,
                                reduceMotion = reduceMotion,
                                enterFromRight = targetState,
                            )
                        },
                        label = "cover controls",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .bleedHorizontal(dims.timeline.horizontalPadding),
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
                        } else {
                            TimelineCoverControls(
                                onBack = onBack,
                                onJumpToDate = onJumpToDate,
                                onChangeTheme = onChangeTheme,
                            )
                        }
                    }
                }
            }
        }
    }
    // Home's replacement for the app bar: profile (and, once the timeline dominates, calendar and
    // theme) float over the surface instead of sitting in a band above it, so the canvas gradient
    // owns the whole screen. Long-pressing a moment still swaps in the contextual action header,
    // overlaid rather than in-flow so entering the mode never pushes the surface down.
    if (isHomeSurface) AnimatedContent(
        targetState = isContextualActionMode,
        transitionSpec = {
            reliveSequentialSlideFade(
                motion = motion,
                reduceMotion = reduceMotion,
                enterFromRight = targetState,
            )
        },
        label = "moment selection app bar",
        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
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
        } else {
            // Calendar and the timeline theme entry belong to the timeline, so they are present
            // and active only once the timeline dominates the screen (ADR-0061).
            HomeFloatingHeaderActions(
                leading = homeAppBarLeading,
                onJumpToDate = onJumpToDate.takeIf { isFocusedAllMoments },
                onChangeTheme = onChangeTheme?.takeIf { isFocusedAllMoments },
            )
        }
    }
    }
}

/**
 * The window reserving the cover's space and the lead-in below it. Keep in step with the feed's
 * item order in `TimelineScreenContent`.
 */
private const val SLIDING_COVER_HEADER_ITEM_COUNT = 2

/**
 * How far down the screen a sliding cover's sheet comes to rest: clear of the status bar, clear of
 * the pinned Back / theme / calendar row, plus a gap. Read by both the feed's inset and the window
 * that reserves the cover's space, so the sheet's top edge and that window's bottom edge stay the
 * same line at every scroll position.
 */
@Composable
private fun coverControlsInset(): Dp {
    val dims = ReliveTheme.dimensions
    return WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
        dims.minTouchTarget + dims.spacing.md * 2
}

/**
 * The cover photo drawn behind the feed, plus the opaque surface the timeline sheet rides on.
 *
 * The counterpart to Home's welcome backdrop (ADR-0062). The sheet's top edge sits at
 * `coverHeight - scrolledIntoCover`, so scrolling up raises it over the cover and scrolling down
 * lowers it again; past that the sheet is pushed off the bottom by [expansionPx] and the cover
 * grows by the same amount, which is exactly the distance left for it to fill the viewport.
 *
 * [scrolledIntoCover] tops out at [coverTravelPx] rather than at [coverHeightPx], which is what
 * leaves the sheet resting clear of the pinned controls instead of running under them.
 */
@Composable
private fun TimelineCoverBackdrop(
    name: String,
    coverPhotoRef: MediaStorageRef?,
    mediaStore: MediaStore,
    onUpdateCover: (() -> Unit)?,
    coverHeightPx: Int,
    coverTravelPx: Int,
    scrolledIntoCover: Int,
    expansionPx: Float,
    wallpaper: com.vaibhav.relive.domain.model.TimelineWallpaper,
    onViewportMeasured: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val sheetShape = RoundedCornerShape(topStart = dims.radii.xl, topEnd = dims.radii.xl)
    val covered = if (coverTravelPx > 0) {
        (scrolledIntoCover.toFloat() / coverTravelPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .bleedHorizontal(dims.timeline.horizontalPadding)
            // The cover trails the sheet, so without this it would ride up past the top of the
            // content area and show through behind the pinned controls.
            .clipToBounds()
            .onGloballyPositioned { onViewportMeasured(it.size.height) }
            // Painted behind the cover so the expanded state has a ground of its own on the frames
            // where the photo has not been decoded yet.
            .background(colors.bgCanvas),
    ) {
        TimelineCoverHero(
            name = name,
            coverPhotoRef = coverPhotoRef,
            mediaStore = mediaStore,
            // Back, theme and calendar are pinned above the sheet instead, so the cover carries
            // only its photo and its name.
            onBack = null,
            onUpdateCover = onUpdateCover,
            stretchPx = expansionPx,
            stretchZoom = false,
            modifier = Modifier.graphicsLayer {
                // Trails the sheet instead of matching it, which is what makes the timeline read
                // as passing in front of the photo rather than pushing it.
                translationY = -scrolledIntoCover * BACKDROP_PARALLAX
                alpha = 1f - covered * 0.4f
            },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        0,
                        (coverHeightPx - scrolledIntoCover).coerceAtLeast(0) +
                            expansionPx.roundToInt(),
                    )
                }
                .shadow(
                    elevation = dims.timelineHome.cardElevation * covered,
                    shape = sheetShape,
                    clip = false,
                    ambientColor = colors.shadow,
                    spotColor = colors.shadow,
                )
                .background(colors.bgCanvas, sheetShape)
                .clip(sheetShape),
        ) {
            // The sheet carries the timeline's own wallpaper, so raising it reads as one material
            // moving rather than a flat panel sliding over a decorated background.
            TimelineWallpaperSurface(wallpaper = wallpaper, modifier = Modifier.fillMaxSize()) {}
        }
    }
}

/**
 * Travels to [targetIndex] — in whichever direction it lies — at a steady, readable pace of one
 * viewport per [millisPerViewport], landing with the item's top edge at the top of the viewport.
 *
 * `animateScrollToItem` runs for a fixed duration however far it has to go, so from deep in the
 * archive it arrives before anything has been drawn in between and reads as a teleport rather than
 * a scroll. Moving a measured amount each frame keeps the speed the same whatever the distance, and
 * lets the last frame be clamped to what is actually left — so the feed lands exactly on the target
 * instead of overshooting past it and springing back.
 *
 * A user drag takes the scroll mutex at a higher priority, so touching the screen cancels this.
 */
private suspend fun LazyListState.scrollToItemAtPace(targetIndex: Int, millisPerViewport: Int) {
    scroll {
        var lastFrameNanos = withFrameNanos { it }
        while (true) {
            val viewport = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            if (viewport <= 0 || millisPerViewport <= 0) break
            // Known exactly once the target has been measured, and only by direction while it is
            // still off screen — which is why the pace, not the distance, drives each step. A
            // positive distance is below the top edge and a negative one above it.
            val measured = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            val remaining = when {
                measured != null -> (measured.offset - layoutInfo.viewportStartOffset).toFloat()
                firstVisibleItemIndex > targetIndex -> null
                else -> Float.MAX_VALUE
            }
            if (remaining == 0f) break
            val frameNanos = withFrameNanos { it }
            val seconds = (frameNanos - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = frameNanos
            val pace = viewport * 1000f / millisPerViewport
            val step = if (remaining == null) {
                -pace * seconds
            } else {
                remaining.coerceIn(-pace * seconds, pace * seconds)
            }
            if (scrollBy(step) == 0f) break
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
