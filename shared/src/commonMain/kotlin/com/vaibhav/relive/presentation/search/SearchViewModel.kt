package com.vaibhav.relive.presentation.search

import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.MomentDateNavigationScope
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import com.vaibhav.relive.presentation.timeline.toPresentation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val results: List<MomentPresentation> = emptyList(),
    val activeIndex: Int? = null,
    val dateNavigation: SearchDateNavigation? = null,
) {
    val activeMomentId: MomentId? get() = activeIndex?.let(results::getOrNull)?.id
    val resultCount: Int get() = results.size
}

data class SearchDateNavigation(val momentId: MomentId?)

class SearchViewModel(
    private val momentRepository: MomentRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        searchJob?.cancel()
        _state.update { it.copy(query = query, results = emptyList(), activeIndex = null) }
        if (query.isBlank()) return
        searchJob = scope.launch {
            delay(SearchDebounceMillis)
            momentRepository.observeSearch(query).collect { moments ->
                // Repositories are newest-first; All Timeline is oldest-first.
                _state.update { current ->
                    if (current.query != query) current else current.copy(
                        results = moments.asReversed().map { it.toPresentation() },
                        activeIndex = if (moments.isEmpty()) null else 0,
                    )
                }
            }
        }
    }

    fun selectNext() = moveActiveBy(1)

    fun selectPrevious() = moveActiveBy(-1)

    fun clear() = updateQuery("")

    fun jumpToDate(date: LocalCalendarDate) {
        scope.launch {
            val target = momentRepository.findDateNavigationTarget(
                scope = MomentDateNavigationScope.All,
                dayStart = RediscoverCalendar.startOfDay(date),
                nextDayStart = RediscoverCalendar.nextDayStart(date),
            )
            _state.update { it.copy(dateNavigation = SearchDateNavigation(target?.id)) }
        }
    }

    fun consumeDateNavigation() {
        _state.update { it.copy(dateNavigation = null) }
    }

    private fun moveActiveBy(delta: Int) {
        _state.update { current ->
            val currentIndex = current.activeIndex ?: return@update current
            val next = (currentIndex + delta).coerceIn(0, current.results.lastIndex)
            current.copy(activeIndex = next)
        }
    }
}

const val SearchDebounceMillis = 150L
