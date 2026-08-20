package com.vaibhav.relive.data.local

import app.cash.sqldelight.db.SqlDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase

/**
 * Builds a fully-configured [ReliveDatabase] on top of a platform-supplied driver.
 * Enforces SQLite `foreign_keys = ON` centrally so cascade behavior is identical on
 * every platform regardless of driver defaults.
 */
object ReliveDatabaseFactory {

    fun create(driver: SqlDriver): ReliveDatabase {
        enableForeignKeys(driver)
        return ReliveDatabase(driver)
    }

    private fun enableForeignKeys(driver: SqlDriver) {
        driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON;", parameters = 0)
    }
}
