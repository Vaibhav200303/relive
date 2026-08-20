package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.TimelineId

/**
 * Tag storage independent of any single moment. Tag rows are shared across
 * moments; unused rows are cleaned up only via [pruneUnused].
 */
interface TagRepository {

    /** Tags attached to a given moment, ordered by canonical form. */
    suspend fun tagsFor(momentId: MomentId): List<Tag>

    /** Attaches [tag] to [momentId], upserting the tag row (label preserved). */
    suspend fun attach(momentId: MomentId, tag: Tag)

    suspend fun detach(momentId: MomentId, tag: Tag)

    suspend fun distinctInTimeline(timelineId: TimelineId): List<Tag>

    suspend fun distinctAll(): List<Tag>

    /** Removes tag rows no longer referenced by any moment_tags row. */
    suspend fun pruneUnused()
}
