package com.vaibhav.relive.domain.time

import kotlin.jvm.JvmInline

@JvmInline
value class Instant(val epochMilliseconds: Long) : Comparable<Instant> {
    override fun compareTo(other: Instant): Int = epochMilliseconds.compareTo(other.epochMilliseconds)

    operator fun plus(duration: Duration): Instant = Instant(epochMilliseconds + duration.inWholeMilliseconds)
    operator fun minus(duration: Duration): Instant = Instant(epochMilliseconds - duration.inWholeMilliseconds)
    operator fun minus(other: Instant): Duration = Duration.ofMilliseconds(epochMilliseconds - other.epochMilliseconds)
}

@JvmInline
value class Duration private constructor(val inWholeMilliseconds: Long) : Comparable<Duration> {
    override fun compareTo(other: Duration): Int = inWholeMilliseconds.compareTo(other.inWholeMilliseconds)

    companion object {
        fun ofMilliseconds(ms: Long): Duration = Duration(ms)
        fun ofSeconds(seconds: Long): Duration = Duration(seconds * 1_000L)
        fun ofMinutes(minutes: Long): Duration = Duration(minutes * 60_000L)
        fun ofHours(hours: Long): Duration = Duration(hours * 3_600_000L)
        fun ofDays(days: Long): Duration = Duration(days * 86_400_000L)
    }
}

fun interface Clock {
    fun now(): Instant
}
