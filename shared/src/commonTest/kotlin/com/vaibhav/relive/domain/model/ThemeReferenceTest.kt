package com.vaibhav.relive.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeReferenceTest {

    @Test
    fun all_three_theme_ids_present() {
        val ids = ThemeReference.entries.toSet()
        assertEquals(
            setOf(
                ThemeReference.WarmJournal,
                ThemeReference.MonochromeArchive,
                ThemeReference.FilmMemory,
            ),
            ids,
        )
    }
}
