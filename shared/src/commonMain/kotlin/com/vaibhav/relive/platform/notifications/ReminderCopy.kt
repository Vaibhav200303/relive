package com.vaibhav.relive.platform.notifications

/**
 * The two kinds of gentle daily reminder Relive can show. Both are deliberately generic: per
 * ADR-0046 no archive field (Moment titles, text, media, or locations) ever appears in a
 * notification, so the copy below tempts by warmth and curiosity alone.
 */
enum class ReminderKind { Capture, Rediscover }

/** A single generic reminder message. */
data class ReminderCopy(val title: String, val body: String)

private val CAPTURE_COPY = listOf(
    ReminderCopy("What happened today?", "Add a moment before the day slips away."),
    ReminderCopy("A minute for yourself", "Capture something worth keeping in Relive."),
    ReminderCopy("Today is worth remembering", "Write it down while it is still fresh."),
    ReminderCopy("One small memory", "Add today to your archive."),
)

private val REDISCOVER_COPY = listOf(
    ReminderCopy(REDISCOVER_NOTIFICATION_TITLE, REDISCOVER_NOTIFICATION_BODY),
    ReminderCopy("On this day", "A memory from your past resurfaced. Take a look."),
    ReminderCopy("Look back for a moment", "Something from this day is waiting in your archive."),
    ReminderCopy("A memory resurfaced", "Open Relive to revisit this day."),
)

/**
 * Deterministically pick a generic reminder message. [seed] is typically the day of the year (with
 * a small per-slot offset) so the copy rotates day to day and morning differs from evening, without
 * ever depending on archive content. Negative seeds are handled.
 */
fun selectReminderCopy(kind: ReminderKind, seed: Int): ReminderCopy {
    val pool = when (kind) {
        ReminderKind.Capture -> CAPTURE_COPY
        ReminderKind.Rediscover -> REDISCOVER_COPY
    }
    val index = ((seed % pool.size) + pool.size) % pool.size
    return pool[index]
}
