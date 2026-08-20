package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.time.Instant

/**
 * Formats a moment's [Instant] as the approved editorial eyebrow, e.g. `SEPTEMBER 28, 2023`,
 * in the device's local time zone. Implemented per platform (JDK on Android, Foundation on
 * iOS) so no calendar arithmetic is hand-rolled and no new date library is added
 * (see ADR platform date formatting).
 */
expect object EditorialDateFormatter {
    fun format(instant: Instant): String
}
