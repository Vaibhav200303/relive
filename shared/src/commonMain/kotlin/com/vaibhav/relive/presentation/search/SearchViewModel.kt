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

enum class SearchFilter { All, Tags, Places }

data class SearchState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.All,
    val results: List<MomentPresentation> = emptyList(),
    val activeIndex: Int? = null,
    val dateNavigation: SearchDateNavigation? = null,
    val recentSearches: List<String> = emptyList(),
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
            searchFlow(query, _state.value.filter).collect { moments ->
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

    fun selectFilter(filter: SearchFilter) {
        if (_state.value.filter == filter) return
        searchJob?.cancel()
        _state.update { it.copy(filter = filter, results = emptyList(), activeIndex = null) }
        updateQuery(_state.value.query)
    }

    fun submitQuery(query: String = _state.value.query) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        if (normalized != _state.value.query) updateQuery(normalized)
        _state.update { current ->
            current.copy(
                recentSearches = listOf(normalized) + current.recentSearches
                    .filterNot { it.equals(normalized, ignoreCase = true) }
                    .take(MaxRecentSearches - 1),
            )
        }
    }

    fun useSuggestion(query: String) {
        updateQuery(query)
        submitQuery(query)
    }

    fun removeRecentSearch(query: String) {
        _state.update { current ->
            current.copy(recentSearches = current.recentSearches.filterNot { it == query })
        }
    }

    fun clearRecentSearches() {
        _state.update { it.copy(recentSearches = emptyList()) }
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

    private fun searchFlow(query: String, filter: SearchFilter) = when (filter) {
        SearchFilter.All -> momentRepository.observeSearch(query)
        SearchFilter.Tags -> momentRepository.observeSearchByTag(query)
        SearchFilter.Places -> momentRepository.observeSearchByPlace(query)
    }
}

const val SearchDebounceMillis = 150L
private const val MaxRecentSearches = 5
