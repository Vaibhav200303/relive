package com.vaibhav.relive.data.local

import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test fun observeCustomIncludesPersistedTimelinesInCreationOrder() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t2", "Family"), Instant(2L))
        fx.timelines.createCustom(sampleCustomTimeline("t1", "Japan 2026"), Instant(1L))

        val observed = fx.timelines.observeCustom().first()

        assertEquals(listOf("t1", "t2"), observed.map { it.id.value })
        assertEquals(listOf("Japan 2026", "Family"), observed.map { it.name })
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

    @Test fun noAssignmentStoresOneMomentWithoutCustomMemberships() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t1", "Family"), Instant(1L))

        fx.moments.insert(sampleMoment(id = "m1", title = "All only"))

        assertEquals(listOf("m1"), fx.moments.listAll().map { it.id.value })
        assertTrue(fx.moments.listInTimeline(TimelineId("t1")).isEmpty())
        assertTrue(fx.timelines.timelinesFor(MomentId("m1")).isEmpty())
        assertEquals(1L, fx.database.momentsQueries.countMoments().executeAsOne())
    }

    @Test fun oneAssignmentStoresOneMomentAndOneMembership() = runTest {
        val timelineId = TimelineId("t1")
        fx.timelines.createCustom(sampleCustomTimeline(timelineId.value, "Family"), Instant(1L))

        fx.moments.insert(
            sampleMoment(id = "m1", title = "Family moment"),
            timelineIds = setOf(timelineId),
        )

        assertEquals(1L, fx.database.momentsQueries.countMoments().executeAsOne())
        assertEquals(
            1L,
            fx.database.membershipsQueries.countMembership("m1", "t1").executeAsOne(),
        )
        assertEquals(listOf(timelineId), fx.timelines.timelinesFor(MomentId("m1")))
    }

    @Test fun multipleAssignmentsDoNotDuplicateListOrObservedMoments() = runTest {
        val familyId = TimelineId("family")
        val travelId = TimelineId("travel")
        fx.timelines.createCustom(sampleCustomTimeline(familyId.value, "Family"), Instant(1L))
        fx.timelines.createCustom(sampleCustomTimeline(travelId.value, "Travel"), Instant(2L))

        fx.moments.insert(
            sampleMoment(id = "m1", title = "Shared moment"),
            timelineIds = setOf(familyId, travelId),
        )

        assertEquals(1L, fx.database.momentsQueries.countMoments().executeAsOne())
        assertEquals(listOf("m1"), fx.moments.listAll().map { it.id.value })
        assertEquals(listOf("m1"), fx.moments.observeAll().first().map { it.id.value })
        assertEquals(listOf("m1"), fx.moments.listInTimeline(familyId).map { it.id.value })
        assertEquals(listOf("m1"), fx.moments.observeInTimeline(familyId).first().map { it.id.value })
        assertEquals(listOf("m1"), fx.moments.listInTimeline(travelId).map { it.id.value })
        assertEquals(listOf("m1"), fx.moments.observeInTimeline(travelId).first().map { it.id.value })
    }

    @Test fun customTimelineUsesRepositoryChronologyWithoutDuplicates() = runTest {
        val timelineId = TimelineId("t1")
        fx.timelines.createCustom(sampleCustomTimeline(timelineId.value), Instant(1L))
        fx.moments.insert(
            sampleMoment("old", createdAtMs = 1_000L, title = "old"),
            setOf(timelineId),
        )
        fx.moments.insert(
            sampleMoment("new", createdAtMs = 3_000L, title = "new"),
            setOf(timelineId),
        )
        fx.moments.insert(
            sampleMoment("middle", createdAtMs = 2_000L, title = "middle"),
            setOf(timelineId),
        )

        val expected = listOf("new", "middle", "old")
        assertEquals(expected, fx.moments.listInTimeline(timelineId).map { it.id.value })
        assertEquals(expected, fx.moments.observeInTimeline(timelineId).first().map { it.id.value })
    }

    @Test fun membershipPreservesTagsMediaAndLocation() = runTest {
        val familyId = TimelineId("family")
        val travelId = TimelineId("travel")
        fx.timelines.createCustom(sampleCustomTimeline(familyId.value, "Family"), Instant(1L))
        fx.timelines.createCustom(sampleCustomTimeline(travelId.value, "Travel"), Instant(2L))
        val moment = sampleMoment(
            id = "m1",
            title = "Kyoto morning",
            location = ReliveLocation(locality = "Kyoto", country = "Japan"),
            tags = listOf(Tag.of("Travel")),
            attachments = listOf(sampleAttachment("photo", MediaType.Image, sortIndex = 0)),
        )

        fx.moments.insert(moment, setOf(familyId, travelId))

        assertEquals(moment, fx.moments.listAll().single())
        assertEquals(moment, fx.moments.listInTimeline(familyId).single())
        assertEquals(moment, fx.moments.listInTimeline(travelId).single())
    }

    @Test fun invalidMembershipRollsBackMomentAndEarlierMemberships() = runTest {
        val validId = TimelineId("valid")
        fx.timelines.createCustom(sampleCustomTimeline(validId.value), Instant(1L))

        assertFailsWith<IllegalArgumentException> {
            fx.moments.insert(
                sampleMoment(id = "m1", title = "must roll back"),
                timelineIds = linkedSetOf(validId, TimelineId("missing")),
            )
        }

        assertEquals(0L, fx.database.momentsQueries.countMoments().executeAsOne())
        assertEquals(
            0L,
            fx.database.membershipsQueries.countMembership("m1", "valid").executeAsOne(),
        )
        assertTrue(fx.moments.listInTimeline(validId).isEmpty())
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
