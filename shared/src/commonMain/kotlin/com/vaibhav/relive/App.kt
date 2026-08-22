package com.vaibhav.relive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.tooling.preview.Preview
import com.vaibhav.relive.di.ReliveAppContainer
import com.vaibhav.relive.ui.screens.TimelineScreen
import com.vaibhav.relive.ui.screens.TimelineHomeScreen
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.ReliveThemeId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.presentation.timelinehome.TimelineHomeViewModel

private sealed interface ReliveDestination {
    data object TimelineHome : ReliveDestination
    data class TimelineDetail(val scope: CurrentTimeline) : ReliveDestination
}

@Composable
@Preview
fun App(container: ReliveAppContainer) {
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
        var destination by remember { mutableStateOf<ReliveDestination>(ReliveDestination.TimelineHome) }
        when (val active = destination) {
            ReliveDestination.TimelineHome -> TimelineHomeScreen(
                viewModel = homeViewModel,
                mediaStore = container.mediaStore,
                listState = homeListState,
                onOpenTimeline = { timeline ->
                    destination = ReliveDestination.TimelineDetail(
                        when (timeline) {
                            Timeline.All -> CurrentTimeline.All
                            is Timeline.Custom -> CurrentTimeline.Custom(timeline.id)
                        },
                    )
                },
            )
            is ReliveDestination.TimelineDetail -> TimelineScreen(
                momentRepository = container.momentRepository,
                timelineRepository = container.timelineRepository,
                clock = container.clock,
                idGenerator = container.idGenerator,
                mediaStore = container.mediaStore,
                mediaProcessor = container.mediaProcessor,
                initialTimeline = active.scope,
                onBackToTimelineHome = { destination = ReliveDestination.TimelineHome },
            )
        }
    }
}
