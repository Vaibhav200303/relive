package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.TimelineHomeRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.presentation.timeline.TimelineCreationOutcome
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.EntitlementState
import com.vaibhav.relive.domain.entitlement.PurchaseOutcome
import com.vaibhav.relive.domain.entitlement.RelivePurchaseOption
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CompletableDeferred
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
        val creationFeedback = async(start = CoroutineStart.UNDISPATCHED) { viewModel.creationOutcomes.first() }
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
        assertEquals(TimelineCreationOutcome.Succeeded, creationFeedback.await())
        assertEquals(Instant(23), repository.created.single().second)
    }

    @Test
    fun invalidTimelineCreationEmitsRejectedOutcome() = runTest {
        val viewModel = TimelineHomeViewModel(
            homeRepository = FakeTimelineHomeRepository(),
            timelineRepository = FakeTimelineRepository(),
            clock = Clock { Instant(23) },
            idGenerator = IdGenerator { "unused" },
            scope = backgroundScope,
        )
        val outcome = async(start = CoroutineStart.UNDISPATCHED) { viewModel.creationOutcomes.first() }

        viewModel.showTimelineCreation()
        viewModel.createTimeline()

        assertEquals(TimelineCreationOutcome.Rejected, outcome.await())
    }

    @Test
    fun plusEntrySendsFreeUserToProBeforeShowingTheCreationDialogAtTheirLimit() = runTest {
        val viewModel = TimelineHomeViewModel(
            homeRepository = FakeTimelineHomeRepository(
                List(3) { index -> summary("timeline-$index", "Timeline $index", index.toLong()) },
            ),
            timelineRepository = FakeTimelineRepository(),
            clock = Clock { Instant(23) },
            idGenerator = IdGenerator { "unused" },
            scope = backgroundScope,
            entitlementProvider = FakeEntitlementProvider(),
        )
        val outcome = async(start = CoroutineStart.UNDISPATCHED) { viewModel.creationOutcomes.first() }

        viewModel.state.first { it.content is TimelineHomeContent.Loaded }
        viewModel.showTimelineCreation()

        assertEquals(TimelineCreationOutcome.RequiresPro, outcome.await())
        assertFalse(viewModel.creationState.value.isVisible)
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

    @Test
    fun renamingACustomTimelineDelegatesToTheTimelineRepository() = runTest {
        val repository = FakeTimelineRepository()
        val timeline = Timeline.Custom(TimelineId("college"), "College")
        val viewModel = TimelineHomeViewModel(
            homeRepository = FakeTimelineHomeRepository(),
            timelineRepository = repository,
            clock = Clock { Instant(23) },
            idGenerator = IdGenerator { "unused" },
            scope = backgroundScope,
        )
        val succeeded = CompletableDeferred<Boolean>()

        viewModel.renameTimeline(timeline, "University", succeeded::complete)

        assertEquals(true, succeeded.await())
        assertEquals(TimelineId("college") to "University", repository.renamed.single())
    }

    @Test
    fun deletingACustomTimelineDelegatesToTheTimelineRepository() = runTest {
        val repository = FakeTimelineRepository()
        val timeline = Timeline.Custom(TimelineId("college"), "College")
        val viewModel = TimelineHomeViewModel(
            homeRepository = FakeTimelineHomeRepository(),
            timelineRepository = repository,
            clock = Clock { Instant(23) },
            idGenerator = IdGenerator { "unused" },
            scope = backgroundScope,
        )
        val succeeded = CompletableDeferred<Boolean>()

        viewModel.deleteTimeline(timeline, succeeded::complete)

        assertEquals(true, succeeded.await())
        assertEquals(TimelineId("college"), repository.deleted.single())
    }

    @Test
    fun deletingMultipleCustomTimelinesDelegatesEachToTheTimelineRepository() = runTest {
        val repository = FakeTimelineRepository()
        val timelines = listOf(
            Timeline.Custom(TimelineId("college"), "College"),
            Timeline.Custom(TimelineId("trips"), "Trips"),
        )
        val viewModel = TimelineHomeViewModel(
            homeRepository = FakeTimelineHomeRepository(),
            timelineRepository = repository,
            clock = Clock { Instant(23) },
            idGenerator = IdGenerator { "unused" },
            scope = backgroundScope,
        )
        val succeeded = CompletableDeferred<Boolean>()

        viewModel.deleteTimelines(timelines, succeeded::complete)

        assertEquals(true, succeeded.await())
        assertEquals(timelines.map { it.id }, repository.deleted)
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

private class FakeTimelineHomeRepository(
    summaries: List<TimelineHomeSummary> = emptyList(),
) : TimelineHomeRepository {
    override fun observeSummaries(): Flow<List<TimelineHomeSummary>> = MutableStateFlow(summaries)
    override fun observeAllCollageCandidates(bucket: Long): Flow<List<MediaAttachment>> =
        MutableStateFlow(emptyList())
}

private class FakeEntitlementProvider : EntitlementProvider {
    override val state = MutableStateFlow(EntitlementState())
    override suspend fun purchase(option: RelivePurchaseOption) = PurchaseOutcome.Unavailable("Unavailable")
    override suspend fun restorePurchases() = PurchaseOutcome.Unavailable("Unavailable")
}

private class FakeTimelineRepository : TimelineRepository {
    private val timelines = MutableStateFlow<List<Timeline.Custom>>(emptyList())
    val created = mutableListOf<Pair<Timeline.Custom, Instant>>()
    val renamed = mutableListOf<Pair<TimelineId, String>>()
    val deleted = mutableListOf<TimelineId>()

    override suspend fun createCustom(timeline: Timeline.Custom, createdAt: Instant) {
        created += timeline to createdAt
        timelines.value = listOf(timeline) + timelines.value
    }

    override suspend fun findCustom(id: TimelineId): Timeline.Custom? = timelines.value.firstOrNull { it.id == id }
    override suspend fun listCustom(): List<Timeline.Custom> = timelines.value
    override fun observeCustom(): Flow<List<Timeline.Custom>> = timelines
    override suspend fun rename(id: TimelineId, newName: String) {
        renamed += id to newName
    }
    override suspend fun updateAppearance(id: TimelineId, appearance: TimelineAppearance) = Unit
    override suspend fun updateCoverPhoto(id: TimelineId, coverPhotoRef: MediaStorageRef?) = Unit
    override suspend fun deleteCustom(id: TimelineId) {
        deleted += id
    }
    override suspend fun addMembership(momentId: com.vaibhav.relive.domain.model.MomentId, timelineId: TimelineId) = Unit
    override suspend fun removeMembership(momentId: com.vaibhav.relive.domain.model.MomentId, timelineId: TimelineId) = Unit
    override suspend fun timelinesFor(momentId: com.vaibhav.relive.domain.model.MomentId): List<TimelineId> = emptyList()
}
