package com.vaibhav.relive.platform.notifications

import com.vaibhav.relive.domain.repository.MomentRepository
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.Foundation.*
import platform.UserNotifications.*

class IosRediscoverReminderService(private val moments: MomentRepository) : RediscoverReminderService {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private var cachedPermission = NotificationPermissionState.NotRequested
    override fun permissionState() = cachedPermission

    override suspend fun requestPermission(): NotificationPermissionState = suspendCoroutine { continuation ->
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            cachedPermission = if (granted) NotificationPermissionState.Granted else NotificationPermissionState.Denied
            continuation.resume(cachedPermission)
        }
    }

    override suspend fun synchronize(enabled: Boolean): ReminderSchedulingResult {
        center.removePendingNotificationRequestsWithIdentifiers(pendingIdentifiers())
        center.removeDeliveredNotificationsWithIdentifiers(pendingIdentifiers())
        if (!enabled) return ReminderSchedulingResult.Cancelled
        val calendar = NSCalendar.currentCalendar
        val now = NSDate()
        val nowParts = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = now)
        val dates = moments.listAll().mapNotNull { moment ->
            val date = NSDate(timeIntervalSinceReferenceDate = moment.createdAt.epochMilliseconds / 1000.0 - REFERENCE_EPOCH_SECONDS)
            val parts = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = date)
            if (parts.year >= nowParts.year) null else (parts.month.toInt() to parts.day.toInt())
        }.distinct().take(MAX_PENDING)
        if (dates.isEmpty()) return ReminderSchedulingResult.NoEligibleMemories
        for ((month, day) in dates) {
            val content = UNMutableNotificationContent().apply { setTitle(REDISCOVER_NOTIFICATION_TITLE); setBody(REDISCOVER_NOTIFICATION_BODY); setSound(UNNotificationSound.defaultSound) }
            val components = NSDateComponents().apply { setMonth(month.toLong()); setDay(day.toLong()); setHour(9) }
            val id = identifier(month, day)
            val request = UNNotificationRequest.requestWithIdentifier(id, content, UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true))
            val added = suspendCoroutine { continuation -> center.addNotificationRequest(request) { error -> continuation.resume(error == null) } }
            if (!added) return ReminderSchedulingResult.Failed
        }
        return ReminderSchedulingResult.Scheduled
    }

    private suspend fun pendingIdentifiers(): List<String> = suspendCoroutine { continuation ->
        center.getPendingNotificationRequestsWithCompletionHandler { requests -> continuation.resume(requests.orEmpty().mapNotNull { (it as? UNNotificationRequest)?.identifier }) }
    }
    private fun identifier(month: Int, day: Int) = "relive.rediscover.$month.$day"
    private companion object { const val MAX_PENDING = 60; const val REFERENCE_EPOCH_SECONDS = 978_307_200.0 }
}
