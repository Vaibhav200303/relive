package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.localTimeZone

actual object TimelineCreatedDateFormatter {
    actual fun format(instant: Instant): String = NSDateFormatter().apply {
        dateFormat = "d MMMM yyyy"
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
        timeZone = localTimeZone
    }.stringFromDate(NSDate(timeIntervalSince1970 = instant.epochMilliseconds / 1_000.0))
}
