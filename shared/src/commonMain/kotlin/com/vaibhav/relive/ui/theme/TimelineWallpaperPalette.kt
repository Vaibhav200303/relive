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
 * Moment copy sits directly on a timeline wallpaper, so resolve every foreground
 * against that wallpaper regardless of the app's appearance mode.
 */
internal fun timelineMomentForegroundColors(
    colors: ReliveColors,
    wallpaper: TimelineWallpaperPalette,
): TimelineMomentForegroundColors = TimelineMomentForegroundColors(
    textPrimary = readableTimelineForeground(colors.textPrimary, wallpaper.backgroundColor),
    textSecondary = readableTimelineForeground(colors.textSecondary, wallpaper.backgroundColor),
    textMuted = readableTimelineForeground(colors.textMuted, wallpaper.backgroundColor),
    accent = readableTimelineForeground(colors.accent, wallpaper.backgroundColor),
    accentMuted = readableTimelineForeground(colors.accentMuted, wallpaper.backgroundColor),
)

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
        TimelineWallpaper.MidnightNavy -> TimelineWallpaperPalette(Color(0xFF10233D), Color(0xFF6D87AC))
        TimelineWallpaper.Evergreen -> TimelineWallpaperPalette(Color(0xFF273B27), Color(0xFF8D9A78))
        TimelineWallpaper.MauveDusk -> TimelineWallpaperPalette(Color(0xFF584854), Color(0xFFD9B5C4))
        TimelineWallpaper.TerracottaGlow -> TimelineWallpaperPalette(Color(0xFF6A3326), Color(0xFFFFC395))
        TimelineWallpaper.CharcoalMist -> TimelineWallpaperPalette(Color(0xFF2F302E), Color(0xFF928B82))
        TimelineWallpaper.CoralBloom -> TimelineWallpaperPalette(Color(0xFF6A2F2B), Color(0xFFFFA090))
        TimelineWallpaper.AquaSky -> TimelineWallpaperPalette(Color(0xFF174D56), Color(0xFF7DECF3))
        TimelineWallpaper.GoldenHour -> TimelineWallpaperPalette(Color(0xFF63450E), Color(0xFFFFE18A))
        TimelineWallpaper.VioletHaze -> TimelineWallpaperPalette(Color(0xFF44206F), Color(0xFFD9B6FF))
        TimelineWallpaper.SapphireBlue -> TimelineWallpaperPalette(Color(0xFF1B347E), Color(0xFFA8C5FF))
    }
} else {
    when (wallpaper) {
        TimelineWallpaper.WarmCream -> TimelineWallpaperPalette(Color(0xFFFAF3E9), Color(0xFFE7D5BF))
        TimelineWallpaper.BlushPink -> TimelineWallpaperPalette(Color(0xFFFDE7E7), Color(0xFFF0B8BA))
        TimelineWallpaper.SageGreen -> TimelineWallpaperPalette(Color(0xFFE4E9DD), Color(0xFFB8C5AE))
        TimelineWallpaper.Lavender -> TimelineWallpaperPalette(Color(0xFFEDE6F9), Color(0xFFCBBEE5))
        TimelineWallpaper.PowderBlue -> TimelineWallpaperPalette(Color(0xFFE1EEFA), Color(0xFFB1CFEA))
        TimelineWallpaper.SoftPeach -> TimelineWallpaperPalette(Color(0xFFFEEBE1), Color(0xFFF5B99B))
        TimelineWallpaper.MidnightNavy -> TimelineWallpaperPalette(Color(0xFF10233D), Color(0xFF6D87AC))
        TimelineWallpaper.Evergreen -> TimelineWallpaperPalette(Color(0xFF273B27), Color(0xFF8D9A78))
        TimelineWallpaper.MauveDusk -> TimelineWallpaperPalette(Color(0xFF6B5362), Color(0xFFD9B5C4))
        TimelineWallpaper.TerracottaGlow -> TimelineWallpaperPalette(Color(0xFF87432A), Color(0xFFFFC395))
        TimelineWallpaper.CharcoalMist -> TimelineWallpaperPalette(Color(0xFF2F302E), Color(0xFF928B82))
        TimelineWallpaper.CoralBloom -> TimelineWallpaperPalette(Color(0xFFFF856F), Color(0xFFD64A3E))
        TimelineWallpaper.AquaSky -> TimelineWallpaperPalette(Color(0xFF6DDDE8), Color(0xFF2FB2CD))
        TimelineWallpaper.GoldenHour -> TimelineWallpaperPalette(Color(0xFFFFD34D), Color(0xFFE99A1D))
        TimelineWallpaper.VioletHaze -> TimelineWallpaperPalette(Color(0xFFA85BEA), Color(0xFF6F37BA))
        TimelineWallpaper.SapphireBlue -> TimelineWallpaperPalette(Color(0xFF5D90FF), Color(0xFF3557D6))
    }
}

private fun higherContrastText(background: Color, candidates: List<Color>): Color =
    candidates.maxBy { contrastRatio(it, background) }

private fun readableTimelineForeground(semanticColor: Color, background: Color): Color {
    val opaqueSemanticColor = semanticColor.copy(alpha = 1f)
    return if (contrastRatio(opaqueSemanticColor, background) >= 4.5f) {
        opaqueSemanticColor
    } else {
        higherContrastText(background, listOf(opaqueSemanticColor, Color.Black, Color.White))
    }
}
