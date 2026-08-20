package com.vaibhav.relive.data.local

import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CascadesAndForeignKeysTest {
    private lateinit var fx: TestFixture

    @BeforeTest fun setup() { fx = TestFixture() }
    @AfterTest fun tearDown() { fx.close() }

    @Test fun deletingMomentCascadesEverything() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t1"), Instant(1L))
        val m = sampleMoment(
            id = "m1",
            title = "x",
            tags = listOf(Tag.of("alpha")),
            attachments = listOf(sampleAttachment("a1", sortIndex = 0)),
        )
        fx.moments.insert(m, timelineIds = setOf(TimelineId("t1")))
        fx.moments.delete(MomentId("m1"))

        assertNull(fx.moments.findById(MomentId("m1")))
        // memberships purged
        assertEquals(0, fx.moments.listInTimeline(TimelineId("t1")).size)
        // moment_tags purged (via cascade)
        assertTrue(fx.tags.tagsFor(MomentId("m1")).isEmpty())
        // media_attachments purged
        val attachmentCount = fx.database.mediaAttachmentsQueries
            .selectAttachmentsForMoment("m1").executeAsList().size
        assertEquals(0, attachmentCount)
        // tag ROW survives (not auto-cascaded); explicit prune removes it
        val tagRowsBefore = fx.database.tagsQueries.selectAllTags().executeAsList()
        assertEquals(1, tagRowsBefore.size)
        fx.tags.pruneUnused()
        val tagRowsAfter = fx.database.tagsQueries.selectAllTags().executeAsList()
        assertEquals(0, tagRowsAfter.size)
    }

    @Test fun foreignKeyEnforcementBlocksOrphanMembership() = runTest {
        // No custom_timelines row exists, membership insert must fail via FK.
        val ex = assertFailsWith<Exception> {
            fx.database.membershipsQueries.insertMembership(
                moment_id = "ghost-moment",
                timeline_id = "ghost-timeline",
            )
        }
        val msg = (ex.message ?: "").lowercase()
        assertTrue(
            msg.contains("foreign key") || msg.contains("constraint"),
            "expected FK failure, got: ${ex.message}",
        )
    }

    @Test fun insertRollsBackWhenTimelineIdMissing() = runTest {
        val m = sampleMoment(id = "m1", title = "x", tags = listOf(Tag.of("t")))
        assertFailsWith<IllegalArgumentException> {
            fx.moments.insert(m, timelineIds = setOf(TimelineId("does-not-exist")))
        }
        assertNull(fx.moments.findById(MomentId("m1")))
        assertEquals(0, fx.database.tagsQueries.selectAllTags().executeAsList().size)
    }

    @Test fun pruneUnusedKeepsReferencedTagsButDropsOrphans() = runTest {
        fx.moments.insert(sampleMoment(id = "m1", title = "x", tags = listOf(Tag.of("used"))))
        // seed an orphan tag directly (no moment_tags row references it)
        fx.database.tagsQueries.insertTagIfAbsent("orphan", "Orphan")
        assertEquals(
            setOf("used", "orphan"),
            fx.database.tagsQueries.selectAllTags().executeAsList().map { it.canonical }.toSet(),
        )

        fx.tags.pruneUnused()

        val remaining = fx.database.tagsQueries.selectAllTags().executeAsList()
            .map { it.canonical }.toSet()
        assertEquals(setOf("used"), remaining)
    }
}
