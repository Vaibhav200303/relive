package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.domain.model.TimelineId
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineHomePlaceholderTest {

    @Test
    fun emptyCustomTimelineWaitsForItsFirstMoment() {
        assertEquals(
            "Friends is waiting for its first moment.",
            summary(name = "Friends", momentCount = 0).emptyPreviewPlaceholderText(),
        )
    }

    @Test
    fun textOnlyCustomTimelineWaitsForANewMoment() {
        assertEquals(
            "Friends is waiting for a new moment.",
            summary(name = "Friends", momentCount = 3).emptyPreviewPlaceholderText(),
        )
    }

    @Test
    fun allWithNoMomentsUsesTheGlobalFirstMomentMessage() {
        assertEquals(
            "Your story is waiting for its first moment.",
            TimelineHomeSummary(Timeline.All, momentCount = 0, previewAttachments = emptyList())
                .emptyPreviewPlaceholderText(),
        )
    }

    @Test
    fun allWithOnlyNonVisualMomentsUsesTheGlobalNewMomentMessage() {
        assertEquals(
            "Your story is waiting for a new moment.",
            TimelineHomeSummary(Timeline.All, momentCount = 1, previewAttachments = emptyList())
                .emptyPreviewPlaceholderText(),
        )
    }

    private fun summary(name: String, momentCount: Long) = TimelineHomeSummary(
        timeline = Timeline.Custom(TimelineId("timeline"), name),
        momentCount = momentCount,
        previewAttachments = emptyList(),
    )
}
