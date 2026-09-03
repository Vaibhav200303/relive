package com.vaibhav.relive.presentation.insights

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MomentFeelingSample
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MoodInsightsCalculator
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.dayOfWeekIndex
import com.vaibhav.relive.domain.model.plusDays
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The Home mood-bar/insights state holder (PRODUCT_SPEC §10A, ADR-0064). Its job is to read the
 * bounded sample projection, resolve each sample to a device-local day, and hand the numbers to
 * the pure calculator — never to hydrate Moments.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoodInsightsViewModelTest {

    @Test
    fun `reads only the bounded sample projection`() = runTest {
        val repository = FakeFeelingRepository()
        val now = Instant(1_764_000_000_000L)

        val viewModel = MoodInsightsViewModel(
            momentRepository = repository,
            clock = Clock { now },
            scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
        viewModel.insights.first { it != null }

        assertEquals(0, repository.fullArchiveReads, "insights must never hydrate the archive")
        assertEquals(1, repository.sampleReads.size)
        // The cutoff covers the six months the charts show, and nothing older.
        val cutoff = repository.sampleReads.single()
        val expected = RediscoverCalendar.startOfDay(
            RediscoverCalendar.localDate(now).plusDays(-MoodInsightsCalculator.HISTORY_DAYS),
        )
        assertEquals(expected.epochMilliseconds, cutoff.epochMilliseconds)
        assertTrue(cutoff < now)
    }

    @Test
    fun `emits insights derived from the samples and re-emits on change`() = runTest {
        val now = Instant(1_764_000_000_000L)
        val today = RediscoverCalendar.localDate(now)
        val repository = FakeFeelingRepository(
            initial = listOf(MomentFeelingSample(createdAt = now, feeling = MomentFeeling.Great)),
        )

        val viewModel = MoodInsightsViewModel(
            momentRepository = repository,
            clock = Clock { now },
            scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )

        val first = viewModel.insights.first { it != null }
        assertNotNull(first)
        assertEquals(MomentFeeling.Great, first.thisWeek?.verdict)
        assertEquals(1, first.momentCount)
        assertEquals(1, first.splitCounts[MomentFeeling.Great])
        // Today's own curve point carries the feeling.
        assertEquals(MomentFeeling.Great, first.weekDays[today.dayOfWeekIndex()].verdict)

        repository.emit(
            listOf(
                MomentFeelingSample(createdAt = now, feeling = MomentFeeling.Great),
                MomentFeelingSample(createdAt = now, feeling = MomentFeeling.Low),
            ),
        )

        val second = withTimeout(1_000) {
            viewModel.insights.first { it != null && it.momentCount == 2 }
        }
        // (3 + 1) / 2 = 2 → Good.
        assertEquals(MomentFeeling.Good, second!!.thisWeek?.verdict)
    }

    @Test
    fun `an unfelt archive yields a quiet, complete shape`() = runTest {
        val now = Instant(1_764_000_000_000L)
        val repository = FakeFeelingRepository(
            initial = listOf(MomentFeelingSample(createdAt = now, feeling = null)),
        )

        val viewModel = MoodInsightsViewModel(
            momentRepository = repository,
            clock = Clock { now },
            scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )

        val insights = viewModel.insights.first { it != null }!!
        assertEquals(null, insights.thisWeek)
        assertEquals(null, insights.lastWeek)
        assertEquals(false, insights.hasAnyFeeling)
        assertEquals(7, insights.weekDays.size)
        assertEquals(6, insights.months.size)
        assertEquals(1, insights.momentCount)
    }
}

/**
 * Records which reads happen. [observeAll] throwing is what proves the insights path never
 * falls back to the archive-hydrating default on [MomentRepository].
 */
private class FakeFeelingRepository(
    initial: List<MomentFeelingSample> = emptyList(),
) : MomentRepository {
    private val samples = MutableStateFlow(initial)
    val sampleReads = mutableListOf<Instant>()
    var fullArchiveReads = 0
        private set

    fun emit(next: List<MomentFeelingSample>) {
        samples.value = next
    }

    override fun observeFeelingSamplesSince(cutoff: Instant): Flow<List<MomentFeelingSample>> {
        sampleReads += cutoff
        return samples.asStateFlow()
    }

    override fun observeAll(): Flow<List<Moment>> {
        fullArchiveReads++
        error("Mood insights must not hydrate the archive")
    }

    override suspend fun insert(moment: Moment, timelineIds: Set<TimelineId>) = Unit
    override suspend fun findById(id: MomentId): Moment? = null
    override suspend fun updateEditable(moment: Moment) = Unit
    override suspend fun setFavorite(id: MomentId, isFavorite: Boolean) = Unit
    override suspend fun setFeeling(id: MomentId, feeling: MomentFeeling?) = Unit
    override suspend fun delete(id: MomentId) = Unit
    override suspend fun listAll(): List<Moment> = emptyList()
    override fun observeSearch(query: String): Flow<List<Moment>> = MutableStateFlow(emptyList())
    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> = emptyList()
    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> =
        MutableStateFlow(emptyList())
}
