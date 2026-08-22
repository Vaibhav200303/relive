package com.vaibhav.relive.presentation.composer

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MomentValidation
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.media.AudioRecorder
import com.vaibhav.relive.platform.media.MediaProcessor
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.ProcessedMedia
import com.vaibhav.relive.platform.media.RawMedia
import com.vaibhav.relive.platform.media.RecordingState
import com.vaibhav.relive.platform.permission.MicPermissionResult
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun tagsPersistAndDeduplicate() = runTest {
        val repo = RecordingRepository()
        val vm = newViewModel(repo)
        vm.updateTitle("t")
        vm.addTag("Travel"); vm.addTag("travel"); vm.addTag("  TRAVEL ")
        vm.keepMoment()
        assertEquals(1, repo.inserts.single().first.tags.size)
    }

    @Test
    fun newTagLabelLowercasedOnCommit() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.updatePendingTagInput("Travel")
        vm.commitPendingTag()
        assertEquals("travel", vm.state.value.tags.single().label)
    }

    @Test
    fun leadingHashStrippedFromCommittedTag() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.updatePendingTagInput("#Travel")
        vm.commitPendingTag()
        assertEquals("travel", vm.state.value.tags.single().label)
    }

    @Test
    fun multipleLeadingHashesStripped() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.updatePendingTagInput("###College Life")
        vm.commitPendingTag()
        assertEquals("college life", vm.state.value.tags.single().label)
    }

    @Test
    fun surroundingWhitespaceTrimmed() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.updatePendingTagInput("  College Life  ")
        vm.commitPendingTag()
        assertEquals("college life", vm.state.value.tags.single().label)
    }

    @Test
    fun onlyHashRejected() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.updatePendingTagInput("#")
        vm.commitPendingTag()
        assertTrue(vm.state.value.tags.isEmpty())
        assertEquals("", vm.state.value.pendingTagInput)
    }

    @Test
    fun emptyInputRejected() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.updatePendingTagInput("   ")
        vm.commitPendingTag()
        assertTrue(vm.state.value.tags.isEmpty())
    }

    @Test
    fun addTagNormalizesLabel() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.addTag("#Memories")
        assertEquals("memories", vm.state.value.tags.single().label)
    }

    @Test
    fun canonicalDedupSurvivesNormalization() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.updatePendingTagInput("#Travel"); vm.commitPendingTag()
        vm.updatePendingTagInput("TRAVEL"); vm.commitPendingTag()
        vm.updatePendingTagInput("travel"); vm.commitPendingTag()
        assertEquals(1, vm.state.value.tags.size)
        assertEquals("travel", vm.state.value.tags.single().label)
    }

    @Test
    fun resetClearsTransientState() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.updateTitle("t"); vm.updateContent("c"); vm.addTag("Family")
        vm.updatePendingTagInput("Travel"); vm.toggleAddMedia()
        vm.reset()
        assertEquals(MomentComposerState(), vm.state.value)
    }

    // -- media: processing lifecycle ------------------------------------

    @Test
    fun processRawAddsAttachment() = runTest {
        val store = FakeMediaStore()
        val proc = FakeMediaProcessor(store)
        val vm = newViewModel(RecordingRepository(), store = store, processor = proc)

        vm.processRaw(RawMedia(MediaType.Image, "/tmp/x.jpg", ownedByRelive = true))

        val att = vm.state.value.attachments.single()
        assertEquals(MediaType.Image, att.type)
        assertIs<DraftMediaStatus.Ready>(att.status)
    }

    @Test
    fun processRawBatchPreservesOrder() = runTest {
        val store = FakeMediaStore()
        val vm = newViewModel(RecordingRepository(), store = store, processor = FakeMediaProcessor(store))
        vm.processRawBatch(
            listOf(
                RawMedia(MediaType.Image, "a", ownedByRelive = true),
                RawMedia(MediaType.Video, "b", ownedByRelive = true),
                RawMedia(MediaType.Audio, "c", ownedByRelive = true),
            ),
        )
        val types = vm.state.value.attachments.map { it.type }
        assertEquals(listOf(MediaType.Image, MediaType.Video, MediaType.Audio), types)
    }

    @Test
    fun processRawBatchMixedOrderPreserved() = runTest {
        val store = FakeMediaStore()
        val vm = newViewModel(RecordingRepository(), store = store, processor = FakeMediaProcessor(store))
        vm.processRawBatch(
            listOf(
                RawMedia(MediaType.Video, "v1", ownedByRelive = true),
                RawMedia(MediaType.Image, "p1", ownedByRelive = true),
                RawMedia(MediaType.Video, "v2", ownedByRelive = true),
                RawMedia(MediaType.Image, "p2", ownedByRelive = true),
            ),
        )
        val types = vm.state.value.attachments.map { it.type }
        assertEquals(
            listOf(MediaType.Video, MediaType.Image, MediaType.Video, MediaType.Image),
            types,
        )
    }

    @Test
    fun placeholdersAppearImmediately() = runTest {
        val store = FakeMediaStore()
        val proc = GateableProcessor(store)
        val vm = newViewModel(RecordingRepository(), store = store, processor = proc)

        vm.processRawBatch(
            listOf(
                RawMedia(MediaType.Image, "a", ownedByRelive = true),
                RawMedia(MediaType.Image, "b", ownedByRelive = true),
                RawMedia(MediaType.Image, "c", ownedByRelive = true),
                RawMedia(MediaType.Image, "d", ownedByRelive = true),
                RawMedia(MediaType.Image, "e", ownedByRelive = true),
            ),
        )

        // Before any processing completes, all 5 tiles must already be visible.
        val slots = vm.state.value.attachments
        assertEquals(5, slots.size)
        slots.forEach { assertNotEquals(DraftMediaStatus.Ready::class, it.status::class) }

        proc.releaseAll()
        assertTrue(vm.state.value.attachments.all { it.status is DraftMediaStatus.Ready })
    }

    @Test
    fun draftIdsAreStableAcrossStatusTransitions() = runTest {
        val store = FakeMediaStore()
        val proc = GateableProcessor(store)
        val vm = newViewModel(RecordingRepository(), store = store, processor = proc)

        vm.processRaw(RawMedia(MediaType.Image, "a", ownedByRelive = true))
        val idBefore = vm.state.value.attachments.single().draftId
        val before = vm.state.value.attachments.single().status
        assertTrue(
            before is DraftMediaStatus.Pending || before is DraftMediaStatus.Processing,
            "must expose an explicit pre-Ready status, was $before",
        )

        proc.releaseAll()
        val idAfter = vm.state.value.attachments.single().draftId
        assertEquals(idBefore, idAfter)
        assertIs<DraftMediaStatus.Ready>(vm.state.value.attachments.single().status)
    }

    @Test
    fun processingBoundedByConcurrencyLimit() = runTest {
        val store = FakeMediaStore()
        val proc = GateableProcessor(store)
        val vm = newViewModel(
            RecordingRepository(),
            store = store,
            processor = proc,
            processingConcurrency = 2,
        )

        vm.processRawBatch(
            listOf(
                RawMedia(MediaType.Video, "a", ownedByRelive = true),
                RawMedia(MediaType.Video, "b", ownedByRelive = true),
                RawMedia(MediaType.Video, "c", ownedByRelive = true),
                RawMedia(MediaType.Video, "d", ownedByRelive = true),
            ),
        )

        assertEquals(
            2,
            proc.activeCount,
            "no more than concurrency permits may be processing at once",
        )

        proc.releaseOne(); proc.releaseOne()
        assertEquals(2, proc.activeCount, "next batch takes over as permits free up")
        proc.releaseAll()
        assertTrue(vm.state.value.attachments.all { it.status is DraftMediaStatus.Ready })
    }

    @Test
    fun failureIsolatedFromSiblings() = runTest {
        val store = FakeMediaStore()
        val proc = FailingProcessor(store, failOnPath = "bad")
        val vm = newViewModel(RecordingRepository(), store = store, processor = proc)

        vm.processRawBatch(
            listOf(
                RawMedia(MediaType.Image, "ok1", ownedByRelive = true),
                RawMedia(MediaType.Image, "bad", ownedByRelive = true),
                RawMedia(MediaType.Image, "ok2", ownedByRelive = true),
            ),
        )

        val statuses = vm.state.value.attachments.map { it.status }
        assertIs<DraftMediaStatus.Ready>(statuses[0])
        assertIs<DraftMediaStatus.Failed>(statuses[1])
        assertIs<DraftMediaStatus.Ready>(statuses[2])
    }

    @Test
    fun retryTransitionsFailedToReady() = runTest {
        val store = FakeMediaStore()
        val proc = FailingProcessor(store, failOnPath = "bad")
        val vm = newViewModel(RecordingRepository(), store = store, processor = proc)

        vm.processRaw(RawMedia(MediaType.Image, "bad", ownedByRelive = true))
        val draftId = vm.state.value.attachments.single().draftId
        assertIs<DraftMediaStatus.Failed>(vm.state.value.attachments.single().status)

        proc.failNext = false
        vm.retryAttachment(draftId)
        assertIs<DraftMediaStatus.Ready>(vm.state.value.attachments.single().status)
        assertEquals(draftId, vm.state.value.attachments.single().draftId)
    }

    @Test
    fun removeAttachmentDeletesReadyFile() = runTest {
        val store = FakeMediaStore()
        val vm = newViewModel(RecordingRepository(), store = store, processor = FakeMediaProcessor(store))
        vm.processRaw(RawMedia(MediaType.Image, "x", ownedByRelive = true))
        val slot = vm.state.value.attachments.single()
        val ref = (slot.status as DraftMediaStatus.Ready).storageRef
        vm.removeAttachment(slot.draftId)
        assertTrue(vm.state.value.attachments.isEmpty())
        assertTrue(store.deleted.contains(ref))
    }

    @Test
    fun removeOnePendingDoesNotAffectSiblings() = runTest {
        val store = FakeMediaStore()
        val proc = GateableProcessor(store)
        val vm = newViewModel(RecordingRepository(), store = store, processor = proc)

        vm.processRawBatch(
            listOf(
                RawMedia(MediaType.Image, "a", ownedByRelive = true),
                RawMedia(MediaType.Image, "b", ownedByRelive = true),
                RawMedia(MediaType.Image, "c", ownedByRelive = true),
            ),
        )
        val middleId = vm.state.value.attachments[1].draftId
        vm.removeAttachment(middleId)
        assertEquals(2, vm.state.value.attachments.size)

        proc.releaseAll()
        assertTrue(vm.state.value.attachments.all { it.status is DraftMediaStatus.Ready })
    }

    @Test
    fun resetDeletesAllReadyDraftMedia() = runTest {
        val store = FakeMediaStore()
        val vm = newViewModel(RecordingRepository(), store = store, processor = FakeMediaProcessor(store))
        vm.processRaw(RawMedia(MediaType.Image, "a", ownedByRelive = true))
        vm.processRaw(RawMedia(MediaType.Video, "b", ownedByRelive = true))
        val refs = vm.state.value.attachments.mapNotNull { (it.status as? DraftMediaStatus.Ready)?.storageRef }
        vm.reset()
        assertTrue(vm.state.value.attachments.isEmpty())
        assertTrue(store.deleted.containsAll(refs))
    }

    @Test
    fun keepMomentBlockedWhileProcessing() = runTest {
        val store = FakeMediaStore()
        val proc = GateableProcessor(store)
        val repo = RecordingRepository()
        val vm = newViewModel(repo, store = store, processor = proc)
        vm.updateTitle("t")
        vm.processRaw(RawMedia(MediaType.Video, "v", ownedByRelive = true))

        vm.keepMoment()
        assertEquals(SaveState.AwaitingProcessing, vm.state.value.saveState)
        assertTrue(repo.inserts.isEmpty(), "must not persist while a draft is processing")

        proc.releaseAll()
        vm.keepMoment()
        assertEquals(1, repo.inserts.size)
    }

    @Test
    fun keepMomentPersistsAttachmentsWithSortIndex() = runTest {
        val store = FakeMediaStore()
        val repo = RecordingRepository()
        val vm = newViewModel(repo, store = store, processor = FakeMediaProcessor(store))
        vm.updateTitle("t")
        vm.processRaw(RawMedia(MediaType.Image, "a", ownedByRelive = true))
        vm.processRaw(RawMedia(MediaType.Video, "b", ownedByRelive = true))
        vm.processRaw(RawMedia(MediaType.Audio, "c", ownedByRelive = true))
        vm.keepMoment()

        val saved = repo.inserts.single().first
        assertEquals(3, saved.attachments.size)
        assertEquals(listOf(0, 1, 2), saved.attachments.map { it.sortIndex })
        assertEquals(
            listOf(MediaType.Image, MediaType.Video, MediaType.Audio),
            saved.attachments.map { it.type },
        )
        assertTrue(store.deleted.isEmpty(), "committed files must not be deleted")
    }

    @Test
    fun failedSaveRetainsDraftMediaForRetry() = runTest {
        val store = FakeMediaStore()
        val repo = RecordingRepository(failWith = RuntimeException("db"))
        val vm = newViewModel(repo, store = store, processor = FakeMediaProcessor(store))
        vm.updateTitle("t")
        vm.processRaw(RawMedia(MediaType.Image, "a", ownedByRelive = true))
        vm.keepMoment()

        val state = vm.state.value
        assertIs<SaveState.Failure>(state.saveState)
        assertEquals(1, state.attachments.size, "draft attachments must be retained")
        assertTrue(store.deleted.isEmpty(), "draft files must not be deleted on failure")
    }

    @Test
    fun recordingBlocksKeepMoment() = runTest {
        val store = FakeMediaStore()
        val recorder = FakeAudioRecorder()
        val vm = newViewModel(
            RecordingRepository(),
            store = store,
            processor = FakeMediaProcessor(store),
            recorderFactory = { recorder },
        )
        vm.updateTitle("t")
        vm.startRecording()
        vm.keepMoment()
        assertEquals(SaveState.Idle, vm.state.value.saveState)
        assertNotNull(vm.state.value.mediaError)
    }

    // -- mic permission --------------------------------------------------

    @Test
    fun requestMicPermissionMarksPending() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.requestMicPermission()
        assertTrue(vm.state.value.pendingMicPermissionRequest)
        assertEquals(MicPermissionUiState.Idle, vm.state.value.micPermission)
    }

    @Test
    fun grantedPermissionStartsRecordingAndClearsPending() = runTest {
        val recorder = FakeAudioRecorder()
        val vm = newViewModel(RecordingRepository(), recorderFactory = { recorder })
        vm.requestMicPermission()
        vm.onMicPermissionResult(MicPermissionResult.Granted)
        assertFalse(vm.state.value.pendingMicPermissionRequest)
        assertTrue(vm.state.value.isRecording)
        assertEquals(MicPermissionUiState.Idle, vm.state.value.micPermission)
    }

    @Test
    fun deniedPermissionSetsRecoverableState() = runTest {
        val vm = newViewModel(RecordingRepository(), recorderFactory = { FakeAudioRecorder() })
        vm.requestMicPermission()
        vm.onMicPermissionResult(MicPermissionResult.Denied)
        assertFalse(vm.state.value.pendingMicPermissionRequest)
        assertFalse(vm.state.value.isRecording)
        assertEquals(MicPermissionUiState.Recoverable, vm.state.value.micPermission)
    }

    @Test
    fun permanentlyDeniedExposesSettingsState() = runTest {
        val vm = newViewModel(RecordingRepository(), recorderFactory = { FakeAudioRecorder() })
        vm.requestMicPermission()
        vm.onMicPermissionResult(MicPermissionResult.PermanentlyDenied)
        assertEquals(MicPermissionUiState.SettingsRequired, vm.state.value.micPermission)
        assertFalse(vm.state.value.isRecording)
    }

    @Test
    fun deniedPermissionPreservesComposerDraft() = runTest {
        val vm = newViewModel(RecordingRepository(), recorderFactory = { FakeAudioRecorder() })
        vm.updateTitle("t"); vm.updateContent("c"); vm.addTag("Family")
        vm.requestMicPermission()
        vm.onMicPermissionResult(MicPermissionResult.Denied)
        val s = vm.state.value
        assertEquals("t", s.title); assertEquals("c", s.content)
        assertEquals(1, s.tags.size)
    }

    @Test
    fun dismissMicPermissionMessageResetsHint() = runTest {
        val vm = newViewModel(RecordingRepository(), recorderFactory = { FakeAudioRecorder() })
        vm.requestMicPermission()
        vm.onMicPermissionResult(MicPermissionResult.Denied)
        vm.dismissMicPermissionMessage()
        assertEquals(MicPermissionUiState.Idle, vm.state.value.micPermission)
    }

    @Test
    fun requestMicPermissionNoOpWhileRecording() = runTest {
        val recorder = FakeAudioRecorder()
        val vm = newViewModel(RecordingRepository(), recorderFactory = { recorder })
        vm.startRecording()
        vm.requestMicPermission()
        assertFalse(vm.state.value.pendingMicPermissionRequest)
    }

    // -- camera cancel / add-media back ---------------------------------

    @Test
    fun cameraCancelPreservesComposerDraft() = runTest {
        val store = FakeMediaStore()
        val vm = newViewModel(RecordingRepository(), store = store, processor = FakeMediaProcessor(store))
        vm.updateTitle("t"); vm.updateContent("c"); vm.addTag("Family")
        vm.processRaw(RawMedia(MediaType.Image, "a", ownedByRelive = true))
        vm.openCamera()
        vm.dismissOverlay()
        val s = vm.state.value
        assertEquals("t", s.title); assertEquals("c", s.content)
        assertEquals(1, s.tags.size)
        assertEquals(1, s.attachments.size)
        assertEquals(ComposerOverlay.None, s.overlay)
    }

    @Test
    fun toggleAddMediaCollapsesWhenExpanded() = runTest {
        val vm = newViewModel(RecordingRepository())
        vm.toggleAddMedia()
        assertTrue(vm.state.value.addMediaExpanded)
        vm.toggleAddMedia()
        assertFalse(vm.state.value.addMediaExpanded)
    }

    @Test
    fun cancelRecordingClearsRecordingState() = runTest {
        val store = FakeMediaStore()
        val recorder = FakeAudioRecorder()
        val vm = newViewModel(
            RecordingRepository(),
            store = store,
            processor = FakeMediaProcessor(store),
            recorderFactory = { recorder },
        )
        vm.startRecording()
        vm.cancelRecording()
        assertNull(vm.state.value.recording)
        assertTrue(recorder.cancelled)
        assertFalse(recorder.stopped)
    }

    // --- helpers ---

    private fun TestScope.newViewModel(
        repo: MomentRepository,
        clockValue: Instant = Instant(0L),
        store: MediaStore = FakeMediaStore(),
        processor: MediaProcessor = FakeMediaProcessor(store as? FakeMediaStore ?: FakeMediaStore()),
        recorderFactory: () -> AudioRecorder = { error("not used") },
        processingConcurrency: Int = 4,
        testScope: TestScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
    ): MomentComposerViewModel {
        var counter = 0
        return MomentComposerViewModel(
            momentRepository = repo,
            clock = Clock { clockValue },
            idGenerator = IdGenerator { "id-${counter++}" },
            scope = testScope,
            mediaStore = store,
            mediaProcessor = processor,
            audioRecorderFactory = recorderFactory,
            processingConcurrency = processingConcurrency,
        )
    }
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

