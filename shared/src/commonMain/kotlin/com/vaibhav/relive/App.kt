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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.vaibhav.relive.di.ReliveAppContainer
import com.vaibhav.relive.ui.screens.TimelineScreen
import com.vaibhav.relive.ui.screens.TimelineHomeScreen
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
import com.vaibhav.relive.presentation.profile.MediaStorageViewModel
import com.vaibhav.relive.ui.components.navigation.ReliveFloatingBottomControls
import com.vaibhav.relive.ui.components.navigation.ReliveTopLevelDestination
import com.vaibhav.relive.ui.components.composer.quickCaptureSharedBounds
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
import com.vaibhav.relive.presentation.profile.BackupRestoreViewModel
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
import com.vaibhav.relive.platform.system.openAppSettings
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.platform.share.IncomingSharePayload
import com.vaibhav.relive.platform.share.IncomingShareState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

private sealed interface TimelinesDestination {
    data object TimelineHome : TimelinesDestination
    data class TimelineDetail(
        val scope: CurrentTimeline,
        val selectedMomentId: MomentId? = null,
        val openComposerOnEnter: Boolean = false,
        val incomingShare: IncomingSharePayload? = null,
        val cameFromQuickCapture: Boolean = false,
    ) : TimelinesDestination
    data class TimelineTheme(val returnTo: TimelineDetail) : TimelinesDestination
}

private sealed interface RediscoverDestination {
    data object Root : RediscoverDestination
    data object AllPhotos : RediscoverDestination

