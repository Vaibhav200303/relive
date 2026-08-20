package com.vaibhav.relive.platform.system

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun ReliveBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
