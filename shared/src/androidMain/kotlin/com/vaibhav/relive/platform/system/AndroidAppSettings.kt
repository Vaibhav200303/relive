package com.vaibhav.relive.platform.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

private var contextProvider: (() -> Context)? = null

fun installAndroidAppSettingsContext(provider: () -> Context) {
    contextProvider = provider
}

actual fun openAppSettings() {
    val ctx = contextProvider?.invoke() ?: return
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", ctx.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { ctx.startActivity(intent) }
}
