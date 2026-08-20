package com.vaibhav.relive.di

import com.vaibhav.relive.data.local.DatabaseDriverFactory
import com.vaibhav.relive.data.local.ReliveDatabaseFactory
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.presentation.id.UuidGenerator
import com.vaibhav.relive.presentation.time.SystemClock

/**
 * iOS-side factory. Kotlin/Native has no application context, so this is parameterless.
 * Called from `MainViewController` (and available to Swift via `ReliveAppContainerFactoryKt`).
 */
fun createDefaultReliveAppContainer(): ReliveAppContainer {
    val driver = DatabaseDriverFactory().create()
    val database = ReliveDatabaseFactory.create(driver)
    return ReliveAppContainer(
        momentRepository = SqlDelightMomentRepository(database),
        clock = SystemClock,
        idGenerator = UuidGenerator,
    )
}
