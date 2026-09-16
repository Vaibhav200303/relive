package com.vaibhav.relive.platform.backup

private var backupAuthDebugLoggingEnabled = false

actual fun backupAuthLog(message: String) {
    if (backupAuthDebugLoggingEnabled) println("ReliveBackupAuth: $message")
}

actual fun installBackupAuthDebugLogging(enabled: Boolean) {
    backupAuthDebugLoggingEnabled = enabled
}
