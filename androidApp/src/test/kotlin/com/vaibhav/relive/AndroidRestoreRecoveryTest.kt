package com.vaibhav.relive

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidRestoreRecoveryTest {
    @Test
    fun stagedRecoveryIsSafeAndIdempotent() {
        val root = Files.createTempDirectory("relive-recovery").toFile()
        try {
            val staging = java.io.File(root, "relive-restore-staging").apply { mkdirs() }
            java.io.File(staging, "partial").writeText("x")
            java.io.File(root, "relive-restore-journal").writeText("STAGED")
            AndroidRestoreRecovery.recover(root)
            AndroidRestoreRecovery.recover(root)
            assertFalse(staging.exists())
            assertFalse(java.io.File(root, "relive-restore-journal").exists())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun promotionStartedRecoveryRetainsArchiveAndCleansStaging() {
        val root = Files.createTempDirectory("relive-recovery").toFile()
        try {
            java.io.File(root, "relive.db").writeText("known-good")
            val staging = java.io.File(root, "relive-restore-staging").apply { mkdirs() }
            java.io.File(staging, "partial").writeText("x")
            java.io.File(root, "relive-restore-journal").writeText("PROMOTION_STARTED")
            AndroidRestoreRecovery.recover(root)
            AndroidRestoreRecovery.recover(root)
            assertTrue(java.io.File(root, "relive.db").readText() == "known-good")
            assertFalse(staging.exists())
            assertFalse(java.io.File(root, "relive-restore-journal").exists())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun promotedRecoveryKeepsArchiveAndFinishesCleanup() {
        val root = Files.createTempDirectory("relive-recovery").toFile()
        try {
            java.io.File(root, "relive.db").writeText("restored")
            java.io.File(root, "relive-restore-staging").mkdirs()
            java.io.File(root, "relive-restore-journal").writeText("PROMOTED")
            AndroidRestoreRecovery.recover(root)
            AndroidRestoreRecovery.recover(root)
            assertTrue(java.io.File(root, "relive.db").readText() == "restored")
            assertFalse(java.io.File(root, "relive-restore-journal").exists())
        } finally { root.deleteRecursively() }
    }
}
