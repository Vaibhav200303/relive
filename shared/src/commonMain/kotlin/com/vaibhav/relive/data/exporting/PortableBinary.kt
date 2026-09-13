package com.vaibhav.relive.data.exporting

internal data class StoredZipEntry(val name: String, val bytes: ByteArray)

/** Minimal dependency-free ZIP STORE codec used by the iOS portable archive boundary. */
internal object StoredZip {
    fun encode(entries: List<StoredZipEntry>): ByteArray {
        require(entries.size <= 65_535)
        val out = ByteSink()
        val directory = mutableListOf<Central>()
        entries.forEach { entry ->
            val name = entry.name.encodeToByteArray()
            val offset = out.size
            val crc = crc32(entry.bytes)
            out.u32(LOCAL); out.u16(20); out.u16(UTF8); out.u16(0); out.u16(0); out.u16(0)
            out.u32(crc); out.u32(entry.bytes.size.toLong()); out.u32(entry.bytes.size.toLong())
            out.u16(name.size); out.u16(0); out.bytes(name); out.bytes(entry.bytes)
            directory += Central(name, crc, entry.bytes.size, offset)
        }
        val centralOffset = out.size
        directory.forEach { item ->
            out.u32(CENTRAL); out.u16(20); out.u16(20); out.u16(UTF8); out.u16(0); out.u16(0); out.u16(0)
            out.u32(item.crc); out.u32(item.size.toLong()); out.u32(item.size.toLong())
            out.u16(item.name.size); out.u16(0); out.u16(0); out.u16(0); out.u16(0); out.u32(0); out.u32(item.offset.toLong()); out.bytes(item.name)
        }
        val centralSize = out.size - centralOffset
        out.u32(END); out.u16(0); out.u16(0); out.u16(directory.size); out.u16(directory.size)
        out.u32(centralSize.toLong()); out.u32(centralOffset.toLong()); out.u16(0)
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray, maxEntries: Int = 100_000): List<StoredZipEntry> {
        val entries = mutableListOf<StoredZipEntry>()
        var offset = 0
        while (offset + 4 <= bytes.size && bytes.u32(offset) == LOCAL) {
            require(entries.size < maxEntries) { "This archive contains too many entries." }
            require(bytes.u16(offset + 8) == 0) { "This archive uses unsupported compression." }
            val crc = bytes.u32(offset + 14)
            val compressed = bytes.u32(offset + 18).toIntChecked()
            val size = bytes.u32(offset + 22).toIntChecked()
            require(compressed == size) { "Invalid stored ZIP entry." }
            val nameLength = bytes.u16(offset + 26)
            val extraLength = bytes.u16(offset + 28)
            val nameStart = offset + 30
            val dataStart = nameStart + nameLength + extraLength
            require(nameStart >= 0 && dataStart >= nameStart && dataStart + size <= bytes.size) { "Truncated ZIP entry." }
            val name = bytes.copyOfRange(nameStart, nameStart + nameLength).decodeToString()
            val data = bytes.copyOfRange(dataStart, dataStart + size)
            require(crc32(data) == crc) { "ZIP checksum does not match." }
            entries += StoredZipEntry(name, data)
            offset = dataStart + size
        }
        require(entries.isNotEmpty() && offset + 4 <= bytes.size && bytes.u32(offset) == CENTRAL) { "This is not a supported ZIP archive." }
        return entries
    }

    private data class Central(val name: ByteArray, val crc: Long, val size: Int, val offset: Int)
    private const val UTF8 = 0x800
    private const val LOCAL = 0x04034b50L
    private const val CENTRAL = 0x02014b50L
    private const val END = 0x06054b50L
}

internal fun sha256(bytes: ByteArray): String {
    val h = intArrayOf(0x6a09e667, 0xbb67ae85.toInt(), 0x3c6ef372, 0xa54ff53a.toInt(), 0x510e527f, 0x9b05688c.toInt(), 0x1f83d9ab, 0x5be0cd19)
    val bitLength = bytes.size.toLong() * 8
    val paddedSize = ((bytes.size + 9 + 63) / 64) * 64
    val input = ByteArray(paddedSize)
    bytes.copyInto(input)
    input[bytes.size] = 0x80.toByte()
    repeat(8) { input[paddedSize - 1 - it] = (bitLength ushr (it * 8)).toByte() }
    val w = IntArray(64)
    for (chunk in input.indices step 64) {
        repeat(16) { i ->
            val p = chunk + i * 4
            w[i] = (input[p].toInt() and 255 shl 24) or (input[p + 1].toInt() and 255 shl 16) or (input[p + 2].toInt() and 255 shl 8) or (input[p + 3].toInt() and 255)
        }
        for (i in 16 until 64) {
            val s0 = w[i - 15].rotateRight(7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
            val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight(19) xor (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }
        var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]; var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
        repeat(64) { i ->
            val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
            val ch = (e and f) xor (e.inv() and g)
            val t1 = hh + s1 + ch + SHA_K[i] + w[i]
            val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val t2 = s0 + maj
            hh = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2
        }
        h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh
    }
    return h.joinToString("") { it.toUInt().toString(16).padStart(8, '0') }
}

private val SHA_K = intArrayOf(
    0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(), 0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
    0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
    0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(), 0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
    0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(), 0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(), 0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt(),
)

private fun Int.rotateRight(bits: Int) = (this ushr bits) or (this shl (32 - bits))
private fun Long.toIntChecked(): Int { require(this <= Int.MAX_VALUE); return toInt() }
private fun ByteArray.u16(offset: Int) = (this[offset].toInt() and 255) or ((this[offset + 1].toInt() and 255) shl 8)
private fun ByteArray.u32(offset: Int) = u16(offset).toLong() or (u16(offset + 2).toLong() shl 16)
private fun crc32(bytes: ByteArray): Long {
    var crc = -1
    bytes.forEach { byte ->
        crc = crc xor (byte.toInt() and 255)
        repeat(8) { crc = (crc ushr 1) xor if (crc and 1 != 0) 0xedb88320.toInt() else 0 }
    }
    return crc.inv().toLong() and 0xffffffffL
}

private class ByteSink {
    private val value = mutableListOf<Byte>()
    val size get() = value.size
    fun u16(number: Int) { value += number.toByte(); value += (number ushr 8).toByte() }
    fun u32(number: Long) { u16(number.toInt()); u16((number ushr 16).toInt()) }
    fun bytes(bytes: ByteArray) { bytes.forEach(value::add) }
    fun toByteArray() = value.toByteArray()
}
