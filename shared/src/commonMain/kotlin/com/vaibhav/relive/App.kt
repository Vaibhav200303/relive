package com.vaibhav.relive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.vaibhav.relive.di.ReliveAppContainer
import com.vaibhav.relive.ui.screens.TimelineScreen
import com.vaibhav.relive.ui.screens.TimelineHomeScreen
import com.vaibhav.relive.ui.screens.RediscoverScreen
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.ReliveThemeId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.presentation.timeline.TimelineMode
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeViewModel
import com.vaibhav.relive.ui.components.navigation.ReliveBottomBar
import com.vaibhav.relive.ui.components.navigation.ReliveTopLevelDestination
import com.vaibhav.relive.platform.media.ActivePlayback

private sealed interface TimelinesDestination {
    data object TimelineHome : TimelinesDestination
    data class TimelineDetail(val scope: CurrentTimeline) : TimelinesDestination
}

private sealed interface RediscoverDestination {
    data object Root : RediscoverDestination
    data class Favorites(val selectedMomentId: MomentId?) : RediscoverDestination
}

@Composable
@Preview
fun App(
    container: ReliveAppContainer,
    rediscoverDebugControls: (@Composable () -> Unit)? = null,
) {
    ReliveTheme(themeId = ReliveThemeId.WarmJournal) {
        val scope = rememberCoroutineScope()
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
        val rediscoverListState = rememberLazyListState()
        var topLevel by remember { mutableStateOf(ReliveTopLevelDestination.Timelines) }
        var timelinesDestination by remember { mutableStateOf<TimelinesDestination>(TimelinesDestination.TimelineHome) }
        var rediscoverDestination by remember { mutableStateOf<RediscoverDestination>(RediscoverDestination.Root) }
        when (val active = timelinesDestination) {
            is TimelinesDestination.TimelineDetail -> TimelineScreen(
                momentRepository = container.momentRepository,
                timelineRepository = container.timelineRepository,
                rediscoverRepository = container.rediscoverRepository,
                clock = container.clock,
                idGenerator = container.idGenerator,
                mediaStore = container.mediaStore,
                mediaProcessor = container.mediaProcessor,
                initialTimeline = active.scope,
                onBackToTimelineHome = { timelinesDestination = TimelinesDestination.TimelineHome },
            )
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
                    initialTimeline = CurrentTimeline.Favorites,
                    mode = TimelineMode.ReadOnlySystemCollection(title = "Favorites"),
                    selectedMomentId = favorites.selectedMomentId,
                    onBackToTimelineHome = { rediscoverDestination = RediscoverDestination.Root },
                )
            } else Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (topLevel) {
                        ReliveTopLevelDestination.Timelines -> TimelineHomeScreen(
                            viewModel = homeViewModel,
                            mediaStore = container.mediaStore,
                            listState = homeListState,
                            onOpenTimeline = { timeline ->
                                timelinesDestination = TimelinesDestination.TimelineDetail(
                                    when (timeline) {
                                        Timeline.All -> CurrentTimeline.All
                                        is Timeline.Custom -> CurrentTimeline.Custom(timeline.id)
                                    },
                                )
                            },
                        )
                        ReliveTopLevelDestination.Rediscover -> RediscoverScreen(
                            repository = container.rediscoverRepository,
                            mediaStore = container.mediaStore,
                            listState = rediscoverListState,
                            onOpenFavorites = { selectedMomentId ->
                                rediscoverDestination = RediscoverDestination.Favorites(selectedMomentId)
                            },
                            debugControls = rediscoverDebugControls,
                        )
                    }
                }
                ReliveBottomBar(selected = topLevel, onSelect = {
                    ActivePlayback.stopActive()
                    topLevel = it
                })
            }
        }
    }
}
