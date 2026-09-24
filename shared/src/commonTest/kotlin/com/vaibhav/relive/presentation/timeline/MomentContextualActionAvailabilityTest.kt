package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.TimelineId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MomentContextualActionAvailabilityTest {

    @Test
    fun allMomentOffersEditAddAndForgetWhenCustomTimelinesExist() {
        val actions = resolveMomentContextualActionAvailability(
            mode = TimelineMode.Editable,
            currentTimeline = CurrentTimeline.All,
            hasCustomTimelines = true,
        )

        assertTrue(actions.canEnter)
        assertTrue(actions.canEdit)
        assertTrue(actions.canAddToTimeline)
        assertTrue(actions.canForget)
    }

    @Test
    fun allMomentOffersEditAndForgetWithoutCustomTimelines() {
        val actions = resolveMomentContextualActionAvailability(
            mode = TimelineMode.Editable,
            currentTimeline = CurrentTimeline.All,
            hasCustomTimelines = false,
        )

        assertTrue(actions.canEnter)
        assertTrue(actions.canEdit)
        assertFalse(actions.canAddToTimeline)
        assertTrue(actions.canForget)
    }

    @Test
    fun customTimelineMomentOffersEditAndForgetInContextualBar() {
        val actions = resolveMomentContextualActionAvailability(
            mode = TimelineMode.Editable,
            currentTimeline = CurrentTimeline.Custom(TimelineId("trip")),
            hasCustomTimelines = true,
        )

        assertTrue(actions.canEnter)
        assertTrue(actions.canEdit)
        assertFalse(actions.canAddToTimeline)
        assertTrue(actions.canForget)
    }

    @Test
    fun readOnlyCollectionsNeverOfferContextualActions() {
        val actions = resolveMomentContextualActionAvailability(
            mode = TimelineMode.ReadOnlySystemCollection("Favorites"),
            currentTimeline = CurrentTimeline.All,
            hasCustomTimelines = true,
        )

        assertFalse(actions.canEnter)
        assertFalse(actions.canEdit)
        assertFalse(actions.canAddToTimeline)
        assertFalse(actions.canForget)
    }
}
