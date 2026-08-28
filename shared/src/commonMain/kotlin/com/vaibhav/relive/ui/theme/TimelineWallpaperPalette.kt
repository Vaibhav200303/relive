package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.vaibhav.relive.domain.model.TimelineWallpaper

/** The two colors that belong to a timeline wallpaper, independent of the app theme. */
@Immutable
data class TimelineWallpaperPalette(
    val backgroundColor: Color,
    val doodleColor: Color,
)

/** Foreground colors selected for readable Moment content over the bundled wallpaper artwork. */
@Immutable
data class TimelineMomentForegroundColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentMuted: Color,
)

/**
 * The approved wallpaper assets are light in every appearance mode. Select the
 * stronger of the active semantic color and a dark editorial fallback so Moment
 * text retains contrast without modifying the wallpaper.
 */
internal fun timelineMomentForegroundColors(
    colors: ReliveColors,
    wallpaper: TimelineWallpaperPalette,
    isDark: Boolean,
): TimelineMomentForegroundColors {
    if (!isDark) {
        return TimelineMomentForegroundColors(
            textPrimary = colors.textPrimary,
            textSecondary = colors.textSecondary,
            textMuted = colors.textMuted,
            accent = colors.accent,
            accentMuted = colors.accentMuted,
        )
    }
    val textPrimary = higherContrastText(
        background = wallpaper.backgroundColor,
        candidates = listOf(colors.textPrimary, Color(0xFF1C1916)),
    )
    val accent = higherContrastText(
        background = wallpaper.backgroundColor,
        candidates = listOf(colors.accent, Color(0xFF6F4E37)),
    )
    return TimelineMomentForegroundColors(
        textPrimary = textPrimary,
        textSecondary = blend(textPrimary, wallpaper.backgroundColor, towardWeight = 0.18f),
        textMuted = blend(textPrimary, wallpaper.backgroundColor, towardWeight = 0.32f),
        accent = accent,
        accentMuted = blend(accent, wallpaper.backgroundColor, towardWeight = 0.18f),
    )
}

/** Resolves the saved wallpaper identity without consulting the global app palette. */
fun timelineWallpaperPalette(
    wallpaper: TimelineWallpaper,
    isDark: Boolean,
): TimelineWallpaperPalette = if (isDark) {
    when (wallpaper) {
        TimelineWallpaper.WarmCream -> TimelineWallpaperPalette(Color(0xFF1C1916), Color(0xFF3B332A))
        TimelineWallpaper.BlushPink -> TimelineWallpaperPalette(Color(0xFF201719), Color(0xFF4A3035))
        TimelineWallpaper.SageGreen -> TimelineWallpaperPalette(Color(0xFF181D17), Color(0xFF35402F))
        TimelineWallpaper.Lavender -> TimelineWallpaperPalette(Color(0xFF1C1921), Color(0xFF40364C))
        TimelineWallpaper.PowderBlue -> TimelineWallpaperPalette(Color(0xFF161B21), Color(0xFF304050))
        TimelineWallpaper.SoftPeach -> TimelineWallpaperPalette(Color(0xFF201915), Color(0xFF4B3328))
    }
} else {
    when (wallpaper) {
        TimelineWallpaper.WarmCream -> TimelineWallpaperPalette(Color(0xFFFAF3E9), Color(0xFFE7D5BF))
        TimelineWallpaper.BlushPink -> TimelineWallpaperPalette(Color(0xFFFDE7E7), Color(0xFFF0B8BA))
        TimelineWallpaper.SageGreen -> TimelineWallpaperPalette(Color(0xFFE4E9DD), Color(0xFFB8C5AE))
        TimelineWallpaper.Lavender -> TimelineWallpaperPalette(Color(0xFFEDE6F9), Color(0xFFCBBEE5))
        TimelineWallpaper.PowderBlue -> TimelineWallpaperPalette(Color(0xFFE1EEFA), Color(0xFFB1CFEA))
        TimelineWallpaper.SoftPeach -> TimelineWallpaperPalette(Color(0xFFFEEBE1), Color(0xFFF5B99B))
    }
}

private fun higherContrastText(background: Color, candidates: List<Color>): Color =
    candidates.maxBy { contrastRatio(it, background) }

private fun blend(from: Color, toward: Color, towardWeight: Float): Color = Color(
    red = from.red + (toward.red - from.red) * towardWeight,
    green = from.green + (toward.green - from.green) * towardWeight,
    blue = from.blue + (toward.blue - from.blue) * towardWeight,
)
