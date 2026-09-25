package com.vaibhav.relive

import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class IncomingArchiveCopyTest {
    @Test
    fun copiesAnArchiveWithinTheConfiguredLimit() {
        val destination = Files.createTempFile("relive-archive", ".relive").toFile()
        try {
            copyIncomingArchive(ByteArrayInputStream(byteArrayOf(1, 2, 3)), destination)
            assertEquals(listOf<Byte>(1, 2, 3), destination.readBytes().toList())
        } finally {
            destination.delete()
        }
    }

    @Test
    fun removesThePartialFileWhenCopyFails() {
        val destination = Files.createTempFile("relive-archive", ".relive").toFile()
        val excessiveInput = object : java.io.InputStream() {
            private var remaining = 4L

            override fun read(): Int = if (remaining-- > 0) 0 else -1

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (remaining <= 0) return -1
                val count = minOf(length.toLong(), remaining).toInt()
                remaining -= count
                return count
            }
        }
        assertFailsWith<IllegalArgumentException> {
            copyIncomingArchive(excessiveInput, destination, maximumBytes = 3)
        }
        assertFalse(destination.exists())
    }
}
