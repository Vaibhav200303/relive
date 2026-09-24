package com.vaibhav.relive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.vaibhav.relive.di.ReliveAppContainer
import com.vaibhav.relive.ui.screens.TimelineScreen
import com.vaibhav.relive.ui.screens.TimelineHomeScreen
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeContent
import com.vaibhav.relive.ui.screens.AppLockScreen
import com.vaibhav.relive.ui.screens.OnboardingScreen
import com.vaibhav.relive.ui.screens.HomeScreen
import com.vaibhav.relive.ui.screens.rememberHomeSurfaceState
import com.vaibhav.relive.ui.screens.ShareTimelinePickerScreen
import com.vaibhav.relive.ui.screens.TimelineThemeScreen
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.reliveSequentialSlideFade
import com.vaibhav.relive.ui.theme.reliveForwardBackward
import com.vaibhav.relive.ui.theme.spec
import com.vaibhav.relive.ui.theme.toReliveThemeId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.presentation.timeline.TimelineMode
import com.vaibhav.relive.presentation.timeline.TimelineThemeDestination
import com.vaibhav.relive.presentation.timeline.timelineThemeDestinationOrNull
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeViewModel
import com.vaibhav.relive.presentation.profile.ProfileViewModel
import com.vaibhav.relive.presentation.profile.ProfileNavigationState
import com.vaibhav.relive.presentation.profile.ProfileDestination
import com.vaibhav.relive.presentation.profile.ExternalActivityGuard
import com.vaibhav.relive.presentation.profile.MediaStorageViewModel
import com.vaibhav.relive.ui.components.navigation.ReliveFloatingBottomControls
import com.vaibhav.relive.ui.components.navigation.ReliveTopLevelDestination
import com.vaibhav.relive.ui.components.composer.quickCaptureSharedBounds
import com.vaibhav.relive.ui.components.timeline.timelineCardSharedBounds
import com.vaibhav.relive.presentation.timeline.SystemCollectionCover
import com.vaibhav.relive.platform.media.ActivePlayback
import com.vaibhav.relive.ui.screens.ProfileScreen
import com.vaibhav.relive.ui.screens.PreferencesScreen
import com.vaibhav.relive.ui.screens.MediaStorageScreen
import com.vaibhav.relive.ui.screens.BackupRestoreScreen
import com.vaibhav.relive.ui.screens.LocationScreen
import com.vaibhav.relive.ui.screens.RediscoverNotificationsScreen
import com.vaibhav.relive.ui.screens.PrivacySecurityScreen
import com.vaibhav.relive.ui.screens.HelpFeedbackScreen
import com.vaibhav.relive.ui.screens.AboutReliveScreen
import com.vaibhav.relive.ui.screens.LicensesScreen
import com.vaibhav.relive.ui.screens.ExportScreen
import com.vaibhav.relive.ui.screens.PortableArchiveScreen
import com.vaibhav.relive.ui.screens.PortableArchiveErrorScreen
import com.vaibhav.relive.presentation.profile.BackupRestoreViewModel
import com.vaibhav.relive.presentation.exporting.ExportViewModel
import com.vaibhav.relive.ui.screens.SearchScreen
import com.vaibhav.relive.ui.screens.UpgradeToProScreen
import com.vaibhav.relive.presentation.search.SearchViewModel
import com.vaibhav.relive.presentation.navigation.QuickCaptureSurface
import com.vaibhav.relive.presentation.navigation.quickCaptureCommand
import com.vaibhav.relive.presentation.composer.TimelineComposerDraftStore
import com.vaibhav.relive.presentation.settings.AppearanceViewModel
import com.vaibhav.relive.presentation.settings.BehaviorPreferencesViewModel
import com.vaibhav.relive.presentation.settings.resolveDarkMode
import com.vaibhav.relive.presentation.profile.AppLockController
import com.vaibhav.relive.presentation.profile.RediscoverReminderController
import com.vaibhav.relive.presentation.onboarding.CURRENT_ONBOARDING_VERSION
import com.vaibhav.relive.presentation.onboarding.OnboardingResolution
import com.vaibhav.relive.presentation.onboarding.resolveOnboarding
import com.vaibhav.relive.presentation.home.preloadHomeRediscover
import com.vaibhav.relive.platform.system.openAppSettings
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.platform.system.toLauncherIcon
import com.vaibhav.relive.platform.exporting.rememberExportFileHandle
import com.vaibhav.relive.platform.share.IncomingSharePayload
import com.vaibhav.relive.platform.share.IncomingShareState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private sealed interface TimelinesDestination {
    data object TimelineHome : TimelinesDestination
    data class TimelineDetail(
        val scope: CurrentTimeline,
        val selectedMomentId: MomentId? = null,
        val openComposerOnEnter: Boolean = false,
        val incomingShare: IncomingSharePayload? = null,
        val cameFromQuickCapture: Boolean = false,
        /** Entered by tapping this timeline's card on Timeline Home, so the two morph (ADR-0063). */
        val cameFromCard: Boolean = false,
        /**
         * The tapped card's own timeline, carried through the route so the detail's first frame
         * wears the real wallpaper and cover instead of the defaults popping mid-morph
         * (ADR-0063) — the same pattern as the Rediscover routes carrying their cover.
         */
        val seedTimeline: Timeline.Custom? = null,
    ) : TimelinesDestination
    data class TimelineTheme(val returnTo: TimelineDetail) : TimelinesDestination
}

private sealed interface RediscoverDestination {
    data object Root : RediscoverDestination

    /**
     * The All timeline's appearance, reached from Home's app bar. Home is the surface that band
     * belongs to, so it returns to Home rather than to a timeline detail screen.
     */
    data object AllTheme : RediscoverDestination

    // Every collection route carries the cover its Home card was wearing when tapped, so the
    // screen opens under the same image the card showed and the container transform between the
    // two is continuous (ADR-0065).
    data class AllPhotos(val cover: SystemCollectionCover? = null) : RediscoverDestination
    data class Favorites(
        val selectedMomentId: MomentId?,
        val cover: SystemCollectionCover? = null,
    ) : RediscoverDestination
    data class OnThisDay(
        val selectedMomentId: MomentId?,
        val date: com.vaibhav.relive.domain.model.LocalCalendarDate,
        val cover: SystemCollectionCover? = null,
    ) : RediscoverDestination
    data class FromYourPast(
        val selectedMomentId: MomentId?,
        val query: RediscoverQuery,
        val cover: SystemCollectionCover? = null,
    ) : RediscoverDestination
}

