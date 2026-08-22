package com.vaibhav.relive.di

import com.vaibhav.relive.data.local.DatabaseDriverFactory
import com.vaibhav.relive.data.local.ReliveDatabaseFactory
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.data.local.repository.SqlDelightTimelineRepository
import com.vaibhav.relive.data.local.repository.SqlDelightTimelineHomeRepository
import com.vaibhav.relive.platform.media.IosMediaProcessor
import com.vaibhav.relive.platform.media.IosMediaStore
import com.vaibhav.relive.presentation.id.UuidGenerator
import com.vaibhav.relive.presentation.time.SystemClock

fun createDefaultReliveAppContainer(): ReliveAppContainer {
    val driver = DatabaseDriverFactory().create()
    val database = ReliveDatabaseFactory.create(driver)
    val store = IosMediaStore()
    val processor = IosMediaProcessor(store)
    return ReliveAppContainer(
        momentRepository = SqlDelightMomentRepository(database),
        timelineRepository = SqlDelightTimelineRepository(database),
        timelineHomeRepository = SqlDelightTimelineHomeRepository(database),
        clock = SystemClock,
        idGenerator = UuidGenerator,
        mediaStore = store,
        mediaProcessor = processor,
    )
}
