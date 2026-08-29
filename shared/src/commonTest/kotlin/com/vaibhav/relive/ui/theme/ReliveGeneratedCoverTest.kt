package com.vaibhav.relive.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReliveGeneratedCoverTest {

    @Test
    fun sameKeySelectsTheSameGradient() {
        assertEquals(
            generatedCoverSelection("moment-42", DefaultGeneratedCoverPalette),
            generatedCoverSelection("moment-42", DefaultGeneratedCoverPalette),
        )
    }

    @Test
    fun differentKeysDistributeAcrossThePalette() {
        val selectedIndices = (0..64).map { key ->
            generatedCoverSelection("moment-$key", DefaultGeneratedCoverPalette).paletteIndex
        }.toSet()
        assertTrue(selectedIndices.size > 1)
    }

    @Test
    fun recomputationIsStable() {
        val first = (0..16).map { generatedCoverSelection("timeline-$it", DefaultGeneratedCoverPalette) }
        val second = (0..16).map { generatedCoverSelection("timeline-$it", DefaultGeneratedCoverPalette) }
        assertEquals(first, second)
    }

    @Test
    fun everyThemeAndModeProvidesDeterministicCoverChoices() {
        ReliveThemeId.entries.forEach { theme ->
            listOf(false, true).forEach { isDark ->
                val palette = reliveTokensFor(theme, isDark).generatedCoverPalette
                val first = generatedCoverSelection("timeline-memory", palette)
                val second = generatedCoverSelection("timeline-memory", palette)
                assertEquals(first, second)
                palette.covers.forEach { cover ->
                    assertEquals(1f, cover.start.alpha)
                    assertEquals(1f, cover.end.alpha)
                }
            }
        }
    }
}
