package com.vaibhav.relive

import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.domain.backup.*
import com.vaibhav.relive.platform.backup.backupAuthLog
import com.vaibhav.relive.platform.media.AndroidMediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class AndroidBackupCoordinator(
    private val context: android.content.Context,
    database: ReliveDatabase,
    mediaStore: AndroidMediaStore,
    private val authorization: AndroidDriveAccessTokenProvider,
    private val onRestoreCompleted: () -> Unit = {},
) : BackupCoordinator {
    private val packageStore = AndroidBackupPackageStore(context, database, mediaStore)
    private val drive = AndroidDriveAppDataClient()
    override suspend fun backup(networkPolicy: BackupNetworkPolicy, onProgress: (BackupProgress) -> Unit): BackupSummary =
        backupMutex.withLock {
            withContext(Dispatchers.IO) {
            enforceNetworkPolicy(networkPolicy)
            onProgress(BackupProgress(0, 0, "Preparing"))
            val packaged = packageStore.create()
            try {
                onProgress(BackupProgress(0, packaged.byteCount, "Uploading"))
                val token = authorization.accessToken() ?: throw GoogleDriveAuthorizationUnavailableException("Google Drive authorization requires reconnecting your Google account.")
                val summary = drive.upload(packaged, token) { completed ->
                    onProgress(BackupProgress(completed, packaged.byteCount, "Uploading"))
                }
                onProgress(BackupProgress(packaged.byteCount, packaged.byteCount, "Uploaded"))
                summary
            } finally {
                java.io.File(packaged.path).delete()
            }
        }
        }

    private fun enforceNetworkPolicy(policy: BackupNetworkPolicy) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity?.activeNetwork?.let(connectivity::getNetworkCapabilities)
        if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) != true) {
            throw BackupUnavailableException("No validated internet connection is available.")
        }
        if (policy == BackupNetworkPolicy.WifiOnly && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI).not()) {
            throw BackupUnavailableException("Waiting for Wi-Fi")
        }
    }

    override suspend fun discoverRestore(): RestorePreview? = withContext(Dispatchers.IO) {
        val token = authorization.accessToken() ?: throw GoogleDriveAuthorizationUnavailableException("Google Drive authorization requires reconnecting your Google account.")
        backupAuthLog("restore discovery started")
        val summary = drive.discover(token)
        backupAuthLog("restore discovery completed found=${summary != null}")
        summary?.let { RestorePreview(it) }
    }

    override suspend fun restore(preview: RestorePreview, onProgress: (BackupProgress) -> Unit) =
        withContext(Dispatchers.IO) {
            val token = authorization.accessToken() ?: throw GoogleDriveAuthorizationUnavailableException("Google Drive authorization requires reconnecting your Google account.")
            val stagedBundle = java.io.File.createTempFile("relive-restore-", ".bundle", context.cacheDir)
            try {
                onProgress(BackupProgress(0, preview.summary.manifest.logicalBytes, "Downloading"))
                drive.download(preview.summary, stagedBundle, token) { completed ->
                    onProgress(BackupProgress(completed, preview.summary.manifest.logicalBytes, "Downloading"))
                }
                require(sha256(stagedBundle) == preview.summary.manifest.bundleSha256) {
                    "Downloaded backup checksum did not match the selected backup."
                }
                onProgress(BackupProgress(stagedBundle.length(), stagedBundle.length(), "Restoring"))
                packageStore.restore(stagedBundle.absolutePath)
                onProgress(BackupProgress(stagedBundle.length(), stagedBundle.length(), "Restored"))
                // Keep the completed restore state on screen long enough for the
                // Compose transition to be perceptible before rebuilding app state.
                delay(550)
                withContext(Dispatchers.Main.immediate) { onRestoreCompleted() }
            } finally {
                stagedBundle.delete()
            }
        }

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        /** One app process may host both an interactive and a WorkManager backup. */
        val backupMutex = Mutex()
    }
}
