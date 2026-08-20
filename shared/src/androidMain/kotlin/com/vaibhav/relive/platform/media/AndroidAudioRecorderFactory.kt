package com.vaibhav.relive.platform.media

import android.content.Context

/**
 * Android factory relies on a process-scope [Context] provider set once at
 * container construction. Keeps the shared expect signature parameterless
 * beyond the media store while giving Android access to the platform APIs
 * it needs.
 */
private var contextProvider: (() -> Context)? = null

fun installAndroidMediaContext(provider: () -> Context) {
    contextProvider = provider
}

actual fun createAudioRecorder(mediaStore: MediaStore): AudioRecorder {
    val ctx = contextProvider?.invoke()
        ?: error("Android media context not installed. Call installAndroidMediaContext first.")
    val android = mediaStore as? AndroidMediaStore
        ?: error("createAudioRecorder requires AndroidMediaStore")
    return AndroidAudioRecorder(ctx, android)
}
