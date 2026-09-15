package com.vaibhav.relive.ui.screens

import androidx.compose.ui.graphics.Color
import com.vaibhav.relive.ui.theme.ReliveThemeId
import com.vaibhav.relive.ui.theme.reliveTokensFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingThemeContractTest {
    @Test
    fun onboardingAlwaysUsesTheLightSunsetReferenceTheme() {
        assertEquals(ReliveThemeId.Sunset, ONBOARDING_THEME_ID)
        assertFalse(ONBOARDING_DARK_MODE)
        assertTrue(
            reliveTokensFor(ONBOARDING_THEME_ID, ONBOARDING_DARK_MODE)
                .systemBarIconsDark,
        )
    }

    @Test
    fun onboardingUsesItsApprovedIvoryCanvasException() {
        assertEquals(Color(0xFFF7F4F1), ONBOARDING_REFERENCE_CANVAS)
    }

    @Test
    fun reliveMomentsPreviewUsesDistinctSafeSampleMoments() {
        assertEquals(3, ONBOARDING_RELIVE_MOMENT_PREVIEWS.size)
        assertEquals(
            ONBOARDING_RELIVE_MOMENT_PREVIEWS.size,
            ONBOARDING_RELIVE_MOMENT_PREVIEWS.map { it.id }.distinct().size,
        )
        assertTrue(ONBOARDING_RELIVE_MOMENT_PREVIEWS.all { it.attachments.isEmpty() })
    }
}
