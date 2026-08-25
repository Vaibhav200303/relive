package com.vaibhav.relive

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import com.vaibhav.relive.platform.system.AuthenticationCapabilities
import com.vaibhav.relive.platform.system.AuthenticationResult
import com.vaibhav.relive.platform.system.DeviceAuthentication
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidDeviceAuthentication(private val activity: Activity) : DeviceAuthentication {
    private val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    @Suppress("DEPRECATION")
    private val fingerprintAvailable = if (Build.VERSION.SDK_INT >= 29) {
        (activity.getSystemService(Context.BIOMETRIC_SERVICE) as android.hardware.biometrics.BiometricManager).canAuthenticate() == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
    } else Build.VERSION.SDK_INT >= 23 && (activity.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager)?.run { isHardwareDetected && hasEnrolledFingerprints() } == true

    override val capabilities = AuthenticationCapabilities(
        deviceAuthenticationAvailable = keyguard.isDeviceSecure,
        biometricsAvailable = fingerprintAvailable,
        biometricsExplanation = if (fingerprintAvailable) null else "No enrolled biometric authentication is available on this device.",
    )

    private var credentialCompletion: ((AuthenticationResult) -> Unit)? = null

    override suspend fun authenticate(biometricsOnly: Boolean, reason: String): AuthenticationResult {
        if (!keyguard.isDeviceSecure || (biometricsOnly && !fingerprintAvailable)) return AuthenticationResult.Unavailable
        return if (Build.VERSION.SDK_INT >= 28) prompt(biometricsOnly, reason) else credential(reason)
    }

    @androidx.annotation.RequiresApi(28)
    private suspend fun prompt(biometricsOnly: Boolean, reason: String) = suspendCancellableCoroutine<AuthenticationResult> { continuation ->
        val cancel = CancellationSignal()
        continuation.invokeOnCancellation { cancel.cancel() }
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle("Unlock Relive")
            .setSubtitle(reason)
            .apply {
                if (biometricsOnly) setNegativeButton("Cancel", activity.mainExecutor) { _, _ ->
                    if (continuation.isActive) continuation.resume(AuthenticationResult.Cancelled)
                } else setDeviceCredentialAllowed(true)
            }
            .build()
        prompt.authenticate(cancel, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                if (continuation.isActive) continuation.resume(AuthenticationResult.Authenticated)
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                if (continuation.isActive) continuation.resume(if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED || errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED) AuthenticationResult.Cancelled else AuthenticationResult.Failed)
            }
        })
    }

    @Suppress("DEPRECATION")
    private suspend fun credential(reason: String) = suspendCancellableCoroutine<AuthenticationResult> { continuation ->
        val intent = keyguard.createConfirmDeviceCredentialIntent("Unlock Relive", reason)
        if (intent == null) continuation.resume(AuthenticationResult.Unavailable) else {
            credentialCompletion = { if (continuation.isActive) continuation.resume(it) }
            continuation.invokeOnCancellation { credentialCompletion = null }
            activity.startActivityForResult(intent, REQUEST_CODE)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_CODE) return false
        credentialCompletion?.invoke(if (resultCode == Activity.RESULT_OK) AuthenticationResult.Authenticated else AuthenticationResult.Cancelled)
        credentialCompletion = null
        return true
    }

    private companion object { const val REQUEST_CODE = 9017 }
}
