package com.vaibhav.relive

import android.app.Application
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReliveApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        val preferences = AndroidBackupPreferencesRepository(this)
        val scheduler = AndroidBackupScheduler(this)
        scope.launch {
            combine(preferences.cadence, preferences.networkPolicy) { cadence, network -> cadence to network }
                .collect { (cadence, network) -> scheduler.reconcile(cadence, network) }
        }
    }
}
