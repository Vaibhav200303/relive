package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.ThemeReference

internal const val APPEARANCE_MODE_KEY: String = "relive.appearance.mode"
internal const val APPEARANCE_THEME_KEY: String = "relive.appearance.palette"

internal fun AppearanceMode.encodePreference(): String = when (this) {
    AppearanceMode.System -> "system"
    AppearanceMode.Light -> "light"
    AppearanceMode.Dark -> "dark"
}

internal fun ThemeReference.encodePreference(): String = when (this) {
    ThemeReference.WarmJournal -> "original"
    ThemeReference.Evergreen -> "evergreen"
    ThemeReference.LilacDusk -> "lilac_dusk"
    ThemeReference.CrimsonKeepsake -> "crimson_keepsake"
    ThemeReference.BlueHour -> "blue_hour"
    ThemeReference.Rosewood -> "rosewood"
}

internal fun decodeAppearancePreferences(mode: String?, theme: String?): AppearancePreferences =
    AppearancePreferences(
        mode = when (mode) {
            "light" -> AppearanceMode.Light
            "dark" -> AppearanceMode.Dark
            else -> AppearanceMode.System
        },
        defaultTheme = when (theme) {
            "evergreen" -> ThemeReference.Evergreen
            "lilac_dusk" -> ThemeReference.LilacDusk
            "crimson_keepsake" -> ThemeReference.CrimsonKeepsake
            "blue_hour" -> ThemeReference.BlueHour
            "rosewood" -> ThemeReference.Rosewood
            else -> ThemeReference.WarmJournal
        },
    )
