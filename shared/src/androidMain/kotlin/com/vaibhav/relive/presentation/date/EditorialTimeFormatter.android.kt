package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual object EditorialTimeFormatter {
    actual fun format(instant: Instant): String {
        val fmt = SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return fmt.format(Date(instant.epochMilliseconds))
    }
}
