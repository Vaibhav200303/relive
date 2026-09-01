package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.backup.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.vaibhav.relive.platform.backup.backupAuthLog
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.EntitlementPolicy
import com.vaibhav.relive.domain.entitlement.UnavailableEntitlementProvider

data class BackupRestoreUiState(val account: GoogleDriveAccount?, val cadence: BackupCadence, val networkPolicy: BackupNetworkPolicy, val operation: BackupOperationState, val remoteSummary: BackupSummary? = null, val restorePreview: RestorePreview? = null, val upgradeRequired: Boolean = false)

class BackupRestoreViewModel(private val preferences: BackupPreferencesRepository, private val accountManager: GoogleDriveAccountManager, private val coordinator: BackupCoordinator, private val scope: CoroutineScope, private val entitlementProvider: EntitlementProvider = UnavailableEntitlementProvider()) {
    private val restorePreview = MutableStateFlow<RestorePreview?>(null)
    private val upgradeRequired = MutableStateFlow(false)
    private val baseState = combine(
        combine(preferences.account, preferences.cadence, preferences.networkPolicy, preferences.operation, preferences.remoteSummary) { a, c, n, o, s ->
            BackupRestoreUiState(a, c, n, o, s)
        },
        upgradeRequired,
    ) { base, upgrade -> base.copy(upgradeRequired = upgrade) }
    val state: StateFlow<BackupRestoreUiState> = combine(baseState, restorePreview) { base, preview -> base.copy(restorePreview = preview) }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), BackupRestoreUiState(null, BackupCadence.Weekly, BackupNetworkPolicy.WifiOnly, BackupOperationState.Idle))
    fun setCadence(value: BackupCadence) = scope.launch {
        if (value != BackupCadence.Off && !EntitlementPolicy(entitlementProvider.state.value).mayScheduleBackup()) {
            upgradeRequired.value = true
        } else {
            upgradeRequired.value = false
            preferences.setCadence(value)
        }
    }
    fun clearUpgradeRequired() { upgradeRequired.value = false }
    fun setNetworkPolicy(value: BackupNetworkPolicy) = scope.launch { preferences.setNetworkPolicy(value) }
    fun connectAccount() = scope.launch { backupAuthLog("viewModel connect event"); connectThen(null) }
    fun disconnectAccount() = scope.launch { accountManager.disconnect(); preferences.setAccount(null); preferences.setOperation(BackupOperationState.Idle) }
    fun backUpNow() = scope.launch { ensureConnectedThen { runBackup() } }
    fun restore() = scope.launch { ensureConnectedThen { runRestoreDiscovery() } }
    fun confirmRestore() = scope.launch {
        val preview = restorePreview.value ?: return@launch
        // The confirmation is complete once the user chooses replacement. Keep the
        // in-screen restore progress visible instead of leaving the dialog on top.
        restorePreview.value = null
        preferences.setOperation(BackupOperationState.PreparingRestore)
        try {
            coordinator.restore(preview) { progress -> scope.launch { preferences.setOperation(BackupOperationState.Downloading(progress)) } }
            preferences.setOperation(BackupOperationState.Succeeded(preview.summary))
        } catch (error: Exception) {
            preferences.setOperation(BackupOperationState.Failed(error.message ?: "Restore failed."))
        }
    }
    fun clearOperation() = scope.launch { restorePreview.value = null; preferences.setOperation(BackupOperationState.Idle) }
    private suspend fun connectThen(next: (suspend () -> Unit)?) {
        try {
            backupAuthLog("account manager connect entered; implementation=${accountManager::class.simpleName}")
            val account = accountManager.connect() ?: return
            preferences.setAccount(account)
            preferences.setOperation(BackupOperationState.Idle)
            next?.invoke()
        } catch (error: GoogleDriveAuthorizationUnavailableException) {
            backupAuthLog("error type=${error::class.simpleName} message=${error.message}")
            preferences.setOperation(BackupOperationState.Failed(error.message ?: "Google Drive authorization is unavailable."))
        } catch (error: Exception) {
            backupAuthLog("error type=${error::class.simpleName} message=${error.message}")
            preferences.setOperation(BackupOperationState.Failed(error.message ?: "Google account connection could not be completed."))
        }
    }

    private suspend fun ensureConnectedThen(next: suspend () -> Unit) {
        if (preferences.account.first() != null) {
            next()
        } else {
            connectThen(next)
        }
    }

    private suspend fun runBackup() {
        preferences.setOperation(BackupOperationState.Preparing)
        try {
            val summary = coordinator.backup(preferences.networkPolicy.first()) { progress ->
                scope.launch { preferences.setOperation(BackupOperationState.Uploading(progress)) }
            }
            preferences.setRemoteSummary(summary)
            preferences.setOperation(BackupOperationState.Succeeded(summary))
        } catch (error: GoogleDriveAuthorizationUnavailableException) {
            preferences.setOperation(BackupOperationState.AuthorizationRequired)
        } catch (error: Exception) {
            backupAuthLog("backup failed type=${error::class.simpleName} message=${error.message}")
            preferences.setOperation(BackupOperationState.Failed(error.message ?: "Backup failed."))
        }
    }

    private suspend fun runRestoreDiscovery() {
        preferences.setOperation(BackupOperationState.PreparingRestore)
        try {
            val preview = coordinator.discoverRestore()
            if (preview == null) preferences.setOperation(BackupOperationState.Failed("No Relive backup was found for this Google account."))
            else { restorePreview.value = preview; preferences.setOperation(BackupOperationState.Idle) }
        } catch (error: GoogleDriveAuthorizationUnavailableException) {
            preferences.setOperation(BackupOperationState.AuthorizationRequired)
        } catch (error: Exception) {
            backupAuthLog("restore discovery failed type=${error::class.simpleName} message=${error.message}")
            preferences.setOperation(BackupOperationState.Failed(error.message ?: "Restore discovery failed."))
        }
    }
}
