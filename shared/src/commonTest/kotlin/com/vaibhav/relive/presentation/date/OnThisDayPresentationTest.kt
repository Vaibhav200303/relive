package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.model.LocalCalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OnThisDayPresentationTest {
    @Test fun editorial_day_month_uses_the_approved_order() {
        assertEquals("23 August", LocalCalendarDate(2026, 8, 23).editorialDayMonth())
    }

    @Test fun anniversary_label_uses_exact_calendar_years() {
        assertEquals("1 YEAR AGO", anniversaryYearLabel(2026, 2025))
        assertEquals("2 YEARS AGO", anniversaryYearLabel(2026, 2024))
        assertEquals("5 YEARS AGO", anniversaryYearLabel(2026, 2021))
    }

    @Test fun anniversary_label_rejects_current_year() {
        assertFailsWith<IllegalArgumentException> { anniversaryYearLabel(2026, 2026) }
    }
}
