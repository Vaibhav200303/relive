package com.vaibhav.relive.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-owned SQLite driver construction. Concrete factories live in
 * androidMain / iosMain source sets and receive the platform primitives they need
 * (Android [android.content.Context], iOS just the database name).
 *
 * Kept deliberately narrow: the factory only knows how to hand back a raw driver.
 * Schema creation and cross-driver PRAGMAs (foreign-key enforcement) belong to
 * [ReliveDatabaseFactory] so the same rules apply to every platform.
 */
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}

const val RELIVE_DATABASE_NAME: String = "relive.db"
