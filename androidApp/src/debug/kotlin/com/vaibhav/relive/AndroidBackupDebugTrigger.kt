package com.vaibhav.relive

import com.vaibhav.relive.domain.backup.BackupNetworkPolicy

object AndroidBackupDebugTrigger {
    @Volatile var scheduler: AndroidBackupScheduler? = null
    fun triggerNowForDeviceQa(policy: BackupNetworkPolicy) { scheduler?.triggerNow(policy) }
}
