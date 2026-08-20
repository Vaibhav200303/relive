package com.vaibhav.relive.data.local

import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TagPersistenceTest {
    private lateinit var fx: TestFixture

    @BeforeTest fun setup() { fx = TestFixture() }
    @AfterTest fun tearDown() { fx.close() }

    @Test fun firstPersistedLabelWinsAcrossMoments() = runTest {
        fx.moments.insert(sampleMoment(id = "m1", title = "a", tags = listOf(Tag.of("Travel"))))
        fx.moments.insert(sampleMoment(id = "m2", title = "b", tags = listOf(Tag.of("travel"))))
        fx.moments.insert(sampleMoment(id = "m3", title = "c", tags = listOf(Tag.of("TRAVEL"))))

        val rows = fx.database.tagsQueries.selectAllTags().executeAsList()
        assertEquals(1, rows.size)
        assertEquals("travel", rows[0].canonical)
        assertEquals("Travel", rows[0].label)
    }

    @Test fun equivalentCanonicalFormsDeduplicate() = runTest {
        fx.moments.insert(sampleMoment(id = "m1", title = "a", tags = listOf(Tag.of("Travel"))))
        fx.moments.insert(sampleMoment(id = "m2", title = "b", tags = listOf(Tag.of("  TRAVEL "))))

        val distinct = fx.tags.distinctAll()
        assertEquals(1, distinct.size)
        assertEquals("travel", distinct[0].canonical)
        assertEquals("Travel", distinct[0].label)
    }

    @Test fun attachingEquivalentTagToAnotherMomentDoesNotMutateDisplayLabel() = runTest {
        fx.moments.insert(sampleMoment(id = "m1", title = "a", tags = listOf(Tag.of("Travel"))))
        fx.moments.insert(sampleMoment(id = "m2", title = "b"))
        fx.tags.attach(MomentId("m2"), Tag.of("TRAVEL"))

        val row = fx.database.tagsQueries.selectTagByCanonical("travel").executeAsOne()
        assertEquals("Travel", row.label)
    }

    @Test fun bothMomentsReferenceSameCanonicalTag() = runTest {
        fx.moments.insert(sampleMoment(id = "m1", title = "a", tags = listOf(Tag.of("Travel"))))
        fx.moments.insert(sampleMoment(id = "m2", title = "b", tags = listOf(Tag.of("travel"))))

        val t1 = fx.tags.tagsFor(MomentId("m1")).single()
        val t2 = fx.tags.tagsFor(MomentId("m2")).single()
        assertEquals("travel", t1.canonical)
        assertEquals("travel", t2.canonical)
        assertEquals(t1.canonical, t2.canonical)
        assertEquals("Travel", t1.label)
        assertEquals("Travel", t2.label)
    }

    @Test fun firstLabelPreservedWhenReattachedViaAttach() = runTest {
        fx.moments.insert(sampleMoment(id = "m1", title = "a"))
        fx.tags.attach(MomentId("m1"), Tag.of("Travel"))
        fx.tags.attach(MomentId("m1"), Tag.of("travel"))
        fx.tags.attach(MomentId("m1"), Tag.of("TRAVEL"))

        val rows = fx.database.tagsQueries.selectAllTags().executeAsList()
        assertEquals(1, rows.size)
        assertEquals("Travel", rows[0].label)
    }
}
