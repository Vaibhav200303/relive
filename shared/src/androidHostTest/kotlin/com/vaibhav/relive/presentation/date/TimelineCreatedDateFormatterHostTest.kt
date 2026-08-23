package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineCreatedDateFormatterHostTest {

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
    fun formatsPersistedDateWithoutCreatedPrefix() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("23 August 2026", TimelineCreatedDateFormatter.format(Instant(1_787_500_800_000L)))
    }
}
