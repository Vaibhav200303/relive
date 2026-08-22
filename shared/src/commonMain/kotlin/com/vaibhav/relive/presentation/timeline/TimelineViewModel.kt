package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
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
) {

    private val _state = MutableStateFlow(TimelineScreenState())
    val state: StateFlow<TimelineScreenState> = _state.asStateFlow()

    private var momentsJob: Job? = null

    init {
        scope.launch {
            timelineRepository.observeCustom().collect { timelines ->
                _state.update { it.copy(customTimelines = timelines) }
            }
        }
        observe(CurrentTimeline.All)
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

    fun showTimelineCreation() {
        _state.update {
            it.copy(creation = TimelineCreationState(isVisible = true))
        }
    }

    fun dismissTimelineCreation() {
        if (_state.value.creation.isSaving) return
        _state.update { it.copy(creation = TimelineCreationState()) }
    }

    fun updateTimelineName(value: String) {
        _state.update {
            it.copy(
                creation = it.creation.copy(
                    name = value,
                    errorMessage = null,
                ),
            )
        }
    }

    fun createTimeline() {
        val creation = _state.value.creation
        if (creation.isSaving) return
        val trimmed = creation.name.trim()
        val validationError = when {
            trimmed.isEmpty() -> "Enter a timeline name."
            trimmed.length > Timeline.Custom.MAX_NAME_LENGTH ->
                "Use ${Timeline.Custom.MAX_NAME_LENGTH} characters or fewer."
            else -> null
        }
        if (validationError != null) {
            _state.update {
                it.copy(creation = it.creation.copy(errorMessage = validationError))
            }
            return
        }

        _state.update { it.copy(creation = it.creation.copy(isSaving = true)) }
        scope.launch {
            try {
                timelineRepository.createCustom(
                    timeline = Timeline.Custom(
                        id = TimelineId(idGenerator.newId()),
                        name = trimmed,
                    ),
                    createdAt = clock.now(),
                )
                _state.update { it.copy(creation = TimelineCreationState()) }
            } catch (_: Throwable) {
                _state.update {
                    it.copy(
                        creation = it.creation.copy(
                            isSaving = false,
                            errorMessage = "Couldn't create this timeline. Try again.",
                        ),
                    )
                }
            }
        }
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
