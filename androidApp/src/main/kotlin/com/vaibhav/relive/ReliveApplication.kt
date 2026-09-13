package com.vaibhav.relive

import android.app.Application
import com.vaibhav.relive.domain.backup.BackupCadence
import com.vaibhav.relive.domain.entitlement.EntitlementState
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.PurchaseOutcome
import com.vaibhav.relive.domain.entitlement.RelivePurchaseOption
import com.vaibhav.relive.domain.entitlement.entitlementProviderFor
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReliveApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val entitlementProvider: EntitlementProvider by lazy {
        if (BuildConfig.DEBUG) {
            TemporaryDebugProEntitlementProvider()
        } else {
            entitlementProviderFor(
                publicApiKey = BuildConfig.REVENUECAT_PUBLIC_API_KEY,
                enableDebugLogging = false,
                allowTestStore = false,
            )
        }
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

/** Temporary local-development override. Release builds never instantiate this provider. */
private class TemporaryDebugProEntitlementProvider : EntitlementProvider {
    override val state: StateFlow<EntitlementState> = MutableStateFlow(
        EntitlementState(isPro = true, purchasingAvailable = false),
    )

    override suspend fun purchase(option: RelivePurchaseOption): PurchaseOutcome =
        PurchaseOutcome.Unavailable("Purchasing is disabled while temporary debug Pro access is active.")

    override suspend fun restorePurchases(): PurchaseOutcome =
        PurchaseOutcome.Unavailable("Restore is disabled while temporary debug Pro access is active.")
}
