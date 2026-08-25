package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.model.LockAfter
import com.vaibhav.relive.domain.repository.ProfileSettingsRepository
import com.vaibhav.relive.platform.system.AuthenticationResult
import com.vaibhav.relive.platform.system.DeviceAuthentication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockController(
    private val settingsRepository: ProfileSettingsRepository,
    private val authentication: DeviceAuthentication,
    private val nowMillis: () -> Long,
) {
    private val mutableLocked = MutableStateFlow(settingsRepository.settings.value.appLockEnabled)
    val locked = mutableLocked.asStateFlow()
    var externalActivityActive: Boolean = false
        private set
    private var backgroundedAt: Long? = null

    suspend fun setEnabled(enabled: Boolean): AuthenticationResult {
        if (!enabled) {
            return if (settingsRepository.setAppLockEnabled(false).isSuccess) {
                mutableLocked.value = false
                AuthenticationResult.Authenticated
            } else AuthenticationResult.Failed
        }
        val result = authenticate(false, "Confirm before enabling App Lock")
        if (result != AuthenticationResult.Authenticated) return result
        return if (settingsRepository.setAppLockEnabled(true).isSuccess) {
            mutableLocked.value = false
            AuthenticationResult.Authenticated
        } else AuthenticationResult.Failed
    }

    suspend fun unlock(): AuthenticationResult {
        val settings = settingsRepository.settings.value
        val result = authenticate(settings.biometricUnlockEnabled, "Authenticate to open your archive")
        if (result == AuthenticationResult.Authenticated) mutableLocked.value = false
        return result
    }

    fun onBackground() { if (!externalActivityActive && !ExternalActivityGuard.active) backgroundedAt = nowMillis() }
    fun onForeground() {
        val settings = settingsRepository.settings.value
        val elapsed = backgroundedAt?.let { nowMillis() - it } ?: return
        if (settings.appLockEnabled && elapsed >= settings.lockAfter.timeoutMillis) mutableLocked.value = true
        backgroundedAt = null
    }

    suspend fun setBiometrics(enabled: Boolean): Result<Unit> = if (!settingsRepository.settings.value.appLockEnabled || (enabled && !authentication.capabilities.biometricsAvailable)) {
        Result.failure(IllegalStateException("Biometrics unavailable"))
    } else settingsRepository.setBiometricUnlockEnabled(enabled)

    suspend fun setLockAfter(value: LockAfter) = settingsRepository.setLockAfter(value)

    private suspend fun authenticate(biometricsOnly: Boolean, reason: String): AuthenticationResult {
        externalActivityActive = true
        return try { authentication.authenticate(biometricsOnly, reason) } finally { externalActivityActive = false }
    }
}

object ExternalActivityGuard { var active: Boolean = false }