    /**
     * The All timeline's appearance, reached from Home's app bar. Home is the surface that band
     * belongs to, so it returns to Home rather than to a timeline detail screen.
     */
    data object AllTheme : RediscoverDestination
    data class Favorites(val selectedMomentId: MomentId?) : RediscoverDestination
    data class OnThisDay(
        val selectedMomentId: MomentId,
        val date: com.vaibhav.relive.domain.model.LocalCalendarDate,
    ) : RediscoverDestination
    data class FromYourPast(
        val selectedMomentId: MomentId?,
        val query: RediscoverQuery,
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
        val homeFeedListState = rememberLazyListState()
        // Survives Home being swapped out for Profile, a collection or another destination.
        val homeSurfaceState = rememberHomeSurfaceState()
        val incomingShareState by container.incomingShareGateway.state.collectAsState()
        val rediscoverListState = rememberLazyListState()
        val searchListState = rememberLazyListState()
        val searchViewModel = remember(container, scope) { SearchViewModel(container.momentRepository, scope) }
        val profileViewModel = remember(container, scope) { ProfileViewModel(container.profileRepository, container.profileSettingsRepository, container.mediaStore, scope) }
        val profileSettings by container.profileSettingsRepository.settings.collectAsState()
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
        var quickCaptureTransformActive by remember { mutableStateOf(false) }
        /** Bumped by `+ New` while on Home; the Home surface expands its composer in place. */
        var homeComposerRequest by remember { mutableIntStateOf(0) }
        var quickCaptureTransformEpoch by remember { mutableIntStateOf(0) }
        val quickCaptureTransformDuration = ReliveTheme.motion.durations.long2
        var selectedIncomingShareId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(incomingShareState) {
            val ready = incomingShareState as? IncomingShareState.Ready
            if (ready != null && ready.payload.requestId != selectedIncomingShareId) {
                selectedIncomingShareId = null
            }
        }
        val openQuickCapture: (QuickCaptureSurface) -> Unit = { surface ->
            quickCaptureCommand(surface)?.let { command ->
                ActivePlayback.stopActive()
                val onHomeSurface = topLevel == ReliveTopLevelDestination.Home &&
                    rediscoverDestination == RediscoverDestination.Root
                if (onHomeSurface && command.timeline == CurrentTimeline.All) {
                    // All moments is already part of this surface, so `+ New` expands the existing
                    // inline composer in place. It must never navigate to a separate All screen
                    // (ADR-0061). A monotonic counter rather than a boolean, because Home is a
                    // persistent surface: a latched flag would make the second tap a silent no-op.
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
        if (locked) ReliveLockSurface(onUnlock = { scope.launch { lockController.unlock() } }) else {
            val motion = ReliveTheme.motion
            val reduceMotion = ReliveTheme.reduceMotion
            val showIncomingSharePicker = incomingShareState !is IncomingShareState.Idle &&
                (incomingShareState !is IncomingShareState.Ready ||
                    (incomingShareState as IncomingShareState.Ready).payload.requestId != selectedIncomingShareId)
            AnimatedContent(
                targetState = showIncomingSharePicker,
                transitionSpec = {
                    reliveSequentialSlideFade(
                        motion = motion,
                        reduceMotion = reduceMotion,
                        enterFromRight = targetState,
                    )
                },
                label = "incoming share route",
            ) { showingPicker ->
                if (showingPicker) {
                    val summaries = (homeState.content as? com.vaibhav.relive.presentation.timelinehome.TimelineHomeContent.Loaded)
                        ?.summaries
                        .orEmpty()
                    ShareTimelinePickerScreen(
                        shareState = incomingShareState,
                        summaries = summaries,
                        mediaStore = container.mediaStore,
                        hasDraft = { timeline -> composerDraftStore.restore(timeline)?.hasUserDraft == true },
                        onSelect = { timeline ->
                            (incomingShareState as? IncomingShareState.Ready)?.let { ready ->
                                selectedIncomingShareId = ready.payload.requestId
                                ActivePlayback.stopActive()
                                profileNavigation = ProfileNavigationState()
                                topLevel = ReliveTopLevelDestination.Timelines
                                timelinesDestination = TimelinesDestination.TimelineDetail(
                                    scope = when (timeline) {
                                        Timeline.All -> CurrentTimeline.All
                                        is Timeline.Custom -> CurrentTimeline.Custom(timeline.id)
                                    },
                                    openComposerOnEnter = true,
                                    incomingShare = ready.payload,
                                )
                            }
                        },
                        onCancel = {
                            container.incomingShareGateway.cancel()
                            onIncomingShareCancelled?.invoke()
                        },
                        onRetry = container.incomingShareGateway::retry,
                    )
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
                onBack = { profileNavigation = profileNavigation.returnToTimelineHome() },
                onOpenPreferences = { profileNavigation = profileNavigation.openPreferences() },
                onOpenMediaStorage = { profileNavigation = profileNavigation.openMediaStorage() },
                onOpenBackupRestore = { profileNavigation = profileNavigation.openBackupRestore() },
                onOpenUpgrade = { profileNavigation = profileNavigation.openUpgrade() },
                onOpenLocation = { profileNavigation = profileNavigation.openLocation() },
                onOpenNotifications = { profileNavigation = profileNavigation.openNotifications() },
                onOpenPrivacy = { profileNavigation = profileNavigation.openPrivacy() },
                onOpenHelp = { profileNavigation = profileNavigation.openHelp() },
                onOpenAbout = { profileNavigation = profileNavigation.openAbout() },
                mediaStore = container.mediaStore,
                mediaProcessor = container.mediaProcessor,
                entitlementProvider = container.entitlementProvider,
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
                onUpgrade = { profileNavigation = profileNavigation.openUpgrade() },
            )
            ProfileDestination.Upgrade -> UpgradeToProScreen(
                entitlementProvider = container.entitlementProvider,
                legalLinks = container.legalLinks,
                onBack = { profileNavigation = profileNavigation.returnToProfile() },
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
            ProfileDestination.HelpFeedback -> HelpFeedbackScreen(onBack = { profileNavigation = profileNavigation.returnToProfile() }, onMessage = {})
            ProfileDestination.AboutRelive -> AboutReliveScreen(onOpenLicenses = { profileNavigation = profileNavigation.openLicenses() }, onBack = { profileNavigation = profileNavigation.returnToProfile() })
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
                                    if (isThemeNavigation) {
                                        reliveForwardBackward(
                                            motion = motion,
                                            reduceMotion = reduceMotion,
                                            movingForward = targetState is TimelinesDestination.TimelineTheme,
                                        )
                                    } else if (isQuickCaptureSwap && !reduceMotion) {
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
                val containerModifier = if (transformActive) {
                    Modifier.fillMaxSize().quickCaptureSharedBounds(
                        sharedScope = sharedTransitionScope,
                        animatedScope = animatedScope,
                        reduceMotion = reduceMotion,
                    )
                } else {
                    Modifier.fillMaxSize()
                }
                val innerModifier = if (transformActive && !reduceMotion) {
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
                        draftStore = composerDraftStore,
                        initialTimeline = active.scope,
                        selectedMomentId = active.selectedMomentId,
                        openComposerOnEnter = active.openComposerOnEnter,
                        incomingShare = active.incomingShare,
                        onIncomingShareApplied = { requestId ->
                            container.incomingShareGateway.claim(requestId)
                            (timelinesDestination as? TimelinesDestination.TimelineDetail)?.let { current ->
                                timelinesDestination = current.copy(incomingShare = null)
                            }
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
                        onUpgrade = { profileNavigation = profileNavigation.openUpgrade() },
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
                                timelinesDestination = TimelinesDestination.TimelineDetail(
                                    scope = when (val timeline = destination.timeline) {
                                        Timeline.All -> CurrentTimeline.All
                                        is Timeline.Custom -> CurrentTimeline.Custom(timeline.id)
                                    },
                                    openComposerOnEnter = destination.openComposerOnEnter,
                                )
                            },
                            onOpenProfile = { profileNavigation = profileNavigation.openProfile() },
                            profilePhoto = profileSettings.profilePhoto,
                            onCreateMoment = { openQuickCapture(QuickCaptureSurface.TimelineHome) },
                            navigationToolbarExpanded = navigationToolbarExpanded,
                            onNavigationToolbarExpand = { navigationToolbarExpanded = true },
                            onNavigationToolbarCollapse = { navigationToolbarExpanded = false },
                            onUpgrade = { profileNavigation = profileNavigation.openUpgrade() },
                        )
                        ReliveTopLevelDestination.Home -> {
                AnimatedContent(
                    targetState = rediscoverDestination,
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
                                easing = motion.easings.standard,
                            ),
                        )
                        fadeIn(enterSpec) togetherWith fadeOut(exitSpec)
                    },
                    label = "rediscover collection fade through",
                ) { destination ->
                    when (destination) {
                        RediscoverDestination.Root -> HomeScreen(
                            momentRepository = container.momentRepository,
                            timelineRepository = container.timelineRepository,
                            appearanceRepository = container.appearanceRepository,
                            rediscoverRepository = container.rediscoverRepository,
                            profileSettingsRepository = container.profileSettingsRepository,
                            clock = container.clock,
                            idGenerator = container.idGenerator,
                            mediaStore = container.mediaStore,
                            mediaProcessor = container.mediaProcessor,
                            listState = homeFeedListState,
                            surfaceState = homeSurfaceState,
                            draftStore = composerDraftStore,
                            onOpenFavorites = { selectedMomentId ->
                                rediscoverDestination = RediscoverDestination.Favorites(selectedMomentId)
                            },
                            onOpenOnThisDay = { selectedMomentId, date ->
                                rediscoverDestination = RediscoverDestination.OnThisDay(selectedMomentId, date)
                            },
                            onOpenFromYourPast = { selectedMomentId, query ->
                                rediscoverDestination = RediscoverDestination.FromYourPast(selectedMomentId, query)
                            },
                            onOpenAllPhotos = {
                                rediscoverDestination = RediscoverDestination.AllPhotos
                            },
                            expandComposerRequest = homeComposerRequest,
                            onOpenTimelineTheme = {
                                rediscoverDestination = RediscoverDestination.AllTheme
                            },
                            onOpenProfile = { profileNavigation = profileNavigation.openProfile() },
                            navigationToolbarExpanded = navigationToolbarExpanded,
                            onNavigationToolbarExpand = { navigationToolbarExpanded = true },
                            onNavigationToolbarCollapse = { navigationToolbarExpanded = false },
                            behaviorPreferences = behaviorState.preferences,
                            wallpaper = appearanceState.preferences.allTimelineAppearance.wallpaper,
                        )
                        RediscoverDestination.AllTheme -> TimelineThemeScreen(
                            timelineRepository = container.timelineRepository,
                            appearanceRepository = container.appearanceRepository,
                            destination = TimelineThemeDestination.All,
                            onBack = { rediscoverDestination = RediscoverDestination.Root },
                            entitlementProvider = container.entitlementProvider,
                            onUpgrade = { profileNavigation = profileNavigation.openUpgrade() },
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
                                    draftStore = composerDraftStore,
                                    initialTimeline = CurrentTimeline.Favorites,
                                    mode = TimelineMode.ReadOnlySystemCollection(title = "Favorites"),
                                    selectedMomentId = favorites.selectedMomentId,
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
                                    draftStore = composerDraftStore,
                                    initialTimeline = CurrentTimeline.OnThisDay(onThisDay.date),
                                    mode = TimelineMode.ReadOnlySystemCollection(title = "On This Day"),
                                    selectedMomentId = onThisDay.selectedMomentId,
                                    onBackToTimelineHome = { rediscoverDestination = RediscoverDestination.Root },
                                    behaviorPreferences = behaviorState.preferences,
                            )
                        }
                        RediscoverDestination.AllPhotos -> {
                            TimelineScreen(
                                    momentRepository = container.momentRepository,
                                    timelineRepository = container.timelineRepository,
                                    appearanceRepository = container.appearanceRepository,
                                    rediscoverRepository = container.rediscoverRepository,
                                    clock = container.clock,
                                    idGenerator = container.idGenerator,
                                    mediaStore = container.mediaStore,
                                    mediaProcessor = container.mediaProcessor,
                                    draftStore = composerDraftStore,
                                    initialTimeline = CurrentTimeline.AllPhotos,
                                    mode = TimelineMode.ReadOnlySystemCollection(title = "All Photos"),
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
                                    draftStore = composerDraftStore,
                                    initialTimeline = CurrentTimeline.FromYourPast(fromYourPast.query),
                                    mode = TimelineMode.ReadOnlySystemCollection(title = "From Your Past"),
                                    selectedMomentId = fromYourPast.selectedMomentId,
                                    onBackToTimelineHome = { rediscoverDestination = RediscoverDestination.Root },
                                    behaviorPreferences = behaviorState.preferences,
                            )
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
                    topLevel != ReliveTopLevelDestination.Home ||
                        rediscoverDestination is RediscoverDestination.Root
                ) ReliveFloatingBottomControls(
                    selected = topLevel,
                    expanded = navigationToolbarExpanded,
                    modifier = Modifier.align(Alignment.BottomCenter),
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

@Composable
private fun ReliveLockSurface(onUnlock: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush()).clickable(onClick = onUnlock),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text("Relive is locked", style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
            androidx.compose.material3.Text("Tap to unlock", style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
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
    ProfileDestination.Upgrade,
    ProfileDestination.Location,
    ProfileDestination.RediscoverNotifications,
    ProfileDestination.PrivacySecurity,
    ProfileDestination.HelpFeedback,
    ProfileDestination.AboutRelive,
    -> 2
}
