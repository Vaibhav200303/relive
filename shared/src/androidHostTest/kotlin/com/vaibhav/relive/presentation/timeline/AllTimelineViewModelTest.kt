package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Instant
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AllTimelineViewModelTest {

    private var previousDefault: TimeZone? = null

    @BeforeTest
    fun setup() {
        previousDefault = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterTest
    fun teardown() {
        previousDefault?.let { TimeZone.setDefault(it) }
    }

    @Test
    fun emptyRepositoryYieldsEmptyState() = runTest {
        val repo = FakeMomentRepository()
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val vm = AllTimelineViewModel(repo, scope)
        assertEquals(AllTimelineUiState.Empty, vm.state.value)
    }

    @Test
    fun momentsRepositoryYieldsLoadedStateWithFormattedDate() = runTest {
        val moments = listOf(
            Moment(
                id = MomentId("m-a"),
                createdAt = Instant(1_695_913_200_000L),
                title = "Quiet morning light",
                content = "hi",
            ),
        )
        val repo = FakeMomentRepository(moments)
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val vm = AllTimelineViewModel(repo, scope)
        val state = assertIs<AllTimelineUiState.Loaded>(vm.state.value)
        assertEquals(1, state.moments.size)
        assertEquals("SEPTEMBER 28, 2023", state.moments.single().formattedDate)
    }

    @Test
    fun reversesNewestFirstRepositoryOrderingIntoOldestFirstPresentation() = runTest {
        // Repository contract is newest-first (see MomentRepository.observeAll);
        // the UI wants WhatsApp-style oldest-at-top so it can render composer
        // at the bottom. This test locks in the presentation-layer reversal
        // without asking the repository to change its contract.
        val newest = Moment(
            id = MomentId("m-new"),
            createdAt = Instant(3_000_000_000_000L),
            title = "newest",
        )
        val middle = Moment(
            id = MomentId("m-mid"),
            createdAt = Instant(2_000_000_000_000L),
            title = "middle",
        )
        val oldest = Moment(
            id = MomentId("m-old"),
            createdAt = Instant(1_000_000_000_000L),
            title = "oldest",
        )
        val repo = FakeMomentRepository(listOf(newest, middle, oldest))
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val vm = AllTimelineViewModel(repo, scope)

        // Repo still newest-first.
        assertEquals(
            listOf("m-new", "m-mid", "m-old"),
            repo.observeAll().first().map { it.id.value },
        )

        val state = assertIs<AllTimelineUiState.Loaded>(vm.state.value)
        assertEquals(
            listOf("m-old", "m-mid", "m-new"),
            state.moments.map { it.id.value },
        )
    }

    @Test
    fun newlySavedMomentBecomesLastPresentedEntry() = runTest {
        // After Keep Moment: repo emits newest-first, presentation places the
        // new moment last (just above the composer, which is a UI-layer slot).
        val existing = Moment(
            id = MomentId("m-existing"),
            createdAt = Instant(1_000_000_000_000L),
            title = "existing",
        )
        val repo = FakeMomentRepository(listOf(existing))
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val vm = AllTimelineViewModel(repo, scope)

        val fresh = Moment(
            id = MomentId("m-fresh"),
            createdAt = Instant(2_000_000_000_000L),
            title = "fresh",
        )
        repo.insert(fresh)

        val state = assertIs<AllTimelineUiState.Loaded>(vm.state.value)
        assertEquals(2, state.moments.size)
        assertEquals("m-existing", state.moments.first().id.value)
        assertEquals("m-fresh", state.moments.last().id.value)
        // Presentation never duplicates the emitted moment.
        assertEquals(state.moments.size, state.moments.map { it.id }.toSet().size)
    }

    @Test
    fun setFavoriteRoutesThroughRepository() = runTest {
        val moments = listOf(
            Moment(
                id = MomentId("m-b"),
                createdAt = Instant(1_695_913_200_000L),
                title = "t",
                isFavorite = false,
            ),
        )
        val repo = FakeMomentRepository(moments)
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val vm = AllTimelineViewModel(repo, scope)
        vm.setFavorite(MomentId("m-b"), true)
        val state = assertIs<AllTimelineUiState.Loaded>(vm.state.value)
        assertTrue(state.moments.single().isFavorite)
    }
}

private class FakeMomentRepository(initial: List<Moment> = emptyList()) : MomentRepository {
    private val flow = MutableStateFlow(initial)

    override suspend fun insert(moment: Moment, timelineIds: Set<TimelineId>) {
        // Match MomentRepository.observeAll contract: newest first, ties broken
        // by descending id — so tests exercise the real ordering guarantee.
        flow.value = (flow.value + moment).sortedWith(
            compareByDescending<Moment> { it.createdAt }
                .thenByDescending { it.id.value },
        )
    }

    override suspend fun findById(id: MomentId): Moment? = flow.value.firstOrNull { it.id == id }

    override suspend fun updateEditable(moment: Moment) {
        flow.value = flow.value.map { if (it.id == moment.id) moment else it }
    }

    override suspend fun setFavorite(id: MomentId, isFavorite: Boolean) {
        flow.value = flow.value.map {
            if (it.id == id) it.copy(isFavorite = isFavorite) else it
        }
    }

    override suspend fun delete(id: MomentId) {
        flow.value = flow.value.filterNot { it.id == id }
    }

    override suspend fun listAll(): List<Moment> = flow.value

    override fun observeAll(): Flow<List<Moment>> = flow.asStateFlow()

    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> = emptyList()

    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> = flow.asStateFlow()
}
