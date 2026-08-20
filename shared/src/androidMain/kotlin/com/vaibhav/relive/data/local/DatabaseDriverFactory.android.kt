package com.vaibhav.relive.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver =
        AndroidSqliteDriver(
            schema = ReliveDatabase.Schema,
            context = context.applicationContext,
            name = RELIVE_DATABASE_NAME,
        )
}
