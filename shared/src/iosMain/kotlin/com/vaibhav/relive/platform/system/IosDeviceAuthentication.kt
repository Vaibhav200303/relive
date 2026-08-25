package com.vaibhav.relive.platform.system

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IosDeviceAuthentication : DeviceAuthentication {
    override val capabilities: AuthenticationCapabilities
        get() {
            val device = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)
            val biometric = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
            return AuthenticationCapabilities(device, biometric, if (biometric) null else "No enrolled biometric authentication is available on this device.")
        }

    override suspend fun authenticate(biometricsOnly: Boolean, reason: String) = suspendCancellableCoroutine { continuation ->
        val context = LAContext()
        val policy = if (biometricsOnly) LAPolicyDeviceOwnerAuthenticationWithBiometrics else LAPolicyDeviceOwnerAuthentication
        if (!context.canEvaluatePolicy(policy, null)) {
            continuation.resume(AuthenticationResult.Unavailable)
        } else context.evaluatePolicy(policy, localizedReason = reason) { success, error ->
            if (continuation.isActive) continuation.resume(
                if (success) AuthenticationResult.Authenticated else if (error == null) AuthenticationResult.Cancelled else AuthenticationResult.Failed,
            )
        }
    }
}
