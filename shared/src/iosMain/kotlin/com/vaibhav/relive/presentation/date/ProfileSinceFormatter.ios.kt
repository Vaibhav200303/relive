package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.localTimeZone

actual object ProfileSinceFormatter {
    actual fun format(instant: Instant): String = NSDateFormatter().apply {
        dateFormat = "MMMM yyyy"
        locale = NSLocale(localeIdentifier = "en_US")
        timeZone = localTimeZone
    }.stringFromDate(NSDate(timeIntervalSince1970 = instant.epochMilliseconds / 1_000.0))
}
