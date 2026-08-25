package com.vaibhav.relive.domain.backup

import kotlinx.coroutines.flow.Flow

enum class BackupCadence { Off, Daily, Weekly, Monthly }
enum class BackupNetworkPolicy { WifiOnly, WifiOrCellular }

data class GoogleDriveAccount(val subjectId: String, val email: String)
data class BackupManifest(val formatVersion: Int, val generation: String, val createdAt: Long, val momentCount: Long, val logicalBytes: Long, val bundleSha256: String)
data class BackupSummary(val manifest: BackupManifest, val remoteBundleId: String)
data class BackupStatus(val summary: BackupSummary?, val lastError: String? = null)
data class BackupProgress(val completedBytes: Long, val totalBytes: Long, val phase: String)
data class RestorePreview(val summary: BackupSummary, val requiresEmptyArchive: Boolean = true)

sealed interface BackupOperationState {
    data object Idle : BackupOperationState
    data object Preparing : BackupOperationState
    data object PreparingRestore : BackupOperationState
    data class Uploading(val progress: BackupProgress) : BackupOperationState
    data class Downloading(val progress: BackupProgress) : BackupOperationState
    data object WaitingForWifi : BackupOperationState
    data object AuthorizationRequired : BackupOperationState
    data object QuotaUnavailable : BackupOperationState
    data class Failed(val message: String) : BackupOperationState
    data class Corrupt(val message: String) : BackupOperationState
    data class UnsupportedVersion(val version: Int) : BackupOperationState
    data class Succeeded(val summary: BackupSummary) : BackupOperationState
}

interface BackupPreferencesRepository {
    val cadence: Flow<BackupCadence>
    val networkPolicy: Flow<BackupNetworkPolicy>
    val account: Flow<GoogleDriveAccount?>
    val operation: Flow<BackupOperationState>
    val remoteSummary: Flow<BackupSummary?> get() = kotlinx.coroutines.flow.flowOf(null)
    suspend fun setCadence(value: BackupCadence)
    suspend fun setNetworkPolicy(value: BackupNetworkPolicy)
    suspend fun setAccount(value: GoogleDriveAccount?)
    suspend fun setOperation(value: BackupOperationState)
    suspend fun setRemoteSummary(value: BackupSummary)
}

interface GoogleDriveAccountManager {
    suspend fun connect(): GoogleDriveAccount?
    suspend fun disconnect()
}

class GoogleDriveAuthorizationUnavailableException(message: String) : IllegalStateException(message)

interface DriveAppDataClient {
    suspend fun backup(packagePath: String, manifest: BackupManifest): BackupSummary
    suspend fun discover(): BackupSummary?
    suspend fun download(summary: BackupSummary, destinationPath: String)
}

interface BackupScheduler { fun schedule(cadence: BackupCadence); fun cancel() }
data class PackagedBackup(val path: String, val manifest: BackupManifest, val byteCount: Long)
interface BackupPackageStore { suspend fun create(): PackagedBackup }
interface ConnectivityProvider { fun isAllowed(policy: BackupNetworkPolicy): Boolean }

interface BackupCoordinator {
    suspend fun backup(networkPolicy: BackupNetworkPolicy, onProgress: (BackupProgress) -> Unit = {}): BackupSummary
    suspend fun discoverRestore(): RestorePreview?
    suspend fun restore(preview: RestorePreview, onProgress: (BackupProgress) -> Unit = {})
}

class BackupUnavailableException(message: String) : IllegalStateException(message)
