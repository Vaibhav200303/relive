package com.vaibhav.relive.platform.media

import androidx.compose.runtime.Composable

/**
 * Full-screen camera surface. Presents a single camera experience where the
 * user can switch between Photo and Video before capturing, then returns to
 * the caller with a [RawMedia] in Relive-owned temporary storage.
 *
 * Capture is intentionally moderate quality — Relive normalizes the result
 * afterwards via [MediaProcessor]. Themes are never baked into the capture.
 */
@Composable
expect fun CameraCaptureSurface(
    mediaStore: MediaStore,
    onCaptured: (RawMedia) -> Unit,
    onCancel: () -> Unit,
)
