package com.vaibhav.relive

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vaibhav.relive.data.local.DatabaseDriverFactory
import com.vaibhav.relive.data.local.ReliveDatabaseFactory
import com.vaibhav.relive.data.local.repository.SqlDelightRediscoverRepository
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.notifications.NotificationPermissionState
import com.vaibhav.relive.platform.notifications.REDISCOVER_NOTIFICATION_BODY
import com.vaibhav.relive.platform.notifications.REDISCOVER_NOTIFICATION_TITLE
import com.vaibhav.relive.platform.notifications.RediscoverReminderService
import com.vaibhav.relive.platform.notifications.ReminderSchedulingResult
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine

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
            manager.cancelUniqueWork(WORK_NAME)
            NotificationManagerCompat.from(activity).cancel(NOTIFICATION_ID)
            return ReminderSchedulingResult.Cancelled
        }
        val request = PeriodicWorkRequestBuilder<RediscoverReminderWorker>(24, TimeUnit.HOURS).build()
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        return ReminderSchedulingResult.Scheduled
    }
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode != REQUEST_CODE) return false
        lastPermissionResult = if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) NotificationPermissionState.Granted else NotificationPermissionState.Denied
        permissionCompletion?.invoke(lastPermissionResult!!)
        permissionCompletion = null
        return true
    }
    companion object { const val WORK_NAME = "relive-rediscover-reminder"; const val NOTIFICATION_ID = 1907; private const val REQUEST_CODE = 9018 }
}

class RediscoverReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val calendar = Calendar.getInstance()
        val today = LocalCalendarDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val driver = DatabaseDriverFactory(applicationContext).create()
        val eligible = try {
            SqlDelightRediscoverRepository(ReliveDatabaseFactory.create(driver)).observeOnThisDayPreviews(today, Instant(calendar.timeInMillis), 1).first().isNotEmpty()
        } finally { driver.close() }
        if (!eligible) { NotificationManagerCompat.from(applicationContext).cancel(AndroidRediscoverReminderService.NOTIFICATION_ID); return Result.success() }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Rediscover", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(REDISCOVER_NOTIFICATION_TITLE)
            .setContentText(REDISCOVER_NOTIFICATION_BODY)
            .setContentIntent(PendingIntent.getActivity(applicationContext, 0, applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true).build()
        if (Build.VERSION.SDK_INT < 33 || applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(AndroidRediscoverReminderService.NOTIFICATION_ID, notification)
        }
        return Result.success()
    }
    private companion object { const val CHANNEL = "relive_rediscover" }
}
