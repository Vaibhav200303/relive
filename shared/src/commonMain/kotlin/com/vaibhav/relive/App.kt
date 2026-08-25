package com.vaibhav.relive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.vaibhav.relive.ui.screens.RediscoverScreen
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.toReliveThemeId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.model.StartDestination
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.presentation.timeline.TimelineMode
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeViewModel
import com.vaibhav.relive.presentation.profile.ProfileViewModel
import com.vaibhav.relive.presentation.profile.ProfileNavigationState
import com.vaibhav.relive.presentation.profile.ProfileDestination
import com.vaibhav.relive.presentation.profile.MediaStorageViewModel
import com.vaibhav.relive.ui.components.navigation.ReliveFloatingBottomControls
import com.vaibhav.relive.ui.components.navigation.ReliveTopLevelDestination
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
import com.vaibhav.relive.presentation.search.SearchViewModel
import com.vaibhav.relive.presentation.navigation.QuickCaptureSurface
import com.vaibhav.relive.presentation.navigation.quickCaptureCommand
import com.vaibhav.relive.presentation.navigation.resolveStartupDestination
import com.vaibhav.relive.presentation.composer.TimelineComposerDraftStore
import com.vaibhav.relive.presentation.settings.AppearanceViewModel
import com.vaibhav.relive.presentation.settings.BehaviorPreferencesViewModel
import com.vaibhav.relive.presentation.settings.resolveDarkMode
import com.vaibhav.relive.presentation.settings.resolveTimelineTheme
import com.vaibhav.relive.presentation.profile.AppLockController
import com.vaibhav.relive.presentation.profile.RediscoverReminderController
import com.vaibhav.relive.platform.system.openAppSettings
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
    ) : TimelinesDestination
}

