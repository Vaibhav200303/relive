package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant

/** Formats a persisted Profile creation time in the device's local time zone. */
expect object ProfileSinceFormatter {
    fun format(instant: Instant): String
}
