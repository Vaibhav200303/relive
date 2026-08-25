package com.vaibhav.relive.platform.notifications

enum class NotificationPermissionState { NotRequested, Granted, Denied, Unavailable }
enum class ReminderSchedulingResult { Scheduled, Cancelled, PermissionDenied, NoEligibleMemories, Failed }

interface RediscoverReminderService {
    fun permissionState(): NotificationPermissionState
    suspend fun requestPermission(): NotificationPermissionState
    suspend fun synchronize(enabled: Boolean): ReminderSchedulingResult
}

internal object UnavailableRediscoverReminderService : RediscoverReminderService {
    override fun permissionState() = NotificationPermissionState.Unavailable
    override suspend fun requestPermission() = NotificationPermissionState.Unavailable
    override suspend fun synchronize(enabled: Boolean) = if (enabled) ReminderSchedulingResult.Failed else ReminderSchedulingResult.Cancelled
}

const val REDISCOVER_NOTIFICATION_TITLE = "A memory is waiting"
const val REDISCOVER_NOTIFICATION_BODY = "Open Relive to revisit this day in your archive."
