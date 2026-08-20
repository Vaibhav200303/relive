package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant

/**
 * Formats a moment's [Instant] as the composer's time-of-day label, e.g.
 * `10:48 AM`, in the device's local time zone. Implemented per platform (JDK on
 * Android, Foundation on iOS) so no calendar arithmetic is hand-rolled and no
 * new date library is added.
 */
expect object EditorialTimeFormatter {
    fun format(instant: Instant): String
}
