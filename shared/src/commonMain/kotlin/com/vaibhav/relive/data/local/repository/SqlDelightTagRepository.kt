package com.vaibhav.relive.data.local.repository

import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.mapper.decodeTag
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.TagRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightTagRepository(
    private val database: ReliveDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TagRepository {

    override suspend fun tagsFor(momentId: MomentId): List<Tag> = withContext(dispatcher) {
        database.momentTagsQueries.selectTagsForMoment(momentId.value).executeAsList()
            .map { decodeTag(it.canonical, it.label) }
    }

    override suspend fun attach(momentId: MomentId, tag: Tag) = withContext(dispatcher) {
        database.transaction {
            database.tagsQueries.insertTagIfAbsent(canonical = tag.canonical, label = tag.label)
            database.momentTagsQueries.insertMomentTag(
                moment_id = momentId.value,
                tag_canonical = tag.canonical,
            )
        }
    }

    override suspend fun detach(momentId: MomentId, tag: Tag) {
        withContext(dispatcher) {
            database.momentTagsQueries.deleteMomentTag(
                momentId = momentId.value,
                tagCanonical = tag.canonical,
            )
        }
    }

    override suspend fun distinctInTimeline(timelineId: TimelineId): List<Tag> =
        withContext(dispatcher) {
            database.momentTagsQueries.selectDistinctTagsInTimeline(timelineId.value)
                .executeAsList()
                .map { decodeTag(it.canonical, it.label) }
        }

    override suspend fun distinctAll(): List<Tag> = withContext(dispatcher) {
        database.momentTagsQueries.selectDistinctTagsAll().executeAsList()
            .map { decodeTag(it.canonical, it.label) }
    }

    override suspend fun pruneUnused() {
        withContext(dispatcher) {
            database.tagsQueries.deleteUnusedTags()
        }
    }
}
