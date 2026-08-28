package com.vaibhav.relive.domain.model

/**
 * Appearance owned by one custom timeline.
 *
 * The values are persisted with the timeline and deliberately do not reference
 * [AppearancePreferences] or the app-level [ThemeReference]. Wallpaper rendering
 * and richer Moment styling are introduced by later redesign parts.
 */
data class TimelineAppearance(
    val wallpaper: TimelineWallpaper = TimelineWallpaper.WarmCream,
    val momentTheme: MomentTheme = MomentTheme.WarmTerracotta,
)

/** Selectable wallpaper identities for a timeline. */
enum class TimelineWallpaper {
    WarmCream,
    BlushPink,
    SageGreen,
    Lavender,
    PowderBlue,
    SoftPeach,
    MidnightNavy,
    Evergreen,
    MauveDusk,
    TerracottaGlow,
    CharcoalMist,
    CoralBloom,
    AquaSky,
    GoldenHour,
    VioletHaze,
    SapphireBlue,
}

/**
 * Timeline-owned identities corresponding to the already-supported visual
 * treatments. This stage adds no new selectable treatment.
 */
enum class MomentTheme {
    WarmTerracotta,
    Rose,
    Sage,
    Lavender,
    Ocean,
    Monochrome,
}
