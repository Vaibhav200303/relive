package com.vaibhav.relive

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Duration
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.ProcessedMedia
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RediscoverQaSeederTest {
    private val now = RediscoverCalendar.startOfDay(com.vaibhav.relive.domain.model.LocalCalendarDate(2026, 8, 23)) + Duration.ofHours(12)

    @Test
    fun seed_uses_prior_local_anniversaries_and_old_non_anniversary_dates() = runTest {
        val repository = FakeMomentRepository()
        RediscoverQaSeeder(repository, FakeMediaStore(), now).seed(media())

        val dates = repository.moments.values.associateBy { it.title }
        assertEquals(2025, RediscoverCalendar.localDate(dates.getValue("[QA] Photo memory").createdAt).year)
        assertEquals(2024, RediscoverCalendar.localDate(dates.getValue("[QA] Video memory").createdAt).year)
        assertEquals(2023, RediscoverCalendar.localDate(dates.getValue("[QA] First week of college").createdAt).year)
        assertEquals(2022, RediscoverCalendar.localDate(dates.getValue("[QA] Voice memory").createdAt).year)
        assertTrue(dates.getValue("[QA] Old trip").createdAt <= now - Duration.ofDays(90))
        assertTrue(dates.getValue("[QA] Recent memory").createdAt > now - Duration.ofDays(90))
        assertTrue(dates.getValue("[QA] Old trip").tags.any { it.canonical == "travel" })
        assertEquals("QA Pune", dates.getValue("[QA] Old trip").location?.locality)
    }

    @Test
    fun repeated_seed_is_idempotent_and_remove_preserves_real_moments() = runTest {
        val repository = FakeMomentRepository()
        val store = FakeMediaStore()
        val real = Moment(MomentId("real-moment"), now, title = "Real memory")
        repository.insert(real)
        val seeder = RediscoverQaSeeder(repository, store, now)

        seeder.seed(media())
        val firstCount = repository.moments.size
        seeder.seed(media())
        assertEquals(firstCount, repository.moments.size)

        seeder.remove()
        assertEquals(setOf(real.id.value), repository.moments.keys)
        assertFalse(store.deleted.isEmpty())
    }

    private fun media() = QaMedia(
        image = ProcessedMedia(MediaType.Image, MediaStorageRef("images/qa.jpg")),
        video = ProcessedMedia(MediaType.Video, MediaStorageRef("videos/qa.mp4")),
        audio = ProcessedMedia(MediaType.Audio, MediaStorageRef("audio/qa.m4a")),
    )
}

private class FakeMomentRepository : MomentRepository {
    val moments = linkedMapOf<String, Moment>()
    override suspend fun insert(moment: Moment, timelineIds: Set<TimelineId>) { moments[moment.id.value] = moment }
    override suspend fun findById(id: MomentId): Moment? = moments[id.value]
    override suspend fun updateEditable(moment: Moment) { moments[moment.id.value] = moment }
    override suspend fun setFavorite(id: MomentId, isFavorite: Boolean) = Unit
    override suspend fun setFeeling(id: MomentId, feeling: com.vaibhav.relive.domain.model.MomentFeeling?) = Unit
    override suspend fun delete(id: MomentId) { moments.remove(id.value) }
    override suspend fun listAll(): List<Moment> = moments.values.toList()
    override fun observeAll(): Flow<List<Moment>> = flowOf(moments.values.toList())
    override fun observeSearch(query: String): Flow<List<Moment>> = flowOf(
        moments.values.filter { it.title.contains(query, true) || it.content.contains(query, true) },
    )
    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> = emptyList()
    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> = flowOf(emptyList())
}

private class FakeMediaStore : MediaStore {
    val deleted = mutableListOf<MediaStorageRef>()
    override fun extensionFor(type: MediaType): String = "tmp"
    override fun allocateKey(type: MediaType): MediaStorageRef = MediaStorageRef("tmp/new")
    override fun resolveAbsolutePath(ref: MediaStorageRef): String = ref.value
    override fun exists(ref: MediaStorageRef): Boolean = true
    override fun delete(ref: MediaStorageRef) { deleted += ref }
    override fun sizeBytes(ref: MediaStorageRef): Long = 1L
}
