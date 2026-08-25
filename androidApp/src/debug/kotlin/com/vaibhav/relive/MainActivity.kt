package com.vaibhav.relive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vaibhav.relive.di.createDefaultReliveAppContainer
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var deviceAuthentication: AndroidDeviceAuthentication
    private lateinit var reminderService: AndroidRediscoverReminderService

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidRestoreRecovery.recover(applicationContext)
        Log.d("ReliveBackupAuth", "MainActivity constructing AndroidGoogleDriveAccountManager")
        val backupPreferences = AndroidBackupPreferencesRepository(applicationContext)
        val accountManager = AndroidGoogleDriveAccountManager(this, backupPreferences)
        deviceAuthentication = AndroidDeviceAuthentication(this)
        reminderService = AndroidRediscoverReminderService(this)
        val container = createDefaultReliveAppContainer(applicationContext, googleDriveAccountManager = accountManager, backupPreferencesRepository = backupPreferences, backupCoordinatorFactory = { database, mediaStore, _ -> AndroidBackupCoordinator(applicationContext, database, mediaStore, accountManager) { recreate() } }, deviceAuthentication = deviceAuthentication, rediscoverReminderService = reminderService)
        android.util.Log.d("ReliveBackupAuth", "BackupCoordinator runtime=${container.backupCoordinator::class.java.name}")
        AndroidBackupDebugTrigger.scheduler = AndroidBackupScheduler(applicationContext)
        setContent { App(container) }
    }

    @Deprecated("Platform credential fallback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        if (!deviceAuthentication.onActivityResult(requestCode, resultCode)) super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (!reminderService.onRequestPermissionsResult(requestCode, grantResults)) super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
