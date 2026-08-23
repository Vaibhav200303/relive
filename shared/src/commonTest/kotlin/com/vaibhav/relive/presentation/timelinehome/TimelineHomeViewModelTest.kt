package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.TimelineHomeRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineHomeViewModelTest {
    @Test
    fun creatingATimelineNavigatesToThePersistedCustomTimeline() = runTest {
        val repository = FakeTimelineRepository()
        val viewModel = TimelineHomeViewModel(
            homeRepository = FakeTimelineHomeRepository(),
            timelineRepository = repository,
            clock = Clock { Instant(23) },
            idGenerator = IdGenerator { "newest" },
            scope = backgroundScope,
        )

        val destination = async(start = CoroutineStart.UNDISPATCHED) { viewModel.navigation.first() }
        viewModel.showTimelineCreation()
        viewModel.updateTimelineName("  Japan 2026  ")
        viewModel.createTimeline()

        assertEquals(Timeline.Custom(TimelineId("newest"), "Japan 2026"), destination.await())
        assertEquals(Instant(23), repository.created.single().second)
    }
}

private class FakeTimelineHomeRepository : TimelineHomeRepository {
    override fun observeSummaries(): Flow<List<TimelineHomeSummary>> = MutableStateFlow(emptyList())
}

private class FakeTimelineRepository : TimelineRepository {
    private val timelines = MutableStateFlow<List<Timeline.Custom>>(emptyList())
    val created = mutableListOf<Pair<Timeline.Custom, Instant>>()

    override suspend fun createCustom(timeline: Timeline.Custom, createdAt: Instant) {
        created += timeline to createdAt
        timelines.value = listOf(timeline) + timelines.value
    }

    override suspend fun findCustom(id: TimelineId): Timeline.Custom? = timelines.value.firstOrNull { it.id == id }
    override suspend fun listCustom(): List<Timeline.Custom> = timelines.value
    override fun observeCustom(): Flow<List<Timeline.Custom>> = timelines
    override suspend fun rename(id: TimelineId, newName: String) = Unit
    override suspend fun updateTheme(id: TimelineId, theme: ThemeReference?) = Unit
    override suspend fun deleteCustom(id: TimelineId) = Unit
    override suspend fun addMembership(momentId: com.vaibhav.relive.domain.model.MomentId, timelineId: TimelineId) = Unit
    override suspend fun removeMembership(momentId: com.vaibhav.relive.domain.model.MomentId, timelineId: TimelineId) = Unit
    override suspend fun timelinesFor(momentId: com.vaibhav.relive.domain.model.MomentId): List<TimelineId> = emptyList()
}
