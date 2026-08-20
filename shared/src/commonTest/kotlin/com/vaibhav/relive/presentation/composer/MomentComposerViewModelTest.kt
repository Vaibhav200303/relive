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

        assertEquals(1, vm.state.value.attachments.size)
        assertEquals(MediaType.Image, vm.state.value.attachments.single().type)
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
    fun removeAttachmentDeletesFile() = runTest {
        val store = FakeMediaStore()
        val vm = newViewModel(RecordingRepository(), store = store, processor = FakeMediaProcessor(store))
        vm.processRaw(RawMedia(MediaType.Image, "x", ownedByRelive = true))
        val ref = vm.state.value.attachments.single().storageRef
        vm.removeAttachment(ref)
        assertTrue(vm.state.value.attachments.isEmpty())
        assertTrue(store.deleted.contains(ref))
    }

    @Test
    fun resetDeletesAllDraftMedia() = runTest {
        val store = FakeMediaStore()
        val vm = newViewModel(RecordingRepository(), store = store, processor = FakeMediaProcessor(store))
        vm.processRaw(RawMedia(MediaType.Image, "a", ownedByRelive = true))
        vm.processRaw(RawMedia(MediaType.Video, "b", ownedByRelive = true))
        val refs = vm.state.value.attachments.map { it.storageRef }
        vm.reset()
        assertTrue(vm.state.value.attachments.isEmpty())
        assertTrue(store.deleted.containsAll(refs))
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
        // Successful save clears composer without deleting the committed files.
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
        idValue: String = "id-fixed",
        store: MediaStore = FakeMediaStore(),
        processor: MediaProcessor = FakeMediaProcessor(store as? FakeMediaStore ?: FakeMediaStore()),
        recorderFactory: () -> AudioRecorder = { error("not used") },
        testScope: TestScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
    ): MomentComposerViewModel {
        var counter = 0
        return MomentComposerViewModel(
            momentRepository = repo,
            clock = Clock { clockValue },
            idGenerator = IdGenerator { if (counter++ == 0) idValue else "$idValue-${counter}" },
            scope = testScope,
            mediaStore = store,
            mediaProcessor = processor,
            audioRecorderFactory = recorderFactory,
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
