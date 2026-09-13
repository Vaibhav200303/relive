package com.vaibhav.relive.platform.exporting

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.Foundation.NSUUID
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Posts a small, generic completion nudge when iOS notifications are already
 * enabled. Export completion must never become an implicit permission prompt.
 */
@OptIn(ExperimentalForeignApi::class)
class IosExportCompletionNotifier : ExportCompletionNotifier {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun notify(completion: ExportCompletion) {
        val authorized = suspendCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(
                    settings?.authorizationStatus == UNAuthorizationStatusAuthorized ||
                        settings?.authorizationStatus == UNAuthorizationStatusProvisional ||
                        settings?.authorizationStatus == UNAuthorizationStatusEphemeral,
                )
            }
        }
        if (!authorized) return

        val content = UNMutableNotificationContent().apply {
            if (completion == ExportCompletion.Ready) {
                setTitle("Export ready")
                setBody("Your Relive export is ready to view or share.")
            } else {
                setTitle("Export failed")
                setBody("Relive couldn't finish the export. Please try again.")
            }
            setSound(null)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(0.1, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(
            "relive.export.${NSUUID().UUIDString}",
            content,
            trigger,
        )
        suspendCoroutine<Unit> { continuation ->
            center.addNotificationRequest(request) { continuation.resume(Unit) }
        }
    }
}
