package com.vaibhav.relive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vaibhav.relive.data.local.DatabaseDriverFactory
import com.vaibhav.relive.data.local.ReliveDatabaseFactory
import com.vaibhav.relive.domain.backup.BackupOperationState
import com.vaibhav.relive.domain.backup.BackupUnavailableException
import com.vaibhav.relive.domain.backup.GoogleDriveAuthorizationUnavailableException
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import com.vaibhav.relive.platform.media.AndroidMediaStore
import kotlinx.coroutines.flow.first

class AndroidAutomaticBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = AndroidBackupPreferencesRepository(applicationContext)
        if (prefs.cadence.first().name == "Off") return Result.success()
        val driver = DatabaseDriverFactory(applicationContext).create()
        return try {
            val coordinator = AndroidBackupCoordinator(
                context = applicationContext,
                database = ReliveDatabaseFactory.create(driver),
                mediaStore = AndroidMediaStore(applicationContext),
                authorization = AndroidBackgroundDriveAccessTokenProvider(applicationContext),
            )
            val summary = coordinator.backup(prefs.networkPolicy.first())
            prefs.setRemoteSummary(summary)
            prefs.setOperation(BackupOperationState.Succeeded(summary))
            // Monthly is intentionally an approximate 30-day cadence. Reconciliation
            // replaces the same unique one-time work, preventing duplicate chains.
            AndroidBackupScheduler(applicationContext).reconcile(prefs.cadence.first(), prefs.networkPolicy.first())
            Result.success()
        } catch (error: GoogleDriveAuthorizationUnavailableException) {
            prefs.setOperation(BackupOperationState.AuthorizationRequired)
            Result.failure()
        } catch (error: BackupUnavailableException) {
            if (error.message == "Waiting for Wi-Fi" || error.message?.contains("validated internet") == true) Result.retry() else Result.failure()
        } catch (error: java.io.IOException) {
            Result.retry()
        } catch (error: Exception) {
            prefs.setOperation(BackupOperationState.Failed(error.message ?: "Automatic backup failed."))
            Result.failure()
        } finally {
            driver.close()
        }
    }
}
