package com.vaibhav.relive.platform.system

import androidx.compose.runtime.Composable

/**
 * Multiplatform back-press adapter. Android maps to
 * `androidx.activity.compose.BackHandler`; iOS is a no-op — the platform has
 * no equivalent hardware/system Back key.
 */
@Composable
expect fun ReliveBackHandler(enabled: Boolean, onBack: () -> Unit)
