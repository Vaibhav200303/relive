package com.vaibhav.relive.platform.media

actual fun createAudioRecorder(mediaStore: MediaStore): AudioRecorder {
    val ios = mediaStore as? IosMediaStore
        ?: error("createAudioRecorder requires IosMediaStore")
    return IosAudioRecorder(ios)
}
