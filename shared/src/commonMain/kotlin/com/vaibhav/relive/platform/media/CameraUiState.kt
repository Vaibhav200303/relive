package com.vaibhav.relive.platform.media

/**
 * Camera capture flow state. Live is the normal camera preview; PhotoReview /
 * VideoReview hold a just-captured item the user has not yet accepted. The
 * captured temp file lives on disk while in review — it becomes a persisted
 * Relive attachment only when [confirmReview] fires; retake/back deletes it.
 *
 * Photo and Video share the same review architecture: enter review after
 * capture, expose retake/confirm, delete the temp file on discard.
 */
sealed interface CameraUiState {
    data object Live : CameraUiState
    data class PhotoReview(val captured: RawMedia) : CameraUiState
    data class VideoReview(val captured: RawMedia) : CameraUiState
}

/** Enter review after a photo capture. */
fun enterPhotoReview(captured: RawMedia): CameraUiState.PhotoReview =
    CameraUiState.PhotoReview(captured)

/** Enter review after a video recording finalizes successfully. */
fun enterVideoReview(captured: RawMedia): CameraUiState.VideoReview =
    CameraUiState.VideoReview(captured)

/**
 * Discard the pending capture and return to Live. Deletes the temp file via
 * [deleteFile] so repeated retakes leave no orphan files on disk. No-op when
 * already Live.
 */
fun CameraUiState.discardReview(deleteFile: (String) -> Unit): CameraUiState {
    when (this) {
        is CameraUiState.PhotoReview -> deleteFile(captured.sourcePath)
        is CameraUiState.VideoReview -> deleteFile(captured.sourcePath)
        CameraUiState.Live -> Unit
    }
    return CameraUiState.Live
}

/**
 * Accept the pending capture. Returns the RawMedia to hand back to the caller
 * along with the next state (Live). No-op when not in review.
 */
fun CameraUiState.confirmReview(): Pair<CameraUiState, RawMedia?> = when (this) {
    is CameraUiState.PhotoReview -> CameraUiState.Live to captured
    is CameraUiState.VideoReview -> CameraUiState.Live to captured
    CameraUiState.Live -> this to null
}

/** True while the user is reviewing any captured item. */
val CameraUiState.isReviewing: Boolean
    get() = this is CameraUiState.PhotoReview || this is CameraUiState.VideoReview

/**
 * Which camera controls should stay interactive. Everything except the
 * retake/confirm pair is disabled during review — the camera is not rebound,
 * so mode/lens/flash/zoom would otherwise let the user desync UI from an
 * un-live preview.
 */
fun CameraUiState.liveControlsEnabled(): Boolean = !isReviewing
