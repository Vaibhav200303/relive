package com.vaibhav.relive.presentation.rediscover

import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import kotlin.test.Test
import kotlin.test.assertTrue

class RediscoverCalendarIosTest {
    @Test fun local_calendar_can_resolve_a_day_boundary() {
        val instant = Instant(1_788_000_000_000L)
        val date = RediscoverCalendar.localDate(instant)
        val start = RediscoverCalendar.startOfDay(date)
        assertTrue(start <= instant)
        assertTrue(RediscoverCalendar.millisecondsUntilNextDay(instant) > 0L)
    }
}
