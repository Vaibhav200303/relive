package com.vaibhav.relive.presentation.composer

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
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
) {

    private val _state = MutableStateFlow(MomentComposerState())
    val state: StateFlow<MomentComposerState> = _state.asStateFlow()

    private var recorder: AudioRecorder? = null
    private var recorderJob: Job? = null

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

    /**
     * Entry point for the Mic tap. Guards against re-entry and delegates the
     * actual permission dialog to the platform adapter observed by the UI.
     */
    fun requestMicPermission() {
        val current = _state.value
        if (current.isRecording || current.pendingMicPermissionRequest) return
        _state.update {
            it.copy(pendingMicPermissionRequest = true, micPermission = MicPermissionUiState.Idle, mediaError = null)
        }
    }

    /**
     * Called by the platform mic-permission adapter with the outcome of a
     * requested prompt. Clears the pending flag and either starts recording
     * or surfaces the appropriate recoverable UI state.
     */
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

    /** Called by the UI layer once a platform handle returns raw media. */
    fun processRaw(raw: RawMedia) {
        scope.launch {
            try {
                val processed = mediaProcessor.process(raw)
                _state.update { current ->
                    current.copy(
                        attachments = current.attachments + DraftAttachment(
                            storageRef = processed.storageRef,
                            type = processed.type,
                            durationMs = processed.durationMs,
                            widthPx = processed.widthPx,
                            heightPx = processed.heightPx,
                        ),
                        mediaError = null,
                    )
                }
            } catch (t: Throwable) {
                setMediaError("Couldn't process media.")
            }
        }
    }

    fun processRawBatch(items: List<RawMedia>) {
        items.forEach { processRaw(it) }
    }

    fun removeAttachment(ref: com.vaibhav.relive.domain.model.MediaStorageRef) {
        val existing = _state.value.attachments.firstOrNull { it.storageRef == ref } ?: return
        _state.update { it.copy(attachments = it.attachments.filterNot { a -> a.storageRef == ref }) }
        runCatching { mediaStore.delete(existing.storageRef) }
    }

    // -- reset / keep -----------------------------------------------------

    fun reset() {
        val drafts = _state.value.attachments
        cancelRecording()
        _state.value = MomentComposerState()
        drafts.forEach { runCatching { mediaStore.delete(it.storageRef) } }
    }

    fun keepMoment() {
        val snapshot = _state.value
        if (snapshot.isSaving) return
        if (snapshot.isRecording) {
            setMediaError("Stop the recording before keeping this moment.")
            return
        }

        val now = clock.now()
        val attachments = snapshot.attachments.mapIndexed { index, draft ->
            MediaAttachment(
                id = MediaAttachmentId(idGenerator.newId()),
                type = draft.type,
                storageRef = draft.storageRef,
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
            attachments = attachments,
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
                // Committed: files stay, composer resets without deleting them.
                _state.value = MomentComposerState()
            } catch (t: Throwable) {
                // Retain draft files so retry does not recompress / duplicate.
                _state.update { it.copy(saveState = SaveState.Failure(t)) }
            }
        }
    }
}

private fun SaveState.clearedOnEdit(): SaveState = when (this) {
    is SaveState.Invalid, is SaveState.Failure -> SaveState.Idle
    SaveState.Idle, SaveState.Saving -> this
}
