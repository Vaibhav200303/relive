package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.TimelineId
import kotlinx.coroutines.flow.Flow

/**
 * Persistence-facing operations on [Moment]. The 4-day edit/forget policy is
 * enforced by the application layer, not here; the repository still refuses to
 * change immutable persistence fields (in particular `createdAt`).
 */
interface MomentRepository {

    /**
     * Atomically writes the moment and its aggregates (tags, attachments) and,
     * if [timelineIds] is non-empty, records custom-timeline memberships.
     *
     * `Timeline.All` is not a valid membership target — pass only [TimelineId]
     * values corresponding to persisted custom timelines. Duplicate ids in
     * [timelineIds] are collapsed by the underlying set.
     *
     * Fails if a moment with the same id already exists.
     */
    suspend fun insert(moment: Moment, timelineIds: Set<TimelineId> = emptySet())

    suspend fun findById(id: MomentId): Moment?

    /**
     * Updates only the editable fields of the moment (title, content, favorite,
     * location, tags, attachments) plus [Moment.updatedAt]. `createdAt` is
     * preserved exactly as originally stored; if the caller-supplied moment has a
     * different `createdAt`, the update fails.
     */
    suspend fun updateEditable(moment: Moment)

    suspend fun setFavorite(id: MomentId, isFavorite: Boolean)

    /** Deletes the moment and all cascaded rows (memberships, tag links, media). */
    suspend fun delete(id: MomentId)

    /** All moments, newest `createdAt` first (ties broken by descending id). */
    suspend fun listAll(): List<Moment>

    /** Same ordering as [listAll], emitting a fresh snapshot on any relevant change. */
    fun observeAll(): Flow<List<Moment>>

    /**
     * Local, case-insensitive title/content search. Results use the repository's
     * normal newest-first ordering; presentation reverses them to the All
     * timeline's oldest-first order.
     */
    fun observeSearch(query: String): Flow<List<Moment>>

    /** Moments that belong to the given custom timeline, newest first. */
    suspend fun listInTimeline(timelineId: TimelineId): List<Moment>

    fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>>
}
