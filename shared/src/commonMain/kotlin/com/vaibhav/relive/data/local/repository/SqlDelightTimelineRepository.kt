package com.vaibhav.relive.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.mapper.encodeThemeName
import com.vaibhav.relive.data.local.mapper.toDomain
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightTimelineRepository(
    private val database: ReliveDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TimelineRepository {

    override suspend fun createCustom(timeline: Timeline.Custom, createdAt: Instant) {
        withContext(dispatcher) {
            database.customTimelinesQueries.insertCustomTimeline(
                id = timeline.id.value,
                name = timeline.name,
                theme = encodeThemeName(timeline.theme),
                cover_photo_ref = timeline.coverPhotoRef?.value,
                created_at = createdAt.epochMilliseconds,
            )
        }
    }

    override suspend fun findCustom(id: TimelineId): Timeline.Custom? = withContext(dispatcher) {
        database.customTimelinesQueries.selectCustomTimelineById(id.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun listCustom(): List<Timeline.Custom> = withContext(dispatcher) {
        database.customTimelinesQueries.selectAllCustomTimelines().executeAsList()
            .map { it.toDomain() }
    }

    override fun observeCustom(): Flow<List<Timeline.Custom>> =
        database.customTimelinesQueries.selectAllCustomTimelines()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun rename(id: TimelineId, newName: String) = withContext(dispatcher) {
        val trimmed = newName.trim()
        require(trimmed.isNotEmpty()) { "Timeline name must not be blank" }
        require(trimmed.length <= Timeline.Custom.MAX_NAME_LENGTH) {
            "Timeline name exceeds ${Timeline.Custom.MAX_NAME_LENGTH} chars"
        }
        val existing = database.customTimelinesQueries.selectCustomTimelineById(id.value)
            .executeAsOneOrNull() ?: return@withContext
        database.customTimelinesQueries.updateCustomTimeline(
            name = newName,
            theme = existing.theme,
            coverPhotoRef = existing.cover_photo_ref,
            id = id.value,
        )
    }

    override suspend fun updateTheme(id: TimelineId, theme: ThemeReference?) =
        withContext(dispatcher) {
            val existing = database.customTimelinesQueries.selectCustomTimelineById(id.value)
                .executeAsOneOrNull() ?: return@withContext
            database.customTimelinesQueries.updateCustomTimeline(
                name = existing.name,
                theme = encodeThemeName(theme),
                coverPhotoRef = existing.cover_photo_ref,
                id = id.value,
            )
        }

    override suspend fun updateCoverPhoto(id: TimelineId, coverPhotoRef: com.vaibhav.relive.domain.model.MediaStorageRef?) =
        withContext(dispatcher) {
            val existing = database.customTimelinesQueries.selectCustomTimelineById(id.value)
                .executeAsOneOrNull() ?: return@withContext
            database.customTimelinesQueries.updateCustomTimeline(
                name = existing.name,
                theme = existing.theme,
                coverPhotoRef = coverPhotoRef?.value,
                id = id.value,
            )
        }

    override suspend fun deleteCustom(id: TimelineId) {
        withContext(dispatcher) {
            database.customTimelinesQueries.deleteCustomTimeline(id.value)
        }
    }

    override suspend fun addMembership(momentId: MomentId, timelineId: TimelineId) {
        withContext(dispatcher) {
            database.membershipsQueries.insertMembership(
                moment_id = momentId.value,
                timeline_id = timelineId.value,
            )
        }
    }

    override suspend fun removeMembership(momentId: MomentId, timelineId: TimelineId) {
        withContext(dispatcher) {
            database.membershipsQueries.deleteMembership(
                momentId = momentId.value,
                timelineId = timelineId.value,
            )
        }
    }

    override suspend fun timelinesFor(momentId: MomentId): List<TimelineId> =
        withContext(dispatcher) {
            database.membershipsQueries.selectTimelinesForMoment(momentId.value)
                .executeAsList()
                .map { TimelineId(it) }
        }
}
