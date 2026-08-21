package com.vaibhav.relive.platform.media

import androidx.compose.runtime.Composable

/**
 * Full-screen camera surface. Presents a single camera experience where the
 * user can switch between Photo and Video before capturing, then returns to
 * the caller with a [RawMedia] in Relive-owned temporary storage.
 *
 * Capture is intentionally moderate quality — Relive normalizes the result
 * afterwards via [MediaProcessor]. Themes are never baked into the capture.
 *
 * [onOpenGallery] is invoked when the user taps the in-camera Gallery
 * control. The Android surface uses it to dismiss the camera and hand off to
 * the composer's Library sheet; the iOS surface ignores it because the
 * native picker already surfaces the on-device library.
 */
@Composable
expect fun CameraCaptureSurface(
    mediaStore: MediaStore,
    onCaptured: (RawMedia) -> Unit,
    onCancel: () -> Unit,
    onOpenGallery: () -> Unit,
)
