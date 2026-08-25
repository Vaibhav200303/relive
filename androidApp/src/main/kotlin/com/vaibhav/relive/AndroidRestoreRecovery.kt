package com.vaibhav.relive

import android.content.Context
import android.util.Log
import java.io.File

/** Reconciles an interrupted restore before any archive repositories are opened. */
object AndroidRestoreRecovery {
    private const val TAG = "ReliveBackupRestore"

    fun recover(context: Context) {
        recover(context.applicationContext.filesDir)
    }

    internal fun recover(root: File) {
        val journal = File(root, "relive-restore-journal")
        val staging = File(root, "relive-restore-staging")
        if (!journal.exists()) return
        when (journal.readText().trim()) {
            "STAGED" -> {
                staging.deleteRecursively()
                journal.delete()
                logDebug("discarded abandoned staged restore")
            }
            "PROMOTION_STARTED" -> {
                // SQLDelight transactions are atomic. At this point the DB is either
                // fully old or fully new; media created by the failed attempt is safe
                // to retain as an orphan and is never referenced by an old DB.
                staging.deleteRecursively()
                journal.delete()
                logDebug("reconciled interrupted restore transaction")
            }
            "PROMOTED" -> {
                staging.deleteRecursively()
                journal.delete()
                logDebug("completed cleanup for promoted restore")
            }
            else -> {
                staging.deleteRecursively()
                journal.delete()
                logDebug("removed unknown restore journal state")
            }
        }
    }

    private fun logDebug(message: String) {
        runCatching { Log.d(TAG, message) }
    }
}
