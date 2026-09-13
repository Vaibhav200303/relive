package com.vaibhav.relive.data.exporting

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class PortableBinaryTest {
    @Test
    fun sha256_matches_standard_vector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256("abc".encodeToByteArray()))
    }

    @Test
    fun stored_zip_round_trips_deterministically() {
        val entries = listOf(StoredZipEntry("manifest.json", "{}".encodeToByteArray()), StoredZipEntry("media/a.jpg", byteArrayOf(1, 2, 3)))
        val first = StoredZip.encode(entries)
        assertContentEquals(first, StoredZip.encode(entries))
        val decoded = StoredZip.decode(first)
        assertEquals(entries.map { it.name }, decoded.map { it.name })
        assertContentEquals(entries[1].bytes, decoded[1].bytes)
    }

    @Test
    fun corrupt_zip_checksum_is_rejected() {
        val bytes = StoredZip.encode(listOf(StoredZipEntry("archive.json", "hello".encodeToByteArray())))
        val dataOffset = 30 + "archive.json".encodeToByteArray().size
        bytes[dataOffset] = 'x'.code.toByte()
        assertFails { StoredZip.decode(bytes) }
    }
}
