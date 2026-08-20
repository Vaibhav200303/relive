package com.vaibhav.relive.di

import android.content.Context
import com.vaibhav.relive.data.local.DatabaseDriverFactory
import com.vaibhav.relive.data.local.ReliveDatabaseFactory
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.domain.repository.MomentRepository

/**
 * Android-side factory. Consumers pass an application context so the SQLite
 * driver is bound to the process-wide `Application`, not an activity.
 *
 * Debug builds can override the moment repository by passing a debug-only
 * substitute; the parameter is nullable so release builds simply omit it and
 * the real SQLDelight repository is used.
 */
fun createDefaultReliveAppContainer(
    context: Context,
    momentRepositoryOverride: MomentRepository? = null,
): ReliveAppContainer {
    val momentRepository = momentRepositoryOverride ?: run {
        val driver = DatabaseDriverFactory(context.applicationContext).create()
        val database = ReliveDatabaseFactory.create(driver)
        SqlDelightMomentRepository(database)
    }
    return ReliveAppContainer(momentRepository = momentRepository)
}
