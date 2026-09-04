package com.vaibhav.relive

import com.vaibhav.relive.platform.notifications.ReminderKind
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReliveReminderSchedulerTest {
    private val dayMillis = TimeUnit.HOURS.toMillis(24)

    @Test fun scheduleIsCaptureFocused() {
        val kinds = ReminderSlot.entries.map { it.kind }
        assertEquals(2, kinds.count { it == ReminderKind.Capture }, "expected two capture nudges")
        assertEquals(1, kinds.count { it == ReminderKind.Rediscover }, "expected one rediscover nudge")
    }

    @Test fun everySlotRunsDailyOnTheReminderWorker() {
        for (slot in ReminderSlot.entries) {
            val request = AndroidRediscoverReminderService.requestFor(slot)
            assertEquals(dayMillis, request.workSpec.intervalDuration, "slot ${slot.name} should repeat daily")
            assertEquals(
                "com.vaibhav.relive.ReliveReminderWorker",
                request.workSpec.workerClassName,
                "slot ${slot.name} should run the reminder worker",
            )
        }
    }

    @Test fun requestCarriesKindAndVariant() {
        for (slot in ReminderSlot.entries) {
            val input = AndroidRediscoverReminderService.requestFor(slot).workSpec.input
            assertEquals(slot.kind.name, input.getString(AndroidRediscoverReminderService.KEY_KIND))
            assertEquals(slot.variant, input.getInt(AndroidRediscoverReminderService.KEY_VARIANT, -1))
        }
    }

    @Test fun captureSlotsShareOneNotificationIdRediscoverKeepsItsOwn() {
        val captureIds = ReminderSlot.entries.filter { it.kind == ReminderKind.Capture }.map { it.notificationId }.toSet()
        assertEquals(setOf(AndroidRediscoverReminderService.CAPTURE_NOTIFICATION_ID), captureIds)
        val rediscover = ReminderSlot.entries.first { it.kind == ReminderKind.Rediscover }
        assertEquals(AndroidRediscoverReminderService.NOTIFICATION_ID, rediscover.notificationId)
    }

    @Test fun initialDelayTargetsTheNextOccurrenceOfTheHour() {
        val nineAm = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 9, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        // 10:00 today is one hour away; 08:00 has passed, so it lands tomorrow (23h).
        assertEquals(TimeUnit.HOURS.toMillis(1), AndroidRediscoverReminderService.initialDelayMillisTo(10, nineAm))
        assertEquals(TimeUnit.HOURS.toMillis(23), AndroidRediscoverReminderService.initialDelayMillisTo(8, nineAm))
    }

    @Test fun initialDelayAlwaysWithinADay() {
        for (hour in intArrayOf(10, 13, 19)) {
            val delay = AndroidRediscoverReminderService.initialDelayMillisTo(hour)
            assertTrue(delay in 0..dayMillis, "delay for hour $hour out of range: $delay")
        }
    }
}
