package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.BehaviorPreferences
import kotlin.test.Test
import kotlin.test.assertEquals

class BehaviorPreferenceCodecTest {
    @Test
    fun missingAndInvalidValuesUseSafeDefaults() {
        val missing = decodeBehaviorPreferences(null, null, null, null, null)
        val invalid = decodeBehaviorPreferences("yes", "1", "no", "enabled", "disabled")

        assertEquals(BehaviorPreferences(), missing)
        assertEquals(missing, invalid)
    }

    @Test
    fun everyBooleanValueRoundTripsForEverySetting() {
        listOf(true, false).forEach { value ->
            val encoded = value.encodeBehaviorPreference()
            val decoded = decodeBehaviorPreferences(
                confirmBeforeDiscarding = encoded,
                showLocations = encoded,
                showTags = encoded,
                showOnThisDay = encoded,
                showFavorites = encoded,
            )

            assertEquals(value, decoded.confirmBeforeDiscarding)
            assertEquals(value, decoded.showLocations)
            assertEquals(value, decoded.showTags)
            assertEquals(value, decoded.showOnThisDay)
            assertEquals(value, decoded.showFavorites)
        }
    }
}
