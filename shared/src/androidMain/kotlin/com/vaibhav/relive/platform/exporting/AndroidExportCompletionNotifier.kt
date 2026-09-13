package com.vaibhav.relive.platform.exporting

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Posts a generic export result without exposing archive content in the notification shade.
 * Notification permission is intentionally never requested here; Settings owns that decision.
 */
class AndroidExportCompletionNotifier(context: Context) : ExportCompletionNotifier {
    private val applicationContext = context.applicationContext

    override suspend fun notify(completion: ExportCompletion) {
        if (!notificationsAllowed()) return
        ensureChannel()

        val launchIntent = applicationContext.packageManager.getLaunchIntentForPackage(
            applicationContext.packageName,
        ) ?: Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(applicationContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            EXPORT_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val copy = when (completion) {
            ExportCompletion.Ready -> ExportNotificationCopy(
                title = "Relive export ready",
                body = "Your export is ready to view or share.",
            )
            ExportCompletion.Failed -> ExportNotificationCopy(
                title = "Relive export failed",
                body = "Relive could not finish that export.",
            )
        }
        val icon = applicationContext.resources
            .getIdentifier("ic_relive_notification", "drawable", applicationContext.packageName)
            .takeIf { it != 0 }
            ?: applicationContext.applicationInfo.icon
        val notification = NotificationCompat.Builder(applicationContext, EXPORT_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        // The same stable ID makes a later completion replace an older export result rather than
        // stacking an unbounded set of status notifications. The ID does not overlap reminders.
        NotificationManagerCompat.from(applicationContext).notify(EXPORT_NOTIFICATION_ID, notification)
    }

    private fun notificationsAllowed(): Boolean {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                EXPORT_CHANNEL_ID,
                "Exports",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Export status"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                setShowBadge(false)
            },
        )
    }

    private data class ExportNotificationCopy(val title: String, val body: String)

    private companion object {
        /** Separate from reminder IDs 1907 and 1908; repeated completion calls replace this item. */
        const val EXPORT_NOTIFICATION_ID = 1909
        const val EXPORT_CHANNEL_ID = "relive_exports"
    }
}
