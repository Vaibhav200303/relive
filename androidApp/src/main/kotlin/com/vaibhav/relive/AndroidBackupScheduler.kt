package com.vaibhav.relive

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vaibhav.relive.domain.backup.BackupCadence
import com.vaibhav.relive.domain.backup.BackupNetworkPolicy
import java.util.concurrent.TimeUnit

/** Android scheduling is approximate: Monthly means one run about every 30 days. */
class AndroidBackupScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    fun reconcile(cadence: BackupCadence, network: BackupNetworkPolicy) {
        if (cadence == BackupCadence.Off) { workManager.cancelUniqueWork(NAME); return }
        val constraints = constraintsFor(network)
        when (cadence) {
            BackupCadence.Daily -> workManager.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, periodicRequest(1, constraints))
            BackupCadence.Weekly -> workManager.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, periodicRequest(7, constraints))
            BackupCadence.Monthly -> workManager.enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, monthlyRequest(constraints))
            BackupCadence.Off -> Unit
        }
    }
    fun triggerNow(network: BackupNetworkPolicy) {
        val constraints = constraintsFor(network)
        workManager.enqueueUniqueWork(DEBUG_NAME, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AndroidAutomaticBackupWorker>().setConstraints(constraints).build())
    }
    companion object {
        const val NAME = "relive_google_drive_auto_backup"
        private const val DEBUG_NAME = "relive_google_drive_auto_backup_debug"
        internal fun constraintsFor(network: BackupNetworkPolicy) = Constraints.Builder().setRequiredNetworkType(if (network == BackupNetworkPolicy.WifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build()
        internal fun periodicRequest(days: Long, constraints: Constraints) = PeriodicWorkRequestBuilder<AndroidAutomaticBackupWorker>(days, TimeUnit.DAYS).setConstraints(constraints).build()
        internal fun monthlyRequest(constraints: Constraints) = OneTimeWorkRequestBuilder<AndroidAutomaticBackupWorker>().setInitialDelay(30, TimeUnit.DAYS).setConstraints(constraints).build()
    }
}
