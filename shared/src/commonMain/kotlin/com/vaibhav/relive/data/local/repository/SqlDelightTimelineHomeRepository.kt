package com.vaibhav.relive.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.vaibhav.relive.data.PersistenceMappingException
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.mapper.decodeThemeName
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.TimelineHomeRepository
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SqlDelightTimelineHomeRepository(
    private val database: ReliveDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TimelineHomeRepository {

    override fun observeSummaries(): Flow<List<TimelineHomeSummary>> = combine(
        database.timelineHomeQueries.selectTimelineHomeCounts().asFlow().mapToList(dispatcher),
        database.timelineHomeQueries.selectTimelineHomePreviewAttachments().asFlow().mapToList(dispatcher),
    ) { counts, previews ->
        val previewsByScope = previews.groupBy { it.scope_key }
        counts.map { count ->
            val timeline = if (count.scope_key == ALL_SCOPE_KEY) {
                Timeline.All
            } else {
                Timeline.Custom(
                    id = TimelineId(count.timeline_id ?: error("Missing custom timeline id")),
                    name = count.name,
                    theme = count.theme?.let(::decodeThemeName),
                )
            }
            TimelineHomeSummary(
                timeline = timeline,
                momentCount = count.moment_count,
                createdAt = count.created_at?.let(::Instant),
                previewAttachments = previewsByScope[count.scope_key].orEmpty().map { preview ->
                    MediaAttachment(
                        id = MediaAttachmentId(preview.id),
                        type = decodeMediaType(preview.media_type),
                        storageRef = MediaStorageRef(preview.storage_ref),
                        sortIndex = preview.sort_index.toInt(),
                    )
                },
            )
        }
    }.map { it }

    override fun observeAllCollageCandidates(bucket: Long): Flow<List<MediaAttachment>> =
        database.timelineHomeQueries.selectAllCollageCandidates(bucket)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { row ->
                    MediaAttachment(
                        id = MediaAttachmentId(row.id),
                        type = decodeMediaType(row.media_type),
                        storageRef = MediaStorageRef(row.storage_ref),
                        sortIndex = row.sort_index.toInt(),
                    )
                }
            }

    private fun decodeMediaType(raw: String): MediaType = MediaType.entries.firstOrNull { it.name == raw }
        ?: throw PersistenceMappingException("Unknown media_type='$raw'")

    private companion object {
        const val ALL_SCOPE_KEY = "__all__"
    }
}
