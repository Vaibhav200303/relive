package com.vaibhav.relive.data.local

import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MomentPersistenceTest {
    private lateinit var fx: TestFixture

    @BeforeTest fun setup() { fx = TestFixture() }
    @AfterTest fun tearDown() { fx.close() }

    @Test fun insertAndReadRoundTrip() = runTest {
        val m = sampleMoment(
            id = "m1",
            title = "Kyoto morning",
            content = "Rain on stone",
        )
        fx.moments.insert(m)
        val loaded = fx.moments.findById(MomentId("m1"))
        assertEquals(m, loaded)
    }

    @Test fun textOnlyMoment() = runTest {
        val m = sampleMoment(id = "text-only", title = "Just a title")
        fx.moments.insert(m)
        val loaded = fx.moments.findById(MomentId("text-only"))
        assertNotNull(loaded)
        assertTrue(loaded.attachments.isEmpty())
        assertNull(loaded.location)
    }

    @Test fun mediaOnlyMoment() = runTest {
        val m = sampleMoment(
            id = "media-only",
            attachments = listOf(
                sampleAttachment("a1", MediaType.Image, sortIndex = 0),
            ),
        )
        fx.moments.insert(m)
        val loaded = fx.moments.findById(MomentId("media-only"))
        assertNotNull(loaded)
        assertEquals("", loaded.title)
        assertEquals("", loaded.content)
        assertEquals(1, loaded.attachments.size)
    }

    @Test fun fullLocationRoundTrip() = runTest {
        val loc = ReliveLocation(
            latitude = 34.05,
            longitude = -118.24,
            placeName = "Griffith Observatory",
            locality = "Los Angeles",
            region = "California",
            country = "USA",
        )
        val m = sampleMoment(id = "loc-full", content = "view", location = loc)
        fx.moments.insert(m)
        assertEquals(loc, fx.moments.findById(MomentId("loc-full"))!!.location)
    }

    @Test fun partialHumanReadableLocation() = runTest {
        val loc = ReliveLocation(locality = "Osaka", country = "Japan")
        val m = sampleMoment(id = "loc-partial", content = "dinner", location = loc)
        fx.moments.insert(m)
        val loaded = fx.moments.findById(MomentId("loc-partial"))!!
        val loadedLoc = loaded.location
        assertEquals(loc, loadedLoc)
        assertNull(loadedLoc?.latitude)
        assertNull(loadedLoc?.longitude)
    }

    @Test fun tagsRoundTrip() = runTest {
        val m = sampleMoment(
            id = "with-tags",
            content = "picnic",
            tags = listOf(Tag.of("Family"), Tag.of("Outdoors")),
        )
        fx.moments.insert(m)
        val loaded = fx.moments.findById(MomentId("with-tags"))!!
        assertEquals(setOf(Tag.of("Family"), Tag.of("Outdoors")), loaded.tags.toSet())
    }

    @Test fun tagCanonicalDeduplication() = runTest {
        val m1 = sampleMoment(
            id = "t1",
            title = "one",
            tags = listOf(Tag.of("Travel")),
        )
        val m2 = sampleMoment(
            id = "t2",
            title = "two",
            tags = listOf(Tag.of("  travel ")),
        )
        fx.moments.insert(m1)
        fx.moments.insert(m2)
        val distinct = fx.tags.distinctAll()
        assertEquals(1, distinct.size)
        assertEquals("travel", distinct[0].canonical)
    }

    @Test fun orderedAttachments() = runTest {
        val a0 = sampleAttachment("a0", sortIndex = 0)
        val a1 = sampleAttachment("a1", MediaType.Video, sortIndex = 1)
        val a2 = sampleAttachment("a2", MediaType.Audio, sortIndex = 2)
        val m = sampleMoment(id = "ordered", title = "carousel", attachments = listOf(a2, a0, a1))
        fx.moments.insert(m)
        val loaded = fx.moments.findById(MomentId("ordered"))!!
        assertEquals(listOf(0, 1, 2), loaded.attachments.map { it.sortIndex })
        assertEquals(listOf("a0", "a1", "a2"), loaded.attachments.map { it.id.value })
    }

    @Test fun favoriteUpdate() = runTest {
        val m = sampleMoment(id = "fav", title = "x")
        fx.moments.insert(m)
        fx.moments.setFavorite(MomentId("fav"), true)
        assertTrue(fx.moments.findById(MomentId("fav"))!!.isFavorite)
        fx.moments.setFavorite(MomentId("fav"), false)
        assertTrue(!fx.moments.findById(MomentId("fav"))!!.isFavorite)
    }

    @Test fun editableUpdatePreservesCreatedAt() = runTest {
        val original = sampleMoment(id = "e1", createdAtMs = 100L, title = "orig")
        fx.moments.insert(original)
        val edited = original.copy(
            title = "new",
            content = "expanded",
            updatedAt = Instant(500L),
        )
        fx.moments.updateEditable(edited)
        val loaded = fx.moments.findById(MomentId("e1"))!!
        assertEquals(Instant(100L), loaded.createdAt)
        assertEquals("new", loaded.title)
        assertEquals("expanded", loaded.content)
        assertEquals(Instant(500L), loaded.updatedAt)
    }

    @Test fun updateEditableRejectsMutatedCreatedAt() = runTest {
        fx.moments.insert(sampleMoment(id = "e2", createdAtMs = 200L, title = "x"))
        val mutated = sampleMoment(id = "e2", createdAtMs = 999L, title = "y")
        assertFailsWith<IllegalStateException> { fx.moments.updateEditable(mutated) }
        assertEquals(Instant(200L), fx.moments.findById(MomentId("e2"))!!.createdAt)
    }

    @Test fun editingMomentPreservesCustomTimelineMembershipsWithoutDuplication() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("family", "Family"), Instant(1L))
        fx.timelines.createCustom(sampleCustomTimeline("travel", "Travel"), Instant(1L))
        val original = sampleMoment(id = "shared", createdAtMs = 1L, title = "Before", isFavorite = true)
        fx.moments.insert(original, setOf(TimelineId("family"), TimelineId("travel")))

        fx.moments.updateEditable(original.copy(title = "After", updatedAt = Instant(2L)))

        assertEquals(listOf(MomentId("shared")), fx.moments.listAll().map { it.id })
        assertEquals(listOf(MomentId("shared")), fx.moments.listInTimeline(TimelineId("family")).map { it.id })
        assertEquals(listOf(MomentId("shared")), fx.moments.listInTimeline(TimelineId("travel")).map { it.id })
        assertEquals(1L, fx.database.membershipsQueries.countMembership("shared", "family").executeAsOne())
        assertEquals(1L, fx.database.membershipsQueries.countMembership("shared", "travel").executeAsOne())
        assertEquals("After", fx.moments.findById(MomentId("shared"))!!.title)
    }

    @Test fun updatedAtRemainsNullWhenNotSet() = runTest {
        fx.moments.insert(sampleMoment(id = "u1", title = "x"))
        assertNull(fx.moments.findById(MomentId("u1"))!!.updatedAt)
    }

    @Test fun chronologicalOrderingNewestFirst() = runTest {
        fx.moments.insert(sampleMoment(id = "a", createdAtMs = 1_000L, title = "a"))
        fx.moments.insert(sampleMoment(id = "b", createdAtMs = 3_000L, title = "b"))
        fx.moments.insert(sampleMoment(id = "c", createdAtMs = 2_000L, title = "c"))
        val ids = fx.moments.listAll().map { it.id.value }
        assertEquals(listOf("b", "c", "a"), ids)
    }

    @Test fun searchMatchesTitleAndContentCaseInsensitivelyInChronologicalRepositoryOrder() = runTest {
        fx.moments.insert(sampleMoment(id = "old", createdAtMs = 1L, title = "Keys"))
        fx.moments.insert(sampleMoment(id = "new", createdAtMs = 2L, content = "Lost my KEYS today"))
        fx.moments.insert(sampleMoment(id = "other", createdAtMs = 3L, title = "Notebook"))

        assertEquals(
            listOf("new", "old"),
            fx.moments.observeSearch("keys").first().map { it.id.value },
        )
    }

    @Test fun searchWithNoMatchReturnsNoMoments() = runTest {
        fx.moments.insert(sampleMoment(id = "one", title = "Morning"))

        assertTrue(fx.moments.observeSearch("night").first().isEmpty())
    }
}
