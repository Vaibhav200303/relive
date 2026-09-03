package com.vaibhav.relive.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeReferenceTest {

    @Test
    fun all_selectable_theme_ids_present() {
        val ids = ThemeReference.entries.toSet()
        assertEquals(
            setOf(
                ThemeReference.InkLilac,
                ThemeReference.TealSaffron,
                ThemeReference.EmberAqua,
                ThemeReference.PlumGold,
                ThemeReference.RoseSage,
                ThemeReference.Sunrise,
                ThemeReference.Sunset,
            ),
            ids,
        )
    }
}
