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

class TimelineWallpaperPaletteTest {
    @Test
    fun everyWallpaperResolvesToItsApprovedLightPalette() {
        assertEquals(TimelineWallpaperPalette(Color(0xFFFAF3E9), Color(0xFFE7D5BF)), timelineWallpaperPalette(TimelineWallpaper.WarmCream, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFFDE7E7), Color(0xFFF0B8BA)), timelineWallpaperPalette(TimelineWallpaper.BlushPink, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFE4E9DD), Color(0xFFB8C5AE)), timelineWallpaperPalette(TimelineWallpaper.SageGreen, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFEDE6F9), Color(0xFFCBBEE5)), timelineWallpaperPalette(TimelineWallpaper.Lavender, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFE1EEFA), Color(0xFFB1CFEA)), timelineWallpaperPalette(TimelineWallpaper.PowderBlue, false))
        assertEquals(TimelineWallpaperPalette(Color(0xFFFEEBE1), Color(0xFFF5B99B)), timelineWallpaperPalette(TimelineWallpaper.SoftPeach, false))
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
        )

        expected.forEach { (wallpaper, palette) ->
            assertEquals(palette, timelineWallpaperPalette(wallpaper, true))
            assertNotEquals(palette.backgroundColor, palette.doodleColor)
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