private sealed interface RediscoverDestination {
    data object Root : RediscoverDestination
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
        val composerDraftStore = remember { TimelineComposerDraftStore() }
        val homeViewModel = remember(container, scope) {
            TimelineHomeViewModel(
                homeRepository = container.timelineHomeRepository,
                timelineRepository = container.timelineRepository,
                clock = container.clock,
                idGenerator = container.idGenerator,
                scope = scope,
            )
        }
        val homeListState = rememberLazyListState()
        val customTimelines by remember(container) { container.timelineRepository.observeCustom() }
            .collectAsState(emptyList())
        val rediscoverListState = rememberLazyListState()
        val searchListState = rememberLazyListState()
        val searchViewModel = remember(container, scope) { SearchViewModel(container.momentRepository, scope) }
        val profileViewModel = remember(container, scope) { ProfileViewModel(container.profileRepository, container.profileSettingsRepository, container.mediaStore, scope) }
        val profileSettings by container.profileSettingsRepository.settings.collectAsState()
        val lockController = remember(container) { AppLockController(container.profileSettingsRepository, container.deviceAuthentication) { container.clock.now().epochMilliseconds } }
        val locked by lockController.locked.collectAsState()
        val reminderController = remember(container) { RediscoverReminderController(container.profileSettingsRepository, container.rediscoverReminderService) }
        val reminderArchiveFingerprint by remember(container) {
            container.momentRepository.observeAll().map { moments -> moments.fold(1) { value, moment -> 31 * value + moment.createdAt.epochMilliseconds.hashCode() } }
        }.collectAsState(0)
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
        var topLevel by remember(container.behaviorPreferencesRepository) {
            mutableStateOf(
                resolveStartupDestination(behaviorState.preferences.startDestination).toTopLevelDestination(),
            )
        }
        var searchReturnDestination by remember { mutableStateOf(ReliveTopLevelDestination.Timelines) }
        var timelinesDestination by remember { mutableStateOf<TimelinesDestination>(TimelinesDestination.TimelineHome) }
        var rediscoverDestination by remember { mutableStateOf<RediscoverDestination>(RediscoverDestination.Root) }
        var profileNavigation by remember { mutableStateOf(ProfileNavigationState()) }
        var navigationToolbarExpanded by remember { mutableStateOf(true) }
        val openQuickCapture: (QuickCaptureSurface) -> Unit = { surface ->
            quickCaptureCommand(surface)?.let { command ->
                ActivePlayback.stopActive()
                timelinesDestination = TimelinesDestination.TimelineDetail(
                    scope = command.timeline,
                    openComposerOnEnter = command.openComposer,
                )
            }
        }
        val mediaStorageViewModel = remember(container, scope) {
            MediaStorageViewModel(container.archiveInsightsRepository, scope)
        }
        val backupRestoreViewModel = remember(container, scope) {
            BackupRestoreViewModel(container.backupPreferencesRepository, container.googleDriveAccountManager, container.backupCoordinator, scope)
        }
        if (locked) ReliveLockSurface(onUnlock = { scope.launch { lockController.unlock() } }) else when (profileNavigation.destination) {
            ProfileDestination.Profile -> ProfileScreen(
                viewModel = profileViewModel,
                appearanceViewModel = appearanceViewModel,
                onBack = { profileNavigation = profileNavigation.returnToTimelineHome() },
                onOpenPreferences = { profileNavigation = profileNavigation.openPreferences() },
                onOpenMediaStorage = { profileNavigation = profileNavigation.openMediaStorage() },
                onOpenBackupRestore = { profileNavigation = profileNavigation.openBackupRestore() },
                onOpenLocation = { profileNavigation = profileNavigation.openLocation() },
                onOpenNotifications = { profileNavigation = profileNavigation.openNotifications() },
                onOpenPrivacy = { profileNavigation = profileNavigation.openPrivacy() },
                onOpenHelp = { profileNavigation = profileNavigation.openHelp() },
                onOpenAbout = { profileNavigation = profileNavigation.openAbout() },
                mediaStore = container.mediaStore,
                mediaProcessor = container.mediaProcessor,
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
            ProfileDestination.Closed -> when (val active = timelinesDestination) {
            is TimelinesDestination.TimelineDetail -> {
                val timelineContent: @Composable () -> Unit = {
                    TimelineScreen(
                        momentRepository = container.momentRepository,
                        timelineRepository = container.timelineRepository,
                        rediscoverRepository = container.rediscoverRepository,
                        clock = container.clock,
                        idGenerator = container.idGenerator,
                        mediaStore = container.mediaStore,
                        mediaProcessor = container.mediaProcessor,
                        draftStore = composerDraftStore,
                        initialTimeline = active.scope,
                        selectedMomentId = active.selectedMomentId,
                        openComposerOnEnter = active.openComposerOnEnter,
                        onComposerOpenIntentConsumed = {
                            timelinesDestination = active.copy(openComposerOnEnter = false)
                        },
                        onBackToTimelineHome = {
                            timelinesDestination = TimelinesDestination.TimelineHome
                        },
                        globalTheme = appearanceState.preferences.defaultTheme,
                        behaviorPreferences = behaviorState.preferences,
                    )
                }
                val override = (active.scope as? CurrentTimeline.Custom)?.let { current ->
                    customTimelines.firstOrNull { it.id == current.id }?.theme
                }
                ReliveTheme(
                    themeId = resolveTimelineTheme(
                        override = override,
                        global = appearanceState.preferences.defaultTheme,
                    ).toReliveThemeId(),
                    darkMode = darkMode,
                    content = timelineContent,
                )
            }
            TimelinesDestination.TimelineHome -> if (
                topLevel == ReliveTopLevelDestination.Rediscover && rediscoverDestination is RediscoverDestination.Favorites
            ) {
                val favorites = rediscoverDestination as RediscoverDestination.Favorites
                TimelineScreen(
                    momentRepository = container.momentRepository,
                    timelineRepository = container.timelineRepository,
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
            } else if (
                topLevel == ReliveTopLevelDestination.Rediscover && rediscoverDestination is RediscoverDestination.OnThisDay
            ) {
                val onThisDay = rediscoverDestination as RediscoverDestination.OnThisDay
                TimelineScreen(
                    momentRepository = container.momentRepository,
                    timelineRepository = container.timelineRepository,
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
            } else if (
                topLevel == ReliveTopLevelDestination.Rediscover && rediscoverDestination is RediscoverDestination.FromYourPast
            ) {
                val fromYourPast = rediscoverDestination as RediscoverDestination.FromYourPast
                TimelineScreen(
                    momentRepository = container.momentRepository,
                    timelineRepository = container.timelineRepository,
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
            } else Box(Modifier.fillMaxSize()) {
                when (topLevel) {
                        ReliveTopLevelDestination.Timelines -> TimelineHomeScreen(
                            viewModel = homeViewModel,
                            mediaStore = container.mediaStore,
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
                        )
                        ReliveTopLevelDestination.Rediscover -> RediscoverScreen(
                            repository = container.rediscoverRepository,
                            timelineHomeRepository = container.timelineHomeRepository,
                            clock = container.clock,
                            mediaStore = container.mediaStore,
                            listState = rediscoverListState,
                            onOpenAll = {
                                timelinesDestination = TimelinesDestination.TimelineDetail(CurrentTimeline.All)
                            },
                            onOpenFavorites = { selectedMomentId ->
                                rediscoverDestination = RediscoverDestination.Favorites(selectedMomentId)
                            },
                            onOpenOnThisDay = { selectedMomentId, date ->
                                rediscoverDestination = RediscoverDestination.OnThisDay(selectedMomentId, date)
                            },
                            onOpenFromYourPast = { selectedMomentId, query ->
                                rediscoverDestination = RediscoverDestination.FromYourPast(selectedMomentId, query)
                            },
                            behaviorPreferences = behaviorState.preferences,
                            debugControls = rediscoverDebugControls,
                            onCreateMoment = { openQuickCapture(QuickCaptureSurface.Rediscover) },
                            navigationToolbarExpanded = navigationToolbarExpanded,
                            onNavigationToolbarExpand = { navigationToolbarExpanded = true },
                            onNavigationToolbarCollapse = { navigationToolbarExpanded = false },
                        )
                        ReliveTopLevelDestination.Search -> SearchScreen(
                            viewModel = searchViewModel,
                            mediaStore = container.mediaStore,
                            listState = searchListState,
                            clock = container.clock,
                            onBack = { topLevel = searchReturnDestination },
                            onOpenAllAtMoment = { momentId ->
                                timelinesDestination = TimelinesDestination.TimelineDetail(CurrentTimeline.All, momentId)
                                topLevel = ReliveTopLevelDestination.Timelines
                            },
                            onCreateMoment = { openQuickCapture(QuickCaptureSurface.Search) },
                            navigationToolbarExpanded = navigationToolbarExpanded,
                            onNavigationToolbarExpand = { navigationToolbarExpanded = true },
                            onNavigationToolbarCollapse = { navigationToolbarExpanded = false },
                        )
                    }
                ReliveFloatingBottomControls(
                    selected = topLevel,
                    expanded = navigationToolbarExpanded,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onSelect = {
                        ActivePlayback.stopActive()
                        if (it == ReliveTopLevelDestination.Search && topLevel != ReliveTopLevelDestination.Search) {
                            searchReturnDestination = topLevel
                        }
                        topLevel = it
                        navigationToolbarExpanded = true
                    },
                    onCreateMoment = {
                        openQuickCapture(
                            when (topLevel) {
                                ReliveTopLevelDestination.Timelines -> QuickCaptureSurface.TimelineHome
                                ReliveTopLevelDestination.Rediscover -> QuickCaptureSurface.Rediscover
                                ReliveTopLevelDestination.Search -> QuickCaptureSurface.Search
                            },
                        )
                    },
                )
            }
        }
        }
        }
    }

@Composable
private fun ReliveLockSurface(onUnlock: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas).clickable(onClick = onUnlock),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text("Relive is locked", style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
            androidx.compose.material3.Text("Tap to unlock", style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
        }
    }
}

private fun StartDestination.toTopLevelDestination(): ReliveTopLevelDestination = when (this) {
    StartDestination.Timelines -> ReliveTopLevelDestination.Timelines
    StartDestination.Rediscover -> ReliveTopLevelDestination.Rediscover
}
