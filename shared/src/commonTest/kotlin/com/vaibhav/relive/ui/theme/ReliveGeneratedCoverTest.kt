package com.vaibhav.relive.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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

    @Test
    fun lightModeCoversAreStrictlyDarkerTakesOfTheAccent() {
        ReliveThemeId.entries.forEach { theme ->
            val tokens = reliveTokensFor(theme, isDark = false)
            val accentDepth = contrastRatio(tokens.colors.accent, Color.Black)
            tokens.generatedCoverPalette.covers.forEach { cover ->
                assertTrue(
                    contrastRatio(cover.start, Color.Black) < accentDepth,
                    "$theme light start darker than accent",
                )
                assertTrue(
                    contrastRatio(cover.end, Color.Black) < accentDepth,
                    "$theme light end darker than accent",
                )
            }
        }
    }

    @Test
    fun darkModeCoversAreStrictlyLighterTakesOfTheAccent() {
        ReliveThemeId.entries.forEach { theme ->
            val tokens = reliveTokensFor(theme, isDark = true)
            val accentDepth = contrastRatio(tokens.colors.accent, Color.Black)
            tokens.generatedCoverPalette.covers.forEach { cover ->
                assertTrue(
                    contrastRatio(cover.start, Color.Black) > accentDepth,
                    "$theme dark start lighter than accent",
                )
                assertTrue(
                    contrastRatio(cover.end, Color.Black) > accentDepth,
                    "$theme dark end lighter than accent",
                )
            }
        }
    }

    @Test
    fun lightAndDarkModesDeriveDifferentCoverPalettes() {
        ReliveThemeId.entries.forEach { theme ->
            assertNotEquals(
                reliveTokensFor(theme, isDark = false).generatedCoverPalette,
                reliveTokensFor(theme, isDark = true).generatedCoverPalette,
                "$theme cover palette must differ per mode",
            )
        }
    }

    @Test
    fun stableCoverIndexStaysInRangeAndIsDeterministic() {
        (1..9).forEach { optionCount ->
            (0..32).forEach { key ->
                val index = stableCoverIndex("cover-$key", optionCount)
                assertTrue(index in 0 until optionCount)
                assertEquals(index, stableCoverIndex("cover-$key", optionCount))
            }
        }
    }
}
