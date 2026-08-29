package com.vaibhav.relive.ui.theme

import androidx.compose.ui.graphics.Color
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.ui.components.timeline.MemoryDoodlePattern
import com.vaibhav.relive.ui.components.timeline.timelineWallpaperVisual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TimelineWallpaperPaletteTest {
    @Test
    fun everyWallpaperResolvesToItsApprovedLightPalette() {
        assertEquals(TimelineWallpaperPalette(Color(0xFFFAF3E9), Color(0xFFE7D5BF)), timelineWallpaperPalette(TimelineWallpaper.WarmCream, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFFDE7E7), Color(0xFFF0B8BA)), timelineWallpaperPalette(TimelineWallpaper.BlushPink, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFE4E9DD), Color(0xFFB8C5AE)), timelineWallpaperPalette(TimelineWallpaper.SageGreen, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFEDE6F9), Color(0xFFCBBEE5)), timelineWallpaperPalette(TimelineWallpaper.Lavender, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFE1EEFA), Color(0xFFB1CFEA)), timelineWallpaperPalette(TimelineWallpaper.PowderBlue, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFFEEBE1), Color(0xFFF5B99B)), timelineWallpaperPalette(TimelineWallpaper.SoftPeach, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFF10233D), Color(0xFF6D87AC)), timelineWallpaperPalette(TimelineWallpaper.MidnightNavy, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFF273B27), Color(0xFF8D9A78)), timelineWallpaperPalette(TimelineWallpaper.Evergreen, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFF6B5362), Color(0xFFD9B5C4)), timelineWallpaperPalette(TimelineWallpaper.MauveDusk, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFF87432A), Color(0xFFFFC395)), timelineWallpaperPalette(TimelineWallpaper.TerracottaGlow, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFF2F302E), Color(0xFF928B82)), timelineWallpaperPalette(TimelineWallpaper.CharcoalMist, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFFF856F), Color(0xFFD64A3E)), timelineWallpaperPalette(TimelineWallpaper.CoralBloom, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFF6DDDE8), Color(0xFF2FB2CD)), timelineWallpaperPalette(TimelineWallpaper.AquaSky, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFFFD34D), Color(0xFFE99A1D)), timelineWallpaperPalette(TimelineWallpaper.GoldenHour, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFA85BEA), Color(0xFF6F37BA)), timelineWallpaperPalette(TimelineWallpaper.VioletHaze, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFF5D90FF), Color(0xFF3557D6)), timelineWallpaperPalette(TimelineWallpaper.SapphireBlue, false))
    }

    @Test
    fun everyWallpaperHasAReadableRestrainedDarkPalette() {
        val expected = mapOf(
            TimelineWallpaper.WarmCream to TimelineWallpaperPalette(Color(0xFF1C1916), Color(0xFF3B332A)),
            TimelineWallpaper.BlushPink to TimelineWallpaperPalette(Color(0xFF201719), Color(0xFF4A3035)),
            TimelineWallpaper.SageGreen to TimelineWallpaperPalette(Color(0xFF181D17), Color(0xFF35402F)),
            TimelineWallpaper.Lavender to TimelineWallpaperPalette(Color(0xFF1C1921), Color(0xFF40364C)),
            TimelineWallpaper.PowderBlue to TimelineWallpaperPalette(Color(0xFF161B21), Color(0xFF304050)),
            TimelineWallpaper.SoftPeach to TimelineWallpaperPalette(Color(0xFF201915), Color(0xFF4B3328)),
            TimelineWallpaper.MidnightNavy to TimelineWallpaperPalette(Color(0xFF10233D), Color(0xFF6D87AC)),
            TimelineWallpaper.Evergreen to TimelineWallpaperPalette(Color(0xFF273B27), Color(0xFF8D9A78)),
            TimelineWallpaper.MauveDusk to TimelineWallpaperPalette(Color(0xFF584854), Color(0xFFD9B5C4)),
            TimelineWallpaper.TerracottaGlow to TimelineWallpaperPalette(Color(0xFF6A3326), Color(0xFFFFC395)),
            TimelineWallpaper.CharcoalMist to TimelineWallpaperPalette(Color(0xFF2F302E), Color(0xFF928B82)),
            TimelineWallpaper.CoralBloom to TimelineWallpaperPalette(Color(0xFF6A2F2B), Color(0xFFFFA090)),
            TimelineWallpaper.AquaSky to TimelineWallpaperPalette(Color(0xFF174D56), Color(0xFF7DECF3)),
            TimelineWallpaper.GoldenHour to TimelineWallpaperPalette(Color(0xFF63450E), Color(0xFFFFE18A)),
            TimelineWallpaper.VioletHaze to TimelineWallpaperPalette(Color(0xFF44206F), Color(0xFFD9B6FF)),
            TimelineWallpaper.SapphireBlue to TimelineWallpaperPalette(Color(0xFF1B347E), Color(0xFFA8C5FF)),
        )

        expected.forEach { (wallpaper, palette) ->
            assertEquals(palette, timelineWallpaperPalette(wallpaper, true))
            assertNotEquals(palette.backgroundColor, palette.doodleColor)
        }
    }

    @Test
    fun momentForegroundsStayReadableOverEveryWallpaperInEveryAppearanceMode() {
        listOf(false, true).forEach { isDark ->
            val colors = reliveColorsFor(DefaultRelivePalette.roles(isDark), isDark)

            TimelineWallpaper.entries.forEach { wallpaper ->
                val background = timelineWallpaperPalette(wallpaper, isDark = false)
                val foreground = timelineMomentForegroundColors(colors, background)

                listOf(
                    foreground.textPrimary,
                    foreground.textSecondary,
                    foreground.textMuted,
                    foreground.accent,
                    foreground.accentMuted,
                ).forEach { color ->
                    assertTrue(contrastRatio(color, background.backgroundColor) >= 4.5f)
                }
            }
        }
    }

    @Test
    fun wallpaperPreviewCardsShareOneStableDoodlePattern() {
        val first = timelineWallpaperVisual(TimelineWallpaper.WarmCream, false)
        val repeated = timelineWallpaperVisual(TimelineWallpaper.WarmCream, false)
        assertSame(MemoryDoodlePattern, first.pattern)
        assertSame(first.pattern, repeated.pattern)
        TimelineWallpaper.entries.forEach { wallpaper ->
            assertSame(MemoryDoodlePattern, timelineWallpaperVisual(wallpaper, false).pattern)
        }
    }

    @Test
    fun timelineBackgroundAndLivePreviewChangeOnlyPaletteAcrossSelections() {
        val warm = timelineWallpaperVisual(TimelineWallpaper.WarmCream, false)
        val blue = timelineWallpaperVisual(TimelineWallpaper.PowderBlue, false)

        assertNotEquals(warm.palette, blue.palette)
        assertSame(warm.pattern, blue.pattern)
    }

    @Test
    fun twoTimelineAppearancesResolveTheirOwnWallpaperVisuals() {
        val first = TimelineAppearance(wallpaper = TimelineWallpaper.BlushPink)
        val second = TimelineAppearance(wallpaper = TimelineWallpaper.SageGreen)

        assertNotEquals(
            timelineWallpaperVisual(first.wallpaper, isDark = false).palette,
            timelineWallpaperVisual(second.wallpaper, isDark = false).palette,
        )
        assertSame(
            timelineWallpaperVisual(first.wallpaper, isDark = false).pattern,
            timelineWallpaperVisual(second.wallpaper, isDark = false).pattern,
        )
    }

    @Test
    fun changingGlobalModeDoesNotChangeTheSelectedWallpaperIdentity() {
        val savedWallpaper = TimelineWallpaper.Lavender

        timelineWallpaperVisual(savedWallpaper, isDark = false)
        timelineWallpaperVisual(savedWallpaper, isDark = true)

        assertEquals(TimelineWallpaper.Lavender, savedWallpaper)
    }
}
