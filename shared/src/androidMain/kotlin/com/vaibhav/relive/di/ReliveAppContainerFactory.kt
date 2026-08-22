package com.vaibhav.relive.di

import android.content.Context
import com.vaibhav.relive.data.local.DatabaseDriverFactory
import com.vaibhav.relive.data.local.ReliveDatabaseFactory
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.data.local.repository.SqlDelightTimelineRepository
import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.media.AndroidMediaProcessor
import com.vaibhav.relive.platform.media.AndroidMediaStore
import com.vaibhav.relive.platform.media.installAndroidMediaContext
import com.vaibhav.relive.platform.system.installAndroidAppSettingsContext
import com.vaibhav.relive.presentation.id.UuidGenerator
import com.vaibhav.relive.presentation.time.SystemClock

fun createDefaultReliveAppContainer(
    context: Context,
    momentRepositoryOverride: MomentRepository? = null,
    clock: Clock = SystemClock,
    idGenerator: IdGenerator = UuidGenerator,
): ReliveAppContainer {
    val app = context.applicationContext
    installAndroidMediaContext { app }
    installAndroidAppSettingsContext { app }

    val driver = DatabaseDriverFactory(app).create()
    val database = ReliveDatabaseFactory.create(driver)
    val momentRepository = momentRepositoryOverride ?: SqlDelightMomentRepository(database)
    val timelineRepository = SqlDelightTimelineRepository(database)
    val mediaStore = AndroidMediaStore(app)
    val mediaProcessor = AndroidMediaProcessor(app, mediaStore)
    return ReliveAppContainer(
        momentRepository = momentRepository,
        timelineRepository = timelineRepository,
        clock = clock,
        idGenerator = idGenerator,
        mediaStore = mediaStore,
        mediaProcessor = mediaProcessor,
    )
}
