package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.backup.BackupCadence
import com.vaibhav.relive.domain.backup.BackupCoordinator
import com.vaibhav.relive.domain.backup.BackupManifest
import com.vaibhav.relive.domain.backup.BackupNetworkPolicy
import com.vaibhav.relive.domain.backup.BackupOperationState
import com.vaibhav.relive.domain.backup.BackupPreferencesRepository
import com.vaibhav.relive.domain.backup.BackupProgress
import com.vaibhav.relive.domain.backup.BackupSummary
import com.vaibhav.relive.domain.backup.GoogleDriveAccount
import com.vaibhav.relive.domain.backup.GoogleDriveAccountManager
import com.vaibhav.relive.domain.backup.GoogleDriveAuthorizationUnavailableException
import com.vaibhav.relive.domain.backup.RestorePreview
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.EntitlementState
import com.vaibhav.relive.domain.entitlement.PurchaseOutcome
import com.vaibhav.relive.domain.entitlement.RelivePurchaseOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BackupRestoreViewModelTest {
    @Test
    fun backup_after_restart_uses_persisted_account_without_interactive_connect() = runTest {
        val preferences = FakePreferences(GoogleDriveAccount("subject", "user@example.com"))
        val accounts = FakeAccountManager()
        val coordinator = FakeCoordinator()
        val viewModel = BackupRestoreViewModel(preferences, accounts, coordinator, backgroundScope)

        viewModel.backUpNow()
        runCurrent()

        assertEquals(0, accounts.connectCalls)
        assertEquals(1, coordinator.backupCalls)
        assertEquals(coordinator.summary, preferences.remoteSummary.value)
    }

    @Test
    fun revoked_authorization_exposes_reconnect_required_state_without_reselecting_account() = runTest {
        val preferences = FakePreferences(GoogleDriveAccount("subject", "user@example.com"))
        val accounts = FakeAccountManager()
        val viewModel = BackupRestoreViewModel(
            preferences,
            accounts,
            FakeCoordinator(authorizationFailure = true),
            backgroundScope,
        )

        viewModel.backUpNow()
        runCurrent()

        assertEquals(0, accounts.connectCalls)
        assertIs<BackupOperationState.AuthorizationRequired>(preferences.operation.value)
    }

    @Test
    fun free_user_cannot_enable_scheduled_backup() = runTest {
        val preferences = FakePreferences(initialAccount = null, initialCadence = BackupCadence.Off)
        val viewModel = BackupRestoreViewModel(
            preferences,
            FakeAccountManager(),
            FakeCoordinator(),
            backgroundScope,
            FakeEntitlementProvider(EntitlementState(isPro = false)),
        )

        viewModel.setCadence(BackupCadence.Daily)
        runCurrent()

        assertEquals(BackupCadence.Off, preferences.cadence.value)
        assertTrue(viewModel.state.first { it.upgradeRequired }.upgradeRequired)
    }

    private class FakePreferences(
        initialAccount: GoogleDriveAccount?,
        initialCadence: BackupCadence = BackupCadence.Weekly,
    ) : BackupPreferencesRepository {
        private val cadenceState = MutableStateFlow(initialCadence)
        private val networkState = MutableStateFlow(BackupNetworkPolicy.WifiOnly)
        private val accountState = MutableStateFlow(initialAccount)
        override val operation = MutableStateFlow<BackupOperationState>(BackupOperationState.Idle)
        override val remoteSummary = MutableStateFlow<BackupSummary?>(null)
        override val cadence = cadenceState
        override val networkPolicy = networkState
        override val account = accountState
        override suspend fun setCadence(value: BackupCadence) { cadenceState.value = value }
        override suspend fun setNetworkPolicy(value: BackupNetworkPolicy) { networkState.value = value }
        override suspend fun setAccount(value: GoogleDriveAccount?) { accountState.value = value }
        override suspend fun setOperation(value: BackupOperationState) { operation.value = value }
        override suspend fun setRemoteSummary(value: BackupSummary) { remoteSummary.value = value }
    }

    private class FakeEntitlementProvider(initialState: EntitlementState) : EntitlementProvider {
        override val state = MutableStateFlow(initialState)
        override suspend fun purchase(option: RelivePurchaseOption) = PurchaseOutcome.Unavailable("Unavailable")
        override suspend fun restorePurchases() = PurchaseOutcome.Unavailable("Unavailable")
    }

    private class FakeAccountManager : GoogleDriveAccountManager {
        var connectCalls = 0
        override suspend fun connect(): GoogleDriveAccount? {
            connectCalls += 1
            return GoogleDriveAccount("new-subject", "new@example.com")
        }
        override suspend fun disconnect() = Unit
    }

    private class FakeCoordinator(private val authorizationFailure: Boolean = false) : BackupCoordinator {
        var backupCalls = 0
        val summary = BackupSummary(BackupManifest(1, "generation", 1, 1, 1, "hash"), "bundle")
        override suspend fun backup(networkPolicy: BackupNetworkPolicy, onProgress: (BackupProgress) -> Unit): BackupSummary {
            backupCalls += 1
            if (authorizationFailure) throw GoogleDriveAuthorizationUnavailableException("reconnect")
            return summary
        }
        override suspend fun discoverRestore(): RestorePreview? = null
        override suspend fun restore(preview: RestorePreview, onProgress: (BackupProgress) -> Unit) = Unit
    }
}
