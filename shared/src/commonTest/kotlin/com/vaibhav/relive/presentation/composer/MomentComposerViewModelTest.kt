package com.vaibhav.relive.presentation.composer

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MomentValidation
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MomentComposerViewModelTest {

    @Test
    fun emptyMomentRejected() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.keepMoment()

        val invalid = assertIs<SaveState.Invalid>(vm.state.value.saveState)
        assertTrue(MomentValidation.Reason.Empty in invalid.reasons)
        assertTrue(repo.inserts.isEmpty(), "empty moment must not be persisted")
    }

    @Test
    fun titleOnlySaves() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.updateTitle("A")
        vm.keepMoment()

        assertEquals(1, repo.inserts.size)
        assertEquals("A", repo.inserts.single().first.title)
    }

    @Test
    fun contentOnlySaves() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.updateContent("just a thought")
        vm.keepMoment()

        assertEquals(1, repo.inserts.size)
        assertEquals("just a thought", repo.inserts.single().first.content)
    }

    @Test
    fun titleAndContentSaves() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.updateTitle("T")
        vm.updateContent("C")
        vm.keepMoment()

        val moment = repo.inserts.single().first
        assertEquals("T", moment.title)
        assertEquals("C", moment.content)
    }

    @Test
    fun tagsPersist() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.updateTitle("t")
        vm.addTag("Travel")
        vm.addTag("Family")
        vm.keepMoment()

        val moment = repo.inserts.single().first
        assertEquals(listOf("Travel", "Family"), moment.tags.map { it.label })
    }

    @Test
    fun duplicateEquivalentTagsDeduplicate() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.updateTitle("t")
        vm.addTag("Travel")
        vm.addTag("travel")
        vm.addTag("  TRAVEL  ")
        vm.keepMoment()

        val moment = repo.inserts.single().first
        assertEquals(1, moment.tags.size)
        assertEquals("Travel", moment.tags.single().label)
    }

    @Test
    fun inputPreservedVerbatim() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        val title = "  Quiet morning light  "
        val content = "line one\n\n  line three "
        vm.updateTitle(title)
        vm.updateContent(content)
        vm.keepMoment()

        val moment = repo.inserts.single().first
        assertEquals(title, moment.title)
        assertEquals(content, moment.content)
    }

    @Test
    fun createdAtComesFromInjectedClock() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo, clockValue = Instant(1_700_000_000_000L))

        vm.updateTitle("t")
        vm.keepMoment()

        assertEquals(Instant(1_700_000_000_000L), repo.inserts.single().first.createdAt)
    }

    @Test
    fun stableIdComesFromInjectedGenerator() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo, idValue = "id-42")

        vm.updateTitle("t")
        vm.keepMoment()

        assertEquals(MomentId("id-42"), repo.inserts.single().first.id)
    }

    @Test
    fun successfulSaveResetsComposer() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.updateTitle("t")
        vm.updateContent("c")
        vm.addTag("Family")
        vm.keepMoment()

        val state = vm.state.value
        assertEquals("", state.title)
        assertEquals("", state.content)
        assertTrue(state.tags.isEmpty())
        assertEquals(SaveState.Idle, state.saveState)
    }

    @Test
    fun persistenceFailurePreservesInput() = runTest {
        val repo = RecordingRepository(failWith = RuntimeException("db down"))
        val vm = newViewModel(repo)

        vm.updateTitle("t")
        vm.updateContent("c")
        vm.addTag("Family")
        vm.keepMoment()

        val state = vm.state.value
        assertEquals("t", state.title)
        assertEquals("c", state.content)
        assertEquals(listOf("Family"), state.tags.map { it.label })
        val failure = assertIs<SaveState.Failure>(state.saveState)
        assertEquals("db down", failure.cause.message)
    }

    @Test
    fun doubleKeepMomentWhileSavingProducesOneInsert() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = RecordingRepository(gateBeforeInsert = gate)
        val vm = newViewModel(repo, testScope = TestScope(UnconfinedTestDispatcher(testScheduler)))

        vm.updateTitle("only-once")
        vm.keepMoment()
        // While the first save is suspended waiting on the gate, a second tap
        // must be rejected.
        vm.keepMoment()
        vm.keepMoment()
        assertEquals(SaveState.Saving, vm.state.value.saveState)

        gate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.inserts.size)
    }

    @Test
    fun resetClearsTransientState() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.updateTitle("t")
        vm.updateContent("c")
        vm.addTag("Family")
        vm.updatePendingTagInput("Travel")
        vm.toggleAddMedia()

        vm.reset()

        val state = vm.state.value
        assertEquals(MomentComposerState(), state)
    }

    @Test
    fun allMembershipNeverCreated() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.updateTitle("t")
        vm.keepMoment()

        assertTrue(repo.inserts.single().second.isEmpty(), "no timeline memberships must be created")
    }

    @Test
    fun resetOnEmptyComposerIsHarmless() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)

        vm.reset()

        assertEquals(MomentComposerState(), vm.state.value)
    }

    // --- helpers ---

    private fun TestScope.newViewModel(
        repo: MomentRepository,
        clockValue: Instant = Instant(0L),
        idValue: String = "id-fixed",
        testScope: TestScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
    ): MomentComposerViewModel = MomentComposerViewModel(
        momentRepository = repo,
        clock = Clock { clockValue },
        idGenerator = IdGenerator { idValue },
        scope = testScope,
    )
}

private class RecordingRepository(
    private val failWith: Throwable? = null,
    private val gateBeforeInsert: CompletableDeferred<Unit>? = null,
) : MomentRepository {

    val inserts: MutableList<Pair<Moment, Set<TimelineId>>> = mutableListOf()

    override suspend fun insert(moment: Moment, timelineIds: Set<TimelineId>) {
        gateBeforeInsert?.await()
        failWith?.let { throw it }
        inserts += moment to timelineIds
    }

    override suspend fun findById(id: MomentId): Moment? = inserts.firstOrNull { it.first.id == id }?.first
    override suspend fun updateEditable(moment: Moment) = Unit
    override suspend fun setFavorite(id: MomentId, isFavorite: Boolean) = Unit
    override suspend fun delete(id: MomentId) = Unit
    override suspend fun listAll(): List<Moment> = inserts.map { it.first }
    private val emptyFlow = MutableStateFlow<List<Moment>>(emptyList()).asStateFlow()
    override fun observeAll(): Flow<List<Moment>> = emptyFlow
    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> = emptyList()
    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> = emptyFlow
}
