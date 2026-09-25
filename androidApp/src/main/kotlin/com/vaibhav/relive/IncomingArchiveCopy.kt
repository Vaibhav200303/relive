package com.vaibhav.relive

import java.io.File
import java.io.InputStream

/** Copies an externally supplied portable archive without allowing it to exhaust app storage. */
internal fun copyIncomingArchive(
    input: InputStream,
    destination: File,
    maximumBytes: Long = MAX_INCOMING_ARCHIVE_BYTES,
) {
    var copied = 0L
    try {
        destination.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                copied += read
                require(copied <= maximumBytes) { "Archive is too large to open." }
                output.write(buffer, 0, read)
            }
        }
    } catch (error: Throwable) {
        destination.delete()
        throw error
    }
}

internal const val MAX_INCOMING_ARCHIVE_BYTES = 512L * 1024L * 1024L
