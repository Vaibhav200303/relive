package com.vaibhav.relive.presentation.time

import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant

/**
 * Platform-backed wall-clock [Clock]. Actuals read the host clock (JDK
 * `System.currentTimeMillis()` on Android/JVM, `NSDate.timeIntervalSince1970` on
 * iOS). Kept in presentation so the domain stays platform-free and shared code
 * still has a common Clock reference.
 */
expect object SystemClock : Clock {
    override fun now(): Instant
}
