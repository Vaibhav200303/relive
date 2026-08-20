package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.presentation.date.EditorialDateFormatter
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorialDateFormatterHostTest {

    private var previousDefault: TimeZone? = null

    @BeforeTest
    fun captureDefault() {
        previousDefault = TimeZone.getDefault()
    }

    @AfterTest
    fun restoreDefault() {
        previousDefault?.let { TimeZone.setDefault(it) }
    }

    @Test
    fun formatsInEditorialUppercaseStyle() {
        // 2023-09-28T15:00:00Z is still 2023-09-28 in UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val instant = Instant(1_695_913_200_000L)
        assertEquals("SEPTEMBER 28, 2023", EditorialDateFormatter.format(instant))
    }

    @Test
    fun respectsDeviceLocalTimeZone() {
        // 2024-01-01T02:30:00Z is 2023-12-31 in a UTC-4 zone.
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT+4"))
        val instant = Instant(1_704_076_200_000L)
        assertEquals("DECEMBER 31, 2023", EditorialDateFormatter.format(instant))
    }
}
