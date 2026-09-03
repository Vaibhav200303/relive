package com.vaibhav.relive.data.local

import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Home surface's All moments feed is a bounded newest-first window: the root must never
 * observe or hydrate the complete archive on launch (ADR-0061).
 */
class HomeFeedWindowTest {
    private lateinit var fx: TestFixture

    @BeforeTest fun setup() { fx = TestFixture() }
    @AfterTest fun tearDown() { fx.close() }

    private suspend fun seed(count: Int) {
        repeat(count) { i ->
            fx.moments.insert(
                sampleMoment(
                    id = "m$i",
                    title = "Moment $i",
                    createdAtMs = 1_700_000_000_000L + i * 1_000L,
                ),
            )
        }
    }

    @Test fun windowIsBoundedByLimit() = runTest {
        seed(50)
        val window = fx.moments.observeAllWindow(limit = 30).first()
        assertEquals(30, window.size, "window must not exceed its limit")
    }

    @Test fun windowIsNewestFirst() = runTest {
        seed(10)
        val window = fx.moments.observeAllWindow(limit = 10).first()
        assertEquals("Moment 9", window.first().title, "newest moment leads the window")
        assertEquals("Moment 0", window.last().title, "oldest loaded moment trails it")
        val timestamps = window.map { it.createdAt.epochMilliseconds }
        assertEquals(timestamps.sortedDescending(), timestamps, "strictly newest-first")
    }

    @Test fun windowHoldsTheNewestMomentsNotAnArbitrarySlice() = runTest {
        seed(50)
        val window = fx.moments.observeAllWindow(limit = 5).first()
        assertEquals(
            listOf("Moment 49", "Moment 48", "Moment 47", "Moment 46", "Moment 45"),
            window.map { it.title },
        )
    }

    @Test fun growingTheLimitAppendsOlderMomentsWithoutDisturbingTheHead() = runTest {
        seed(20)
        val firstPage = fx.moments.observeAllWindow(limit = 5).first()
        val secondPage = fx.moments.observeAllWindow(limit = 10).first()
        assertEquals(10, secondPage.size)
        assertEquals(
            firstPage.map { it.id },
            secondPage.take(5).map { it.id },
            "growing the window must not reorder or drop what was already loaded",
        )
        assertTrue(secondPage.map { it.id }.toSet().size == 10, "no duplicate ids across a grown window")
    }

    @Test fun limitLargerThanArchiveReturnsEverything() = runTest {
        seed(3)
        assertEquals(3, fx.moments.observeAllWindow(limit = 30).first().size)
    }

    @Test fun emptyArchiveYieldsEmptyWindow() = runTest {
        assertTrue(fx.moments.observeAllWindow(limit = 30).first().isEmpty())
    }

    @Test fun windowBatchHydratesTagsAndAttachments() = runTest {
        fx.moments.insert(
            sampleMoment(
                id = "rich",
                title = "Rich",
                createdAtMs = 1_700_000_500_000L,
                tags = listOf(Tag.of("Family"), Tag.of("Home")),
                attachments = listOf(
                    sampleAttachment("a1", MediaType.Image, sortIndex = 0),
                    sampleAttachment("a2", MediaType.Video, sortIndex = 1),
                ),
            ),
        )
        fx.moments.insert(sampleMoment(id = "plain", title = "Plain", createdAtMs = 1_700_000_400_000L))

        val window = fx.moments.observeAllWindow(limit = 30).first()
        val rich = window.single { it.id == MomentId("rich") }
        val plain = window.single { it.id == MomentId("plain") }

        assertEquals(2, rich.tags.size, "tags survive batch hydration")
        assertEquals(2, rich.attachments.size, "attachments survive batch hydration")
        assertEquals(
            listOf("a1", "a2"),
            rich.attachments.map { it.id.value },
            "attachment sort order is preserved",
        )
        assertTrue(plain.tags.isEmpty(), "a moment without tags gets none from its neighbour")
        assertTrue(plain.attachments.isEmpty(), "a moment without attachments gets none from its neighbour")
    }

    @Test fun windowMatchesUnboundedReadForTheRangeItCovers() = runTest {
        seed(12)
        val all = fx.moments.observeAll().first()
        val window = fx.moments.observeAllWindow(limit = 12).first()
        assertEquals(all, window, "a full-size window is identical to the unbounded read")
    }

    @Test fun countReportsWhetherOlderMomentsRemain() = runTest {
        seed(7)
        assertEquals(7L, fx.moments.observeAllCount().first())
    }

    @Test fun positionInAllLocatesAMomentInNewestFirstOrder() = runTest {
        seed(5)
        // Moment 4 is newest -> position 0; Moment 0 is oldest -> position 4.
        assertEquals(
            0L,
            fx.moments.positionInAll(MomentId("m4"), Instant(1_700_000_000_000L + 4_000L)),
        )
        assertEquals(
            4L,
            fx.moments.positionInAll(MomentId("m0"), Instant(1_700_000_000_000L)),
        )
    }

    @Test fun positionInAllIsNullForAnAbsentMoment() = runTest {
        seed(3)
        assertNull(fx.moments.positionInAll(MomentId("ghost"), Instant(1_700_000_000_000L)))
    }
}
