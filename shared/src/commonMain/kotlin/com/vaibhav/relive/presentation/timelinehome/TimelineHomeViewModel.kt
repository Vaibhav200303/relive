package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.repository.TimelineHomeRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.presentation.timeline.TimelineCreationController
import com.vaibhav.relive.presentation.timeline.TimelineCreationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TimelineHomeViewModel(
    homeRepository: TimelineHomeRepository,
    timelineRepository: TimelineRepository,
    clock: Clock,
    idGenerator: IdGenerator,
    scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TimelineHomeState())
    val state: StateFlow<TimelineHomeState> = _state.asStateFlow()

    private val _navigation = MutableSharedFlow<Timeline>(extraBufferCapacity = 1)
    val navigation: SharedFlow<Timeline> = _navigation.asSharedFlow()

    private val creation = TimelineCreationController(timelineRepository, clock, idGenerator, scope)
    val creationState: StateFlow<TimelineCreationState> = creation.state

    init {
        scope.launch {
            homeRepository.observeSummaries().collect { summaries ->
                _state.update { it.copy(content = TimelineHomeContent.Loaded(summaries)) }
            }
        }
        scope.launch {
            creation.createdTimelines.collect { timeline -> _navigation.emit(timeline) }
        }
    }

    fun selectTimeline(timeline: Timeline) {
        _navigation.tryEmit(timeline)
    }

    fun showTimelineCreation() = creation.show()
    fun dismissTimelineCreation() = creation.dismiss()
    fun updateTimelineName(value: String) = creation.updateName(value)
    fun createTimeline() = creation.create()
}
