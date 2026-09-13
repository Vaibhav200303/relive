package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ExportRulesTest {
    @Test
    fun inclusive_range_is_oldest_first() {
        val newest = Moment(MomentId("new"), Instant(3), title = "new")
        val oldest = Moment(MomentId("old"), Instant(1), title = "old")
        val outside = Moment(MomentId("outside"), Instant(4), title = "outside")
        val dates = mapOf(oldest.id to LocalCalendarDate(2026, 1, 1), newest.id to LocalCalendarDate(2026, 1, 3), outside.id to LocalCalendarDate(2026, 1, 4))
        val result = filterMomentsForMagazine(listOf(newest, outside, oldest), LocalCalendarDate(2026, 1, 1), LocalCalendarDate(2026, 1, 3)) { dates.getValue(it.id) }
        assertEquals(listOf(oldest, newest), result)
    }

    @Test
    fun reversed_range_is_rejected() {
        assertFails { filterMomentsForMagazine(emptyList(), LocalCalendarDate(2026, 2, 1), LocalCalendarDate(2026, 1, 1)) { error("unused") } }
    }

    @Test
    fun filename_title_is_stable_and_safe() {
        assertEquals("My-Relive", exportFilenameTitle("  My Relive!  "))
        assertEquals("My-Relive", exportFilenameTitle("🤍"))
    }
}
