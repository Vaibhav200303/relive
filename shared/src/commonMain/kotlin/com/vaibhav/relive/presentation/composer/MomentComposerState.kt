package com.vaibhav.relive.presentation.composer

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentValidation
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.Tag

/**
 * Immutable snapshot of the inline composer. Preserves user input verbatim;
 * the only trimming ever performed on title/content is inside the composer's
 * own validation check — the persisted values are the exact text the user
 * entered.
 *
 * Attachments are draft-scope: files are already normalized and live under
 * [com.vaibhav.relive.platform.media.MediaStore] once processing succeeds,
 * but ownership belongs to the composer until Keep Moment succeeds. Reset,
 * remove-attachment, and recording-cancel all delete their draft files. A
 * failed Keep Moment preserves the drafts so a retry does not recompress.
 */
data class MomentComposerState(
    val title: String = "",
    val content: String = "",
    val tags: List<Tag> = emptyList(),
    val pendingTagInput: String = "",
    val location: ReliveLocation? = null,
    val attachments: List<DraftAttachment> = emptyList(),
    val addMediaExpanded: Boolean = false,
    val recording: LiveRecording? = null,
    val pendingMediaAction: PendingMediaAction? = null,
    val pendingMicPermissionRequest: Boolean = false,
    val micPermission: MicPermissionUiState = MicPermissionUiState.Idle,
    val overlay: ComposerOverlay = ComposerOverlay.None,
    val saveState: SaveState = SaveState.Idle,
    val mediaError: String? = null,
) {
    val isSaving: Boolean get() = saveState is SaveState.Saving
    val isRecording: Boolean get() = recording != null
    val hasProcessingAttachments: Boolean
        get() = attachments.any { it.status is DraftMediaStatus.Pending || it.status is DraftMediaStatus.Processing }
    val hasFailedAttachments: Boolean
        get() = attachments.any { it.status is DraftMediaStatus.Failed }
    val allAttachmentsReady: Boolean
        get() = attachments.all { it.status is DraftMediaStatus.Ready }
}

/** Recoverable UI hint about microphone-permission status. */
sealed interface MicPermissionUiState {
    data object Idle : MicPermissionUiState

    /** User denied but may still be re-prompted by tapping Mic again. */
    data object Recoverable : MicPermissionUiState

    /** User selected "Don't ask again"; only Settings can recover. */
    data object SettingsRequired : MicPermissionUiState
}

/**
 * A composer-owned draft attachment. The slot is created immediately when the
 * user selects/captures the underlying media, keyed by a stable [draftId] so
 * the UI tile does not jump when processing completes. Processing status is
 * explicit — callers must never infer it from null fields.
 */
data class DraftAttachment(
    val draftId: String,
    val type: MediaType,
    val status: DraftMediaStatus,
)

/** Explicit lifecycle for a [DraftAttachment]. */
sealed interface DraftMediaStatus {
    /** Slot created; the processing coroutine has not started yet. */
    data object Pending : DraftMediaStatus

    /** Processing is running. Progress is intentionally indeterminate. */
    data object Processing : DraftMediaStatus

    /**
     * Processed. [storageRef] points at a real optimized file inside
     * `MediaStore`; the composer will delete that file if the user removes the
     * attachment or resets the composer before Keep Moment succeeds.
     */
    data class Ready(
        val storageRef: MediaStorageRef,
        val durationMs: Long? = null,
        val widthPx: Int? = null,
        val heightPx: Int? = null,
    ) : DraftMediaStatus

    /** Terminal failure. UI offers Retry / Remove. */
    data class Failed(val cause: Throwable) : DraftMediaStatus
}

/** Live recording snapshot mirrored from the [com.vaibhav.relive.platform.media.AudioRecorder]. */
data class LiveRecording(
    val durationMs: Long,
    val amplitudes: List<Float>,
)

/** One-shot side-effect the UI layer should perform via a platform handle. */
enum class PendingMediaAction { PickImage, PickVideo, PickAudio }

/** Full-screen overlays the composer can host. */
sealed interface ComposerOverlay {
    data object None : ComposerOverlay
    data object Camera : ComposerOverlay
    data object LibraryChoice : ComposerOverlay
}

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState

    /** Domain rejected the moment (see [MomentValidation]). */
    data class Invalid(val reasons: List<MomentValidation.Reason>) : SaveState

    /** Persistence threw while attempting to write the moment. */
    data class Failure(val cause: Throwable) : SaveState

    /** Keep tapped while attachments are still processing. */
    data object AwaitingProcessing : SaveState
}
