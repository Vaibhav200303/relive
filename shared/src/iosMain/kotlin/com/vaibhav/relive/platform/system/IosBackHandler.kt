package com.vaibhav.relive.platform.system

import androidx.compose.runtime.Composable

@Composable
actual fun ReliveBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button; no-op.
}
