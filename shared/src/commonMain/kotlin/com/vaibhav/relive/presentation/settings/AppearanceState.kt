package com.vaibhav.relive.presentation.settings

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences

data class AppearanceState(
    val preferences: AppearancePreferences = AppearancePreferences(),
    val errorMessage: String? = null,
)

fun resolveDarkMode(mode: AppearanceMode, systemDark: Boolean): Boolean = when (mode) {
    AppearanceMode.System -> systemDark
    AppearanceMode.Light -> false
    AppearanceMode.Dark -> true
}
