package com.vaibhav.relive.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.vaibhav.relive.data.local.db.Moments
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.mapper.decodeTag
import com.vaibhav.relive.data.local.mapper.encodeFavorite
import com.vaibhav.relive.data.local.mapper.encodeMediaTypeName
import com.vaibhav.relive.data.local.mapper.toDomain
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.MomentDateNavigationScope
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightMomentRepository(
    private val database: ReliveDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MomentRepository {

    override suspend fun insert(moment: Moment, timelineIds: Set<TimelineId>): Unit =
        withContext(dispatcher) {
            database.transaction {
                writeMomentRow(moment)
                writeTags(moment)
                writeAttachments(moment)
                for (timelineId in timelineIds) {
                    val exists = database.customTimelinesQueries
                        .selectCustomTimelineById(timelineId.value)
                        .executeAsOneOrNull() != null
                    if (!exists) {
                        throw IllegalArgumentException(
                            "Unknown timeline id: ${timelineId.value}",
                        )
                    }
                    database.membershipsQueries.insertMembership(
                        moment_id = moment.id.value,
                        timeline_id = timelineId.value,
                    )
                }
            }
        }

    override suspend fun findById(id: MomentId): Moment? = withContext(dispatcher) {
        val row = database.momentsQueries.selectMomentById(id.value).executeAsOneOrNull()
            ?: return@withContext null
        hydrate(row)
    }

    override suspend fun updateEditable(moment: Moment): Unit = withContext(dispatcher) {
        database.transaction {
            val existing = database.momentsQueries.selectMomentById(moment.id.value)
                .executeAsOneOrNull()
                ?: throw IllegalStateException("Moment ${moment.id.value} does not exist")
            check(existing.created_at == moment.createdAt.epochMilliseconds) {
                "createdAt is immutable via updateEditable"
            }
            database.momentsQueries.updateMomentEditableFields(
                updatedAt = moment.updatedAt?.epochMilliseconds,
                title = moment.title,
                content = moment.content,
                locationLat = moment.location?.latitude,
                locationLon = moment.location?.longitude,
                locationDisplayName = moment.location?.placeName,
                locationLocality = moment.location?.locality,
                locationRegion = moment.location?.region,
                locationCountry = moment.location?.country,
                id = moment.id.value,
            )
            database.momentTagsQueries.deleteMomentTagsForMoment(moment.id.value)
            database.mediaAttachmentsQueries.deleteAttachmentsForMoment(moment.id.value)
            writeTags(moment)
            writeAttachments(moment)
        }
    }

    override suspend fun setFavorite(id: MomentId, isFavorite: Boolean) {
        withContext(dispatcher) {
            database.momentsQueries.updateFavorite(
                isFavorite = encodeFavorite(isFavorite),
                id = id.value,
            )
        }
    }

    override suspend fun delete(id: MomentId) {
        withContext(dispatcher) {
            database.momentsQueries.deleteMoment(id.value)
        }
    }

    override suspend fun listAll(): List<Moment> = withContext(dispatcher) {
        database.momentsQueries.selectAllMoments().executeAsList().map { hydrate(it) }
    }

    override fun observeAll(): Flow<List<Moment>> =
        database.momentsQueries.selectAllMoments()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> withContext(dispatcher) { rows.map { hydrate(it) } } }

    override fun observeAllWindow(limit: Int): Flow<List<Moment>> =
        database.momentsQueries.selectAllMomentsWindow(limit.toLong())
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> withContext(dispatcher) { hydrateAll(rows) } }

    override fun observeAllCount(): Flow<Long> =
        database.momentsQueries.countAllMoments()
            .asFlow()
            .mapToOne(dispatcher)

    override fun observeArchiveFingerprint(): Flow<Long> =
        database.momentsQueries.selectAllMomentsFingerprint()
            .asFlow()
            .mapToOne(dispatcher)
            .map { row ->
                var value = 1L
                value = 31 * value + row.moment_count
                value = 31 * value + row.newest_created_at
                value = 31 * value + row.oldest_created_at
                value
            }

    override suspend fun positionInAll(id: MomentId, createdAt: Instant): Long? =
        withContext(dispatcher) {
            database.momentsQueries.selectMomentById(id.value).executeAsOneOrNull()
                ?: return@withContext null
            database.momentsQueries
                .countMomentsNewerThan(createdAt.epochMilliseconds, id.value)
                .executeAsOne()
        }

    override fun observeSearch(query: String): Flow<List<Moment>> =
        database.momentsQueries.selectMomentsMatchingQuery(query)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> withContext(dispatcher) { rows.map { hydrate(it) } } }

    override suspend fun findDateNavigationTarget(
        scope: MomentDateNavigationScope,
        dayStart: Instant,
        nextDayStart: Instant,
    ): Moment? = withContext(dispatcher) {
        val row = when (scope) {
            MomentDateNavigationScope.All -> database.momentsQueries
                .selectFirstMomentOnOrAfterInAll(dayStart.epochMilliseconds, nextDayStart.epochMilliseconds)
                .executeAsOneOrNull()
                ?: database.momentsQueries.selectFirstMomentAfterInAll(nextDayStart.epochMilliseconds)
                    .executeAsOneOrNull()
                ?: database.momentsQueries.selectLastMomentBeforeInAll(dayStart.epochMilliseconds)
                    .executeAsOneOrNull()
            is MomentDateNavigationScope.Custom -> database.momentsQueries
                .selectFirstMomentOnOrAfterInTimeline(
                    scope.timelineId.value,
                    dayStart.epochMilliseconds,
                    nextDayStart.epochMilliseconds,
                )
                .executeAsOneOrNull()
                ?: database.momentsQueries.selectFirstMomentAfterInTimeline(
                    scope.timelineId.value,
                    nextDayStart.epochMilliseconds,
                ).executeAsOneOrNull()
                ?: database.momentsQueries.selectLastMomentBeforeInTimeline(
                    scope.timelineId.value,
                    dayStart.epochMilliseconds,
                ).executeAsOneOrNull()
        }
        row?.let(::hydrate)
    }

    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> =
        withContext(dispatcher) {
            database.momentsQueries.selectMomentsInTimeline(timelineId.value)
                .executeAsList().map { hydrate(it) }
        }

    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> =
        database.momentsQueries.selectMomentsInTimeline(timelineId.value)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> withContext(dispatcher) { rows.map { hydrate(it) } } }

    /**
     * Hydrates a whole loaded window in three queries rather than `1 + 2N`: the rows are already
     * in hand, and their tags and attachments are batch-loaded by id and grouped in memory. This
     * is the shared read-model contract the Rediscover shelves already follow (ARCHITECTURE.md 14).
     */
    private fun hydrateAll(rows: List<Moments>): List<Moment> {
        if (rows.isEmpty()) return emptyList()
        val ids = rows.map { it.id }
        val tagsByMoment = database.momentTagsQueries.selectTagsForMomentIds(ids)
            .executeAsList()
            .groupBy({ it.moment_id }) { decodeTag(it.canonical, it.label) }
        val attachmentsByMoment = database.mediaAttachmentsQueries
            .selectAttachmentsForMomentIds(ids)
            .executeAsList()
            .groupBy { it.moment_id }
        return rows.map { row ->
            row.toDomain(
                tags = tagsByMoment[row.id].orEmpty(),
                attachments = attachmentsByMoment[row.id].orEmpty().map { it.toDomain() },
            )
        }
    }

    private fun hydrate(row: Moments): Moment {
        val tags = database.momentTagsQueries.selectTagsForMoment(row.id).executeAsList()
            .map { decodeTag(it.canonical, it.label) }
        val attachments = database.mediaAttachmentsQueries.selectAttachmentsForMoment(row.id)
            .executeAsList()
            .map { it.toDomain() }
        return row.toDomain(tags = tags, attachments = attachments)
    }

    // --- private write helpers, ONLY call from inside a database.transaction { ... } ---

    private fun writeMomentRow(moment: Moment) {
        database.momentsQueries.insertMoment(
            id = moment.id.value,
            created_at = moment.createdAt.epochMilliseconds,
            updated_at = moment.updatedAt?.epochMilliseconds,
            title = moment.title,
            content = moment.content,
            is_favorite = encodeFavorite(moment.isFavorite),
            location_lat = moment.location?.latitude,
            location_lon = moment.location?.longitude,
            location_display_name = moment.location?.placeName,
            location_locality = moment.location?.locality,
            location_region = moment.location?.region,
            location_country = moment.location?.country,
        )
    }

    private fun writeTags(moment: Moment) {
        for (tag in moment.tags) {
            writeTagAggregate(moment.id, tag)
        }
    }

    private fun writeAttachments(moment: Moment) {
        for (a in moment.attachments) {
            writeAttachment(moment.id, a)
        }
    }

    private fun writeTagAggregate(momentId: MomentId, tag: Tag) {
        database.tagsQueries.insertTagIfAbsent(canonical = tag.canonical, label = tag.label)
        database.momentTagsQueries.insertMomentTag(
            moment_id = momentId.value,
            tag_canonical = tag.canonical,
        )
    }

    private fun writeAttachment(momentId: MomentId, a: MediaAttachment) {
        database.mediaAttachmentsQueries.insertMediaAttachment(
            id = a.id.value,
            moment_id = momentId.value,
            media_type = encodeMediaTypeName(a.type),
            storage_ref = a.storageRef.value,
            sort_index = a.sortIndex.toLong(),
        )
    }
}
