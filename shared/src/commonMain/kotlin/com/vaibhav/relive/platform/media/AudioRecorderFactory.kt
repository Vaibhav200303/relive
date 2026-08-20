package com.vaibhav.relive.platform.media

/**
 * Constructs a fresh [AudioRecorder] backed by the platform microphone.
 * A recorder is single-use — one start/stop or start/cancel cycle per
 * instance.
 */
expect fun createAudioRecorder(mediaStore: MediaStore): AudioRecorder
