package com.vaibhav.relive.di

import android.content.Context
import com.vaibhav.relive.data.local.DatabaseDriverFactory
import com.vaibhav.relive.data.local.ReliveDatabaseFactory
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.data.local.repository.SqlDelightArchiveInsightsRepository
import com.vaibhav.relive.data.local.repository.SqlDelightTimelineRepository
import com.vaibhav.relive.data.local.repository.SqlDelightTimelineHomeRepository
import com.vaibhav.relive.data.local.repository.SqlDelightRediscoverRepository
import com.vaibhav.relive.data.local.repository.SqlDelightProfileRepository
import com.vaibhav.relive.data.settings.AndroidAppearanceRepository
import com.vaibhav.relive.data.settings.AndroidBehaviorPreferencesRepository
import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.backup.GoogleDriveAccountManager
import com.vaibhav.relive.domain.backup.BackupCoordinator
import com.vaibhav.relive.platform.media.AndroidMediaProcessor
import com.vaibhav.relive.platform.media.AndroidMediaStore
import com.vaibhav.relive.platform.media.installAndroidMediaContext
import com.vaibhav.relive.platform.system.installAndroidAppSettingsContext
import com.vaibhav.relive.presentation.id.UuidGenerator
import com.vaibhav.relive.presentation.time.SystemClock
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository

fun createDefaultReliveAppContainer(
    context: Context,
    momentRepositoryOverride: MomentRepository? = null,
    clock: Clock = SystemClock,
    idGenerator: IdGenerator = UuidGenerator,
    googleDriveAccountManager: GoogleDriveAccountManager? = null,
    backupPreferencesRepository: AndroidBackupPreferencesRepository? = null,
    backupCoordinator: BackupCoordinator? = null,
    backupCoordinatorFactory: ((ReliveDatabase, AndroidMediaStore, GoogleDriveAccountManager) -> BackupCoordinator)? = null,
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
    val backupPreferences = backupPreferencesRepository ?: AndroidBackupPreferencesRepository(app)
    return ReliveAppContainer(
        appearanceRepository = AndroidAppearanceRepository(app),
        behaviorPreferencesRepository = AndroidBehaviorPreferencesRepository(app),
        archiveInsightsRepository = SqlDelightArchiveInsightsRepository(database, mediaStore),
        momentRepository = momentRepository,
        timelineRepository = timelineRepository,
        timelineHomeRepository = SqlDelightTimelineHomeRepository(database),
        rediscoverRepository = SqlDelightRediscoverRepository(database),
        profileRepository = SqlDelightProfileRepository(database),
        clock = clock,
        idGenerator = idGenerator,
        mediaStore = mediaStore,
        mediaProcessor = mediaProcessor,
        backupPreferencesRepository = backupPreferences,
        googleDriveAccountManager = googleDriveAccountManager ?: object : GoogleDriveAccountManager {
            override suspend fun connect() = throw com.vaibhav.relive.domain.backup.GoogleDriveAuthorizationUnavailableException("Google account connection requires an Android activity.")
            override suspend fun disconnect() = Unit
        },
        backupCoordinator = backupCoordinator ?: backupCoordinatorFactory?.invoke(database, mediaStore, googleDriveAccountManager ?: object : GoogleDriveAccountManager {
            override suspend fun connect() = throw com.vaibhav.relive.domain.backup.GoogleDriveAuthorizationUnavailableException("Google account connection requires an Android activity.")
            override suspend fun disconnect() = Unit
        }) ?: com.vaibhav.relive.domain.backup.UnavailableBackupCoordinator(),
    )
}
