package com.vaibhav.relive.presentation.share

import com.vaibhav.relive.platform.share.IncomingSharePayload
import com.vaibhav.relive.platform.share.IncomingShareState

/** What choosing a timeline on the external-share picker can do, given the request's readiness. */
sealed interface SharePickerTapOutcome {
    /** The payload is ready: hand it to the chosen timeline's composer now. */
    data class Commit(val payload: IncomingSharePayload) : SharePickerTapOutcome

    /** Still being read at the platform edge: hold the choice so the tap is not lost. */
    data object Hold : SharePickerTapOutcome

    /** Nothing left to keep — a rejected, cancelled, or already-claimed request drops the choice. */
    data object Drop : SharePickerTapOutcome
}

/**
 * The picker is the share's first screen and is on it from the first frame, so a timeline can be
 * chosen while the payload is still being copied out of the source app. A tap made then is held
 * rather than ignored, and commits the moment the payload lands.
 */
fun resolveSharePickerTap(state: IncomingShareState): SharePickerTapOutcome = when (state) {
    is IncomingShareState.Ready -> SharePickerTapOutcome.Commit(state.payload)
    IncomingShareState.Reading -> SharePickerTapOutcome.Hold
    IncomingShareState.Idle, is IncomingShareState.Error -> SharePickerTapOutcome.Drop
}
