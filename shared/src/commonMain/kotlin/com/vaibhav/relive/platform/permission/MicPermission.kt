package com.vaibhav.relive.platform.permission

import androidx.compose.runtime.Composable

/** Outcome of a runtime microphone-permission request. */
sealed interface MicPermissionResult {
    data object Granted : MicPermissionResult
    /** User denied but may be re-prompted. */
    data object Denied : MicPermissionResult
    /** User selected "Don't ask again" (or an equivalent OS-level block). */
    data object PermanentlyDenied : MicPermissionResult
}

/**
 * Runs the platform microphone-permission flow when [pending] transitions to
 * `true`. The result is delivered exactly once via [onResult]; the caller is
 * responsible for clearing `pending` after consumption.
 *
 * iOS: mic access is arbitrated by AVAudioSession at recorder init; this
 * adapter always resolves `Granted` so the recorder can attempt to start and
 * surface any denial as a recorder-level failure.
 */
@Composable
expect fun MicPermissionAdapter(
    pending: Boolean,
    onResult: (MicPermissionResult) -> Unit,
)
