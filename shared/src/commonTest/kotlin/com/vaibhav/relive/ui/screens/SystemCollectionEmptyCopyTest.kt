package com.vaibhav.relive.ui.screens

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemCollectionEmptyCopyTest {
    @Test
    fun eachSystemCollectionUsesItsOwnEmptyCopy() {
        assertEquals(
            SystemCollectionEmptyCopy(
                title = "No favorite moments yet.",
                message = "Moments you favorite will appear here.",
            ),
            systemCollectionEmptyCopy(CurrentTimeline.Favorites),
        )
        assertEquals(
            SystemCollectionEmptyCopy(
                title = "No photos or videos yet.",
                message = "Moments with photos or videos will appear here.",
            ),
            systemCollectionEmptyCopy(CurrentTimeline.AllPhotos),
        )
        assertEquals(
            SystemCollectionEmptyCopy(
                title = "No moments from this day yet.",
                message = "Memories from this date in past years will appear here.",
            ),
            systemCollectionEmptyCopy(CurrentTimeline.OnThisDay(LocalCalendarDate(2026, 9, 11))),
        )
        assertEquals(
            SystemCollectionEmptyCopy(
                title = "No moments from your past yet.",
                message = "Older memories will appear here.",
            ),
            systemCollectionEmptyCopy(CurrentTimeline.FromYourPast(testQuery())),
        )
    }

    private fun testQuery(): RediscoverQuery = RediscoverQuery(
        today = LocalCalendarDate(2026, 9, 11),
        startOfToday = Instant(0),
        recentCutoff = Instant(0),
        dailySeed = 0,
    )
}
