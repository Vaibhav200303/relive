package com.vaibhav.relive.presentation.settings

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.ThemeReference

data class AppearanceState(
    val preferences: AppearancePreferences = AppearancePreferences(),
    val errorMessage: String? = null,
)

fun resolveDarkMode(mode: AppearanceMode, systemDark: Boolean): Boolean = when (mode) {
    AppearanceMode.System -> systemDark
    AppearanceMode.Light -> false
    AppearanceMode.Dark -> true
}

fun resolveTimelineTheme(
    override: ThemeReference?,
    global: ThemeReference,
): ThemeReference = override ?: global
