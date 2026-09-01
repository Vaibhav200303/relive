package com.vaibhav.relive

import android.app.Application
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.entitlementProviderFor
import com.vaibhav.relive.domain.backup.BackupCadence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReliveApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val entitlementProvider: EntitlementProvider by lazy {
        entitlementProviderFor(BuildConfig.REVENUECAT_PUBLIC_API_KEY, enableDebugLogging = BuildConfig.DEBUG)
    }
    override fun onCreate() {
        super.onCreate()
        val preferences = AndroidBackupPreferencesRepository(this)
        val scheduler = AndroidBackupScheduler(this)
        scope.launch {
            combine(preferences.cadence, preferences.networkPolicy, entitlementProvider.state) { cadence, network, entitlement ->
                if (entitlement.isPro) cadence to network else BackupCadence.Off to network
            }.collect { (cadence, network) -> scheduler.reconcile(cadence, network) }
        }
    }
}
