package com.vaibhav.relive.ui.screens

import com.vaibhav.relive.ui.theme.ReliveThemeId
import com.vaibhav.relive.ui.theme.reliveTokensFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingThemeContractTest {
    @Test
    fun onboardingUsesFiveExistingDarkAppPalettesInAContinuousHuePath() {
        assertTrue(ONBOARDING_DARK_MODE)
        assertEquals(
            listOf(
                ReliveThemeId.TealSaffron,
                ReliveThemeId.InkLilac,
                ReliveThemeId.PlumGold,
                ReliveThemeId.IvoryGold,
                ReliveThemeId.VelvetRose,
            ),
            ONBOARDING_CHAPTER_THEME_IDS,
        )
        assertEquals(5, ONBOARDING_CHAPTER_THEME_IDS.distinct().size)
        assertTrue(
            ONBOARDING_CHAPTER_THEME_IDS.all { themeId ->
                !reliveTokensFor(themeId, ONBOARDING_DARK_MODE).systemBarIconsDark
            },
        )
    }

    @Test
    fun capturePreviewUsesOneSafeSampleMoment() {
        assertEquals(1, ONBOARDING_RELIVE_MOMENT_PREVIEWS.size)
        val moment = ONBOARDING_RELIVE_MOMENT_PREVIEWS.single()
        assertEquals("12 AUG 2026", moment.formattedDate)
        assertEquals("8:24 AM", moment.formattedTime)
        assertEquals("Mountain lake", moment.locationLabel)
        assertEquals("A quiet morning", moment.title)
        assertTrue(moment.content.isEmpty())
        assertEquals(
            ONBOARDING_RELIVE_MOMENT_PREVIEWS.size,
            ONBOARDING_RELIVE_MOMENT_PREVIEWS.map { it.id }.distinct().size,
        )
        assertTrue(ONBOARDING_RELIVE_MOMENT_PREVIEWS.all { it.attachments.isEmpty() })
    }

    @Test
    fun timelinePreviewKeepsTheTransferredMomentAndAddsAContinuousStory() {
        assertEquals(4, ONBOARDING_TIMELINE_MOMENT_PREVIEWS.size)
        assertEquals(
            ONBOARDING_RELIVE_MOMENT_PREVIEWS.single(),
            ONBOARDING_TIMELINE_MOMENT_PREVIEWS.first(),
        )
        assertEquals(
            listOf("A quiet morning", "Forest trails", "A coastal escape", "Slow afternoons"),
            ONBOARDING_TIMELINE_MOMENT_PREVIEWS.map { it.title },
        )
        assertEquals(
            listOf("12 AUG 2026", "16 AUG 2026", "18 AUG 2026", "14 AUG 2026"),
            ONBOARDING_TIMELINE_MOMENT_PREVIEWS.map { it.formattedDate },
        )
    }

    @Test
    fun readyPreviewUsesFiveIndividualMomentCards() {
        assertEquals(5, ONBOARDING_READY_MOMENT_PREVIEWS.size)
        assertEquals(
            ONBOARDING_TIMELINE_MOMENT_PREVIEWS,
            ONBOARDING_READY_MOMENT_PREVIEWS.take(4),
        )
        assertEquals(5, ONBOARDING_READY_MOMENT_PREVIEWS.map { it.id }.distinct().size)
        assertTrue(ONBOARDING_READY_MOMENT_PREVIEWS.all { it.attachments.isEmpty() })
    }

    @Test
    fun momentFocusHasOneExactMaximumAndNeverExceedsCaptureSize() {
        assertEquals(1f, onboardingMomentFocus(240f, 240f, 300f))
        assertEquals(0f, onboardingMomentFocus(600f, 240f, 300f))
        assertEquals(
            ONBOARDING_FOCUSED_MOMENT_SCALE,
            onboardingMomentScale(1f),
        )
        assertEquals(
            ONBOARDING_RESTING_MOMENT_SCALE,
            onboardingMomentScale(0f),
        )
        assertEquals(
            ONBOARDING_FOCUSED_MOMENT_SCALE,
            onboardingMomentScale(2f),
        )
    }

    @Test
    fun screenThreeSuctionRetreatsFromTheBottomBeforePullingTheTop() {
        val start = onboardingSheetDeformation(0f)
        val initialPull = onboardingSheetDeformation(.3f)
        val middle = onboardingSheetDeformation(.6f)
        val end = onboardingSheetDeformation(1f)

        assertEquals(0f, start.retreat)
        assertEquals(.5f, start.tailWidthFraction)
        assertEquals(0f, start.topInsetFraction)
        assertEquals(0f, initialPull.topInsetFraction)
        assertTrue(initialPull.retreat > start.retreat)
        assertTrue(middle.retreat > initialPull.retreat)
        assertTrue(middle.tailWidthFraction < initialPull.tailWidthFraction)
        assertEquals(1f, end.retreat, .0001f)
        assertEquals(.006f, end.tailWidthFraction, .0001f)
        assertEquals(.494f, end.topInsetFraction, .0001f)
        assertEquals(1f, end.finalPull)
    }

    @Test
    fun screenThreeCardStartsFullyHiddenAndRevealsContinuously() {
        assertEquals(0f, onboardingCardRevealProgress(0f))
        assertEquals(1f, onboardingCardRevealProgress(1f))
        assertTrue(onboardingCardRevealProgress(.3f) < onboardingCardRevealProgress(.6f))
    }
}
