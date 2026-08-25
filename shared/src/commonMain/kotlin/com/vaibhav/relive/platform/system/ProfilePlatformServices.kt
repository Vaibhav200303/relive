package com.vaibhav.relive.platform.system

data class AuthenticationCapabilities(
    val deviceAuthenticationAvailable: Boolean,
    val biometricsAvailable: Boolean,
    val biometricsExplanation: String? = null,
)

enum class AuthenticationResult { Authenticated, Cancelled, Failed, Unavailable }

interface DeviceAuthentication {
    val capabilities: AuthenticationCapabilities
    suspend fun authenticate(biometricsOnly: Boolean, reason: String): AuthenticationResult
}

data class PlatformAppInfo(val version: String, val build: String, val platform: String, val osVersion: String) {
    val versionAndBuild: String get() = "$version ($build)"
}

data class MailRequest(val subject: String, val body: String, val recipient: String)

interface MailComposer { fun open(request: MailRequest): Boolean }

fun buildSafeDiagnosticMail(subject: String, info: PlatformAppInfo, recipient: String) = MailRequest(
    subject = subject,
    recipient = recipient,
    body = "\n\n—\nRelive ${info.versionAndBuild}\n${info.platform} ${info.osVersion}",
)

expect fun platformAppInfo(): PlatformAppInfo
expect fun platformMailComposer(): MailComposer

internal object UnavailableDeviceAuthentication : DeviceAuthentication {
    override val capabilities = AuthenticationCapabilities(false, false, "Device authentication is unavailable.")
    override suspend fun authenticate(biometricsOnly: Boolean, reason: String) = AuthenticationResult.Unavailable
}
