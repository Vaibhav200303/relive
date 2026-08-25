package com.vaibhav.relive.domain.backup

class UnavailableBackupCoordinator : BackupCoordinator {
    override suspend fun backup(networkPolicy: BackupNetworkPolicy, onProgress: (BackupProgress) -> Unit): BackupSummary =
        throw BackupUnavailableException("Backup package and Drive upload are not configured on this platform.")

    override suspend fun discoverRestore(): RestorePreview? =
        throw BackupUnavailableException("Restore discovery is not configured on this platform.")

    override suspend fun restore(preview: RestorePreview, onProgress: (BackupProgress) -> Unit) =
        throw BackupUnavailableException("Restore activation is not configured on this platform.")
}
