package com.vaibhav.relive.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The rule that keeps a sliding-backdrop surface from ever resting between its three positions
 * (ADR-0061, ADR-0062): a decisive fling wins outright, and a slow release goes wherever the sheet
 * is already closer to.
 */
class SlidingBackdropTest {

    private val max = 800f

    @Test
    fun aDecisiveDownwardFlingExpandsTheBackdropWhereverItStarted() {
        assertEquals(
            max,
            settleTargetFor(expansionPx = 1f, maxExpansionPx = max, velocityY = 900f),
            "a fling that is still opening the backdrop finishes opening it",
        )
    }

    @Test
    fun aDecisiveUpwardFlingBringsTheFeedBack() {
        assertEquals(
            0f,
            settleTargetFor(expansionPx = max - 1f, maxExpansionPx = max, velocityY = -900f),
            "a fling that is still closing the backdrop finishes closing it",
        )
    }

    @Test
    fun aSlowReleasePastHalfwayExpands() {
        assertEquals(
            max,
            settleTargetFor(expansionPx = max / 2f, maxExpansionPx = max, velocityY = 0f),
            "exactly halfway settles open, so the boundary is never a dead zone",
        )
    }

    @Test
    fun aSlowReleaseBeforeHalfwayReturnsTheFeed() {
        assertEquals(
            0f,
            settleTargetFor(expansionPx = max / 2f - 1f, maxExpansionPx = max, velocityY = 0f),
        )
    }

    @Test
    fun aDriftBelowTheFlingThresholdIsTreatedAsASlowRelease() {
        assertEquals(
            0f,
            settleTargetFor(expansionPx = 10f, maxExpansionPx = max, velocityY = 300f),
            "a lazy drift downward is not enough on its own to expand from near the bottom",
        )
    }

    @Test
    fun aBackdropWithNoRoomToExpandStaysClosed() {
        assertEquals(
            0f,
            settleTargetFor(expansionPx = 0f, maxExpansionPx = 0f, velocityY = -900f),
            "a backdrop taller than its viewport has nowhere to travel",
        )
    }
}
