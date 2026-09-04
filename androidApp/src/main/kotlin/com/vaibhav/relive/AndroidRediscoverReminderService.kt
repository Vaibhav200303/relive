package com.vaibhav.relive

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vaibhav.relive.data.local.DatabaseDriverFactory
import com.vaibhav.relive.data.local.ReliveDatabaseFactory
import com.vaibhav.relive.data.local.repository.SqlDelightRediscoverRepository
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.notifications.NotificationPermissionState
import com.vaibhav.relive.platform.notifications.RediscoverReminderService
import com.vaibhav.relive.platform.notifications.ReminderKind
import com.vaibhav.relive.platform.notifications.ReminderSchedulingResult
import com.vaibhav.relive.platform.notifications.selectReminderCopy
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Schedules Relive's daily reminders. Named for history (ADR-0046 shipped only the Rediscover
 * reminder), it now drives the capture-focused set from ADR-0067: two "add a moment" nudges and one
 * eligibility-gated "revisit a memory" nudge per day. All copy stays generic — no archive field is
 * ever placed in a notification.
 */
class AndroidRediscoverReminderService(private val activity: Activity) : RediscoverReminderService {
    private var permissionCompletion: ((NotificationPermissionState) -> Unit)? = null
    private var lastPermissionResult: NotificationPermissionState? = null

    override fun permissionState(): NotificationPermissionState = when {
        Build.VERSION.SDK_INT < 33 -> NotificationPermissionState.Granted
        activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> NotificationPermissionState.Granted
        lastPermissionResult != null -> lastPermissionResult!!
        activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> NotificationPermissionState.Denied
        else -> NotificationPermissionState.NotRequested
    }

    override suspend fun requestPermission(): NotificationPermissionState {
        if (Build.VERSION.SDK_INT < 33) return NotificationPermissionState.Granted
        return suspendCancellableCoroutine { continuation ->
            permissionCompletion = { if (continuation.isActive) continuation.resume(it) }
            continuation.invokeOnCancellation { permissionCompletion = null }
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
        }
    }

    override suspend fun synchronize(enabled: Boolean): ReminderSchedulingResult {
        val manager = WorkManager.getInstance(activity.applicationContext)
        if (!enabled) {
            ReminderSlot.entries.forEach { manager.cancelUniqueWork(it.workName) }
            ReminderSlot.entries.map { it.notificationId }.toSet().forEach {
                NotificationManagerCompat.from(activity).cancel(it)
            }
            return ReminderSchedulingResult.Cancelled
        }
        ReminderSlot.entries.forEach { slot ->
            manager.enqueueUniquePeriodicWork(slot.workName, ExistingPeriodicWorkPolicy.UPDATE, requestFor(slot))
        }
        return ReminderSchedulingResult.Scheduled
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode != REQUEST_CODE) return false
        lastPermissionResult = if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) NotificationPermissionState.Granted else NotificationPermissionState.Denied
        permissionCompletion?.invoke(lastPermissionResult!!)
        permissionCompletion = null
        return true
    }

    companion object {
        /** Notification id for the Rediscover reminder — retained from ADR-0046. */
        const val NOTIFICATION_ID = 1907
        /** Both capture nudges share one id, so the evening reminder replaces an ignored morning one. */
        const val CAPTURE_NOTIFICATION_ID = 1908
        const val KEY_KIND = "reminder_kind"
        const val KEY_VARIANT = "reminder_variant"
        private const val REQUEST_CODE = 9018

        /** Builds the daily periodic work for one slot. Context-free so it is unit-testable. */
        internal fun requestFor(slot: ReminderSlot): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<ReliveReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMillisTo(slot.hour), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_KIND to slot.kind.name, KEY_VARIANT to slot.variant))
                .build()

        /** Milliseconds from now until the next occurrence of [hour]:00 local time. */
        internal fun initialDelayMillisTo(hour: Int, now: Calendar = Calendar.getInstance()): Long {
            val target = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}

/**
 * The three daily reminder slots. Capture is weighted 2:1 over Rediscover per the user's
 * capture-focused choice. The Rediscover slot keeps the original unique work name so an existing
 * schedule is updated in place rather than orphaned.
 */
internal enum class ReminderSlot(
    val workName: String,
    val hour: Int,
    val kind: ReminderKind,
    val variant: Int,
    val notificationId: Int,
) {
    CaptureMorning("relive-reminder-capture-morning", 10, ReminderKind.Capture, 0, AndroidRediscoverReminderService.CAPTURE_NOTIFICATION_ID),
    Rediscover("relive-rediscover-reminder", 13, ReminderKind.Rediscover, 1, AndroidRediscoverReminderService.NOTIFICATION_ID),
    CaptureEvening("relive-reminder-capture-evening", 19, ReminderKind.Capture, 2, AndroidRediscoverReminderService.CAPTURE_NOTIFICATION_ID),
}

class ReliveReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val kind = runCatching {
            ReminderKind.valueOf(inputData.getString(AndroidRediscoverReminderService.KEY_KIND) ?: ReminderKind.Rediscover.name)
        }.getOrDefault(ReminderKind.Rediscover)
        val variant = inputData.getInt(AndroidRediscoverReminderService.KEY_VARIANT, 0)
        val calendar = Calendar.getInstance()
        val notificationId = if (kind == ReminderKind.Capture) {
            AndroidRediscoverReminderService.CAPTURE_NOTIFICATION_ID
        } else {
            AndroidRediscoverReminderService.NOTIFICATION_ID
        }

        // The Rediscover nudge only fires on days with an eligible On This Day memory; the capture
        // nudge is a gentle habit prompt that always applies. Neither reads archive content into copy.
        if (kind == ReminderKind.Rediscover) {
            val today = LocalCalendarDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
            val startOfToday = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val driver = DatabaseDriverFactory(applicationContext).create()
            val eligible = try {
                SqlDelightRediscoverRepository(ReliveDatabaseFactory.create(driver))
                    .observeOnThisDayPreviews(today, Instant(startOfToday.timeInMillis), 1).first().isNotEmpty()
            } finally {
                driver.close()
            }
            if (!eligible) {
                NotificationManagerCompat.from(applicationContext).cancel(notificationId)
                return Result.success()
            }
        }

        val copy = selectReminderCopy(kind, calendar.get(Calendar.DAY_OF_YEAR) + variant)
        val channelId = if (kind == ReminderKind.Capture) CHANNEL_CAPTURE else CHANNEL_REDISCOVER
        if (Build.VERSION.SDK_INT >= 26) {
            val channelName = if (kind == ReminderKind.Capture) "Capture reminders" else "Rediscover"
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT))
        }
        val contentIntent = if (kind == ReminderKind.Capture) {
            Intent(applicationContext, MainActivity::class.java)
                .setAction(ReliveIntents.ACTION_ADD_MOMENT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } else {
            applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                ?: Intent(applicationContext, MainActivity::class.java)
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_relive_notification)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setContentIntent(PendingIntent.getActivity(applicationContext, notificationId, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT < 33 || applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        }
        return Result.success()
    }

    private companion object {
        const val CHANNEL_REDISCOVER = "relive_rediscover"
        const val CHANNEL_CAPTURE = "relive_capture"
    }
}
