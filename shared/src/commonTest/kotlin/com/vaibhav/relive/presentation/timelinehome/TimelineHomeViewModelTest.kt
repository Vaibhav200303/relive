package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.TimelineHomeRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TimelineHomeViewModelTest {
    @Test
    fun blankQueryReturnsEveryCustomTimelineInObservedOrder() {
        val state = loadedState(query = "")

        assertEquals(listOf("Trips", "College", "Family"), state.visibleCustomSummaries.map { it.name })
    }

    @Test
    fun matchingIsCaseInsensitive() {
        val state = loadedState(query = "fAmIlY")

        assertEquals(listOf("Family"), state.visibleCustomSummaries.map { it.name })
    }

    @Test
    fun matchingAcceptsPartialTimelineNames() {
        val state = loadedState(query = "oll")

        assertEquals(listOf("College"), state.visibleCustomSummaries.map { it.name })
    }

    @Test
    fun unmatchedQueryReturnsNoTimelines() {
        val state = loadedState(query = "work")

        assertEquals(emptyList(), state.visibleCustomSummaries)
    }

    @Test
    fun matchingPreservesNewestFirstObservedOrder() {
        val state = loadedState(query = "i")

        assertEquals(listOf("Trips", "Family"), state.visibleCustomSummaries.map { it.name })
    }

    @Test
    fun allIsNeverIncludedInTimelineSearchResults() {
        val state = loadedState(query = "")

        assertEquals(false, state.visibleCustomSummaries.any { it.timeline == Timeline.All })
    }

    @Test
    fun previewDataDoesNotAffectTimelineNameMatching() {
        val attachment = MediaAttachment(
            id = MediaAttachmentId("secret-memory"),
            type = MediaType.Image,
            storageRef = MediaStorageRef("secret-memory-content"),
            sortIndex = 0,
        )
        val family = summary("family", "Family", createdAt = 1, attachments = listOf(attachment))
        val state = TimelineHomeState(
            content = TimelineHomeContent.Loaded(listOf(family)),
            query = "secret-memory",
        )

        assertEquals(emptyList(), state.visibleCustomSummaries)
    }

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

        assertEquals(
            TimelineHomeNavigation(
                timeline = Timeline.Custom(TimelineId("newest"), "Japan 2026"),
                openComposerOnEnter = true,
            ),
            destination.await(),
        )
        assertEquals(Instant(23), repository.created.single().second)
    }

    @Test
    fun normalTimelineEntryDoesNotRequestComposerOpen() = runTest {
        val timeline = Timeline.Custom(TimelineId("saved"), "Saved")
        val viewModel = TimelineHomeViewModel(
            homeRepository = FakeTimelineHomeRepository(),
            timelineRepository = FakeTimelineRepository(),
            clock = Clock { Instant(23) },
            idGenerator = IdGenerator { "unused" },
            scope = backgroundScope,
        )

        val destination = async(start = CoroutineStart.UNDISPATCHED) { viewModel.navigation.first() }
        viewModel.selectTimeline(timeline)

        val navigation = destination.await()
        assertEquals(timeline, navigation.timeline)
        assertFalse(navigation.openComposerOnEnter)
    }

    private fun loadedState(query: String): TimelineHomeState = TimelineHomeState(
        content = TimelineHomeContent.Loaded(
            listOf(
                TimelineHomeSummary(Timeline.All, momentCount = 4, previewAttachments = emptyList()),
                summary("trips", "Trips", createdAt = 3),
                summary("college", "College", createdAt = 2),
                summary("family", "Family", createdAt = 1),
            ),
        ),
        query = query,
    )

    private fun summary(
        id: String,
        name: String,
        createdAt: Long,
        attachments: List<MediaAttachment> = emptyList(),
    ) = TimelineHomeSummary(
        timeline = Timeline.Custom(TimelineId(id), name),
        momentCount = 1,
        previewAttachments = attachments,
        createdAt = Instant(createdAt),
    )
}

private class FakeTimelineHomeRepository : TimelineHomeRepository {
    override fun observeSummaries(): Flow<List<TimelineHomeSummary>> = MutableStateFlow(emptyList())
    override fun observeAllCollageCandidates(bucket: Long): Flow<List<MediaAttachment>> =
        MutableStateFlow(emptyList())
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
