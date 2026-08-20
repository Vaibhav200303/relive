package com.vaibhav.relive.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(
            schema = ReliveDatabase.Schema,
            name = RELIVE_DATABASE_NAME,
        )
}
