package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.repository.ProfileSettingsRepository
import com.vaibhav.relive.platform.notifications.NotificationPermissionState
import com.vaibhav.relive.platform.notifications.RediscoverReminderService
import com.vaibhav.relive.platform.notifications.ReminderSchedulingResult

class RediscoverReminderController(
    private val settings: ProfileSettingsRepository,
    private val service: RediscoverReminderService,
) {
    suspend fun setEnabled(enabled: Boolean): ReminderSchedulingResult {
        if (!enabled) {
            service.synchronize(false)
            settings.setRediscoverRemindersEnabled(false)
            return ReminderSchedulingResult.Cancelled
        }
        val permission = when (service.permissionState()) {
            NotificationPermissionState.Granted -> NotificationPermissionState.Granted
            else -> service.requestPermission()
        }
        if (permission != NotificationPermissionState.Granted) {
            settings.setRediscoverRemindersEnabled(false)
            return ReminderSchedulingResult.PermissionDenied
        }
        val result = service.synchronize(true)
        if (result == ReminderSchedulingResult.Scheduled || result == ReminderSchedulingResult.NoEligibleMemories) {
            if (settings.setRediscoverRemindersEnabled(true).isFailure) {
                service.synchronize(false)
                return ReminderSchedulingResult.Failed
            }
        }
        return result
    }
}
