package com.vaibhav.relive.platform.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

private var profileContext: (() -> Context)? = null
fun installAndroidProfilePlatformContext(provider: () -> Context) { profileContext = provider }

actual fun platformAppInfo(): PlatformAppInfo {
    val context = profileContext?.invoke() ?: return PlatformAppInfo("Unavailable", "Unavailable", "Android", Build.VERSION.RELEASE)
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    @Suppress("DEPRECATION") val build = info.longVersionCode.toString()
    return PlatformAppInfo(info.versionName ?: "Unavailable", build, "Android", Build.VERSION.RELEASE)
}

actual fun platformMailComposer(): MailComposer = object : MailComposer {
    override fun open(request: MailRequest): Boolean {
        val context = profileContext?.invoke() ?: return false
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(request.recipient)}")).apply {
            putExtra(Intent.EXTRA_SUBJECT, request.subject)
            putExtra(Intent.EXTRA_TEXT, request.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}
