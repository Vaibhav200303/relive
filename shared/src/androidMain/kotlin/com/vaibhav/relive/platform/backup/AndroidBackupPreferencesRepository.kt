package com.vaibhav.relive.platform.backup

import android.content.Context
import com.vaibhav.relive.domain.backup.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidBackupPreferencesRepository(context: Context) : BackupPreferencesRepository {
    private val prefs = context.getSharedPreferences("relive_backup", Context.MODE_PRIVATE)
    private val cadenceState = MutableStateFlow(runCatching { BackupCadence.valueOf(prefs.getString("cadence", "Weekly")!!) }.getOrDefault(BackupCadence.Weekly))
    private val networkState = MutableStateFlow(runCatching { BackupNetworkPolicy.valueOf(prefs.getString("network", "WifiOnly")!!) }.getOrDefault(BackupNetworkPolicy.WifiOnly))
    private val accountState = MutableStateFlow(
        prefs.getString("account_email", null)?.takeIf { it.isNotBlank() }?.let { email ->
            GoogleDriveAccount(
                subjectId = prefs.getString("account_subject_id", null)?.takeIf { it.isNotBlank() } ?: email,
                email = email,
            )
        },
    )
    private val operationState = MutableStateFlow<BackupOperationState>(
        if (prefs.getBoolean("authorization_required", false)) BackupOperationState.AuthorizationRequired else BackupOperationState.Idle,
    )
    private val summaryState = MutableStateFlow(
        prefs.getString("summary_generation", null)?.takeIf { it.isNotBlank() }?.let { generation ->
            BackupSummary(
                manifest = BackupManifest(
                    formatVersion = prefs.getInt("summary_format_version", 1),
                    generation = generation,
                    createdAt = prefs.getLong("summary_created_at", 0L),
                    momentCount = prefs.getLong("summary_moment_count", 0L),
                    logicalBytes = prefs.getLong("summary_logical_bytes", 0L),
                    bundleSha256 = prefs.getString("summary_bundle_sha256", "") ?: "",
                ),
                remoteBundleId = prefs.getString("summary_remote_bundle_id", "") ?: "",
            )
        },
    )
    override val cadence: StateFlow<BackupCadence> = cadenceState
    override val networkPolicy: StateFlow<BackupNetworkPolicy> = networkState
    override val account: StateFlow<GoogleDriveAccount?> = accountState
    override val operation: StateFlow<BackupOperationState> = operationState
    override val remoteSummary: StateFlow<BackupSummary?> = summaryState
    override suspend fun setCadence(value: BackupCadence) { cadenceState.value = value; prefs.edit().putString("cadence", value.name).apply() }
    override suspend fun setNetworkPolicy(value: BackupNetworkPolicy) { networkState.value = value; prefs.edit().putString("network", value.name).apply() }
    override suspend fun setAccount(value: GoogleDriveAccount?) {
        accountState.value = value
        prefs.edit().apply {
            if (value == null) {
                remove("account_email")
                remove("account_subject_id")
            } else {
                putString("account_email", value.email)
                putString("account_subject_id", value.subjectId)
            }
        }.apply()
    }
    override suspend fun setOperation(value: BackupOperationState) {
        operationState.value = value
        prefs.edit().putBoolean("authorization_required", value is BackupOperationState.AuthorizationRequired).apply()
    }
    override suspend fun setRemoteSummary(value: BackupSummary) {
        summaryState.value = value
        prefs.edit()
            .putInt("summary_format_version", value.manifest.formatVersion)
            .putString("summary_generation", value.manifest.generation)
            .putLong("summary_created_at", value.manifest.createdAt)
            .putLong("summary_moment_count", value.manifest.momentCount)
            .putLong("summary_logical_bytes", value.manifest.logicalBytes)
            .putString("summary_bundle_sha256", value.manifest.bundleSha256)
            .putString("summary_remote_bundle_id", value.remoteBundleId)
            .apply()
    }
}
