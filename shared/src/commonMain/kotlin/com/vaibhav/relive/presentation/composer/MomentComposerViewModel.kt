package com.vaibhav.relive.presentation.composer

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MomentValidation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.media.AudioRecorder
import com.vaibhav.relive.platform.media.MediaProcessor
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RawMedia
import com.vaibhav.relive.platform.media.RecordingState
import com.vaibhav.relive.platform.media.createAudioRecorder
import com.vaibhav.relive.platform.permission.MicPermissionResult
import com.vaibhav.relive.platform.system.openAppSettings as platformOpenAppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Inline composer state holder. Extends the Phase 3 composer with media
 * capture (mic, camera, library) governed by the temporary → processed →
 * committed lifecycle described in ADR-0018.
 *
 * The VM never touches platform APIs directly — it delegates to
 * [MediaStore] / [MediaProcessor] and the [AudioRecorder] handed in by the
 * platform composition. Camera and picker flows are driven through
 * [PendingMediaAction] / [ComposerOverlay] state that the composer UI
 * observes and services with platform-specific composables.
 */
class MomentComposerViewModel(
    private val momentRepository: MomentRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val scope: CoroutineScope,
    private val mediaStore: MediaStore,
    private val mediaProcessor: MediaProcessor,
    private val audioRecorderFactory: () -> AudioRecorder = { createAudioRecorder(mediaStore) },
    processingConcurrency: Int = DEFAULT_PROCESSING_CONCURRENCY,
) {

    private val _state = MutableStateFlow(MomentComposerState())
    val state: StateFlow<MomentComposerState> = _state.asStateFlow()

    private var recorder: AudioRecorder? = null
    private var recorderJob: Job? = null

    // Bounds concurrent processing so several large videos do not run in
    // parallel and blow memory. Small permit count keeps UI responsive.
    private val processingLimiter = Semaphore(permits = processingConcurrency.coerceAtLeast(1))

    // Keeps the original RawMedia around per draftId so Retry does not need
    // the picker or camera again.
    private val rawByDraftId = mutableMapOf<String, RawMedia>()

    // -- text/tag ---------------------------------------------------------

    fun updateTitle(value: String) {
        _state.update { it.copy(title = value, saveState = it.saveState.clearedOnEdit()) }
    }

    fun updateContent(value: String) {
        _state.update { it.copy(content = value, saveState = it.saveState.clearedOnEdit()) }
    }

    fun updatePendingTagInput(value: String) {
        _state.update { it.copy(pendingTagInput = value) }
    }

    fun commitPendingTag() {
        _state.update { current ->
            val candidate = Tag.ofOrNull(current.pendingTagInput) ?: return@update current.copy(
                pendingTagInput = "",
            )
            val tags = if (current.tags.contains(candidate)) current.tags else current.tags + candidate
            current.copy(tags = tags, pendingTagInput = "")
        }
    }

    fun addTag(raw: String) {
        val candidate = Tag.ofOrNull(raw) ?: return
        _state.update { current ->
            if (current.tags.contains(candidate)) current else current.copy(tags = current.tags + candidate)
        }
    }

    fun removeTag(tag: Tag) {
        _state.update { it.copy(tags = it.tags.filterNot { existing -> existing == tag }) }
    }

    // -- add-media surface ------------------------------------------------

    fun toggleAddMedia() {
        _state.update { it.copy(addMediaExpanded = !it.addMediaExpanded, mediaError = null) }
    }

    fun openCamera() {
        _state.update { it.copy(overlay = ComposerOverlay.Camera, addMediaExpanded = false, mediaError = null) }
    }

    fun openLibraryChoice() {
        _state.update { it.copy(overlay = ComposerOverlay.LibraryChoice, addMediaExpanded = false, mediaError = null) }
    }

    fun dismissOverlay() {
        _state.update { it.copy(overlay = ComposerOverlay.None) }
    }

    fun requestPick(type: MediaType) {
        val action = when (type) {
            MediaType.Image -> PendingMediaAction.PickImage
            MediaType.Video -> PendingMediaAction.PickVideo
            MediaType.Audio -> PendingMediaAction.PickAudio
        }
        _state.update { it.copy(pendingMediaAction = action, overlay = ComposerOverlay.None) }
    }

    fun clearPendingMediaAction() {
        _state.update { it.copy(pendingMediaAction = null) }
    }

    fun setMediaError(message: String?) {
        _state.update { it.copy(mediaError = message) }
    }

    // -- mic permission ---------------------------------------------------

    fun requestMicPermission() {
        val current = _state.value
        if (current.isRecording || current.pendingMicPermissionRequest) return
        _state.update {
            it.copy(pendingMicPermissionRequest = true, micPermission = MicPermissionUiState.Idle, mediaError = null)
        }
    }

    fun onMicPermissionResult(result: MicPermissionResult) {
        _state.update { it.copy(pendingMicPermissionRequest = false) }
        when (result) {
            MicPermissionResult.Granted -> {
                _state.update { it.copy(micPermission = MicPermissionUiState.Idle) }
                startRecording()
            }
            MicPermissionResult.Denied ->
                _state.update { it.copy(micPermission = MicPermissionUiState.Recoverable) }
            MicPermissionResult.PermanentlyDenied ->
                _state.update { it.copy(micPermission = MicPermissionUiState.SettingsRequired) }
        }
    }

    fun dismissMicPermissionMessage() {
        _state.update { it.copy(micPermission = MicPermissionUiState.Idle) }
    }

    fun openAppSettings() {
        platformOpenAppSettings()
    }

    // -- recording --------------------------------------------------------

    fun startRecording() {
        if (_state.value.isRecording) return
        val rec = audioRecorderFactory().also { recorder = it }
        scope.launch {
            val result = rec.start()
            if (result.isFailure) {
                recorder = null
                setMediaError("Couldn't start recording.")
                return@launch
            }
            _state.update { it.copy(recording = LiveRecording(0L, emptyList()), mediaError = null) }
            recorderJob = launch {
                rec.state.collect { s: RecordingState ->
                    if (s.isRecording) {
                        _state.update {
                            it.copy(recording = LiveRecording(s.durationMs, s.amplitudes))
                        }
                    }
                }
            }
        }
    }

    fun stopRecording() {
        val rec = recorder ?: return
        scope.launch {
            recorderJob?.cancel()
            recorderJob = null
            try {
                val raw = rec.stop()
                processRaw(raw)
            } catch (t: Throwable) {
                setMediaError("Couldn't finish recording.")
            } finally {
                recorder = null
                _state.update { it.copy(recording = null) }
            }
        }
    }

    fun cancelRecording() {
        val rec = recorder ?: return
        scope.launch {
            recorderJob?.cancel()
            recorderJob = null
            try { rec.cancel() } catch (_: Throwable) { /* best-effort */ }
            recorder = null
            _state.update { it.copy(recording = null) }
        }
    }

    // -- raw → processed --------------------------------------------------

    /**
     * Immediately creates a placeholder draft slot and enqueues processing.
     * The placeholder is visible on the composer as soon as this returns so
     * the user always sees that something is happening.
     */
    fun processRaw(raw: RawMedia) {
        val draftId = idGenerator.newId()
        appendDraftPlaceholder(draftId, raw)
        launchProcessing(draftId, raw)
    }

    /**
     * Batch entry point for picker results. All placeholders are appended in
     * the caller-supplied order before any processing starts, so a 5-item
     * selection produces 5 tiles up front.
     */
    fun processRawBatch(items: List<RawMedia>) {
        if (items.isEmpty()) return
        val enqueued = items.map { raw ->
            val draftId = idGenerator.newId()
            appendDraftPlaceholder(draftId, raw)
            draftId to raw
        }
        enqueued.forEach { (draftId, raw) -> launchProcessing(draftId, raw) }
    }

    private fun appendDraftPlaceholder(draftId: String, raw: RawMedia) {
        rawByDraftId[draftId] = raw
        _state.update { current ->
            current.copy(
                attachments = current.attachments + DraftAttachment(
                    draftId = draftId,
                    type = raw.type,
                    status = DraftMediaStatus.Pending,
                    sourcePath = raw.sourcePath,
                ),
                mediaError = null,
                saveState = current.saveState.clearedOnAttachmentChange(),
            )
        }
    }

    private fun launchProcessing(draftId: String, raw: RawMedia) {
        scope.launch {
            processingLimiter.withPermit {
                if (_state.value.attachments.none { it.draftId == draftId }) {
                    return@withPermit // user removed the slot before we started
                }
                updateStatus(draftId) { DraftMediaStatus.Processing }
                try {
                    val processed = mediaProcessor.process(raw)
                    updateStatus(draftId) {
                        DraftMediaStatus.Ready(
                            storageRef = processed.storageRef,
                            durationMs = processed.durationMs,
                            widthPx = processed.widthPx,
                            heightPx = processed.heightPx,
                        )
                    }
                } catch (t: Throwable) {
                    updateStatus(draftId) { DraftMediaStatus.Failed(t) }
                }
            }
        }
    }

    private inline fun updateStatus(draftId: String, transform: (DraftMediaStatus) -> DraftMediaStatus) {
        _state.update { current ->
            val idx = current.attachments.indexOfFirst { it.draftId == draftId }
            if (idx < 0) return@update current
            val existing = current.attachments[idx]
            val next = existing.copy(status = transform(existing.status))
            current.copy(attachments = current.attachments.toMutableList().also { it[idx] = next })
        }
    }

    /** Retry a failed attachment. Idempotent for non-failed slots. */
    fun retryAttachment(draftId: String) {
        val slot = _state.value.attachments.firstOrNull { it.draftId == draftId } ?: return
        if (slot.status !is DraftMediaStatus.Failed) return
        val raw = rawByDraftId[draftId] ?: return
        updateStatus(draftId) { DraftMediaStatus.Pending }
        launchProcessing(draftId, raw)
    }

    fun removeAttachment(draftId: String) {
        val existing = _state.value.attachments.firstOrNull { it.draftId == draftId } ?: return
        _state.update {
            it.copy(
                attachments = it.attachments.filterNot { a -> a.draftId == draftId },
                saveState = it.saveState.clearedOnAttachmentChange(),
            )
        }
        rawByDraftId.remove(draftId)
        val status = existing.status
        if (status is DraftMediaStatus.Ready) {
            runCatching { mediaStore.delete(status.storageRef) }
        }
    }

    /** Legacy path — kept so tile code that already holds a ref can still remove. */
    fun removeAttachmentByRef(ref: MediaStorageRef) {
        val slot = _state.value.attachments.firstOrNull { att ->
            val s = att.status
            s is DraftMediaStatus.Ready && s.storageRef == ref
        } ?: return
        removeAttachment(slot.draftId)
    }

    // -- reset / keep -----------------------------------------------------

    fun reset() {
        val drafts = _state.value.attachments
        cancelRecording()
        rawByDraftId.clear()
        _state.value = MomentComposerState()
        drafts.forEach { att ->
            val s = att.status
            if (s is DraftMediaStatus.Ready) runCatching { mediaStore.delete(s.storageRef) }
        }
    }

    fun keepMoment() {
        val snapshot = _state.value
        if (snapshot.isSaving) return
        if (snapshot.isRecording) {
            setMediaError("Stop the recording before keeping this moment.")
            return
        }
        if (snapshot.hasProcessingAttachments) {
            _state.update { it.copy(saveState = SaveState.AwaitingProcessing) }
            return
        }
        if (snapshot.hasFailedAttachments) {
            setMediaError("Retry or remove failed media before keeping.")
            return
        }

        val now = clock.now()
        val readyAttachments = snapshot.attachments.mapIndexedNotNull { index, draft ->
            val status = draft.status as? DraftMediaStatus.Ready ?: return@mapIndexedNotNull null
            MediaAttachment(
                id = MediaAttachmentId(idGenerator.newId()),
                type = draft.type,
                storageRef = status.storageRef,
                sortIndex = index,
            )
        }
        val moment = Moment(
            id = MomentId(idGenerator.newId()),
            createdAt = now,
            title = snapshot.title,
            content = snapshot.content,
            location = snapshot.location,
            tags = snapshot.tags,
            attachments = readyAttachments,
        )

        when (val result = MomentValidation.validate(moment)) {
            is MomentValidation.Result.Invalid -> {
                _state.update { it.copy(saveState = SaveState.Invalid(result.reasons)) }
                return
            }
            MomentValidation.Result.Ok -> Unit
        }

        _state.update { it.copy(saveState = SaveState.Saving) }

        scope.launch {
            try {
                momentRepository.insert(moment, emptySet())
                rawByDraftId.clear()
                _state.value = MomentComposerState()
            } catch (t: Throwable) {
                _state.update { it.copy(saveState = SaveState.Failure(t)) }
            }
        }
    }

    companion object {
        /** Bounded concurrency so multiple large videos never all run at once. */
        const val DEFAULT_PROCESSING_CONCURRENCY: Int = 2
    }
}

private fun SaveState.clearedOnEdit(): SaveState = when (this) {
    is SaveState.Invalid, is SaveState.Failure, SaveState.AwaitingProcessing -> SaveState.Idle
    SaveState.Idle, SaveState.Saving -> this
}

private fun SaveState.clearedOnAttachmentChange(): SaveState = when (this) {
    SaveState.AwaitingProcessing, is SaveState.Invalid -> SaveState.Idle
    else -> this
}
