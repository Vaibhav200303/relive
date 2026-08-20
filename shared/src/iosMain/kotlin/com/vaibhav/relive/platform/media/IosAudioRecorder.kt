package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaType
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
class IosAudioRecorder(private val store: IosMediaStore) : AudioRecorder {

    private val _state = MutableStateFlow(RecordingState())
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var recorder: AVAudioRecorder? = null
    private var tmpPath: String? = null
    private var startedAt: Double = 0.0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    override suspend fun start(): Result<Unit> = runCatching {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)

        val path = store.newTempPath("m4a")
        tmpPath = path
        val url = NSURL.fileURLWithPath(path)
        val settings = mapOf<Any?, Any?>(
            AVFormatIDKey to NSNumber(unsignedInt = kAudioFormatMPEG4AAC),
            AVSampleRateKey to NSNumber(int = 44_100),
            AVNumberOfChannelsKey to NSNumber(int = 1),
            AVEncoderBitRateKey to NSNumber(int = 64_000),
        )
        val rec = AVAudioRecorder(uRL = url, settings = settings, error = null)
        rec.meteringEnabled = true
        if (!rec.prepareToRecord()) error("prepareToRecord failed")
        if (!rec.record()) error("record failed")
        recorder = rec
        startedAt = NSDate().timeIntervalSince1970
        _state.value = RecordingState(isRecording = true)
        pollJob = scope.launch {
            while (recorder != null) {
                rec.updateMeters()
                val db = rec.averagePowerForChannel(0u).coerceIn(-60f, 0f)
                val amp = (db + 60f) / 60f
                val elapsed = ((NSDate().timeIntervalSince1970 - startedAt) * 1000.0).toLong()
                _state.value = _state.value.copy(
                    isRecording = true,
                    durationMs = elapsed,
                    amplitudes = (_state.value.amplitudes + amp).takeLast(WINDOW),
                )
                delay(60)
            }
        }
    }

    override suspend fun stop(): RawMedia {
        val rec = recorder ?: error("Not recording")
        val path = tmpPath ?: error("Missing tmp file")
        pollJob?.cancel(); pollJob = null
        rec.stop()
        recorder = null
        _state.value = RecordingState()
        return RawMedia(type = MediaType.Audio, sourcePath = path, ownedByRelive = true)
    }

    override suspend fun cancel() {
        pollJob?.cancel(); pollJob = null
        recorder?.stop()
        recorder = null
        tmpPath?.let { NSFileManager.defaultManager.removeItemAtPath(it, error = null) }
        tmpPath = null
        _state.value = RecordingState()
        scope.cancel()
    }

    private companion object {
        const val WINDOW = 96
    }
}
