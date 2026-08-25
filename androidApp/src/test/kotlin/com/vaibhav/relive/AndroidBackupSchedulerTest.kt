package com.vaibhav.relive

import androidx.work.NetworkType
import com.vaibhav.relive.domain.backup.BackupNetworkPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidBackupSchedulerTest {
    @Test fun wifiOnlyUsesUnmetered() = assertEquals(NetworkType.UNMETERED, AndroidBackupScheduler.constraintsFor(BackupNetworkPolicy.WifiOnly).requiredNetworkType)
    @Test fun cellularPolicyUsesConnected() = assertEquals(NetworkType.CONNECTED, AndroidBackupScheduler.constraintsFor(BackupNetworkPolicy.WifiOrCellular).requiredNetworkType)
    @Test fun cadenceRequestsHaveExpectedKinds() {
        val constraints = AndroidBackupScheduler.constraintsFor(BackupNetworkPolicy.WifiOnly)
        assertEquals(1, AndroidBackupScheduler.periodicRequest(1, constraints).workSpec.intervalDuration / (24 * 60 * 60 * 1000))
        assertEquals(7, AndroidBackupScheduler.periodicRequest(7, constraints).workSpec.intervalDuration / (24 * 60 * 60 * 1000))
        assertTrue(AndroidBackupScheduler.monthlyRequest(constraints).workSpec.initialDelay >= 30L * 24 * 60 * 60 * 1000)
    }
}
