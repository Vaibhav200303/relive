package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Custom timelines and their moment memberships. `Timeline.All` is never
 * accepted by this API — it is a logical timeline surfaced by
 * [MomentRepository.listAll].
 */
interface TimelineRepository {

    suspend fun createCustom(timeline: Timeline.Custom, createdAt: Instant)

    suspend fun findCustom(id: TimelineId): Timeline.Custom?

    /** Custom timelines ordered by creation ascending (deterministic). */
    suspend fun listCustom(): List<Timeline.Custom>

    fun observeCustom(): Flow<List<Timeline.Custom>>

    suspend fun rename(id: TimelineId, newName: String)

    suspend fun updateAppearance(id: TimelineId, appearance: TimelineAppearance)

    /** Stores an optional Relive-managed image used only as this timeline's cover. */
    suspend fun updateCoverPhoto(id: TimelineId, coverPhotoRef: com.vaibhav.relive.domain.model.MediaStorageRef?)

    /**
     * Deletes the custom timeline and every membership row referencing it.
     * Does NOT delete referenced moments.
     */
    suspend fun deleteCustom(id: TimelineId)

    /** Idempotent: a repeat call for an already-existing pair is a no-op. */
    suspend fun addMembership(momentId: MomentId, timelineId: TimelineId)

    suspend fun removeMembership(momentId: MomentId, timelineId: TimelineId)

    suspend fun timelinesFor(momentId: MomentId): List<TimelineId>
}
