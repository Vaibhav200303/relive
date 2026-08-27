package com.vaibhav.relive.ui.screens

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineReturnToBottomTest {
    @Test
    fun affordanceIsHiddenUntilManualMovementTowardOlderMoments() {
        val state = TimelineReturnToBottomState()

        assertFalse(state.isVisible(canScrollForward = true))
        assertFalse(
            state.onManualPositionChanged(
                movedTowardOlderMoments = false,
                canScrollForward = true,
            ).isVisible(canScrollForward = true),
        )
        assertTrue(
            state.onManualPositionChanged(
                movedTowardOlderMoments = true,
                canScrollForward = true,
            ).isVisible(canScrollForward = true),
        )
    }

    @Test
    fun affordanceRemainsVisibleOnReturnMovementUntilTheBottomIsReached() {
        val revealed = TimelineReturnToBottomState().onManualPositionChanged(
            movedTowardOlderMoments = true,
            canScrollForward = true,
        )

        assertTrue(
            revealed.onManualPositionChanged(
                movedTowardOlderMoments = false,
                canScrollForward = true,
            ).isVisible(canScrollForward = true),
        )
        assertFalse(
            revealed.onManualPositionChanged(
                movedTowardOlderMoments = false,
                canScrollForward = false,
            ).isVisible(canScrollForward = false),
        )
    }

    @Test
    fun programmaticPositionChangesDoNotRevealTheAffordance() {
        val state = TimelineReturnToBottomState().onPositionChanged(
            isProgrammaticScroll = true,
            movedTowardOlderMoments = true,
            canScrollForward = true,
        )

        assertFalse(state.isVisible(canScrollForward = true))
    }

    @Test
    fun autoScrollTemporarilyHidesTheAffordanceAndRestoresItWhenCancelledAwayFromBottom() {
        val revealed = TimelineReturnToBottomState().onManualPositionChanged(
            movedTowardOlderMoments = true,
            canScrollForward = true,
        )

        val scrolling = revealed.onAutoScrollStarted()
        assertFalse(scrolling.isVisible(canScrollForward = true))
        assertTrue(scrolling.onAutoScrollStopped(canScrollForward = true).isVisible(canScrollForward = true))
        assertFalse(scrolling.onAutoScrollStopped(canScrollForward = false).isVisible(canScrollForward = false))
    }

    @Test
    fun controllerRejectsDuplicateStartsAndCancelsTheActiveScroll() = runTest(
        StandardTestDispatcher(),
    ) {
        val controller = TimelineAutoScrollController()
        var starts = 0
        var stops = 0

        controller.start(
            scope = this,
            scroll = {
                starts += 1
                awaitCancellation()
            },
            onStopped = { stops += 1 },
        )
        controller.start(
            scope = this,
            scroll = { starts += 1 },
            onStopped = { stops += 1 },
        )
        runCurrent()

        assertEquals(1, starts)
        assertTrue(controller.isRunning)

        controller.cancel()
        runCurrent()

        assertEquals(1, stops)
        assertFalse(controller.isRunning)
    }
}
