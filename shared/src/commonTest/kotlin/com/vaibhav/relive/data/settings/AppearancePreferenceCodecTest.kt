package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.ThemeReference
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearancePreferenceCodecTest {
    @Test
    fun missingAndInvalidValuesUseSystemOriginal() {
        val missing = decodeAppearancePreferences(null, null)
        val invalid = decodeAppearancePreferences("unexpected", "unknown")

        assertEquals(AppearanceMode.System, missing.mode)
        assertEquals(ThemeReference.WarmJournal, missing.defaultTheme)
        assertEquals(missing, invalid)
    }

    @Test
    fun everySupportedValueRoundTrips() {
        AppearanceMode.entries.forEach { mode ->
            assertEquals(mode, decodeAppearancePreferences(mode.encodePreference(), null).mode)
        }
        ThemeReference.entries.forEach { theme ->
            assertEquals(theme, decodeAppearancePreferences(null, theme.encodePreference()).defaultTheme)
        }
    }
}
