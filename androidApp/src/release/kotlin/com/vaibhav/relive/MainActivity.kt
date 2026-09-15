package com.vaibhav.relive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vaibhav.relive.di.createDefaultReliveAppContainer
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import android.util.Log
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.vaibhav.relive.platform.capture.QuickCaptureRequestBus
import com.vaibhav.relive.platform.exporting.PortableArchiveRequestBus
import com.vaibhav.relive.platform.share.AndroidIncomingShareGateway
import com.vaibhav.relive.platform.system.LauncherIconController
import com.vaibhav.relive.widget.ReliveQuickCaptureWidget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var deviceAuthentication: AndroidDeviceAuthentication
    private lateinit var reminderService: AndroidRediscoverReminderService
    private val shareScope = MainScope()
    private lateinit var incomingShareGateway: AndroidIncomingShareGateway
    private val quickCaptureRequestBus = QuickCaptureRequestBus()
    private val portableArchiveRequestBus = PortableArchiveRequestBus()
    private var launcherIconController: LauncherIconController? = null

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
        val container = createDefaultReliveAppContainer(applicationContext, googleDriveAccountManager = accountManager, backupPreferencesRepository = backupPreferences, backupCoordinatorFactory = { database, mediaStore, _ -> AndroidBackupCoordinator(applicationContext, database, mediaStore, accountManager) { recreate() } }, deviceAuthentication = deviceAuthentication, rediscoverReminderService = reminderService, incomingShareGateway = incomingShareGateway, quickCaptureRequestBus = quickCaptureRequestBus, portableArchiveRequestBus = portableArchiveRequestBus, entitlementProvider = (application as ReliveApplication).entitlementProvider, termsOfServiceUrl = BuildConfig.TERMS_OF_SERVICE_URL, privacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL, supportEmail = BuildConfig.SUPPORT_EMAIL)
        launcherIconController = container.launcherIconController
        android.util.Log.d("ReliveBackupAuth", "BackupCoordinator runtime=${container.backupCoordinator::class.java.name}")
        routeIntent(intent)
        setIntent(Intent(this, MainActivity::class.java))
        setContent { App(container, onIncomingShareCancelled = ::finish) }
        // Keep the home-screen widget in sync with the chosen palette/mode. The theme can only be
        // changed while the app is running, so observing here covers every real change.
        shareScope.launch {
            container.appearanceRepository.preferences
                .map { it.mode to it.defaultTheme }
                .distinctUntilChanged()
                .drop(1)
                .collect { ReliveQuickCaptureWidget().updateAll(applicationContext) }
        }
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
        routeIntent(intent)
        setIntent(Intent(this, MainActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        shareScope.launch {
            (application as ReliveApplication).entitlementProvider.refresh()
        }
    }

    override fun onStop() {
        // Alias changes are deferred until the activity is no longer visible so launchers cannot
        // tear down the running process while a person is changing the palette.
        super.onStop()
        launcherIconController?.applyPending()
    }

    /** ADD_MOMENT (notification/widget) opens the composer; anything else is a share intent. */
    private fun routeIntent(incoming: Intent?) {
        if (incoming?.action == Intent.ACTION_VIEW && incoming.data != null) {
            val destination = java.io.File(cacheDir, "incoming-${java.util.UUID.randomUUID()}.relive")
            runCatching {
                contentResolver.openInputStream(incoming.data!!)?.use { input -> destination.outputStream().use { input.copyTo(it) } } ?: error("Unreadable archive")
                portableArchiveRequestBus.open(destination.absolutePath)
            }.onFailure { destination.delete() }
        } else if (incoming?.action == ReliveIntents.ACTION_ADD_MOMENT) {
            quickCaptureRequestBus.request()
        } else {
            incomingShareGateway.accept(incoming)
        }
    }

    override fun onDestroy() {
        if (isFinishing) incomingShareGateway.cancel()
        shareScope.cancel()
        super.onDestroy()
    }
}
