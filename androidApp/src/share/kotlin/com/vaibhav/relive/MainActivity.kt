package com.vaibhav.relive

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vaibhav.relive.di.createDefaultReliveAppContainer
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import com.vaibhav.relive.platform.capture.QuickCaptureRequestBus
import com.vaibhav.relive.platform.exporting.PortableArchiveRequestBus
import com.vaibhav.relive.platform.share.AndroidIncomingShareGateway
import com.vaibhav.relive.platform.system.LauncherIconController
import com.vaibhav.relive.widget.updateQuickCaptureWidgets
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Shareable friends entry point; it intentionally uses the same production app wiring. */
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
        val backupPreferences = AndroidBackupPreferencesRepository(applicationContext)
        val accountManager = AndroidGoogleDriveAccountManager(this, backupPreferences)
        deviceAuthentication = AndroidDeviceAuthentication(this)
        reminderService = AndroidRediscoverReminderService(this)
        incomingShareGateway = AndroidIncomingShareGateway(applicationContext, shareScope)
        val container = createDefaultReliveAppContainer(
            applicationContext,
            googleDriveAccountManager = accountManager,
            backupPreferencesRepository = backupPreferences,
            backupCoordinatorFactory = { database, mediaStore, _ ->
                AndroidBackupCoordinator(applicationContext, database, mediaStore, accountManager) { recreate() }
            },
            deviceAuthentication = deviceAuthentication,
            rediscoverReminderService = reminderService,
            incomingShareGateway = incomingShareGateway,
            quickCaptureRequestBus = quickCaptureRequestBus,
            portableArchiveRequestBus = portableArchiveRequestBus,
            entitlementProvider = (application as ReliveApplication).entitlementProvider,
            termsOfServiceUrl = BuildConfig.TERMS_OF_SERVICE_URL,
            privacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL,
            supportEmail = BuildConfig.SUPPORT_EMAIL,
        )
        DemoArchiveBootstrap.prepare(applicationContext, container, shareScope)
        launcherIconController = container.launcherIconController
        routeIntent(intent)
        setIntent(Intent(this, MainActivity::class.java))
        setContent { App(container, onIncomingShareCancelled = ::finish) }
        shareScope.launch {
            combine(
                container.appearanceRepository.preferences,
                container.profileSettingsRepository.settings,
                ::Pair,
            )
                .distinctUntilChanged()
                .collect { (appearance, profile) ->
                    runCatching {
                        updateQuickCaptureWidgets(applicationContext, appearance, profile)
                    }
                }
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
        shareScope.launch { (application as ReliveApplication).entitlementProvider.refresh() }
    }

    override fun onStop() {
        // Swap aliases only after the app is off-screen; changing the active launcher component
        // while visible can remove this task or temporarily expose two Relive entries.
        super.onStop()
        launcherIconController?.applyPending()
    }

    private fun routeIntent(incoming: Intent?) {
        if (incoming?.action == Intent.ACTION_VIEW && incoming.data != null) {
            val destination = java.io.File(cacheDir, "incoming-${java.util.UUID.randomUUID()}.relive")
            runCatching {
                contentResolver.openInputStream(incoming.data!!)?.use { input -> copyIncomingArchive(input, destination) } ?: error("Unreadable archive")
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
