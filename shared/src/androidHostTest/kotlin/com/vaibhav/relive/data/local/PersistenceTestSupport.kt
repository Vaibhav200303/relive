package com.vaibhav.relive.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.data.local.repository.SqlDelightTagRepository
import com.vaibhav.relive.data.local.repository.SqlDelightTimelineRepository
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.Dispatchers

internal class TestFixture {
    val driver: JdbcSqliteDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        ReliveDatabase.Schema.create(it)
    }
    val database: ReliveDatabase = ReliveDatabaseFactory.create(driver)
    val moments = SqlDelightMomentRepository(database, Dispatchers.Unconfined)
    val timelines = SqlDelightTimelineRepository(database, Dispatchers.Unconfined)
    val tags = SqlDelightTagRepository(database, Dispatchers.Unconfined)

    fun close() = driver.close()
}

internal fun sampleMoment(
    id: String = "moment-1",
    createdAtMs: Long = 1_700_000_000_000L,
    updatedAtMs: Long? = null,
    title: String = "",
    content: String = "",
    isFavorite: Boolean = false,
    location: ReliveLocation? = null,
    tags: List<Tag> = emptyList(),
    attachments: List<MediaAttachment> = emptyList(),
): Moment = Moment(
    id = MomentId(id),
    createdAt = Instant(createdAtMs),
    updatedAt = updatedAtMs?.let { Instant(it) },
    title = title,
    content = content,
    isFavorite = isFavorite,
    location = location,
    tags = tags,
    attachments = attachments,
)

internal fun sampleAttachment(
    id: String,
    type: MediaType = MediaType.Image,
    ref: String = "file://$id",
    sortIndex: Int,
): MediaAttachment = MediaAttachment(
    id = MediaAttachmentId(id),
    type = type,
    storageRef = MediaStorageRef(ref),
    sortIndex = sortIndex,
)

internal fun sampleCustomTimeline(
    id: String = "tl-1",
    name: String = "Japan 2026",
    theme: ThemeReference? = ThemeReference.WarmJournal,
): Timeline.Custom = Timeline.Custom(id = TimelineId(id), name = name, theme = theme)
