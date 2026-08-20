package com.vaibhav.relive.platform.media

import androidx.compose.runtime.Composable

/**
 * Composition-scoped picker handle. Implementations wire activity-result
 * launchers (Android) or present view controllers (iOS) under the hood.
 *
 * Cancellation returns an empty list. The resulting [RawMedia] paths live in
 * Relive-owned temporary storage; the user's library file is untouched.
 */
interface MediaPickerHandle {
    suspend fun pickImage(): List<RawMedia>
    suspend fun pickVideo(): List<RawMedia>
    suspend fun pickAudio(): List<RawMedia>
}

@Composable
expect fun rememberMediaPickerHandle(mediaStore: MediaStore): MediaPickerHandle
