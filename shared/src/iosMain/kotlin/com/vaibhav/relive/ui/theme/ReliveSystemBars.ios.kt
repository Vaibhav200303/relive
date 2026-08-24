package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

internal object IosStatusBarAppearance {
    var update: ((Boolean) -> Unit)? = null
}

@Composable
actual fun ApplyReliveSystemBars(tokens: ReliveThemeTokens) {
    SideEffect {
        IosStatusBarAppearance.update?.invoke(tokens.systemBarIconsDark)
    }
}
