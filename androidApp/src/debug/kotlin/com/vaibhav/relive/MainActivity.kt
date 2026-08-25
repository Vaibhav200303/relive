package com.vaibhav.relive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vaibhav.relive.di.createDefaultReliveAppContainer
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidRestoreRecovery.recover(applicationContext)
        Log.d("ReliveBackupAuth", "MainActivity constructing AndroidGoogleDriveAccountManager")
        val backupPreferences = AndroidBackupPreferencesRepository(applicationContext)
        val accountManager = AndroidGoogleDriveAccountManager(this, backupPreferences)
        val container = createDefaultReliveAppContainer(applicationContext, googleDriveAccountManager = accountManager, backupPreferencesRepository = backupPreferences, backupCoordinatorFactory = { database, mediaStore, _ -> AndroidBackupCoordinator(applicationContext, database, mediaStore, accountManager) { recreate() } })
        android.util.Log.d("ReliveBackupAuth", "BackupCoordinator runtime=${container.backupCoordinator::class.java.name}")
        AndroidBackupDebugTrigger.scheduler = AndroidBackupScheduler(applicationContext)
        setContent { App(container) }
    }
}
