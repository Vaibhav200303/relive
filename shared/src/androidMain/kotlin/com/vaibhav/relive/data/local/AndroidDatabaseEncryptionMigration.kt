package com.vaibhav.relive.data.local

import android.content.Context
import java.io.File
import java.io.FileInputStream
import net.zetetic.database.sqlcipher.SQLiteDatabase

/** Converts the legacy plaintext database through a validated temporary file before replacement. */
internal object AndroidDatabaseEncryptionMigration {
    @Synchronized
    fun migrateIfNeeded(context: Context, passphrase: ByteArray) {
        val database = context.getDatabasePath(RELIVE_DATABASE_NAME)
        val encrypted = File(database.parentFile, "${database.name}.encrypted")
        val plaintextBackup = File(database.parentFile, "${database.name}.plaintext-backup")

        recoverInterruptedReplacement(database, plaintextBackup)
        if (!database.isFile) {
            encrypted.delete()
            return
        }
        if (!database.hasPlaintextSqliteHeader()) {
            if (!plaintextBackup.exists()) {
                encrypted.delete()
                return
            }
            try {
                validateEncrypted(database, passphrase)
                check(plaintextBackup.delete()) { "Unable to remove the plaintext database after encryption." }
                encrypted.delete()
                return
            } catch (error: Throwable) {
                if (!plaintextBackup.hasPlaintextSqliteHeader()) throw error
                check(database.delete() && plaintextBackup.renameTo(database)) {
                    "Unable to recover the database encryption migration."
                }
            }
        }

        checkpointPlaintextDatabase(database)
        encrypted.delete()
        exportEncrypted(database, encrypted, passphrase)
        validateEncrypted(encrypted, passphrase)

        check(database.renameTo(plaintextBackup)) { "Unable to preserve the existing database during encryption." }
        try {
            deleteSidecars(database)
            check(encrypted.renameTo(database)) { "Unable to install the encrypted database." }
            validateEncrypted(database, passphrase)
            check(plaintextBackup.delete()) { "Unable to remove the plaintext database after encryption." }
        } catch (error: Throwable) {
            database.delete()
            plaintextBackup.renameTo(database)
            throw error
        } finally {
            encrypted.delete()
        }
    }

    private fun recoverInterruptedReplacement(database: File, backup: File) {
        if (!database.exists() && backup.isFile) {
            check(backup.renameTo(database)) { "Unable to recover the database encryption migration." }
        }
    }

    private fun checkpointPlaintextDatabase(database: File) {
        android.database.sqlite.SQLiteDatabase.openDatabase(
            database.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
        ).use { it.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { cursor -> cursor.moveToFirst() } }
    }

    private fun exportEncrypted(source: File, destination: File, passphrase: ByteArray) {
        val sourceDatabase = SQLiteDatabase.openDatabase(
            source.absolutePath,
            ByteArray(0),
            null,
            SQLiteDatabase.OPEN_READWRITE,
            null,
        )
        try {
            val escapedPath = destination.absolutePath.replace("'", "''")
            val hexPassphrase = passphrase.joinToString("") { "%02x".format(it) }
            sourceDatabase.rawExecSQL("ATTACH DATABASE '$escapedPath' AS encrypted KEY \"x'$hexPassphrase'\"")
            sourceDatabase.rawQuery("SELECT sqlcipher_export('encrypted')", emptyArray()).use { it.moveToFirst() }
            sourceDatabase.rawExecSQL("PRAGMA encrypted.user_version = ${sourceDatabase.version}")
            sourceDatabase.rawExecSQL("DETACH DATABASE encrypted")
        } finally {
            sourceDatabase.close()
        }
    }

    private fun validateEncrypted(database: File, passphrase: ByteArray) {
        check(database.isFile && database.length() > 0L) { "The encrypted database was not created." }
        val encryptedDatabase = SQLiteDatabase.openDatabase(
            database.absolutePath,
            passphrase,
            null,
            SQLiteDatabase.OPEN_READONLY,
            null,
        )
        try {
            encryptedDatabase.rawQuery("SELECT count(*) FROM sqlite_master", emptyArray()).use { cursor ->
                check(cursor.moveToFirst()) { "The encrypted database could not be validated." }
            }
            encryptedDatabase.rawQuery("PRAGMA cipher_integrity_check", emptyArray()).use { cursor ->
                check(!cursor.moveToFirst()) { "The encrypted database failed its integrity check." }
            }
        } finally {
            encryptedDatabase.close()
        }
    }

    private fun deleteSidecars(database: File) {
        File("${database.absolutePath}-wal").delete()
        File("${database.absolutePath}-shm").delete()
        File("${database.absolutePath}-journal").delete()
    }

    private fun File.hasPlaintextSqliteHeader(): Boolean {
        val header = ByteArray(SQLITE_HEADER_BYTES)
        FileInputStream(this).use { input ->
            if (input.read(header) != header.size) return false
        }
        return hasPlaintextSqliteHeader(header)
    }

    private const val SQLITE_HEADER_BYTES = 16
}
