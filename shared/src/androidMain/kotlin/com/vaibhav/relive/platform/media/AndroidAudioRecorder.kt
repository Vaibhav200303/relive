package com.vaibhav.relive.platform.media

import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.content.Context
import com.vaibhav.relive.domain.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Records AAC/M4A at a voice-appropriate bitrate. Amplitudes are sampled
 * from [MediaRecorder.getMaxAmplitude] every [SAMPLE_INTERVAL_MS] and kept
 * as a rolling window so recomposition costs stay bounded.
 */
class AndroidAudioRecorder(
    private val context: Context,
    private val store: AndroidMediaStore,
) : AudioRecorder {

    private val _state = MutableStateFlow(RecordingState())
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var tmpFile: File? = null
    private var startedAt: Long = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    override suspend fun start(): Result<Unit> = runCatching {
        val out = store.newTempFile("relive-rec-", ".m4a")
        tmpFile = out
        val rec = createRecorder(out)
        rec.prepare()
        rec.start()
        recorder = rec
        startedAt = SystemClock.elapsedRealtime()
        _state.value = RecordingState(isRecording = true, durationMs = 0L, amplitudes = emptyList())
        pollJob = scope.launch {
            while (recorder != null) {
                val amp = try { recorder?.maxAmplitude ?: 0 } catch (_: Throwable) { 0 }
                val normalized = (amp / MAX_AMP).coerceIn(0f, 1f)
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                _state.value = _state.value.copy(
                    isRecording = true,
                    durationMs = elapsed,
                    amplitudes = (_state.value.amplitudes + normalized).takeLast(WINDOW),
                )
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    private fun createRecorder(dest: File): MediaRecorder {
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(BITRATE)
        rec.setAudioSamplingRate(SAMPLE_RATE)
        rec.setOutputFile(dest.absolutePath)
        return rec
    }

    override suspend fun stop(): RawMedia {
        val rec = recorder ?: error("Not recording")
        val file = tmpFile ?: error("Missing tmp file")
        pollJob?.cancel(); pollJob = null
        try { rec.stop() } catch (_: Throwable) { /* short recordings can throw */ }
        rec.release()
        recorder = null
        _state.value = RecordingState()
        return RawMedia(type = MediaType.Audio, sourcePath = file.absolutePath, ownedByRelive = true)
    }

    override suspend fun cancel() {
        pollJob?.cancel(); pollJob = null
        try { recorder?.stop() } catch (_: Throwable) {}
        recorder?.release()
        recorder = null
        tmpFile?.delete()
        tmpFile = null
        _state.value = RecordingState()
        scope.cancel()
    }

    private companion object {
        const val SAMPLE_INTERVAL_MS = 60L
        const val WINDOW = 96
        const val BITRATE = 64_000
        const val SAMPLE_RATE = 44_100
        const val MAX_AMP = 20_000f
    }
}
