package com.vaibhav.relive.platform.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun MicPermissionAdapter(
    pending: Boolean,
    onResult: (MicPermissionResult) -> Unit,
) {
    LaunchedEffect(pending) {
        if (pending) onResult(MicPermissionResult.Granted)
    }
}
