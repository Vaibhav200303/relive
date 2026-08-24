package com.vaibhav.relive.ui.components.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.ui.theme.ReliveDimensions

class FloatingToolbarDestinationsTest {
    @Test
    fun responsiveToolbarLayoutKeepsThreeNavigationTargetsAndTheRequestedGap() {
        val layout = floatingToolbarLayout(320.dp, ReliveDimensions())

        assertEquals(152.dp, layout.navigationExpandedWidth)
        assertEquals(128.dp, layout.newExpandedWidth)
    }

    @Test
    fun narrowLayoutKeepsNavigationTargetsByCollapsingNewToItsSingleActionWidth() {
        val layout = floatingToolbarLayout(256.dp, ReliveDimensions())

        assertEquals(152.dp, layout.navigationExpandedWidth)
        assertEquals(64.dp, layout.newExpandedWidth)
    }

    @Test
    fun timelineKeepsLaterDestinationsInTrailingOrder() {
        val destinations = floatingToolbarDestinations(ReliveTopLevelDestination.Timelines)

        assertEquals(emptyList(), destinations.leading)
        assertEquals(ReliveTopLevelDestination.Timelines, destinations.active)
        assertEquals(
            listOf(ReliveTopLevelDestination.Rediscover, ReliveTopLevelDestination.Search),
            destinations.trailing,
        )
    }

    @Test
    fun rediscoverKeepsTheExpandedStripInDestinationOrder() {
        val destinations = floatingToolbarDestinations(ReliveTopLevelDestination.Rediscover)

        assertEquals(listOf(ReliveTopLevelDestination.Timelines), destinations.leading)
        assertEquals(ReliveTopLevelDestination.Rediscover, destinations.active)
        assertEquals(listOf(ReliveTopLevelDestination.Search), destinations.trailing)
    }

    @Test
    fun searchKeepsEarlierDestinationsInLeadingOrder() {
        val destinations = floatingToolbarDestinations(ReliveTopLevelDestination.Search)

        assertEquals(
            listOf(ReliveTopLevelDestination.Timelines, ReliveTopLevelDestination.Rediscover),
            destinations.leading,
        )
        assertEquals(ReliveTopLevelDestination.Search, destinations.active)
        assertEquals(emptyList(), destinations.trailing)
    }
}
