package com.vaibhav.relive.data.local

import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimelinePersistenceTest {
    private lateinit var fx: TestFixture

    @BeforeTest fun setup() { fx = TestFixture() }
    @AfterTest fun tearDown() { fx.close() }

    @Test fun customTimelineCreationAndListing() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t1", "Japan 2026"), Instant(1L))
        fx.timelines.createCustom(sampleCustomTimeline("t2", "Family"), Instant(2L))
        val listed = fx.timelines.listCustom().map { it.id.value }
        assertEquals(listOf("t1", "t2"), listed)
    }

    @Test fun momentMembership() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t1"), Instant(1L))
        fx.moments.insert(
            sampleMoment(id = "m1", title = "x"),
            timelineIds = setOf(TimelineId("t1")),
        )
        val in1 = fx.moments.listInTimeline(TimelineId("t1"))
        assertEquals(1, in1.size)
        assertEquals("m1", in1[0].id.value)
    }

    @Test fun sameMomentInMultipleCustomTimelines() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t1", "Japan 2026"), Instant(1L))
        fx.timelines.createCustom(sampleCustomTimeline("t2", "Travel"), Instant(2L))
        fx.moments.insert(
            sampleMoment(id = "m1", title = "shrine"),
            timelineIds = setOf(TimelineId("t1"), TimelineId("t2")),
        )
        assertEquals(1, fx.moments.listInTimeline(TimelineId("t1")).size)
        assertEquals(1, fx.moments.listInTimeline(TimelineId("t2")).size)
        // moment stored once
        assertEquals(1, fx.moments.listAll().size)
    }

    @Test fun allNotPersistedAsMemberships() = runTest {
        fx.moments.insert(sampleMoment(id = "m1", title = "solo"))
        // No custom timelines exist and no memberships written → All is purely a listAll query.
        assertEquals(1, fx.moments.listAll().size)
        assertTrue(fx.timelines.listCustom().isEmpty())
    }

    @Test fun deletingCustomTimelinePreservesMoments() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t1"), Instant(1L))
        fx.moments.insert(
            sampleMoment(id = "m1", title = "x"),
            timelineIds = setOf(TimelineId("t1")),
        )
        fx.timelines.deleteCustom(TimelineId("t1"))
        assertNull(fx.timelines.findCustom(TimelineId("t1")))
        assertNotNull(fx.moments.findById(MomentId("m1")))
        assertTrue(fx.timelines.timelinesFor(MomentId("m1")).isEmpty())
    }

    @Test fun duplicateMembershipIsIdempotent() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t1"), Instant(1L))
        fx.moments.insert(sampleMoment(id = "m1", title = "x"))
        fx.timelines.addMembership(MomentId("m1"), TimelineId("t1"))
        fx.timelines.addMembership(MomentId("m1"), TimelineId("t1"))
        assertEquals(listOf(TimelineId("t1")), fx.timelines.timelinesFor(MomentId("m1")))
        assertEquals(1, fx.moments.listInTimeline(TimelineId("t1")).size)
    }

    @Test fun renameAndUpdateTheme() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t1", "Old", ThemeReference.WarmJournal), Instant(1L))
        fx.timelines.rename(TimelineId("t1"), "New")
        fx.timelines.updateTheme(TimelineId("t1"), ThemeReference.FilmMemory)
        val t = fx.timelines.findCustom(TimelineId("t1"))!!
        assertEquals("New", t.name)
        assertEquals(ThemeReference.FilmMemory, t.theme)
    }
}
