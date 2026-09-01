package com.vaibhav.relive.di

import com.vaibhav.relive.domain.backup.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class InMemoryBackupPreferencesRepository : BackupPreferencesRepository {
    private val cadenceState = MutableStateFlow(BackupCadence.Off)
    private val networkState = MutableStateFlow(BackupNetworkPolicy.WifiOnly)
    private val accountState = MutableStateFlow<GoogleDriveAccount?>(null)
    private val operationState = MutableStateFlow<BackupOperationState>(BackupOperationState.Idle)
    private val summaryState = MutableStateFlow<com.vaibhav.relive.domain.backup.BackupSummary?>(null)
    override val cadence: StateFlow<BackupCadence> = cadenceState
    override val networkPolicy: StateFlow<BackupNetworkPolicy> = networkState
    override val account: StateFlow<GoogleDriveAccount?> = accountState
    override val operation: StateFlow<BackupOperationState> = operationState
    override val remoteSummary: StateFlow<com.vaibhav.relive.domain.backup.BackupSummary?> = summaryState
    override suspend fun setCadence(value: BackupCadence) { cadenceState.value = value }
    override suspend fun setNetworkPolicy(value: BackupNetworkPolicy) { networkState.value = value }
    override suspend fun setAccount(value: GoogleDriveAccount?) { accountState.value = value }
    override suspend fun setOperation(value: BackupOperationState) { operationState.value = value }
    override suspend fun setRemoteSummary(value: com.vaibhav.relive.domain.backup.BackupSummary) { summaryState.value = value }
}
