package com.vaibhav.relive.domain.model

enum class AppearanceMode {
    System,
    Light,
    Dark,
}

data class AppearancePreferences(
    val mode: AppearanceMode = AppearanceMode.System,
    val defaultTheme: ThemeReference = ThemeReference.WarmJournal,
)
