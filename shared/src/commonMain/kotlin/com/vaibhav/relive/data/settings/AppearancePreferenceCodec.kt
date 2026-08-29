package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.model.MomentTheme

internal const val APPEARANCE_MODE_KEY: String = "relive.appearance.mode"
internal const val APPEARANCE_THEME_KEY: String = "relive.appearance.palette"
internal const val ALL_TIMELINE_WALLPAPER_KEY: String = "relive.timeline.all.wallpaper"
internal const val ALL_TIMELINE_MOMENT_THEME_KEY: String = "relive.timeline.all.moment_theme"

internal fun AppearanceMode.encodePreference(): String = when (this) {
    AppearanceMode.System -> "system"
    AppearanceMode.Light -> "light"
    AppearanceMode.Dark -> "dark"
}

internal fun ThemeReference.encodePreference(): String = when (this) {
    ThemeReference.InkLilac -> "ink_lilac"
    ThemeReference.TealSaffron -> "teal_saffron"
    ThemeReference.EmberAqua -> "ember_aqua"
    ThemeReference.PlumGold -> "plum_gold"
    ThemeReference.RoseSage -> "rose_sage"
}

internal fun TimelineWallpaper.encodePreference(): String = name
internal fun MomentTheme.encodePreference(): String = name

internal fun decodeAppearancePreferences(
    mode: String?,
    theme: String?,
    allTimelineWallpaper: String? = null,
    allTimelineMomentTheme: String? = null,
): AppearancePreferences =
    AppearancePreferences(
        mode = when (mode) {
            "light" -> AppearanceMode.Light
            "dark" -> AppearanceMode.Dark
            else -> AppearanceMode.System
        },
        defaultTheme = when (theme) {
            "teal_saffron" -> ThemeReference.TealSaffron
            "ember_aqua" -> ThemeReference.EmberAqua
            "plum_gold" -> ThemeReference.PlumGold
            "rose_sage" -> ThemeReference.RoseSage
            // Retired palette keys and the default both resolve to Ink & Lilac.
            else -> ThemeReference.InkLilac
        },
        allTimelineAppearance = TimelineAppearance(
            wallpaper = TimelineWallpaper.entries.firstOrNull { it.name == allTimelineWallpaper }
                ?: TimelineWallpaper.WarmCream,
            momentTheme = MomentTheme.entries.firstOrNull { it.name == allTimelineMomentTheme }
                ?: MomentTheme.WarmTerracotta,
        ),
    )