@Composable
@Preview
fun App(
    container: ReliveAppContainer,
    rediscoverDebugControls: (@Composable () -> Unit)? = null,
    onIncomingShareCancelled: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val appearanceViewModel = remember(container, scope) {
        AppearanceViewModel(container.appearanceRepository, scope)
    }
    val appearanceState by appearanceViewModel.state.collectAsState()
    val behaviorPreferencesViewModel = remember(container, scope) {
        BehaviorPreferencesViewModel(container.behaviorPreferencesRepository, scope)
    }
    val behaviorState by behaviorPreferencesViewModel.state.collectAsState()
    val darkMode = resolveDarkMode(
        mode = appearanceState.preferences.mode,
        systemDark = isSystemInDarkTheme(),
    )
    LaunchedEffect(appearanceState.preferences.defaultTheme, container.launcherIconController) {
        container.launcherIconController.synchronize(
            appearanceState.preferences.defaultTheme.toLauncherIcon(),
        )
    }
    ReliveTheme(
        themeId = appearanceState.preferences.defaultTheme.toReliveThemeId(),
        darkMode = darkMode,
    ) {
        @OptIn(ExperimentalSharedTransitionApi::class)
        // The app's one global ground: every screen sits on the current theme's atmospheric
        // canvas gradient, so navigation moves content over a steady light source rather than
        // between differently-painted rooms.
        SharedTransitionLayout(
            modifier = Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush()),
        ) {
        val sharedTransitionScope = this
        val composerDraftStore = remember { TimelineComposerDraftStore() }
        val homeViewModel = remember(container, scope) {
            TimelineHomeViewModel(
                homeRepository = container.timelineHomeRepository,
                timelineRepository = container.timelineRepository,
                clock = container.clock,
                idGenerator = container.idGenerator,
                scope = scope,
                mediaStore = container.mediaStore,
                entitlementProvider = container.entitlementProvider,
            )
        }
        val homeState by homeViewModel.state.collectAsState()
        val homeListState = rememberLazyListState()
        // Home keeps its own list state: it and the Timelines list are different surfaces with
        // different content, and sharing one scroll position let each destroy the other's.
        // Preserve Home while navigating inside this running app, but do not save its focused
        // timeline position across a new app launch: every launch starts on the hero.
        val homeFeedListState = remember { LazyListState() }
        // Survives Home being swapped out for Profile, a collection or another destination.
        val homeSurfaceState = rememberHomeSurfaceState()
        LaunchedEffect(container, homeSurfaceState) {
            val snapshot = preloadHomeRediscover(container.rediscoverRepository, container.clock)
            homeSurfaceState.seedRediscover(snapshot)
        }
        val incomingShareState by container.incomingShareGateway.state.collectAsState()
        val rediscoverListState = rememberLazyListState()
        val searchListState = rememberLazyListState()
        // Profile is temporarily removed while one of its sections is open. Keep its list state
        // here with the navigation owner so returning to Profile restores the row that opened it.
        val profileListState = rememberLazyListState()
        val searchViewModel = remember(container, scope) { SearchViewModel(container.momentRepository, scope) }
        val profileViewModel = remember(container, scope) { ProfileViewModel(container.profileRepository, container.profileSettingsRepository, container.mediaStore, scope) }
        val profileSettings by container.profileSettingsRepository.settings.collectAsState()
        val onboardingPreferences by container.onboardingPreferencesRepository.preferences.collectAsState()
        var onboardingVisible by remember(container) { mutableStateOf<Boolean?>(null) }
        var onboardingBypassedForSession by remember(container) { mutableStateOf(false) }
        LaunchedEffect(onboardingPreferences.completedVersion) {
            if (onboardingPreferences.completedVersion >= CURRENT_ONBOARDING_VERSION) {
                onboardingVisible = false
            } else {
                val profile = container.profileRepository.observeProfile().first()
                when (
                    resolveOnboarding(
                        completedVersion = onboardingPreferences.completedVersion,
                        momentCount = profile.momentCount,
                        customTimelineCount = profile.customTimelineCount,
                    )
                ) {
                    OnboardingResolution.Hidden -> onboardingVisible = false
                    OnboardingResolution.Show -> onboardingVisible = true
                    OnboardingResolution.CompleteExistingInstall -> {
                        val migrated = container.onboardingPreferencesRepository
                            .complete(CURRENT_ONBOARDING_VERSION)
                            .isSuccess
                        onboardingVisible = !migrated
                    }
                }
            }
        }
        val lockController = remember(container) { AppLockController(container.profileSettingsRepository, container.deviceAuthentication) { container.clock.now().epochMilliseconds } }
        val locked by lockController.locked.collectAsState()
        val reminderController = remember(container) { RediscoverReminderController(container.profileSettingsRepository, container.rediscoverReminderService) }
        // Reminder scheduling only needs to know THAT the archive changed. Folding over
        // observeAll() hydrated every Moment (and its tags and attachments) on launch and again on
        // every write; ADR-0061 forbids the root hydrating the archive, so this reads a SQL
        // aggregate instead.
        val reminderArchiveFingerprint by remember(container) {
            container.momentRepository.observeArchiveFingerprint()
        }.collectAsState(0L)
        LaunchedEffect(profileSettings.rediscoverRemindersEnabled, reminderArchiveFingerprint) {
            if (profileSettings.rediscoverRemindersEnabled) container.rediscoverReminderService.synchronize(true)
        }
        var notificationPermission by remember(container.rediscoverReminderService) { mutableStateOf(container.rediscoverReminderService.permissionState()) }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, lockController) {
            val observer = LifecycleEventObserver { _, event -> when (event) {
                Lifecycle.Event.ON_STOP -> lockController.onBackground()
                Lifecycle.Event.ON_START -> {
                    lockController.onForeground()
                    if (container.profileSettingsRepository.settings.value.rediscoverRemindersEnabled) scope.launch { container.rediscoverReminderService.synchronize(true) }
                }
                else -> Unit
            } }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        // One root: the app always opens on the Home surface (ADR-0061). An authoritative
        // restoration or deep-link destination still takes precedence over this initial value.
        var topLevel by remember { mutableStateOf(ReliveTopLevelDestination.Home) }
        var timelinesDestination by remember { mutableStateOf<TimelinesDestination>(TimelinesDestination.TimelineHome) }
        var rediscoverDestination by remember { mutableStateOf<RediscoverDestination>(RediscoverDestination.Root) }
        var profileNavigation by remember { mutableStateOf(ProfileNavigationState()) }
        var navigationToolbarExpanded by remember { mutableStateOf(true) }
        var moodInsightsOpen by remember { mutableStateOf(false) }
        // The card that the timeline detail screen morphs out of and back into (ADR-0063). Held
        // past the forward navigation so the reverse transform has a source to return to; a detail
        // reached any other way carries no `cameFromCard`, so a stale id simply matches nothing.
        var cardTransformTimelineId by remember { mutableStateOf<TimelineId?>(null) }
        // The Rediscover card a collection screen morphs out of and back into (ADR-0065), held the
        // same way: past the forward navigation, so Back still has a source card to return to.
        var quickCaptureTransformActive by remember { mutableStateOf(false) }
        /** Bumped by `+ New` while on Home; the Home surface expands its composer in place. */
        var homeComposerRequest by remember { mutableIntStateOf(0) }
        // True while Home's composer has its full-screen camera up. The floating navigation bar
        // and `+ New` live above every top-level surface, so they must stand down for the camera
        // rather than floating over the viewfinder.
        var homeCaptureOverlayActive by remember { mutableStateOf(false) }
        var quickCaptureTransformEpoch by remember { mutableIntStateOf(0) }
        val quickCaptureTransformDuration = ReliveTheme.motion.durations.long2
        // True while a timeline chosen on the external-share picker is morphing into its detail
        // screen (ADR-0069). Held for exactly the transform's duration, like the quick-capture flag
        // above, so afterwards the detail's Back morphs back to its Timelines card instead.
        var shareCardTransformActive by remember { mutableStateOf(false) }
        var shareCardTransformEpoch by remember { mutableIntStateOf(0) }
        LaunchedEffect(shareCardTransformEpoch) {
            if (shareCardTransformActive) {
                kotlinx.coroutines.delay(quickCaptureTransformDuration.toLong())
                shareCardTransformActive = false
            }
        }
        var selectedIncomingShareId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(incomingShareState) {
            if (incomingShareState !is IncomingShareState.Idle) {
                onboardingBypassedForSession = true
            }
            val ready = incomingShareState as? IncomingShareState.Ready
            if (ready != null && ready.payload.requestId != selectedIncomingShareId) {
                selectedIncomingShareId = null
            }
        }
        val openQuickCapture: (QuickCaptureSurface) -> Unit = { surface ->
            quickCaptureCommand(surface)?.let { command ->
                onboardingBypassedForSession = true
                ActivePlayback.stopActive()
                if (command.timeline == CurrentTimeline.All) {
                    // Every root's `+ New` is the same act of writing to the archive, so they all
                    // land in the same place: Home's inline All composer (ADR-0061). Timelines and
                    // Search switch to the Home surface first rather than opening a separate All
                    // detail screen. A monotonic counter rather than a boolean, because Home is a
                    // persistent surface: a latched flag would make the second tap a silent no-op.
                    topLevel = ReliveTopLevelDestination.Home
                    rediscoverDestination = RediscoverDestination.Root
                    navigationToolbarExpanded = true
                    // Bumped in the swap frame deliberately: a rebuilt Home reads the pending
                    // request to skip its deep-anchor restore (whose scrollToItem snaps would
                    // steal the scroll mutex from the composer travel). The one-beat hold that
                    // lets Home visibly arrive before traveling lives on the Home surface itself.
                    homeComposerRequest += 1
                } else {
                    quickCaptureTransformActive = true
                    quickCaptureTransformEpoch += 1
                    topLevel = ReliveTopLevelDestination.Timelines
                    timelinesDestination = TimelinesDestination.TimelineDetail(
                        scope = command.timeline,
                        openComposerOnEnter = command.openComposer,
                        cameFromQuickCapture = true,
                    )
                }
            }
        }
        // A notification tap or the home-screen widget can ask the running app to add a moment. It
        // routes through the same inline Home composer as the global `+ New`, so a cold start lands
        // in the composer once Home settles and a warm tap re-opens it. The bus is a monotonic count,
        // so the second tap is never a silent no-op.
        val quickCaptureRequest by container.quickCaptureRequestBus.requests.collectAsState()
        LaunchedEffect(quickCaptureRequest) {
            if (quickCaptureRequest > 0) openQuickCapture(QuickCaptureSurface.TimelineHome)
        }
        LaunchedEffect(quickCaptureTransformEpoch) {
            if (quickCaptureTransformActive) {
                kotlinx.coroutines.delay(quickCaptureTransformDuration.toLong())
                quickCaptureTransformActive = false
            }
        }
        val mediaStorageViewModel = remember(container, scope) {
            MediaStorageViewModel(container.archiveInsightsRepository, scope)
        }
        val backupRestoreViewModel = remember(container, scope) {
            BackupRestoreViewModel(container.backupPreferencesRepository, container.googleDriveAccountManager, container.backupCoordinator, scope, container.entitlementProvider)
        }
        val exportViewModel = remember(container, scope) {
            ExportViewModel(
                container.momentRepository,
                container.timelineRepository,
                container.appearanceRepository,
                container.entitlementProvider,
                container.exportService,
                container.clock,
                scope,
                container.exportCompletionNotifier,
            )
        }
        DisposableEffect(exportViewModel) { onDispose(exportViewModel::close) }
        val exportFileHandle = rememberExportFileHandle()
        var portableArchive by remember(container) {
            mutableStateOf<com.vaibhav.relive.platform.exporting.OpenedPortableArchive?>(null)
        }
        var portableArchiveError by remember(container) { mutableStateOf<String?>(null) }
        val portableArchiveRequest by container.portableArchiveRequestBus.request.collectAsState()
        LaunchedEffect(portableArchiveRequest, locked) {
            val request = portableArchiveRequest ?: return@LaunchedEffect
            if (!locked) {
                onboardingBypassedForSession = true
                runCatching { container.exportService.openPortableArchive(request.path) }
                    .onSuccess { portableArchive = it }
                    .onFailure { portableArchiveError = it.message ?: "Relive could not open this archive." }
                container.exportService.deleteTemporaryFile(request.path)
                container.portableArchiveRequestBus.consume(request.id)
            }
        }
        // Home's own sub-destination: Back closes the appearance screen onto the Home surface,
        // which keeps its scroll position because the list state is hoisted here.
        ReliveBackHandler(
            enabled = !locked &&
                topLevel == ReliveTopLevelDestination.Home &&
                rediscoverDestination == RediscoverDestination.AllTheme &&
                profileNavigation.destination == ProfileDestination.Closed,
        ) { rediscoverDestination = RediscoverDestination.Root }
        val canReturnToTimelineHome = profileNavigation.destination == ProfileDestination.Closed &&
            timelinesDestination == TimelinesDestination.TimelineHome &&
            rediscoverDestination == RediscoverDestination.Root &&
            topLevel != ReliveTopLevelDestination.Home
        val returnToTimelineHome = {
            ActivePlayback.stopActive()
            topLevel = ReliveTopLevelDestination.Home
            navigationToolbarExpanded = true
        }
        ReliveBackHandler(enabled = !locked && canReturnToTimelineHome, onBack = returnToTimelineHome)
        val motion = ReliveTheme.motion
        val reduceMotion = ReliveTheme.reduceMotion
        // The lock gate animates in one direction only. Unlocking lifts the veil — it scales up
        // slightly as it fades while the app scales in from 0.92 beneath it, the same
        // fade-through voice as the top-level destination swap. Locking, though, is the
        // privacy-critical direction: it only ever happens when a locked app returns to the
        // foreground, and any cross-fade would leave the archive legible under a settling veil
        // for its first frames — so the veil takes the screen in a single frame, exactly as the
        // unanimated gate always did.
        AnimatedContent(
            targetState = locked,
            transitionSpec = {
                if (targetState) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val enterSpec = motion.spec<Float>(
                        reduceMotion = reduceMotion,
                        full = tween(
                            durationMillis = motion.durations.medium2,
                            delayMillis = motion.durations.short4,
                            easing = motion.easings.emphasizedDecelerate,
                        ),
                    )
                    val exitSpec = motion.spec<Float>(
                        reduceMotion = reduceMotion,
                        full = tween(
                            durationMillis = motion.durations.medium2,
                            easing = motion.easings.emphasizedAccelerate,
                        ),
                    )
                    val enter = fadeIn(enterSpec).let { fade ->
                        if (reduceMotion) fade else fade + scaleIn(enterSpec, initialScale = 0.92f)
                    }
                    val exit = fadeOut(exitSpec).let { fade ->
                        if (reduceMotion) fade else fade + scaleOut(exitSpec, targetScale = 1.06f)
                    }
                    enter togetherWith exit
                }
            },
            label = "app lock gate",
        ) { isLocked ->
        if (isLocked) {
            AppLockScreen(
                biometricsEnabled = profileSettings.biometricUnlockEnabled &&
                    container.deviceAuthentication.capabilities.biometricsAvailable,
                deviceCredentialAvailable = container.deviceAuthentication.capabilities.deviceAuthenticationAvailable,
                onUnlock = { lockController.unlock() },
                onUnlockWithDeviceCredential = { lockController.unlockWithDeviceCredential() },
            )
        } else {
            if (portableArchiveError != null) {
                PortableArchiveErrorScreen(portableArchiveError!!) { portableArchiveError = null }
            } else if (portableArchive != null) {
                PortableArchiveScreen(
                    archive = portableArchive!!,
                    onClose = {
                        container.exportService.releasePortableArchive(portableArchive!!)
                        portableArchive = null
                    },
                )
            } else {
            val showIncomingSharePicker = incomingShareState !is IncomingShareState.Idle &&
                (incomingShareState !is IncomingShareState.Ready ||
                    (incomingShareState as IncomingShareState.Ready).payload.requestId != selectedIncomingShareId)
            AnimatedContent(
                targetState = showIncomingSharePicker,
                transitionSpec = {
                    // Choosing a timeline on the picker is the same container transform as
                    // choosing one on Timelines (ADR-0069), so the route swap cross-fades over the
                    // morph exactly as the Timelines to detail swap does. Cancelling the share, and
                    // the picker's own arrival, keep the short slide/fade.
                    if (shareCardTransformActive && !targetState && !reduceMotion) {
                        val spec = tween<Float>(
                            durationMillis = motion.durations.long2,
                            easing = motion.easings.emphasized,
                        )
                        fadeIn(animationSpec = spec) togetherWith fadeOut(animationSpec = spec)
                    } else {
                        reliveSequentialSlideFade(
                            motion = motion,
                            reduceMotion = reduceMotion,
                            enterFromRight = targetState,
                        )
                    }
                },
                label = "incoming share route",
            ) { showingPicker ->
                // The picker and the detail screen it opens are the two states of this content, so
                // this is the scope that keys their shared container (ADR-0069) — the same
                // arrangement the Timelines/detail transform already runs on.
                val shareRouteScope = this
                if (showingPicker) {
                    ShareTimelinePickerScreen(
                        shareState = incomingShareState,
                        summaries = homeState.customSummaries,
                        timelinesLoading = homeState.content == TimelineHomeContent.Loading,
                        mediaStore = container.mediaStore,
                        hasDraft = { timeline -> composerDraftStore.restore(timeline)?.hasUserDraft == true },
                        onSelect = { timeline, payload ->
                            selectedIncomingShareId = payload.requestId
                            ActivePlayback.stopActive()
                            profileNavigation = ProfileNavigationState()
                            topLevel = ReliveTopLevelDestination.Timelines
                            // The tapped card is what the timeline grows out of, so the id that
                            // keys the two halves is recorded before the route changes.
                            cardTransformTimelineId = timeline.id
                            shareCardTransformActive = true
                            shareCardTransformEpoch += 1
                            timelinesDestination = TimelinesDestination.TimelineDetail(
                                scope = CurrentTimeline.Custom(timeline.id),
                                openComposerOnEnter = true,
                                incomingShare = payload,
                                cameFromCard = true,
                                seedTimeline = timeline,
                            )
                        },
                        onCancel = {
                            container.incomingShareGateway.cancel()
                            onIncomingShareCancelled?.invoke()
                        },
                        onRetry = container.incomingShareGateway::retry,
                        cardContainerModifier = { timeline ->
                            Modifier.timelineCardSharedBounds(
                                timelineId = timeline.id,
                                sharedScope = sharedTransitionScope,
                                animatedScope = shareRouteScope,
                                reduceMotion = reduceMotion,
                            )
                        },
                    )
                } else if (!onboardingBypassedForSession && onboardingVisible == true) {
                    OnboardingScreen(
                        mediaStore = container.mediaStore,
                        onContinue = {
                            scope.launch {
                                if (
                                    container.onboardingPreferencesRepository
                                        .complete(CURRENT_ONBOARDING_VERSION)
                                        .isSuccess
                                ) {
                                    onboardingVisible = false
                                }
                            }
                        },
                        onSkip = {
                            container.onboardingPreferencesRepository
                                .complete(CURRENT_ONBOARDING_VERSION)
                                .isSuccess
                        },
                    )
                } else if (!onboardingBypassedForSession && onboardingVisible == null) {
                    // Resolve the bounded existing-install migration before choosing between the
                    // introduction and Home. The atmospheric ground is already painted outside.
                    Box(Modifier.fillMaxSize())
                } else {
                    AnimatedContent(
                        targetState = profileNavigation.destination,
                        transitionSpec = {
                            reliveForwardBackward(
                                motion = motion,
                                reduceMotion = reduceMotion,
                                movingForward = profileDestinationDepth(targetState) >
                                    profileDestinationDepth(initialState),
                            )
                        },
                        label = "profile hierarchy navigation",
                    ) { destination ->
                        when (destination) {
            ProfileDestination.Profile -> ProfileScreen(
                viewModel = profileViewModel,
                appearanceViewModel = appearanceViewModel,
                listState = profileListState,
                onBack = { profileNavigation = profileNavigation.returnToTimelineHome() },
                onOpenPreferences = { profileNavigation = profileNavigation.openPreferences() },
                onOpenMediaStorage = { profileNavigation = profileNavigation.openMediaStorage() },
                onOpenBackupRestore = { profileNavigation = profileNavigation.openBackupRestore() },
                onOpenExport = {
                    exportViewModel.prepareForEntry()
                    profileNavigation = profileNavigation.openExport()
                },
                onOpenReliveArchive = {
                    scope.launch {
                        ExternalActivityGuard.active = true
                        val path = try {
                            exportFileHandle.choosePortableArchive()
                        } catch (error: Throwable) {
                            portableArchiveError = error.message ?: "Relive could not open this archive."
                            null
                        } finally {
                            ExternalActivityGuard.active = false
                        }
                        if (path != null) {
                            try {
                                portableArchive = container.exportService.openPortableArchive(path)
                                onboardingBypassedForSession = true
                            } catch (error: Throwable) {
                                portableArchiveError = error.message ?: "Relive could not open this archive."
                            } finally {
                                container.exportService.deleteTemporaryFile(path)
                            }
                        }
                    }
                },
                onOpenUpgrade = { profileNavigation = profileNavigation.openUpgrade() },
                onOpenLocation = { profileNavigation = profileNavigation.openLocation() },
                onOpenNotifications = { profileNavigation = profileNavigation.openNotifications() },
                onOpenPrivacy = { profileNavigation = profileNavigation.openPrivacy() },
                onOpenHelp = { profileNavigation = profileNavigation.openHelp() },
                onOpenAbout = { profileNavigation = profileNavigation.openAbout() },
                mediaStore = container.mediaStore,
                mediaProcessor = container.mediaProcessor,
                entitlementProvider = container.entitlementProvider,
                exportState = exportViewModel.state.collectAsState().value,
            )
            ProfileDestination.Preferences -> PreferencesScreen(
                viewModel = behaviorPreferencesViewModel,
                onBack = { profileNavigation = profileNavigation.returnToProfile() },
            )
            ProfileDestination.MediaStorage -> MediaStorageScreen(
                viewModel = mediaStorageViewModel,
                onBack = { profileNavigation = profileNavigation.returnToProfile() },
            )
            ProfileDestination.BackupRestore -> BackupRestoreScreen(
                viewModel = backupRestoreViewModel,
                onBack = { profileNavigation = profileNavigation.returnToProfile() },
                onUpgrade = {
                    profileNavigation = profileNavigation.openUpgrade(ProfileDestination.BackupRestore)
                },
            )
            ProfileDestination.Export -> ExportScreen(
                viewModel = exportViewModel,
                mediaStore = container.mediaStore,
                onBack = { profileNavigation = profileNavigation.returnToProfile() },
                onUpgrade = { profileNavigation = profileNavigation.openUpgrade(ProfileDestination.Export) },
            )
            ProfileDestination.Upgrade -> UpgradeToProScreen(
                entitlementProvider = container.entitlementProvider,
                legalLinks = container.legalLinks,
                onBack = { profileNavigation = profileNavigation.returnFromUpgrade() },
            )
            ProfileDestination.Location -> LocationScreen(behaviorState.preferences.showLocations, behaviorPreferencesViewModel::setShowLocations) { profileNavigation = profileNavigation.returnToProfile() }
            ProfileDestination.RediscoverNotifications -> RediscoverNotificationsScreen(
                profileSettings,
                notificationPermission,
                onEnabledChange = { enabled -> scope.launch { reminderController.setEnabled(enabled); notificationPermission = container.rediscoverReminderService.permissionState() } },
                onOpenSettings = ::openAppSettings,
                onBack = { profileNavigation = profileNavigation.returnToProfile() },
            )
            ProfileDestination.PrivacySecurity -> PrivacySecurityScreen(
                profileSettings,
                container.deviceAuthentication.capabilities.deviceAuthenticationAvailable,
                container.deviceAuthentication.capabilities.biometricsAvailable,
                container.deviceAuthentication.capabilities.biometricsExplanation,
                onAppLockChange = { enabled -> scope.launch { lockController.setEnabled(enabled) } },
                onBiometricsChange = { enabled -> scope.launch { lockController.setBiometrics(enabled) } },
                onLockAfterChange = { value -> scope.launch { lockController.setLockAfter(value) } },
                onBack = { profileNavigation = profileNavigation.returnToProfile() },
            )
            ProfileDestination.HelpFeedback -> HelpFeedbackScreen(supportEmail = container.supportEmail, onBack = { profileNavigation = profileNavigation.returnToProfile() }, onMessage = {})
            ProfileDestination.AboutRelive -> AboutReliveScreen(legalLinks = container.legalLinks, onOpenLicenses = { profileNavigation = profileNavigation.openLicenses() }, onBack = { profileNavigation = profileNavigation.returnToProfile() })
            ProfileDestination.Licenses -> LicensesScreen(onBack = { profileNavigation = profileNavigation.openAbout() })
                            ProfileDestination.Closed -> AnimatedContent(
                                targetState = timelinesDestination,
                                transitionSpec = {
                                    val isThemeNavigation =
                                        (initialState is TimelinesDestination.TimelineDetail &&
                                            targetState is TimelinesDestination.TimelineTheme) ||
                                            (initialState is TimelinesDestination.TimelineTheme &&
                                                targetState is TimelinesDestination.TimelineDetail)
                                    val isQuickCaptureSwap = quickCaptureTransformActive &&
                                        ((initialState is TimelinesDestination.TimelineHome &&
                                            targetState is TimelinesDestination.TimelineDetail) ||
                                            (initialState is TimelinesDestination.TimelineDetail &&
                                                targetState is TimelinesDestination.TimelineHome))
                                    val from = initialState
                                    val to = targetState
                                    val isCardTransform =
                                        (from is TimelinesDestination.TimelineHome &&
                                            to is TimelinesDestination.TimelineDetail &&
                                            to.cameFromCard) ||
                                            (from is TimelinesDestination.TimelineDetail &&
                                                from.cameFromCard &&
                                                to is TimelinesDestination.TimelineHome)
                                    if (isThemeNavigation) {
                                        reliveForwardBackward(
                                            motion = motion,
                                            reduceMotion = reduceMotion,
                                            movingForward = targetState is TimelinesDestination.TimelineTheme,
                                        )
                                    } else if ((isQuickCaptureSwap || isCardTransform) && !reduceMotion) {
                                        val spec = tween<Float>(
                                            durationMillis = motion.durations.long2,
                                            easing = motion.easings.emphasized,
                                        )
                                        fadeIn(animationSpec = spec) togetherWith fadeOut(animationSpec = spec)
                                    } else {
                                        EnterTransition.None togetherWith ExitTransition.None
                                    }
                                },
                                label = "timeline detail theme navigation",
                            ) { destination ->
                                val animatedScope = this
                                when (val active = destination) {
            is TimelinesDestination.TimelineDetail -> {
                val transformActive = quickCaptureTransformActive && active.cameFromQuickCapture
                val cardTransformId = (active.scope as? CurrentTimeline.Custom)
                    ?.id
                    ?.takeIf { active.cameFromCard }
                val containerModifier = when {
                    transformActive -> Modifier.fillMaxSize().quickCaptureSharedBounds(
                        sharedScope = sharedTransitionScope,
                        animatedScope = animatedScope,
                        reduceMotion = reduceMotion,
                    )
                    cardTransformId != null -> Modifier.fillMaxSize().timelineCardSharedBounds(
                        timelineId = cardTransformId,
                        sharedScope = sharedTransitionScope,
                        // Arriving from the share picker, the card being morphed out of belongs to
                        // the share route, not to this one (ADR-0069); once that swap has run its
                        // course the detail rejoins the Timelines route it returns to.
                        animatedScope = if (shareCardTransformActive) shareRouteScope else animatedScope,
                        reduceMotion = reduceMotion,
                    )
                    else -> Modifier.fillMaxSize()
                }
                val innerModifier = if ((transformActive || cardTransformId != null) && !reduceMotion) {
                    with(sharedTransitionScope) {
                        Modifier.fillMaxSize().skipToLookaheadSize()
                    }
                } else Modifier.fillMaxSize()
                Box(containerModifier) { Box(innerModifier) {
                    TimelineScreen(
                        momentRepository = container.momentRepository,
                        timelineRepository = container.timelineRepository,
                        appearanceRepository = container.appearanceRepository,
                        rediscoverRepository = container.rediscoverRepository,
                        clock = container.clock,
                        idGenerator = container.idGenerator,
                        mediaStore = container.mediaStore,
                        mediaProcessor = container.mediaProcessor,
                        mediaDownloadService = container.mediaDownloadService,
                        draftStore = composerDraftStore,
                        initialTimeline = active.scope,
                        seedCustomTimeline = active.seedTimeline,
                        selectedMomentId = active.selectedMomentId,
                        openComposerOnEnter = active.openComposerOnEnter,
                        incomingShare = active.incomingShare,
                        incomingShareApplyReady = !shareCardTransformActive,
                        onIncomingShareApplied = { requestId ->
                            // Claiming transfers the temporary media to the existing composer.
                            // Keep the route itself stable: clearing `incomingShare` here changes
                            // AnimatedContent's target identity, which disposes and recreates the
                            // TimelineScreen (and its remembered composer) while media processing
                            // is starting. That both loses the newly appended attachments and
                            // starts a second, visibly jarring destination transition.
                            container.incomingShareGateway.claim(requestId)
                        },
                        onBackToTimelineHome = {
                            if (active.cameFromQuickCapture) {
                                quickCaptureTransformActive = true
                                quickCaptureTransformEpoch += 1
                            }
                            timelinesDestination = TimelinesDestination.TimelineHome
                        },
                        onOpenTimelineTheme = {
                            if (active.scope.timelineThemeDestinationOrNull() != null) {
                                timelinesDestination = TimelinesDestination.TimelineTheme(active)
                            }
                        },
                        behaviorPreferences = behaviorState.preferences,
                    )
                } }
            }
            is TimelinesDestination.TimelineTheme -> {
                val destination = active.returnTo.scope.timelineThemeDestinationOrNull()
                if (destination != null) {
                    TimelineThemeScreen(
                        timelineRepository = container.timelineRepository,
                        appearanceRepository = container.appearanceRepository,
                        destination = destination,
                        onBack = { timelinesDestination = active.returnTo },
                        entitlementProvider = container.entitlementProvider,
                        onUpgrade = {
                            profileNavigation = profileNavigation.openUpgrade(ProfileDestination.Closed)
                        },
                    )
                } else {
                    timelinesDestination = active.returnTo
                }
            }
            TimelinesDestination.TimelineHome -> Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = topLevel,
                    transitionSpec = {
                        val exitSpec = motion.spec<Float>(
                            reduceMotion = reduceMotion,
                            full = tween(
                                durationMillis = motion.durations.short4,
                                easing = motion.easings.emphasizedAccelerate,
                            ),
                        )
                        val enterSpec = motion.spec<Float>(
                            reduceMotion = reduceMotion,
                            full = tween(
                                durationMillis = motion.durations.medium2,
                                delayMillis = motion.durations.short4,
                                easing = motion.easings.emphasizedDecelerate,
                            ),
                            reduced = tween(
                                durationMillis = motion.durations.short3,
                                delayMillis = motion.durations.short3,
                                easing = motion.easings.standard,
                            ),
                        )
                        val exit = fadeOut(animationSpec = exitSpec).let { fade ->
                            if (reduceMotion) fade else fade + scaleOut(
                                animationSpec = exitSpec,
                                targetScale = 0.92f,
                            )
                        }
                        val enter = fadeIn(animationSpec = enterSpec).let { fade ->
                            if (reduceMotion) fade else fade + scaleIn(
                                animationSpec = enterSpec,
                                initialScale = 0.92f,
                            )
                        }
                        enter togetherWith exit
                    },
                    label = "top-level fade through",
                ) { destination ->
                    when (destination) {
                        ReliveTopLevelDestination.Timelines -> TimelineHomeScreen(
                            viewModel = homeViewModel,
                            mediaStore = container.mediaStore,
                            mediaProcessor = container.mediaProcessor,
                            listState = homeListState,
                            onOpenTimeline = { destination ->
                                val timeline = destination.timeline
                                // The tapped card is what the detail screen grows out of, so the
                                // id that keys the two halves is recorded before the route changes.
                                cardTransformTimelineId = (timeline as? Timeline.Custom)?.id
                                timelinesDestination = TimelinesDestination.TimelineDetail(
                                    scope = when (timeline) {
                                        Timeline.All -> CurrentTimeline.All
                                        is Timeline.Custom -> CurrentTimeline.Custom(timeline.id)
                                    },
                                    openComposerOnEnter = destination.openComposerOnEnter,
                                    cameFromCard = timeline is Timeline.Custom,
                                    seedTimeline = timeline as? Timeline.Custom,
                                )
                            },
                            cardContainerModifier = { timeline ->
                                // Register every composed card before it can be tapped. Waiting
                                // for cardTransformTimelineId means the first source is registered
                                // in the same frame that navigation removes it, so SharedTransition
                                // has no stable measured bounds for the forward transform.
                                Modifier.timelineCardSharedBounds(
                                    timelineId = timeline.id,
                                    sharedScope = sharedTransitionScope,
                                    animatedScope = animatedScope,
                                    reduceMotion = reduceMotion,
                                )
                            },
                            onOpenProfile = { profileNavigation = profileNavigation.openProfile() },
                            profilePhoto = profileSettings.profilePhoto,
                            onCreateMoment = { openQuickCapture(QuickCaptureSurface.TimelineHome) },
                            navigationToolbarExpanded = navigationToolbarExpanded,
                            onNavigationToolbarExpand = { navigationToolbarExpanded = true },
                            onNavigationToolbarCollapse = { navigationToolbarExpanded = false },
                            onUpgrade = {
                                profileNavigation = profileNavigation.openUpgrade(ProfileDestination.Closed)
                            },
                        )
                        ReliveTopLevelDestination.Home -> {
                // Home remains composed while a Rediscover collection is visible. Disposing it in
                // the route swap meant Back exposed a newly measured expanded backdrop and
                // carousel halfway through its fade, which made their independent settle motion
                // visible. The collection is therefore a fading layer over the settled surface,
                // just like Mood insights.
                Box(Modifier.fillMaxSize()) {
                    if (homeSurfaceState.isRediscoverPrepared) {
                        HomeScreen(
                        momentRepository = container.momentRepository,
                        timelineRepository = container.timelineRepository,
                        appearanceRepository = container.appearanceRepository,
                        rediscoverRepository = container.rediscoverRepository,
                        profileSettingsRepository = container.profileSettingsRepository,
                        clock = container.clock,
                        idGenerator = container.idGenerator,
                        mediaStore = container.mediaStore,
                        mediaProcessor = container.mediaProcessor,
                        mediaDownloadService = container.mediaDownloadService,
                        listState = homeFeedListState,
                        surfaceState = homeSurfaceState,
                        draftStore = composerDraftStore,
                        onOpenFavorites = { selectedMomentId, cover ->
                            rediscoverDestination = RediscoverDestination.Favorites(selectedMomentId, cover)
                        },
                        onOpenOnThisDay = { selectedMomentId, date, cover ->
                            rediscoverDestination = RediscoverDestination.OnThisDay(selectedMomentId, date, cover)
                        },
                        onOpenFromYourPast = { selectedMomentId, query, cover ->
                            rediscoverDestination = RediscoverDestination.FromYourPast(selectedMomentId, query, cover)
                        },
                        onOpenAllPhotos = { cover ->
                            rediscoverDestination = RediscoverDestination.AllPhotos(cover)
                        },
                        expandComposerRequest = homeComposerRequest,
                        // Cleared once Home has expanded the composer, so re-entering Home
                        // later does not replay a stale request.
                        onExpandComposerRequestHandled = { homeComposerRequest = 0 },
                        onOpenTimelineTheme = {
                            rediscoverDestination = RediscoverDestination.AllTheme
                        },
                        onOpenProfile = { profileNavigation = profileNavigation.openProfile() },
                        navigationToolbarExpanded = navigationToolbarExpanded,
                        onNavigationToolbarExpand = { navigationToolbarExpanded = true },
                        onNavigationToolbarCollapse = { navigationToolbarExpanded = false },
                        behaviorPreferences = behaviorState.preferences,
                        wallpaper = appearanceState.preferences.allTimelineAppearance.wallpaper,
                        onMediaCaptureOverlayChanged = { homeCaptureOverlayActive = it },
                        onMoodInsightsVisibilityChanged = { moodInsightsOpen = it },
                        )
                    }
                AnimatedContent(
                    targetState = rediscoverDestination,
                    transitionSpec = {
                        // Collections use the same direct fade character as Mood insights. There
                        // is no shared geometry between the masked carousel card and destination,
                        // so opening and Back cannot collapse through a transient card position.
                        val enterSpec = motion.spec<Float>(
                            reduceMotion = reduceMotion,
                            full = tween(durationMillis = motion.durations.medium2),
                            reduced = tween(durationMillis = motion.durations.short3),
                        )
                        val exitSpec = motion.spec<Float>(
                            reduceMotion = reduceMotion,
                            full = tween(durationMillis = motion.durations.short4),
                            reduced = tween(durationMillis = motion.durations.short3),
                        )
                        fadeIn(enterSpec) togetherWith fadeOut(exitSpec)
                    },
                    label = "rediscover collection navigation",
                ) { destination ->
                    when (destination) {
                        // Home is already the base layer. This empty target holds its geometry
                        // still while the departing collection fades away on Back.
                        RediscoverDestination.Root -> Box(Modifier.fillMaxSize())
                        RediscoverDestination.AllTheme -> TimelineThemeScreen(
                            timelineRepository = container.timelineRepository,
                            appearanceRepository = container.appearanceRepository,
                            destination = TimelineThemeDestination.All,
                            onBack = { rediscoverDestination = RediscoverDestination.Root },
                            entitlementProvider = container.entitlementProvider,
                            onUpgrade = {
                                profileNavigation = profileNavigation.openUpgrade(ProfileDestination.Closed)
                            },
                        )
                        is RediscoverDestination.Favorites -> {
                            val favorites = destination
                                TimelineScreen(
                                    momentRepository = container.momentRepository,
                                    timelineRepository = container.timelineRepository,
                                    appearanceRepository = container.appearanceRepository,
                                    rediscoverRepository = container.rediscoverRepository,
                                    clock = container.clock,
                                    idGenerator = container.idGenerator,
                                    mediaStore = container.mediaStore,
                                    mediaProcessor = container.mediaProcessor,
                                    mediaDownloadService = container.mediaDownloadService,
                                    draftStore = composerDraftStore,
                                    initialTimeline = CurrentTimeline.Favorites,
                                    mode = TimelineMode.ReadOnlySystemCollection(title = "Favourites"),
                                    selectedMomentId = favorites.selectedMomentId,
                                    collectionCover = favorites.cover,
                                    onBackToTimelineHome = { rediscoverDestination = RediscoverDestination.Root },
                                    behaviorPreferences = behaviorState.preferences,
                                )
                        }
                        is RediscoverDestination.OnThisDay -> {
                            val onThisDay = destination
                                TimelineScreen(
                                    momentRepository = container.momentRepository,
                                    timelineRepository = container.timelineRepository,
                                    appearanceRepository = container.appearanceRepository,
                                    rediscoverRepository = container.rediscoverRepository,
                                    clock = container.clock,
                                    idGenerator = container.idGenerator,
                                    mediaStore = container.mediaStore,
                                    mediaProcessor = container.mediaProcessor,
                                    mediaDownloadService = container.mediaDownloadService,
                                    draftStore = composerDraftStore,
                                    initialTimeline = CurrentTimeline.OnThisDay(onThisDay.date),
                                    mode = TimelineMode.ReadOnlySystemCollection(title = "On This Day"),
                                    selectedMomentId = onThisDay.selectedMomentId,
                                    collectionCover = onThisDay.cover,
                                    onBackToTimelineHome = { rediscoverDestination = RediscoverDestination.Root },
                                    behaviorPreferences = behaviorState.preferences,
                                )
                        }
                        is RediscoverDestination.AllPhotos -> {
                            val allPhotos = destination
                                TimelineScreen(
                                    momentRepository = container.momentRepository,
                                    timelineRepository = container.timelineRepository,
                                    appearanceRepository = container.appearanceRepository,
                                    rediscoverRepository = container.rediscoverRepository,
                                    clock = container.clock,
                                    idGenerator = container.idGenerator,
                                    mediaStore = container.mediaStore,
                                    mediaProcessor = container.mediaProcessor,
                                    mediaDownloadService = container.mediaDownloadService,
                                    draftStore = composerDraftStore,
                                    initialTimeline = CurrentTimeline.AllPhotos,
                                    mode = TimelineMode.ReadOnlySystemCollection(title = "Media"),
                                    collectionCover = allPhotos.cover,
                                    onBackToTimelineHome = { rediscoverDestination = RediscoverDestination.Root },
                                    behaviorPreferences = behaviorState.preferences,
                                )
                        }
                        is RediscoverDestination.FromYourPast -> {
                            val fromYourPast = destination
                                TimelineScreen(
                                    momentRepository = container.momentRepository,
                                    timelineRepository = container.timelineRepository,
                                    appearanceRepository = container.appearanceRepository,
                                    rediscoverRepository = container.rediscoverRepository,
                                    clock = container.clock,
                                    idGenerator = container.idGenerator,
                                    mediaStore = container.mediaStore,
                                    mediaProcessor = container.mediaProcessor,
                                    mediaDownloadService = container.mediaDownloadService,
                                    draftStore = composerDraftStore,
                                    initialTimeline = CurrentTimeline.FromYourPast(fromYourPast.query),
                                    mode = TimelineMode.ReadOnlySystemCollection(title = "From Your Past"),
                                    selectedMomentId = fromYourPast.selectedMomentId,
                                    collectionCover = fromYourPast.cover,
                                    onBackToTimelineHome = { rediscoverDestination = RediscoverDestination.Root },
                                    behaviorPreferences = behaviorState.preferences,
                                )
                        }
                    }
                }
                }
                        }
                        ReliveTopLevelDestination.Search -> SearchScreen(
                            viewModel = searchViewModel,
                            mediaStore = container.mediaStore,
                            listState = searchListState,
                            clock = container.clock,
                            onBack = returnToTimelineHome,
                            onOpenAllAtMoment = { momentId ->
                                timelinesDestination = TimelinesDestination.TimelineDetail(CurrentTimeline.All, momentId)
                                topLevel = ReliveTopLevelDestination.Timelines
                            },
                            onCreateMoment = { openQuickCapture(QuickCaptureSurface.Search) },
                            wallpaper = appearanceState.preferences.allTimelineAppearance.wallpaper,
                            navigationToolbarExpanded = navigationToolbarExpanded,
                            onNavigationToolbarExpand = { navigationToolbarExpanded = true },
                            onNavigationToolbarCollapse = { navigationToolbarExpanded = false },
                        )
                    }
                }
                if (
                    (topLevel != ReliveTopLevelDestination.Home ||
                        (rediscoverDestination is RediscoverDestination.Root &&
                            homeSurfaceState.isRediscoverPrepared)) &&
                        !moodInsightsOpen
                ) AnimatedVisibility(
                    // The floating chrome stands down while Home's full-screen camera is up —
                    // otherwise it floats over the viewfinder. The exit hides under the opaque
                    // camera surface, so it is fast; the return matches the top-level enter fade.
                    visible = !homeCaptureOverlayActive,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(
                        animationSpec = motion.spec(
                            reduceMotion = reduceMotion,
                            full = tween(
                                durationMillis = motion.durations.medium2,
                                easing = motion.easings.emphasizedDecelerate,
                            ),
                        ),
                    ),
                    exit = fadeOut(
                        animationSpec = motion.spec(
                            reduceMotion = reduceMotion,
                            full = tween(
                                durationMillis = motion.durations.short2,
                                easing = motion.easings.emphasizedAccelerate,
                            ),
                        ),
                    ),
                    label = "home chrome under camera",
                ) {
                    ReliveFloatingBottomControls(
                        selected = topLevel,
                        expanded = navigationToolbarExpanded,
                        onSelect = {
                            ActivePlayback.stopActive()
                            topLevel = it
                            navigationToolbarExpanded = true
                        },
                        onCreateMoment = {
                            openQuickCapture(
                                when (topLevel) {
                                    ReliveTopLevelDestination.Home -> QuickCaptureSurface.Rediscover
                                    ReliveTopLevelDestination.Timelines -> QuickCaptureSurface.TimelineHome
                                    ReliveTopLevelDestination.Search -> QuickCaptureSurface.Search
                                },
                            )
                        },
                        newMomentModifier = if (quickCaptureTransformActive) {
                            Modifier.quickCaptureSharedBounds(
                                sharedScope = sharedTransitionScope,
                                animatedScope = animatedScope,
                                reduceMotion = reduceMotion,
                            )
                        } else Modifier,
                    )
                }
            }
                            }
                            }
                            }
                        }
                    }
                }
        }
        }
        }
        }
    }
}

private fun profileDestinationDepth(destination: ProfileDestination): Int = when (destination) {
    ProfileDestination.Closed -> 0
    ProfileDestination.Profile -> 1
    ProfileDestination.Licenses -> 3
    ProfileDestination.Preferences,
    ProfileDestination.MediaStorage,
    ProfileDestination.BackupRestore,
    ProfileDestination.Export,
    ProfileDestination.Upgrade,
    ProfileDestination.Location,
    ProfileDestination.RediscoverNotifications,
    ProfileDestination.PrivacySecurity,
    ProfileDestination.HelpFeedback,
    ProfileDestination.AboutRelive,
    -> 2
}
