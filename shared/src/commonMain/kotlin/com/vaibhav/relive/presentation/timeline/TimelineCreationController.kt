package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared Create Timeline flow used by both Timeline Home and Timeline detail. */
class TimelineCreationController(
    private val timelineRepository: TimelineRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TimelineCreationState())
    val state: StateFlow<TimelineCreationState> = _state.asStateFlow()
    private val _createdTimelines = MutableSharedFlow<Timeline.Custom>(extraBufferCapacity = 1)
    val createdTimelines: SharedFlow<Timeline.Custom> = _createdTimelines.asSharedFlow()
    private val _outcomes = MutableSharedFlow<TimelineCreationOutcome>(extraBufferCapacity = 1)
    val outcomes: SharedFlow<TimelineCreationOutcome> = _outcomes.asSharedFlow()

    fun show() {
        _state.update { TimelineCreationState(isVisible = true) }
    }

    fun dismiss() {
        if (_state.value.isSaving) return
        _state.update { TimelineCreationState() }
    }

    fun updateName(value: String) {
        _state.update { it.copy(name = value, errorMessage = null) }
    }

    fun create() {
        val creation = _state.value
        if (creation.isSaving) return
        val trimmed = creation.name.trim()
        val error = when {
            trimmed.isEmpty() -> "Enter a timeline name."
            trimmed.length > Timeline.Custom.MAX_NAME_LENGTH ->
                "Use ${Timeline.Custom.MAX_NAME_LENGTH} characters or fewer."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            _outcomes.tryEmit(TimelineCreationOutcome.Rejected)
            return
        }
        _state.update { it.copy(isSaving = true) }
        scope.launch {
            try {
                val timeline = Timeline.Custom(TimelineId(idGenerator.newId()), trimmed)
                timelineRepository.createCustom(
                    timeline = timeline,
                    createdAt = clock.now(),
                )
                _state.value = TimelineCreationState()
                _outcomes.emit(TimelineCreationOutcome.Succeeded)
                _createdTimelines.emit(timeline)
            } catch (_: Throwable) {
                _state.update {
                    it.copy(isSaving = false, errorMessage = "Couldn't create this timeline. Try again.")
                }
                _outcomes.emit(TimelineCreationOutcome.Rejected)
            }
        }
    }
}

enum class TimelineCreationOutcome {
    Succeeded,
    Rejected,
}
