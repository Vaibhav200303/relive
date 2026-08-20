package com.vaibhav.relive.domain.policy

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Duration
import com.vaibhav.relive.domain.time.Instant

/**
 * The 4-day edit / forget rule (ADR-0005, PRODUCT_SPEC §8).
 *
 * A moment may be edited or forgotten only while
 * `createdAt <= now < createdAt + 4 days`. The window is keyed on the immutable
 * [Moment.createdAt] — `updatedAt` never extends it.
 *
 * Fail-closed on `now < createdAt`: a moment whose creation timestamp is in the
 * future represents an invalid clock-skew state, so both edit and forget are
 * disabled rather than granting an extended eligibility period.
 *
 * Boundary at the upper end is strict-less-than: at exactly `createdAt + 4 days`
 * the moment is no longer editable.
 */
object EditWindow {

    val DURATION: Duration = Duration.ofDays(4)

    fun expiresAt(moment: Moment): Instant = moment.createdAt + DURATION

    fun isEditable(moment: Moment, now: Instant): Boolean =
        now >= moment.createdAt && now < expiresAt(moment)

    fun isForgettable(moment: Moment, now: Instant): Boolean = isEditable(moment, now)

    fun isEditable(moment: Moment, clock: Clock): Boolean = isEditable(moment, clock.now())
    fun isForgettable(moment: Moment, clock: Clock): Boolean = isForgettable(moment, clock.now())
}
