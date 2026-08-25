package com.vaibhav.relive.presentation.rediscover

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RediscoverSectionLayoutTest {
    @Test
    fun zeroEligibleMomentsRemovesTheEntireOnThisDaySection() {
        val layout = rediscoverSectionLayout(onThisDayMomentCount = 0)

        assertFalse(layout.showOnThisDay)
    }

    @Test
    fun oneEligibleMomentShowsOnThisDay() {
        assertTrue(rediscoverSectionLayout(onThisDayMomentCount = 1).showOnThisDay)
    }

    @Test
    fun multipleEligibleMomentsShowOnThisDay() {
        assertTrue(rediscoverSectionLayout(onThisDayMomentCount = 3).showOnThisDay)
    }

    @Test
    fun missingOnThisDayUsesNormalSpacingWithoutAPlaceholderGap() {
        val layout = rediscoverSectionLayout(onThisDayMomentCount = 0)

        assertEquals(RediscoverSectionSpacing.Normal, layout.fromYourPastSpacing)
    }

    @Test
    fun visibleOnThisDayKeepsExpandedSpacingBeforeFromYourPast() {
        val layout = rediscoverSectionLayout(onThisDayMomentCount = 1)

        assertEquals(RediscoverSectionSpacing.Expanded, layout.fromYourPastSpacing)
    }

    @Test
    fun onThisDayPreferenceHidesEligibleSectionAndCollapsesSpacing() {
        val layout = rediscoverSectionLayout(
            onThisDayMomentCount = 3,
            showOnThisDay = false,
        )

        assertFalse(layout.showOnThisDay)
        assertEquals(RediscoverSectionSpacing.Normal, layout.fromYourPastSpacing)
    }

    @Test
    fun favoritesPreferenceHidesTheWholeSection() {
        val layout = rediscoverSectionLayout(
            onThisDayMomentCount = 0,
            showFavorites = false,
        )

        assertFalse(layout.showFavorites)
    }
}
