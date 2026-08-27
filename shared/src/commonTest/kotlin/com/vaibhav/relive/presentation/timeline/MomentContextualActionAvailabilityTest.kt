package com.vaibhav.relive.presentation.timeline

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MomentContextualActionAvailabilityTest {

    @Test
    fun recentAllMomentOffersEditAddAndForgetWhenCustomTimelinesExist() {
        val actions = resolveMomentContextualActionAvailability(
            mode = TimelineMode.Editable,
            currentTimeline = CurrentTimeline.All,
            isWithinEditWindow = true,
            hasCustomTimelines = true,
        )

        assertTrue(actions.canEnter)
        assertTrue(actions.canEdit)
        assertTrue(actions.canAddToTimeline)
        assertTrue(actions.canForget)
    }

    @Test
    fun olderAllMomentOffersOnlyTimelineAssignment() {
        val actions = resolveMomentContextualActionAvailability(
            mode = TimelineMode.Editable,
            currentTimeline = CurrentTimeline.All,
            isWithinEditWindow = false,
            hasCustomTimelines = true,
        )

        assertTrue(actions.canEnter)
        assertFalse(actions.canEdit)
        assertTrue(actions.canAddToTimeline)
        assertFalse(actions.canForget)
    }

    @Test
    fun assignmentIsAbsentWithoutCustomTimelines() {
        val actions = resolveMomentContextualActionAvailability(
            mode = TimelineMode.Editable,
            currentTimeline = CurrentTimeline.All,
            isWithinEditWindow = false,
            hasCustomTimelines = false,
        )

        assertFalse(actions.canEnter)
        assertFalse(actions.canAddToTimeline)
    }

    @Test
    fun readOnlyCollectionsNeverOfferContextualActions() {
        val actions = resolveMomentContextualActionAvailability(
            mode = TimelineMode.ReadOnlySystemCollection("Favorites"),
            currentTimeline = CurrentTimeline.All,
            isWithinEditWindow = true,
            hasCustomTimelines = true,
        )

        assertFalse(actions.canEnter)
        assertFalse(actions.canEdit)
        assertFalse(actions.canAddToTimeline)
        assertFalse(actions.canForget)
    }
}
