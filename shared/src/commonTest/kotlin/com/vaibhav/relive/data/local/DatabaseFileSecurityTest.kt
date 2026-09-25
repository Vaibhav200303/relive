package com.vaibhav.relive.data.local

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseFileSecurityTest {
    @Test
    fun recognizesPlaintextSqliteHeader() {
        assertTrue(hasPlaintextSqliteHeader("SQLite format 3\u0000remaining bytes".encodeToByteArray()))
    }

    @Test
    fun encryptedOrTruncatedHeaderIsNotPlaintext() {
        assertFalse(hasPlaintextSqliteHeader(byteArrayOf(1, 2, 3, 4)))
        assertFalse(hasPlaintextSqliteHeader(ByteArray(32) { it.toByte() }))
    }
}
