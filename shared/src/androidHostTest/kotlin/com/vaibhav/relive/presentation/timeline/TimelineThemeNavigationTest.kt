package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.TimelineId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimelineThemeNavigationTest {
    @Test
    fun customTimelineThemeActionHasItsOwnDestination() {
        val id = TimelineId("family")

        assertEquals(TimelineThemeDestination(id), CurrentTimeline.Custom(id).timelineThemeDestinationOrNull())
    }

    @Test
    fun allTimelineHasNoTimelineThemeAction() {
        assertNull(CurrentTimeline.All.timelineThemeDestinationOrNull())
    }
}
