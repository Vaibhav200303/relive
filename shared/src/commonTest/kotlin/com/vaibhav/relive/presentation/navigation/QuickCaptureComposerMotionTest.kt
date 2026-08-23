package com.vaibhav.relive.presentation.navigation

import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickCaptureComposerMotionTest {

    @Test
    fun allComposerWaitsForDestinationThenOpensOnceWithoutDuplication() {
        assertFalse(shouldExpandQuickCaptureComposer(true, CurrentTimeline.All, false, false))
        assertTrue(shouldExpandQuickCaptureComposer(true, CurrentTimeline.All, false, true))
        assertFalse(shouldExpandQuickCaptureComposer(true, CurrentTimeline.All, true, true))
        assertFalse(
            shouldExpandQuickCaptureComposer(
                requested = true,
                currentTimeline = CurrentTimeline.Custom(TimelineId("custom")),
                isAlreadyExpanded = false,
                isDestinationSettled = true,
            ),
        )
    }
}
