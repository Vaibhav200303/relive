package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual object ProfileSinceFormatter {
    actual fun format(instant: Instant): String = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }.format(Date(instant.epochMilliseconds))
}
