package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.policy.EditWindow
import com.vaibhav.relive.domain.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TimelineViewModel(
    private val momentRepository: MomentRepository,
    private val timelineRepository: TimelineRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val scope: CoroutineScope,
    initialTimeline: CurrentTimeline = CurrentTimeline.All,
) {

    private val _state = MutableStateFlow(TimelineScreenState(currentTimeline = initialTimeline))
    val state: StateFlow<TimelineScreenState> = _state.asStateFlow()

    private var momentsJob: Job? = null
    private val creationController = TimelineCreationController(
        timelineRepository = timelineRepository,
        clock = clock,
        idGenerator = idGenerator,
        scope = scope,
    )

    init {
        scope.launch {
            creationController.state.collect { creation ->
                _state.update { it.copy(creation = creation) }
            }
        }
        scope.launch {
            timelineRepository.observeCustom().collect { timelines ->
                _state.update { it.copy(customTimelines = timelines) }
            }
        }
        observe(initialTimeline)
    }

    fun selectTimeline(timeline: CurrentTimeline) {
        if (timeline == _state.value.currentTimeline) return
        _state.update {
            it.copy(currentTimeline = timeline, moments = TimelineMomentsState.Loading)
        }
        observe(timeline)
    }

    fun setFavorite(id: MomentId, isFavorite: Boolean) {
        scope.launch { momentRepository.setFavorite(id, isFavorite) }
    }

    fun canEditOrForget(moment: Moment): Boolean = EditWindow.isEditable(moment, clock)

    /** Checks the policy again at the destructive boundary before touching persistence. */
    fun forget(moment: Moment, onDeleted: (Moment) -> Unit, onFailure: () -> Unit) {
        if (!EditWindow.isForgettable(moment, clock)) {
            onFailure()
            return
        }
        scope.launch {
            try {
                momentRepository.delete(moment.id)
                onDeleted(moment)
            } catch (_: Throwable) {
                onFailure()
            }
        }
    }

    fun showTimelineCreation() {
        creationController.show()
    }

    fun dismissTimelineCreation() {
        creationController.dismiss()
    }

    fun updateTimelineName(value: String) {
        creationController.updateName(value)
    }

    fun createTimeline() {
        creationController.create()
    }

    private fun observe(timeline: CurrentTimeline) {
        momentsJob?.cancel()
        momentsJob = scope.launch {
            timeline.momentsFlow().collect { moments ->
                val presentation = moments.asReversed().map { it.toPresentation() }
                _state.update { current ->
                    if (current.currentTimeline != timeline) {
                        current
                    } else {
                        current.copy(
                            moments = if (presentation.isEmpty()) {
                                TimelineMomentsState.Empty
                            } else {
                                TimelineMomentsState.Loaded(presentation)
                            },
                        )
                    }
                }
            }
        }
    }

    private fun CurrentTimeline.momentsFlow(): Flow<List<Moment>> = when (this) {
        CurrentTimeline.All -> momentRepository.observeAll()
        is CurrentTimeline.Custom -> momentRepository.observeInTimeline(id)
    }
}
