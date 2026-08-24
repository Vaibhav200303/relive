package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.MomentDateNavigationScope
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.policy.EditWindow
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.presentation.date.editorialDayMonth
import com.vaibhav.relive.domain.model.LocalCalendarDate
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
    private val rediscoverRepository: RediscoverRepository,
    private val clock: Clock,
    private val scope: CoroutineScope,
    initialTimeline: CurrentTimeline = CurrentTimeline.All,
    private val mode: TimelineMode = TimelineMode.Editable,
) {

    private val _state = MutableStateFlow(TimelineScreenState(currentTimeline = initialTimeline))
    val state: StateFlow<TimelineScreenState> = _state.asStateFlow()

    private var momentsJob: Job? = null
    init {
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
        if (!mode.allowsMutations) return
        scope.launch { momentRepository.setFavorite(id, isFavorite) }
    }

    fun updateCurrentTimelineTheme(
        theme: ThemeReference?,
        onResult: (Boolean) -> Unit,
    ) {
        val timeline = _state.value.currentTimeline as? CurrentTimeline.Custom ?: return
        scope.launch {
            onResult(runCatching { timelineRepository.updateTheme(timeline.id, theme) }.isSuccess)
        }
    }

    fun jumpToDate(date: LocalCalendarDate) {
        val targetScope = when (val timeline = _state.value.currentTimeline) {
            CurrentTimeline.All -> MomentDateNavigationScope.All
            is CurrentTimeline.Custom -> MomentDateNavigationScope.Custom(timeline.id)
            else -> return
        }
        scope.launch {
            val target = momentRepository.findDateNavigationTarget(
                scope = targetScope,
                dayStart = RediscoverCalendar.startOfDay(date),
                nextDayStart = RediscoverCalendar.nextDayStart(date),
            )
            val message = target?.let {
                val shownDate = RediscoverCalendar.localDate(it.createdAt)
                if (shownDate == date) null else "No moments on ${date.editorialDayMonth()} — showing ${shownDate.editorialDayMonth()}."
            } ?: "No moments in this timeline yet."
            _state.update { it.copy(dateNavigation = DateNavigationState(target?.id, message)) }
        }
    }

    fun consumeDateNavigation() {
        _state.update { it.copy(dateNavigation = null) }
    }

    fun canEditOrForget(moment: Moment): Boolean = EditWindow.isEditable(moment, clock)

    /** Checks the policy again at the destructive boundary before touching persistence. */
    fun forget(moment: Moment, onDeleted: (Moment) -> Unit, onFailure: () -> Unit) {
        if (!mode.allowsMutations) {
            onFailure()
            return
        }
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

    private fun observe(timeline: CurrentTimeline) {
        momentsJob?.cancel()
        momentsJob = scope.launch {
            timeline.momentsFlow().collect { moments ->
                val orderedMoments = if (timeline is CurrentTimeline.FromYourPast) moments else moments.asReversed()
                val presentation = orderedMoments.map { it.toPresentation() }
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
        CurrentTimeline.Favorites -> rediscoverRepository.observeFavoriteMoments()
        is CurrentTimeline.OnThisDay -> rediscoverRepository.observeOnThisDayMoments(
            today = date,
            startOfToday = RediscoverCalendar.startOfDay(date),
        )
        is CurrentTimeline.FromYourPast -> rediscoverRepository.observeFromYourPastMoments(query)
        is CurrentTimeline.Custom -> momentRepository.observeInTimeline(id)
    }
}