private class FakeMediaStore : MediaStore {
    val deleted = mutableListOf<MediaStorageRef>()
    private var counter = 0
    override fun extensionFor(type: MediaType) = when (type) {
        MediaType.Image -> "jpg"; MediaType.Video -> "mp4"; MediaType.Audio -> "m4a"
    }
    override fun allocateKey(type: MediaType): MediaStorageRef =
        MediaStorageRef("${type.name.lowercase()}/${++counter}.${extensionFor(type)}")
    override fun resolveAbsolutePath(ref: MediaStorageRef): String = "/fake/${ref.value}"
    override fun exists(ref: MediaStorageRef): Boolean = !deleted.contains(ref)
    override fun delete(ref: MediaStorageRef) { deleted += ref }
    override fun sizeBytes(ref: MediaStorageRef): Long = 0
}

private class FakeMediaProcessor(private val store: FakeMediaStore) : MediaProcessor {
    override suspend fun process(raw: RawMedia): ProcessedMedia {
        val ref = store.allocateKey(raw.type)
        return ProcessedMedia(type = raw.type, storageRef = ref)
    }
}

/**
 * Processor whose `process` suspends until [releaseOne] / [releaseAll] is
 * called. Used to observe pending/processing state without relying on wall-
 * clock timing.
 */
