package com.vaibhav.relive.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReliveGeneratedCoverTest {

    @Test
    fun sameKeySelectsTheSameGradient() {
        assertEquals(
            generatedCoverSelection("moment-42", WarmJournalGeneratedCoverPalette),
            generatedCoverSelection("moment-42", WarmJournalGeneratedCoverPalette),
        )
    }

    @Test
    fun differentKeysDistributeAcrossThePalette() {
        val selectedIndices = (0..64).map { key ->
            generatedCoverSelection("moment-$key", WarmJournalGeneratedCoverPalette).paletteIndex
        }.toSet()
        assertTrue(selectedIndices.size > 1)
    }

    @Test
    fun recomputationIsStable() {
        val first = (0..16).map { generatedCoverSelection("timeline-$it", WarmJournalGeneratedCoverPalette) }
        val second = (0..16).map { generatedCoverSelection("timeline-$it", WarmJournalGeneratedCoverPalette) }
        assertEquals(first, second)
    }

    @Test
    fun monochromeArchivePaletteContainsOnlyNeutralGradients() {
        MonochromeArchiveGeneratedCoverPalette.covers.forEach { cover ->
            assertEquals(cover.start.red, cover.start.green)
            assertEquals(cover.start.green, cover.start.blue)
            assertEquals(cover.end.red, cover.end.green)
            assertEquals(cover.end.green, cover.end.blue)
        }
    }
}
