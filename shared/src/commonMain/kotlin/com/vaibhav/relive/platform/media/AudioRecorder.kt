package com.vaibhav.relive.platform.media

import kotlinx.coroutines.flow.StateFlow

/**
 * Live audio recorder. Emits real microphone amplitudes and duration while
 * recording; produces a normalized [RawMedia] on stop, or discards its
 * temporary output on cancel.
 */
interface AudioRecorder {

    val state: StateFlow<RecordingState>

    /**
     * Begins recording to a temporary Relive-owned file. Returns a [Result]
     * so the caller can surface permission/device failure without a throw.
     */
    suspend fun start(): Result<Unit>

    /** Stops recording and returns the resulting raw file. */
    suspend fun stop(): RawMedia

    /** Cancels recording and deletes the in-progress temporary file. */
    suspend fun cancel()
}

data class RecordingState(
    val isRecording: Boolean = false,
    val durationMs: Long = 0L,
    val amplitudes: List<Float> = emptyList(),
)
