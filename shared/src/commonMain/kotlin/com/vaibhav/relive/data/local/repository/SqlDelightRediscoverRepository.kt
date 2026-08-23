package com.vaibhav.relive.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.vaibhav.relive.data.PersistenceMappingException
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.mapper.toDomain
import com.vaibhav.relive.domain.model.FavoritesCollectionSummary
import com.vaibhav.relive.domain.model.FavoriteMomentPreview
import com.vaibhav.relive.domain.model.FromYourPastMomentPreview
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.OnThisDayMomentPreview
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.domain.model.RediscoverOverview
import com.vaibhav.relive.domain.model.RediscoverPlaceSummary
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.model.RediscoverTagSummary
import com.vaibhav.relive.domain.model.RediscoveredMoment
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.repository.RediscoverRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class SqlDelightRediscoverRepository(
    private val database: ReliveDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RediscoverRepository {

    override fun observeFavoritesSummary(): Flow<FavoritesCollectionSummary> {
        val count = database.rediscoverQueries.favoriteMomentCount()
            .asFlow().mapToList(dispatcher).map { it.single() }
        val previews = database.rediscoverQueries.selectFavoriteVisualAttachments(
            FavoritesCollectionSummary.MAX_PREVIEW_ATTACHMENTS.toLong(),
        ).asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toDomain() } }
        return combine(count, previews, ::FavoritesCollectionSummary)
    }

    override fun observeFavoriteMoments(): Flow<List<Moment>> =
        database.rediscoverQueries.selectFavoriteMoments()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> withContext(dispatcher) { rows.map(::hydrateMoment) } }

    override fun observeFavoritePreviews(limit: Int): Flow<List<FavoriteMomentPreview>> {
        require(limit in 1..RediscoverRepository.MAX_FAVORITE_PREVIEWS) {
            "Favorite preview limit must be within 1..${RediscoverRepository.MAX_FAVORITE_PREVIEWS}"
        }
        return database.rediscoverQueries.selectFavoritePreviewMoments(limit.toLong())
            .asFlow()
            .mapToList(dispatcher)
            .flatMapLatest(::hydrateFavoritePreviews)
    }

    override fun observeOnThisDayPreviews(
        today: LocalCalendarDate,
        startOfToday: Instant,
        limit: Int,
    ): Flow<List<OnThisDayMomentPreview>> {
        require(limit in 1..RediscoverRepository.MAX_ON_THIS_DAY_PREVIEWS) {
            "On This Day preview limit must be within 1..${RediscoverRepository.MAX_ON_THIS_DAY_PREVIEWS}"
        }
        return database.rediscoverQueries.selectOnThisDayPreviewMoments(
            month = today.month.toLong(),
            day = today.day.toLong(),
            year = today.year.toLong(),
            startOfToday = startOfToday.epochMilliseconds,
            limit = limit.toLong(),
        ).asFlow().mapToList(dispatcher).flatMapLatest(::hydrateOnThisDayPreviews)
    }

    override fun observeOnThisDayMoments(today: LocalCalendarDate, startOfToday: Instant): Flow<List<Moment>> =
        database.rediscoverQueries.selectOnThisDayMoments(
            month = today.month.toLong(),
            day = today.day.toLong(),
            year = today.year.toLong(),
            startOfToday = startOfToday.epochMilliseconds,
        ).asFlow().mapToList(dispatcher).map { rows ->
            withContext(dispatcher) { rows.map(::hydrateMoment) }
        }

    override fun observeFromYourPastPreviews(query: RediscoverQuery): Flow<List<FromYourPastMomentPreview>> =
        pastCore(query).flatMapLatest(::hydrateFromYourPastPreviews)

    override fun observeFromYourPastMoments(query: RediscoverQuery): Flow<List<Moment>> =
        pastCore(query).flatMapLatest(::hydrateFromYourPastMoments)

    override fun observeOverview(query: RediscoverQuery): Flow<RediscoverOverview> {
        val count = database.rediscoverQueries.rediscoverMomentCount()
            .asFlow().mapToList(dispatcher).map { it.single() }
        val onThisDay = database.rediscoverQueries.selectOnThisDayCore(
            month = query.today.month.toLong(),
            day = query.today.day.toLong(),
            year = query.today.year.toLong(),
            startOfToday = query.startOfToday.epochMilliseconds,
        ).asFlow().mapToList(dispatcher).map { rows ->
            rows.map {
                CoreMoment(
                    id = it.id,
                    createdAt = it.created_at,
                    localYear = it.local_year ?: 0L,
                    title = it.title,
                    content = it.content,
                )
            }
        }.flatMapLatest(::hydrate)
        val fromYourPast = pastCore(query).flatMapLatest(::hydrate)
        val places = database.rediscoverQueries.selectRediscoverPlaces()
            .asFlow().mapToList(dispatcher).map { rows ->
                rows.map { RediscoverPlaceSummary(it.place_key ?: "", it.label ?: "", it.moment_count) }
            }
        val tags = database.rediscoverQueries.selectRediscoverTags()
            .asFlow().mapToList(dispatcher).map { rows ->
                rows.map { RediscoverTagSummary(it.canonical, it.moment_count) }
            }
        return combine(count, onThisDay, fromYourPast, places, tags) { total, day, past, place, tag ->
            RediscoverOverview(total, day, past, place, tag)
        }
    }

    private fun hydrate(core: List<CoreMoment>): Flow<List<RediscoveredMoment>> {
        if (core.isEmpty()) return flowOf(emptyList())
        val ids = core.map { it.id }
        val attachments = database.rediscoverQueries.selectRediscoverAttachmentsForMomentIds(ids)
            .asFlow().mapToList(dispatcher)
        val tags = database.rediscoverQueries.selectRediscoverTagsForMomentIds(ids)
            .asFlow().mapToList(dispatcher)
        return combine(attachments, tags) { attachmentRows, tagRows ->
            val attachmentsByMoment = attachmentRows.groupBy { it.moment_id }
                .mapValues { (_, rows) -> rows.map { it.toDomain() } }
            val tagsByMoment = tagRows.groupBy { it.moment_id }
                .mapValues { (_, rows) -> rows.map { decodeTag(it.canonical, it.label) } }
            core.map { row ->
                RediscoveredMoment(
                    id = MomentId(row.id),
                    createdAt = Instant(row.createdAt),
                    localYear = row.localYear.toInt(),
                    title = row.title,
                    content = row.content,
                    attachments = attachmentsByMoment[row.id].orEmpty(),
                    tags = tagsByMoment[row.id].orEmpty(),
                )
            }
        }
    }

    private fun hydrateFavoritePreviews(
        rows: List<com.vaibhav.relive.data.local.db.Moments>,
    ): Flow<List<FavoriteMomentPreview>> {
        if (rows.isEmpty()) return flowOf(emptyList())
        val ids = rows.map { it.id }
        return database.rediscoverQueries.selectRediscoverAttachmentsForMomentIds(ids)
            .asFlow()
            .mapToList(dispatcher)
            .map { attachments ->
                val attachmentsByMoment = attachments.groupBy { it.moment_id }
                    .mapValues { (_, values) -> values.map { it.toDomain() } }
                // Timeline presentation reverses the canonical newest-first query;
                // keep the shelf in that same chronological reading order.
                rows.asReversed().map { row ->
                    FavoriteMomentPreview(
                        id = MomentId(row.id),
                        createdAt = Instant(row.created_at),
                        title = row.title,
                        content = row.content,
                        attachments = attachmentsByMoment[row.id].orEmpty(),
                    )
                }
            }
    }

    private fun hydrateOnThisDayPreviews(
        rows: List<com.vaibhav.relive.data.local.db.SelectOnThisDayPreviewMoments>,
    ): Flow<List<OnThisDayMomentPreview>> {
        if (rows.isEmpty()) return flowOf(emptyList())
        val ids = rows.map { it.id }
        return database.rediscoverQueries.selectRediscoverAttachmentsForMomentIds(ids)
            .asFlow()
            .mapToList(dispatcher)
            .map { attachments ->
                val attachmentsByMoment = attachments.groupBy { it.moment_id }
                    .mapValues { (_, values) -> values.map { it.toDomain() } }
                rows.map { row ->
                    OnThisDayMomentPreview(
                        id = MomentId(row.id),
                        createdAt = Instant(row.created_at),
                        localYear = row.local_year?.toInt() ?: 0,
                        title = row.title,
                        content = row.content,
                        attachments = attachmentsByMoment[row.id].orEmpty(),
                    )
                }
            }
    }

    private fun pastCore(query: RediscoverQuery): Flow<List<CoreMoment>> =
        database.rediscoverQueries.selectPastRediscoveryCore(
            recentCutoff = query.recentCutoff.epochMilliseconds,
            startOfToday = query.startOfToday.epochMilliseconds,
            month = query.today.month.toLong(),
            day = query.today.day.toLong(),
            year = query.today.year.toLong(),
            dailySeed = query.dailySeed,
        ).asFlow().mapToList(dispatcher).map { rows ->
            rows.map {
                CoreMoment(
                    id = it.id,
                    createdAt = it.created_at,
                    updatedAt = it.updated_at,
                    localYear = it.local_year ?: 0L,
                    title = it.title,
                    content = it.content,
                    isFavorite = it.is_favorite,
                    locationLat = it.location_lat,
                    locationLon = it.location_lon,
                    locationDisplayName = it.location_display_name,
                    locationLocality = it.location_locality,
                    locationRegion = it.location_region,
                    locationCountry = it.location_country,
                )
            }
        }

    private fun hydrateFromYourPastPreviews(core: List<CoreMoment>): Flow<List<FromYourPastMomentPreview>> {
        if (core.isEmpty()) return flowOf(emptyList())
        val ids = core.map { it.id }
        return database.rediscoverQueries.selectRediscoverAttachmentsForMomentIds(ids)
            .asFlow()
            .mapToList(dispatcher)
            .map { attachments ->
                val attachmentsByMoment = attachments.groupBy { it.moment_id }
                    .mapValues { (_, values) -> values.map { it.toDomain() } }
                core.map { row ->
                    FromYourPastMomentPreview(
                        id = MomentId(row.id),
                        createdAt = Instant(row.createdAt),
                        title = row.title,
                        content = row.content,
                        attachments = attachmentsByMoment[row.id].orEmpty(),
                    )
                }
            }
    }

    private fun hydrateFromYourPastMoments(core: List<CoreMoment>): Flow<List<Moment>> {
        if (core.isEmpty()) return flowOf(emptyList())
        val ids = core.map { it.id }
        val attachments = database.rediscoverQueries.selectRediscoverAttachmentsForMomentIds(ids)
            .asFlow().mapToList(dispatcher)
        val tags = database.rediscoverQueries.selectRediscoverTagsForMomentIds(ids)
            .asFlow().mapToList(dispatcher)
        return combine(attachments, tags) { attachmentRows, tagRows ->
            val attachmentsByMoment = attachmentRows.groupBy { it.moment_id }
                .mapValues { (_, rows) -> rows.map { it.toDomain() } }
            val tagsByMoment = tagRows.groupBy { it.moment_id }
                .mapValues { (_, rows) -> rows.map { decodeTag(it.canonical, it.label) } }
            core.map { row ->
                Moment(
                    id = MomentId(row.id),
                    createdAt = Instant(row.createdAt),
                    updatedAt = row.updatedAt?.let(::Instant),
                    title = row.title,
                    content = row.content,
                    isFavorite = row.isFavorite != 0L,
                    location = if (
                        row.locationLat == null && row.locationLon == null &&
                        row.locationDisplayName.isNullOrBlank() && row.locationLocality.isNullOrBlank() &&
                        row.locationRegion.isNullOrBlank() && row.locationCountry.isNullOrBlank()
                    ) null else ReliveLocation(
                        latitude = row.locationLat,
                        longitude = row.locationLon,
                        placeName = row.locationDisplayName,
                        locality = row.locationLocality,
                        region = row.locationRegion,
                        country = row.locationCountry,
                    ),
                    tags = tagsByMoment[row.id].orEmpty(),
                    attachments = attachmentsByMoment[row.id].orEmpty(),
                )
            }
        }
    }

    private fun hydrateMoment(row: com.vaibhav.relive.data.local.db.Moments): Moment {
        val tags = database.momentTagsQueries.selectTagsForMoment(row.id).executeAsList()
            .map { decodeTag(it.canonical, it.label) }
        val attachments = database.mediaAttachmentsQueries.selectAttachmentsForMoment(row.id)
            .executeAsList()
            .map { it.toDomain() }
        return row.toDomain(tags = tags, attachments = attachments)
    }

    private fun decodeTag(canonical: String, label: String): Tag = Tag.ofOrNull(label)
        ?: throw PersistenceMappingException("Corrupt tag row canonical=$canonical label=$label")

    private data class CoreMoment(
        val id: String,
        val createdAt: Long,
        val localYear: Long,
        val title: String,
        val content: String,
        val updatedAt: Long? = null,
        val isFavorite: Long = 0L,
        val locationLat: Double? = null,
        val locationLon: Double? = null,
        val locationDisplayName: String? = null,
        val locationLocality: String? = null,
        val locationRegion: String? = null,
        val locationCountry: String? = null,
    )

}
