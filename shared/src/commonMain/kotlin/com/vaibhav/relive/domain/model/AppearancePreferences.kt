package com.vaibhav.relive.domain.model

enum class AppearanceMode {
    System,
    Light,
    Dark,
}

data class AppearancePreferences(
    val mode: AppearanceMode = AppearanceMode.System,
    /** Velvet Rose (the persisted Sunset identity) is the install-time palette. */
    val defaultTheme: ThemeReference = ThemeReference.Sunset,
    /** Appearance owned by the logical All timeline; it never changes the app palette. */
    val allTimelineAppearance: TimelineAppearance = TimelineAppearance(),
)
