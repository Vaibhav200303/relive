package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant

/** Formats a persisted custom-timeline creation time in the device's local time zone. */
expect object TimelineCreatedDateFormatter {
    fun format(instant: Instant): String
}
