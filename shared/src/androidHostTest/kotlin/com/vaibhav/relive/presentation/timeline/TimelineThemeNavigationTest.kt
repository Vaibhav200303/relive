package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.TimelineId
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineThemeNavigationTest {
    @Test
    fun customTimelineThemeActionHasItsOwnDestination() {
        val id = TimelineId("family")

        assertEquals(TimelineThemeDestination.Custom(id), CurrentTimeline.Custom(id).timelineThemeDestinationOrNull())
    }

    @Test
    fun allTimelineThemeActionHasItsOwnDestination() {
        assertEquals(TimelineThemeDestination.All, CurrentTimeline.All.timelineThemeDestinationOrNull())
    }
}
