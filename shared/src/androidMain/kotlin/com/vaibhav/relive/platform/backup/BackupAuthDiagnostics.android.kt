package com.vaibhav.relive.platform.backup

import android.util.Log

actual fun backupAuthLog(message: String) {
    Log.d("ReliveBackupAuth", message)
}
