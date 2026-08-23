package com.vaibhav.relive.presentation.rediscover

import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RediscoverCalendarHostTest {
    @Test fun local_date_and_start_of_day_follow_the_device_timezone() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
            val instant = Instant(1_788_000_000_000L)
            val date = RediscoverCalendar.localDate(instant)
            val start = RediscoverCalendar.startOfDay(date)
            assertTrue(start <= instant)
            assertTrue(RediscoverCalendar.millisecondsUntilNextDay(instant) > 0L)
        } finally {
            TimeZone.setDefault(previous)
        }
    }
}
