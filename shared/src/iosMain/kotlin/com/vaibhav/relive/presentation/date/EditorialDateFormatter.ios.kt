package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

actual object EditorialDateFormatter {
    actual fun format(instant: Instant): String {
        val formatter = NSDateFormatter().apply {
            locale = NSLocale(localeIdentifier = "en_US_POSIX")
            timeZone = NSTimeZone.localTimeZone
            dateFormat = "MMMM d, yyyy"
        }
        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochMilliseconds / 1000.0)
        // `en_US_POSIX` yields ASCII month names, so `uppercase()` is locale-safe here.
        return formatter.stringFromDate(date).uppercase()
    }
}
