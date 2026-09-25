package com.vaibhav.relive.data.local

internal fun hasPlaintextSqliteHeader(header: ByteArray): Boolean =
    header.size >= SQLITE_HEADER.size &&
        header.copyOfRange(0, SQLITE_HEADER.size).contentEquals(SQLITE_HEADER)

private val SQLITE_HEADER = "SQLite format 3\u0000".encodeToByteArray()
