package com.vaibhav.relive.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class FeelingPromptPlacementTest {

    @Test
    fun placementMovesOnlyTheClippedOverflow() {
        assertEquals(
            120,
            feelingPromptOverflow(
                itemOffset = 320,
                itemSize = 500,
                viewportEndOffset = 700,
            ),
        )
    }

    @Test
    fun fullyVisiblePromptDoesNotMoveTheFeed() {
        assertEquals(
            0,
            feelingPromptOverflow(
                itemOffset = 120,
                itemSize = 400,
                viewportEndOffset = 700,
            ),
        )
    }
}