private class GateableProcessor(private val store: FakeMediaStore) : MediaProcessor {
    private val gates = mutableListOf<CompletableDeferred<Unit>>()
    var activeCount: Int = 0
        private set

    override suspend fun process(raw: RawMedia): ProcessedMedia {
        val gate = CompletableDeferred<Unit>()
        gates += gate
        activeCount += 1
        try {
            gate.await()
            val ref = store.allocateKey(raw.type)
            return ProcessedMedia(type = raw.type, storageRef = ref)
        } finally {
            activeCount -= 1
        }
    }

    fun releaseOne() {
        val g = gates.removeFirstOrNull() ?: return
        g.complete(Unit)
    }

    fun releaseAll() {
        while (true) {
            val g = gates.removeFirstOrNull() ?: return
            g.complete(Unit)
        }
    }

}

private class FailingProcessor(
    private val store: FakeMediaStore,
    private val failOnPath: String,
) : MediaProcessor {
    var failNext: Boolean = true
    override suspend fun process(raw: RawMedia): ProcessedMedia {
        if (raw.sourcePath == failOnPath && failNext) {
            throw RuntimeException("boom")
        }
        val ref = store.allocateKey(raw.type)
        return ProcessedMedia(type = raw.type, storageRef = ref)
    }
}

private class FakeAudioRecorder : AudioRecorder {
    private val _state = MutableStateFlow(RecordingState())
    override val state = _state.asStateFlow()
    var stopped = false
    var cancelled = false
    override suspend fun start(): Result<Unit> {
        _state.value = RecordingState(isRecording = true)
        return Result.success(Unit)
    }
    override suspend fun stop(): RawMedia {
        stopped = true
        _state.value = RecordingState()
        return RawMedia(MediaType.Audio, "/tmp/rec.m4a", ownedByRelive = true)
    }
    override suspend fun cancel() {
        cancelled = true
        _state.value = RecordingState()
    }
}
