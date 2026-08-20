package com.vaibhav.relive.debug

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The debug variant swaps in an in-memory repository so the timeline can be
 * verified visually without touching SQLite. Phase 3's Keep Moment relies on
 * `insert` updating the observed flow — this test proves that contract so the
 * debug repo stays semantically aligned with the real SQLDelight repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugSampleMomentRepositoryTest {

    @Test
    fun insertEmitsNewlyCreatedMomentAtTheNewestEdge() = runTest {
        val repo = DebugSampleMomentRepository()
        val seeded = repo.observeAll().first()
        val previousNewestCreatedAt = seeded.first().createdAt

        // Any moment newer than the seeded set — the seeds are pre-2025.
        val fresh = Moment(
            id = MomentId("phase-3-test"),
            createdAt = Instant(2_000_000_000_000L),
            title = "fresh",
        )
        repo.insert(fresh)

        val updated = repo.observeAll().first()
        assertEquals(seeded.size + 1, updated.size)
        assertEquals(fresh.id, updated.first().id)
        assertTrue(updated.first().createdAt >= previousNewestCreatedAt)
    }
}
