package com.vaibhav.relive.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ReliveTimelineThumbnailTest {

    @Test
    fun thumbnailColorsAreDerivedFromTheActiveTheme() {
        val warm = reliveTokensFor(ReliveThemeId.WarmJournal).colors
        val sunset = reliveTokensFor(ReliveThemeId.Sunset).colors

        assertEquals(timelineThumbnailColorsFor(warm), timelineThumbnailColorsFor(warm))
        assertNotEquals(timelineThumbnailColorsFor(warm), timelineThumbnailColorsFor(sunset))
    }

    @Test
    fun darkAndLightModesProduceDifferentLandscapeColors() {
        val light = reliveTokensFor(ReliveThemeId.PlumGold, isDark = false).colors
        val dark = reliveTokensFor(ReliveThemeId.PlumGold, isDark = true).colors

        assertNotEquals(timelineThumbnailColorsFor(light), timelineThumbnailColorsFor(dark))
    }
}
