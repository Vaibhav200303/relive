package com.vaibhav.relive.debug

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Purely in-memory `MomentRepository` seeded with sample moments. Opt-in only —
 * debug builds now use the persistent SQLDelight repository (same as release) so
 * user-created moments survive process death, updates, and Recents removal. This
 * class remains for explicit dev use (e.g. seeding a preview or a test fixture)
 * and MUST NOT be wired into the app composition root; doing so silently wipes
 * user data on every restart.
 */
internal class DebugSampleMomentRepository : MomentRepository {

    private val state = MutableStateFlow(initialSeed())

    override suspend fun insert(moment: Moment, timelineIds: Set<TimelineId>) {
        state.value = (state.value + moment).sortedDescending()
    }

    override suspend fun findById(id: MomentId): Moment? =
        state.value.firstOrNull { it.id == id }

    override suspend fun updateEditable(moment: Moment) {
        state.value = state.value.map { if (it.id == moment.id) moment else it }.sortedDescending()
    }

    override suspend fun setFavorite(id: MomentId, isFavorite: Boolean) {
        state.value = state.value.map {
            if (it.id == id) it.copy(isFavorite = isFavorite) else it
        }
    }

    override suspend fun delete(id: MomentId) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun listAll(): List<Moment> = state.value

    override fun observeAll(): Flow<List<Moment>> = state.asStateFlow()

    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> = emptyList()

    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> =
        state.asStateFlow().map { emptyList() }

    private fun List<Moment>.sortedDescending(): List<Moment> =
        sortedWith(compareByDescending<Moment> { it.createdAt }.thenByDescending { it.id.value })
}

private fun initialSeed(): List<Moment> {
    val samples = listOf(
        Moment(
            id = MomentId("debug-1"),
            createdAt = utcDate(2024, 9, 28, hour = 8),
            title = "Quiet morning light",
            content = "The first cup of coffee and the low autumn sun through the kitchen window.",
            isFavorite = true,
        ),
        Moment(
            id = MomentId("debug-2"),
            createdAt = utcDate(2024, 8, 14, hour = 21),
            title = "",
            content = "Note to self: keep making time for slow evenings.",
            isFavorite = false,
        ),
        Moment(
            id = MomentId("debug-3"),
            createdAt = utcDate(2024, 6, 5, hour = 15),
            title = "Bookshop off the main street",
            content = "Found a little English shelf tucked behind travel guides.",
            location = ReliveLocation(placeName = "Livraria Lello", locality = "Porto", country = "Portugal"),
            isFavorite = false,
        ),
        Moment(
            id = MomentId("debug-4"),
            createdAt = utcDate(2024, 3, 12, hour = 19),
            title = "The long walk after the storm",
            content = LONG_SAMPLE_CONTENT,
            isFavorite = true,
        ),
        Moment(
            id = MomentId("debug-5"),
            createdAt = utcDate(2023, 12, 24, hour = 22),
            title = "Snow at last",
            content = "Everyone stayed up until the flakes started sticking. Even the dog watched from the doorstep.",
            isFavorite = false,
        ),
    )
    return samples.sortedWith(
        compareByDescending<Moment> { it.createdAt }.thenByDescending { it.id.value },
    )
}

/**
 * Deterministic UTC epoch millis for a sample date; the presentation layer
 * re-formats these in the device's local time zone via `EditorialDateFormatter`.
 */
private fun utcDate(year: Int, month: Int, day: Int, hour: Int = 12): Instant {
    // Days-from-civil algorithm (Howard Hinnant): computes days since 1970-01-01 in
    // the proleptic Gregorian calendar. Sample seeds only — real persisted moments
    // never come through this path, so no test-relevant behavior depends on it.
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = (y - era * 400).toLong()
    val monthOffset = if (month > 2) month - 3 else month + 9
    val doy = (153L * monthOffset + 2L) / 5L + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era.toLong() * 146097 + doe - 719468
    val ms = days * 86_400_000L + hour * 3_600_000L
    return Instant(ms)
}

private const val LONG_SAMPLE_CONTENT: String =
    "The rain finally broke around six. We took the trail behind the house because " +
        "no one had been up there since spring, and the light was that heavy gold that " +
        "only shows up after a hard storm. The dog ran ahead like she knew the way. " +
        "By the time we reached the ridge the whole valley was steaming and the town " +
        "looked like it had been drawn in pencil. We didn't say much. There wasn't " +
        "really anything to say. I want to remember this exactly the way it was."
