package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.repository.TimelineHomeRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.presentation.timeline.TimelineCreationController
import com.vaibhav.relive.presentation.timeline.TimelineCreationState
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.platform.media.MediaStore
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
    private val timelineRepository: TimelineRepository,
    clock: Clock,
    idGenerator: IdGenerator,
    private val scope: CoroutineScope,
    mediaStore: MediaStore? = null,
) {
    private val _state = MutableStateFlow(TimelineHomeState())
    val state: StateFlow<TimelineHomeState> = _state.asStateFlow()

    private val _navigation = MutableSharedFlow<TimelineHomeNavigation>(extraBufferCapacity = 1)
    val navigation: SharedFlow<TimelineHomeNavigation> = _navigation.asSharedFlow()

    private val creation = TimelineCreationController(timelineRepository, clock, idGenerator, scope, mediaStore)
    val creationState: StateFlow<TimelineCreationState> = creation.state
    val creationOutcomes = creation.outcomes

    init {
        scope.launch {
            homeRepository.observeSummaries().collect { summaries ->
                _state.update { it.copy(content = TimelineHomeContent.Loaded(summaries)) }
            }
        }
        scope.launch {
            creation.createdTimelines.collect { timeline ->
                _navigation.emit(TimelineHomeNavigation(timeline = timeline, openComposerOnEnter = true))
            }
        }
    }

    fun selectTimeline(timeline: Timeline) {
        _navigation.tryEmit(TimelineHomeNavigation(timeline = timeline))
    }

    fun updateSearchQuery(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun renameTimeline(timeline: Timeline.Custom, newName: String, onResult: (Boolean) -> Unit) {
        scope.launch {
            onResult(runCatching { timelineRepository.rename(timeline.id, newName) }.isSuccess)
        }
    }

    fun deleteTimeline(timeline: Timeline.Custom, onResult: (Boolean) -> Unit) {
        scope.launch {
            onResult(runCatching { timelineRepository.deleteCustom(timeline.id) }.isSuccess)
        }
    }

    fun deleteTimelines(timelines: Collection<Timeline.Custom>, onResult: (Boolean) -> Unit) {
        scope.launch {
            onResult(runCatching { timelines.forEach { timelineRepository.deleteCustom(it.id) } }.isSuccess)
        }
    }

    fun showTimelineCreation() = creation.show()
    fun dismissTimelineCreation() = creation.dismiss()
    fun updateTimelineName(value: String) = creation.updateName(value)
    fun setTimelineCoverPhoto(value: MediaStorageRef?) = creation.setCoverPhoto(value)
    fun setTimelineCoverProcessing(value: Boolean) = creation.setCoverProcessing(value)
    fun createTimeline() = creation.create()
}
