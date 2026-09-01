package com.vaibhav.relive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vaibhav.relive.di.createDefaultReliveAppContainer
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import android.util.Log
import android.content.Intent
import com.vaibhav.relive.platform.share.AndroidIncomingShareGateway
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    private lateinit var deviceAuthentication: AndroidDeviceAuthentication
    private lateinit var reminderService: AndroidRediscoverReminderService
    private val shareScope = MainScope()
    private lateinit var incomingShareGateway: AndroidIncomingShareGateway

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidRestoreRecovery.recover(applicationContext)
        Log.d("ReliveBackupAuth", "MainActivity constructing AndroidGoogleDriveAccountManager")
        val backupPreferences = AndroidBackupPreferencesRepository(applicationContext)
        val accountManager = AndroidGoogleDriveAccountManager(this, backupPreferences)
        deviceAuthentication = AndroidDeviceAuthentication(this)
        reminderService = AndroidRediscoverReminderService(this)
        incomingShareGateway = AndroidIncomingShareGateway(applicationContext, shareScope)
        val container = createDefaultReliveAppContainer(applicationContext, googleDriveAccountManager = accountManager, backupPreferencesRepository = backupPreferences, backupCoordinatorFactory = { database, mediaStore, _ -> AndroidBackupCoordinator(applicationContext, database, mediaStore, accountManager) { recreate() } }, deviceAuthentication = deviceAuthentication, rediscoverReminderService = reminderService, incomingShareGateway = incomingShareGateway, entitlementProvider = (application as ReliveApplication).entitlementProvider, termsOfServiceUrl = BuildConfig.TERMS_OF_SERVICE_URL, privacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL)
        android.util.Log.d("ReliveBackupAuth", "BackupCoordinator runtime=${container.backupCoordinator::class.java.name}")
        incomingShareGateway.accept(intent)
        setIntent(Intent(this, MainActivity::class.java))
        setContent { App(container, onIncomingShareCancelled = ::finish) }
    }

    @Deprecated("Platform credential fallback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        if (!deviceAuthentication.onActivityResult(requestCode, resultCode)) super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (!reminderService.onRequestPermissionsResult(requestCode, grantResults)) super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingShareGateway.accept(intent)
        setIntent(Intent(this, MainActivity::class.java))
    }

    override fun onDestroy() {
        if (isFinishing) incomingShareGateway.cancel()
        shareScope.cancel()
        super.onDestroy()
    }
}
