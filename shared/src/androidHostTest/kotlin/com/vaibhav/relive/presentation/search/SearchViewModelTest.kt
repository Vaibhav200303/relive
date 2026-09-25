package com.vaibhav.relive.presentation.search

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.SearchSuggestion
import com.vaibhav.relive.domain.repository.SearchSuggestionScope
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test fun firstResultIsActiveAndNavigationStopsAtBounds() = runTest {
        val repository = SearchFakeRepository(
            listOf(moment("new", 2), moment("old", 1)),
        )
        val viewModel = SearchViewModel(repository, TestScope(UnconfinedTestDispatcher(testScheduler)))

        viewModel.updateQuery("any")
        advanceTimeBy(SearchDebounceMillis)
        runCurrent()
        assertEquals(listOf("old", "new"), viewModel.state.value.results.map { it.id.value })
        assertEquals(0, viewModel.state.value.activeIndex)

        viewModel.selectPrevious()
        assertEquals(0, viewModel.state.value.activeIndex)
        viewModel.selectNext()
        assertEquals(1, viewModel.state.value.activeIndex)
        viewModel.selectNext()
        assertEquals(1, viewModel.state.value.activeIndex)
        viewModel.selectPrevious()
        assertEquals(MomentId("old"), viewModel.state.value.activeMomentId)
    }

    @Test fun queryChangeAndClearResetActiveState() = runTest {
        val viewModel = SearchViewModel(
            SearchFakeRepository(listOf(moment("one", 1))),
            TestScope(UnconfinedTestDispatcher(testScheduler)),
        )

        viewModel.updateQuery("one")
        advanceTimeBy(SearchDebounceMillis)
        runCurrent()
        assertEquals(0, viewModel.state.value.activeIndex)
        viewModel.updateQuery("two")
        assertEquals(emptyList(), viewModel.state.value.results)
        assertNull(viewModel.state.value.activeIndex)
        viewModel.clear()
        assertEquals("", viewModel.state.value.query)
        assertNull(viewModel.state.value.activeIndex)
    }

    @Test fun filterChangeUsesItsSqlBackedRepositoryPath() = runTest {
        val repository = SearchFakeRepository(listOf(moment("all", 1)))
        val viewModel = SearchViewModel(repository, TestScope(UnconfinedTestDispatcher(testScheduler)))

        viewModel.updateQuery("trip")
        viewModel.selectFilter(SearchFilter.Tags)
        advanceTimeBy(SearchDebounceMillis)
        runCurrent()
        assertEquals(SearchFilter.Tags, viewModel.state.value.filter)
        assertEquals("trip", repository.lastTagQuery)

        viewModel.selectFilter(SearchFilter.Places)
        advanceTimeBy(SearchDebounceMillis)
        runCurrent()
        assertEquals("trip", repository.lastPlaceQuery)
    }

    @Test fun recentSearchesAreDeduplicatedBoundedAndRemovable() = runTest {
        val viewModel = SearchViewModel(
            SearchFakeRepository(emptyList()),
            TestScope(UnconfinedTestDispatcher(testScheduler)),
        )

        listOf("one", "two", "three", "four", "five", "six", "TWO")
            .map { SearchSuggestion(it, SearchSuggestionScope.Tag) }
            .forEach(viewModel::useSuggestion)
        assertEquals(listOf("TWO", "six", "five", "four", "three"), viewModel.state.value.recentSearches)
        viewModel.removeRecentSearch("five")
        assertEquals(listOf("TWO", "six", "four", "three"), viewModel.state.value.recentSearches)
        viewModel.clearRecentSearches()
        assertEquals(emptyList(), viewModel.state.value.recentSearches)
    }

    @Test fun matureArchiveUsesDynamicSuggestionsAndTheirMatchingFilter() = runTest {
        val repository = SearchFakeRepository(
            List(10) { index -> moment("moment-$index", index.toLong()) },
            suggestions = listOf(SearchSuggestion("Weekend", SearchSuggestionScope.Tag)),
        )
        val viewModel = SearchViewModel(repository, TestScope(UnconfinedTestDispatcher(testScheduler)))
        runCurrent()

        assertEquals(listOf("Weekend"), viewModel.state.value.suggestions.map { it.query })
        viewModel.useSuggestion(viewModel.state.value.suggestions.single())
        assertEquals(SearchFilter.Tags, viewModel.state.value.filter)
        assertEquals("Weekend", viewModel.state.value.query)
    }
}

private class SearchFakeRepository(
    initial: List<Moment>,
    private val suggestions: List<SearchSuggestion> = emptyList(),
) : MomentRepository {
    private val moments = MutableStateFlow(initial)
    var lastTagQuery: String? = null
    var lastPlaceQuery: String? = null
    override suspend fun insert(moment: Moment, timelineIds: Set<TimelineId>) = Unit
    override suspend fun findById(id: MomentId): Moment? = null
    override suspend fun updateEditable(moment: Moment) = Unit
    override suspend fun setFavorite(id: MomentId, isFavorite: Boolean) = Unit
    override suspend fun setFeeling(
        id: MomentId,
        feeling: com.vaibhav.relive.domain.model.MomentFeeling?,
    ) = Unit
    override suspend fun delete(id: MomentId) = Unit
    override suspend fun listAll(): List<Moment> = moments.value
    override fun observeAll(): Flow<List<Moment>> = moments.asStateFlow()
    override fun observeSearch(query: String): Flow<List<Moment>> = moments.asStateFlow()
    override fun observeSearchByTag(query: String): Flow<List<Moment>> {
        lastTagQuery = query
        return moments.asStateFlow()
    }
    override fun observeSearchByPlace(query: String): Flow<List<Moment>> {
        lastPlaceQuery = query
        return moments.asStateFlow()
    }
    override fun observeSearchSuggestions(): Flow<List<SearchSuggestion>> = MutableStateFlow(suggestions)
    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> = emptyList()
    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> = MutableStateFlow(emptyList())
}

private fun moment(id: String, createdAt: Long) = Moment(id = MomentId(id), createdAt = Instant(createdAt), title = id)
