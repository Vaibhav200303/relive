package com.vaibhav.relive.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver {
        System.loadLibrary("sqlcipher")
        val passphrase = AndroidDatabaseKeyStore(context).getOrCreatePassphrase()
        AndroidDatabaseEncryptionMigration.migrateIfNeeded(context.applicationContext, passphrase)
        val factoryPassphrase = passphrase.copyOf()
        passphrase.fill(0)
        return AndroidSqliteDriver(
            schema = ReliveDatabase.Schema,
            context = context.applicationContext,
            name = RELIVE_DATABASE_NAME,
            factory = SupportOpenHelperFactory(factoryPassphrase, null, true),
        )
    }
}
