package com.vaibhav.relive.di

import com.vaibhav.relive.data.local.DatabaseDriverFactory
import com.vaibhav.relive.data.local.ReliveDatabaseFactory
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.data.local.repository.SqlDelightArchiveInsightsRepository
import com.vaibhav.relive.data.local.repository.SqlDelightTimelineRepository
import com.vaibhav.relive.data.local.repository.SqlDelightTimelineHomeRepository
import com.vaibhav.relive.data.local.repository.SqlDelightRediscoverRepository
import com.vaibhav.relive.data.local.repository.SqlDelightProfileRepository
import com.vaibhav.relive.data.settings.IosAppearanceRepository
import com.vaibhav.relive.data.settings.IosBehaviorPreferencesRepository
import com.vaibhav.relive.data.settings.IosProfileSettingsRepository
import com.vaibhav.relive.platform.system.IosDeviceAuthentication
import com.vaibhav.relive.platform.notifications.IosRediscoverReminderService
import com.vaibhav.relive.platform.media.IosMediaProcessor
import com.vaibhav.relive.platform.media.IosMediaStore
import com.vaibhav.relive.presentation.id.UuidGenerator
import com.vaibhav.relive.presentation.time.SystemClock

fun createDefaultReliveAppContainer(): ReliveAppContainer {
    val driver = DatabaseDriverFactory().create()
    val database = ReliveDatabaseFactory.create(driver)
    val store = IosMediaStore()
    val processor = IosMediaProcessor(store)
    val momentRepository = SqlDelightMomentRepository(database)
    return ReliveAppContainer(
        appearanceRepository = IosAppearanceRepository(),
        behaviorPreferencesRepository = IosBehaviorPreferencesRepository(),
        archiveInsightsRepository = SqlDelightArchiveInsightsRepository(database, store),
        momentRepository = momentRepository,
        timelineRepository = SqlDelightTimelineRepository(database),
        timelineHomeRepository = SqlDelightTimelineHomeRepository(database),
        rediscoverRepository = SqlDelightRediscoverRepository(database),
        profileRepository = SqlDelightProfileRepository(database),
        profileSettingsRepository = IosProfileSettingsRepository(),
        clock = SystemClock,
        idGenerator = UuidGenerator,
        mediaStore = store,
        mediaProcessor = processor,
        deviceAuthentication = IosDeviceAuthentication(),
        rediscoverReminderService = IosRediscoverReminderService(momentRepository),
    )
}
