package com.vaibhav.relive.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Job

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

    @Test
    fun expansionProgressIsNormalizedAndContinuousInEitherDirection() {
        val state = BackdropExpansionState().apply {
            viewportHeightPx = 1_000
            backdropHeightPx = 200
        }

        state.expansionPx = 0f
        assertEquals(0f, state.progress)
        state.expansionPx = 200f
        assertEquals(0.25f, state.progress)
        state.expansionPx = 600f
        assertEquals(0.75f, state.progress)
        state.expansionPx = 400f
        assertEquals(0.5f, state.progress)
        state.expansionPx = state.maxExpansionPx
        assertEquals(1f, state.progress)
    }

    @Test
    fun expansionProgressRemainsBoundedAtGeometryEdges() {
        val state = BackdropExpansionState().apply {
            viewportHeightPx = 600
            backdropHeightPx = 200
        }

        state.expansionPx = -1f
        assertEquals(0f, state.progress)
        state.expansionPx = 401f
        assertEquals(1f, state.progress)

        state.viewportHeightPx = state.backdropHeightPx
        assertEquals(0f, state.progress)
    }

    @Test
    fun standardBehaviorPreservesTheExistingVisualMapping() {
        assertEquals(BackdropBehavior.Standard, BackdropExpansionState().behavior)
        assertEquals(1f, backdropOpacity(0.5f))
        assertEquals(1f, backdropScale(0.5f))
        assertEquals(0.5f, backdropPosition(0.5f))
    }

    @Test
    fun smoothstepIsBoundedAndReversible() {
        val samples = listOf(0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f)

        assertEquals(0f, smoothstep(-1f))
        assertEquals(1f, smoothstep(2f))
        samples.forEach { progress ->
            assertEquals(
                1f,
                smoothstep(progress) + smoothstep(1f - progress),
                absoluteTolerance = 0.000_001f,
            )
        }
        assertTrue(samples.zipWithNext().all { (start, end) -> smoothstep(start) <= smoothstep(end) })
    }

    @Test
    fun homeStretchMappingsReachTheirEndpointsContinuously() {
        assertEquals(1f, backdropOpacity(0f, BackdropBehavior.HomeStretch))
        assertEquals(0.82f, backdropOpacity(1f, BackdropBehavior.HomeStretch))
        assertEquals(1f, backdropScale(0f, BackdropBehavior.HomeStretch))
        assertEquals(1.04f, backdropScale(1f, BackdropBehavior.HomeStretch))
        assertEquals(0f, backdropPosition(0f, BackdropBehavior.HomeStretch))
        assertEquals(1f, backdropPosition(1f, BackdropBehavior.HomeStretch))

        val samples = (0..10).map { it / 10f }
        assertTrue(
            samples.zipWithNext().all { (start, end) ->
                backdropOpacity(start, BackdropBehavior.HomeStretch) >=
                    backdropOpacity(end, BackdropBehavior.HomeStretch) &&
                    backdropScale(start, BackdropBehavior.HomeStretch) <=
                    backdropScale(end, BackdropBehavior.HomeStretch) &&
                    backdropPosition(start, BackdropBehavior.HomeStretch) <=
                    backdropPosition(end, BackdropBehavior.HomeStretch)
            },
        )
    }

    @Test
    fun homeResistanceBeginsAfterEightyPercentAndReachesThirtyPercent() {
        assertEquals(1f, homeExpansionResponseRatio(0f))
        assertEquals(1f, homeExpansionResponseRatio(0.8f))
        assertEquals(0.65f, homeExpansionResponseRatio(0.9f), absoluteTolerance = 0.000_001f)
        assertEquals(0.3f, homeExpansionResponseRatio(1f), absoluteTolerance = 0.000_001f)

        val samples = (0..100).map { it / 100f }
        assertTrue(
            samples.zipWithNext().all { (start, end) ->
                homeExpansionResponseRatio(start) >= homeExpansionResponseRatio(end)
            },
        )
    }

    @Test
    fun homeResistanceOnlyTransformsOutwardReleaseVelocity() {
        assertEquals(1_000f, homeRenderedVelocity(1_000f, 0.8f))
        assertEquals(300f, homeRenderedVelocity(1_000f, 1f), absoluteTolerance = 0.001f)
        assertEquals(-1_000f, homeRenderedVelocity(-1_000f, 1f))
    }

    @Test
    fun aNewHomeDragCancelsActiveSettlementWithoutChangingPosition() {
        val state = BackdropExpansionState(BackdropBehavior.HomeStretch).apply {
            expansionPx = 640f
            activeSettlement = Job()
        }

        val settlement = state.activeSettlement
        state.cancelActiveSettlement()

        assertTrue(settlement?.isCancelled == true)
        assertEquals(null, state.activeSettlement)
        assertEquals(640f, state.expansionPx)
    }

    @Test
    fun greetingNamesUseTheRequestedEasedFadeIntervals() {
        assertEquals(1f, greetingInlineNameOpacity(0f))
        assertEquals(0.5f, greetingInlineNameOpacity(0.05f))
        assertEquals(0f, greetingInlineNameOpacity(0.1f))
        assertEquals(0f, greetingInlineNameOpacity(1f))

        assertEquals(0f, greetingSecondLineNameOpacity(0f))
        assertEquals(0f, greetingSecondLineNameOpacity(0.8f))
        assertEquals(0.5f, greetingSecondLineNameOpacity(0.9f), absoluteTolerance = 0.000_001f)
        assertEquals(1f, greetingSecondLineNameOpacity(1f))
    }

    @Test
    fun greetingFadeBoundariesAreStableWhenTraversedInEitherDirection() {
        val boundaries = listOf(0f, 0.05f, 0.1f, 0.25f, 0.8f, 0.9f, 1f)
        val forward = boundaries.map { progress ->
            greetingInlineNameOpacity(progress) to greetingSecondLineNameOpacity(progress)
        }
        val reverse = boundaries.asReversed().map { progress ->
            greetingInlineNameOpacity(progress) to greetingSecondLineNameOpacity(progress)
        }.asReversed()

        assertEquals(forward, reverse)
        assertEquals(1f to 0f, forward.first())
        assertEquals(0f to 1f, forward.last())
    }

    @Test
    fun greetingAndSubtitleGeometryAreContinuousAndReversible() {
        assertEquals(1f, greetingSalutationScale(0f))
        assertEquals(1.16f, greetingSalutationScale(0.5f), absoluteTolerance = 0.000_001f)
        assertEquals(1.32f, greetingSalutationScale(1f))
        assertEquals(40, greetingHeight(collapsedHeight = 40, expandedHeight = 104, progress = 0f))
        assertEquals(72, greetingHeight(collapsedHeight = 40, expandedHeight = 104, progress = 0.5f))
        assertEquals(104, greetingHeight(collapsedHeight = 40, expandedHeight = 104, progress = 1f))

        val samples = (0..100).map { it / 100f }
        assertTrue(
            samples.zipWithNext().all { (start, end) ->
                greetingSalutationScale(start) <= greetingSalutationScale(end) &&
                    greetingHeight(40, 104, start) <= greetingHeight(40, 104, end)
            },
        )
    }

    @Test
    fun subtitleGeometryOnlyTracksGreetingGeometry() {
        val collapsed = greetingHeight(collapsedHeight = 40, expandedHeight = 104, progress = 0f)
        val partiallyExpanded = greetingHeight(
            collapsedHeight = 40,
            expandedHeight = 104,
            progress = 0.5f,
        )
        val expanded = greetingHeight(collapsedHeight = 40, expandedHeight = 104, progress = 1f)

        // Mood reveal and lower-content placement have no input to this mapping.
        assertEquals(40, collapsed)
        assertEquals(72, partiallyExpanded)
        assertEquals(104, expanded)
    }

    @Test
    fun moodBarRevealUsesTheRequestedProgressWindow() {
        assertEquals(0f, moodBarReveal(0f))
        assertEquals(0f, moodBarReveal(0.25f))
        assertTrue(moodBarReveal(0.5f) in 0f..1f)
        assertEquals(1f, moodBarReveal(0.8f))
        assertEquals(1f, moodBarReveal(1f))

        val samples = (0..100).map { it / 100f }
        assertTrue(samples.zipWithNext().all { (start, end) -> moodBarReveal(start) <= moodBarReveal(end) })
    }

    @Test
    fun moodRevealBoundariesAreIdenticalOnReverseTravel() {
        val boundaries = listOf(0.25f, 0.5f, 0.8f)
        assertEquals(
            boundaries.map(::moodBarReveal),
            boundaries.asReversed().map(::moodBarReveal).asReversed(),
        )
    }

    @Test
    fun moodBarInteractionWaitsUntilTheRevealIsSubstantial() {
        assertTrue(!moodBarIsSubstantiallyVisible(0.25f))
        assertTrue(!moodBarIsSubstantiallyVisible(0.5f))
        assertTrue(moodBarIsSubstantiallyVisible(0.526f))
        assertTrue(moodBarIsSubstantiallyVisible(0.8f))
    }

    @Test
    fun moodBarSlotOpensContinuouslyWithTheReveal() {
        assertEquals(0f, moodSlotShiftPx(moodHeightPx = 120, spacingIncreasePx = 32, progress = 0f))
        assertEquals(0f, moodSlotShiftPx(moodHeightPx = 120, spacingIncreasePx = 32, progress = 0.25f))
        assertEquals(152f, moodSlotShiftPx(moodHeightPx = 120, spacingIncreasePx = 32, progress = 0.8f))
        assertEquals(152f, moodSlotShiftPx(moodHeightPx = 120, spacingIncreasePx = 32, progress = 1f))

        val samples = (0..100).map { it / 100f }
        assertTrue(
            samples.zipWithNext().all { (start, end) ->
                moodSlotShiftPx(120, 32, start) <= moodSlotShiftPx(120, 32, end)
            },
        )
    }
}
