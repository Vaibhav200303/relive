package com.vaibhav.relive.presentation.time

import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant

actual object SystemClock : Clock {
    actual override fun now(): Instant = Instant(System.currentTimeMillis())
}
