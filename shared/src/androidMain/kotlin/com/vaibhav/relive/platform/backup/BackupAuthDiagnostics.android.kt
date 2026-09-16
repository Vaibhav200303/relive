package com.vaibhav.relive.platform.backup

import android.util.Log

@Volatile
private var backupAuthDebugLoggingEnabled = false

actual fun backupAuthLog(message: String) {
    if (backupAuthDebugLoggingEnabled) Log.d("ReliveBackupAuth", message)
}

actual fun installBackupAuthDebugLogging(enabled: Boolean) {
    backupAuthDebugLoggingEnabled = enabled
}
