package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MomentFeelingSample
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    /**
     * Writes or clears the Moment's optional feeling. Like [setFavorite] this is
     * independent of [updateEditable] and of the 4-day edit window (ADR-0066).
     */
    suspend fun setFeeling(id: MomentId, feeling: MomentFeeling?)

    /**
     * `(createdAt, feeling)` pairs for every Moment created at or after [cutoff],
     * re-emitting on any archive change. This is the bounded projection Mood
     * insights read (PRODUCT_SPEC §10A) — implementations must not hydrate full
     * Moments for it; this default exists for in-memory fakes only.
     */
    fun observeFeelingSamplesSince(cutoff: Instant): Flow<List<MomentFeelingSample>> =
        observeAll().map { moments ->
            moments
                .filter { it.createdAt >= cutoff }
                .map { MomentFeelingSample(it.createdAt, it.feeling) }
        }

    /** Deletes the moment and all cascaded rows (memberships, tag links, media). */
    suspend fun delete(id: MomentId)

    /**
     * All moments, newest `createdAt` first (ties broken by descending id).
     *
     * This reads the **complete archive** and must not back the Home surface's All moments feed;
     * use [observeAllWindow] there. It remains appropriate for surfaces entered on demand and for
     * archive export. See ADR-0061.
     */
    suspend fun listAll(): List<Moment>

    /**
     * Same ordering as [listAll], emitting a fresh snapshot on any relevant change.
     *
     * Like [listAll] this hydrates the whole archive; the Home root must use [observeAllWindow].
     */
    fun observeAll(): Flow<List<Moment>>

    /**
     * A **bounded** newest-first window over All, at most [limit] moments, re-emitting whenever the
     * window's contents change. This is what the Home surface's All moments feed reads so the root
     * never observes or hydrates the complete archive on launch (ADR-0061).
     *
     * Callers grow [limit] by one page as the person scrolls toward older Moments. Because the
     * window is a single ordered read rather than stitched pages, an insert or delete anywhere in
     * it re-emits a consistent list with no duplicate or missing ids.
     */
    fun observeAllWindow(limit: Int): Flow<List<Moment>> = observeAll()

    /** Total number of moments in the archive, so a window knows whether older moments remain. */
    fun observeAllCount(): Flow<Long> = observeAll().map { it.size.toLong() }

    /**
     * A value that changes whenever a moment is added to or removed from the archive, computed
     * without reading any moment row. Callers that only need to know *that* the archive changed
     * must use this rather than folding over [observeAll], which hydrates the whole archive.
     */
    fun observeArchiveFingerprint(): Flow<Long> =
        observeAll().map { moments ->
            moments.fold(1L) { value, moment -> 31 * value + moment.createdAt.epochMilliseconds }
        }

    /**
     * Zero-based position of [id] in the newest-first All ordering, or `null` when it is absent.
     * Used to size the window when the surface is re-anchored at a moment resolved by Calendar
     * navigation or Search.
     */
    suspend fun positionInAll(id: MomentId, createdAt: Instant): Long? = null

    /**
     * Local, case-insensitive title/content search. Results use the repository's
     * normal newest-first ordering; presentation reverses them to the All
     * timeline's oldest-first order.
     */
    fun observeSearch(query: String): Flow<List<Moment>>

    /**
     * Resolves calendar navigation with bounded, scope-aware reads.  [dayStart]
     * and [nextDayStart] are current-device-local calendar boundaries.
     */
    suspend fun findDateNavigationTarget(
        scope: MomentDateNavigationScope,
        dayStart: Instant,
        nextDayStart: Instant,
    ): Moment? = null

    /** Moments that belong to the given custom timeline, newest first. */
    suspend fun listInTimeline(timelineId: TimelineId): List<Moment>

    fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>>
}

sealed interface MomentDateNavigationScope {
    data object All : MomentDateNavigationScope
    data class Custom(val timelineId: TimelineId) : MomentDateNavigationScope
}
