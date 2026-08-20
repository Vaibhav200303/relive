package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual object EditorialDateFormatter {
    actual fun format(instant: Instant): String {
        val fmt = SimpleDateFormat("MMMM d, yyyy", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return fmt.format(Date(instant.epochMilliseconds)).uppercase(Locale.US)
    }
}
