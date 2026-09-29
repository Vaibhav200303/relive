package com.vaibhav.relive

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoExpirationTest {
    private val cutoff = 1_793_471_400_000L

    @Test
    fun buildRemainsAvailableImmediatelyBeforeCutoff() {
        assertFalse(isDemoExpired(cutoff - 1L, cutoff))
    }

    @Test
    fun buildExpiresAtCutoff() {
        assertTrue(isDemoExpired(cutoff, cutoff))
    }

    @Test
    fun buildWithoutCutoffNeverExpires() {
        assertFalse(isDemoExpired(Long.MAX_VALUE, null))
    }
}
