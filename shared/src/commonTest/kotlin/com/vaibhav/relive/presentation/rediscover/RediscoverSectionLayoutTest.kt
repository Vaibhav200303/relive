package com.vaibhav.relive.presentation.rediscover

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RediscoverSectionLayoutTest {
    @Test
    fun zeroEligibleMomentsRemovesTheEntireOnThisDaySection() {
        val layout = rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 0,
            fromYourPastMomentCount = 0,
        )

        assertFalse(layout.showOnThisDay)
    }

    @Test
    fun oneEligibleMomentShowsOnThisDay() {
        assertTrue(rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 1,
            fromYourPastMomentCount = 0,
        ).showOnThisDay)
    }

    @Test
    fun multipleEligibleMomentsShowOnThisDay() {
        assertTrue(rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 3,
            fromYourPastMomentCount = 0,
        ).showOnThisDay)
    }

    @Test
    fun missingOnThisDayUsesNormalSpacingWithoutAPlaceholderGap() {
        val layout = rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 0,
            fromYourPastMomentCount = 0,
        )

        assertEquals(RediscoverSectionSpacing.Normal, layout.fromYourPastSpacing)
    }

    @Test
    fun visibleOnThisDayKeepsExpandedSpacingBeforeFromYourPast() {
        val layout = rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 1,
            fromYourPastMomentCount = 0,
        )

        assertEquals(RediscoverSectionSpacing.Expanded, layout.fromYourPastSpacing)
    }

    @Test
    fun onThisDayPreferenceHidesEligibleSectionAndCollapsesSpacing() {
        val layout = rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 3,
            fromYourPastMomentCount = 0,
            showOnThisDay = false,
        )

        assertFalse(layout.showOnThisDay)
        assertEquals(RediscoverSectionSpacing.Normal, layout.fromYourPastSpacing)
    }

    @Test
    fun favoritesPreferenceHidesTheWholeSection() {
        val layout = rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 0,
            fromYourPastMomentCount = 0,
            showFavorites = false,
        )

        assertFalse(layout.showFavorites)
    }

    @Test
    fun zeroEligibleMomentsRemovesTheEntireFromYourPastSection() {
        assertFalse(rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 0,
            fromYourPastMomentCount = 0,
        ).showFromYourPast)
    }

    @Test
    fun eligibleMomentsShowFromYourPast() {
        assertTrue(rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 0,
            fromYourPastMomentCount = 1,
        ).showFromYourPast)
    }

    @Test
    fun zeroFavoritesKeepsTheSectionVisibleAndShowsTheEmptyCard() {
        val layout = rediscoverSectionLayout(
            favoriteMomentCount = 0,
            onThisDayMomentCount = 0,
            fromYourPastMomentCount = 0,
        )

        assertTrue(layout.showFavorites)
        assertTrue(layout.showFavoritesEmptyState)
    }

    @Test
    fun oneOrMoreFavoritesHideTheEmptyCard() {
        assertFalse(rediscoverSectionLayout(
            favoriteMomentCount = 1,
            onThisDayMomentCount = 0,
            fromYourPastMomentCount = 0,
        ).showFavoritesEmptyState)
        assertFalse(rediscoverSectionLayout(
            favoriteMomentCount = 3,
            onThisDayMomentCount = 0,
            fromYourPastMomentCount = 0,
        ).showFavoritesEmptyState)
    }
}
