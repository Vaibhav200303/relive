package com.vaibhav.relive.presentation.time

import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object SystemClock : Clock {
    actual override fun now(): Instant =
        Instant((NSDate().timeIntervalSince1970 * 1_000.0).toLong())
}
