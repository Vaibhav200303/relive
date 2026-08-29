package com.vaibhav.relive.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.repository.SqlDelightTimelineRepository
import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentTheme
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.repository.MomentDateNavigationScope
import com.vaibhav.relive.domain.time.Instant
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
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
        assertEquals(1L, fx.database.momentsQueries.countMoments().executeAsOne())
    }

    @Test fun renameAndUpdateAppearance() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("t1", "Old"), Instant(1L))
        fx.timelines.rename(TimelineId("t1"), "New")
        val appearance = TimelineAppearance(momentTheme = MomentTheme.Monochrome)
        fx.timelines.updateAppearance(TimelineId("t1"), appearance)
        val t = fx.timelines.findCustom(TimelineId("t1"))!!
        assertEquals("New", t.name)
        assertEquals(appearance, t.appearance)
    }

    @Test fun appearanceIsStoredPerTimelineAndTimelinesCanDiffer() = runTest {
        val familyAppearance = TimelineAppearance(momentTheme = MomentTheme.WarmTerracotta)
        val travelAppearance = TimelineAppearance(momentTheme = MomentTheme.Ocean)
        fx.timelines.createCustom(sampleCustomTimeline("family", "Family", familyAppearance), Instant(1L))
        fx.timelines.createCustom(sampleCustomTimeline("travel", "Travel", travelAppearance), Instant(2L))

        assertEquals(familyAppearance, fx.timelines.findCustom(TimelineId("family"))?.appearance)
        assertEquals(travelAppearance, fx.timelines.findCustom(TimelineId("travel"))?.appearance)
    }

    @Test fun globalAppearanceChangesDoNotOverwriteTimelineAppearance() = runTest {
        val timelineAppearance = TimelineAppearance(momentTheme = MomentTheme.Lavender)
        fx.timelines.createCustom(sampleCustomTimeline("independent", "Independent", timelineAppearance), Instant(1L))
        var global = AppearancePreferences()

        global = global.copy(mode = AppearanceMode.Dark, defaultTheme = ThemeReference.RoseSage)

        assertEquals(AppearanceMode.Dark, global.mode)
        assertEquals(ThemeReference.RoseSage, global.defaultTheme)
        assertEquals(timelineAppearance, fx.timelines.findCustom(TimelineId("independent"))?.appearance)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun appearanceUpdatesAreReactive() = runTest {
        fx.timelines.createCustom(sampleCustomTimeline("reactive-theme", "Reactive"), Instant(1L))
        val appearances = mutableListOf<TimelineAppearance>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fx.timelines.observeCustom()
                .map { timelines -> timelines.single().appearance }
                .take(3)
                .toList(appearances)
        }

        runCurrent()
        val blue = TimelineAppearance(momentTheme = MomentTheme.Ocean)
        fx.timelines.updateAppearance(TimelineId("reactive-theme"), blue)
        runCurrent()
        val rosewood = TimelineAppearance(momentTheme = MomentTheme.Monochrome)
        fx.timelines.updateAppearance(TimelineId("reactive-theme"), rosewood)
        runCurrent()

        assertEquals(listOf(TimelineAppearance(), blue, rosewood), appearances)
        collectJob.cancel()
    }

    @Test fun existingTimelineReceivesDefaultAppearanceDuringMigration() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(
                identifier = null,
                sql = """
                    CREATE TABLE custom_timelines (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        theme TEXT,
                        cover_photo_ref TEXT,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent(),
                parameters = 0,
            )
            driver.execute(
                identifier = null,
                sql = "INSERT INTO custom_timelines VALUES ('old', 'Old timeline', NULL, NULL, 1)",
                parameters = 0,
            )
            driver.execute(
                identifier = null,
                sql = "INSERT INTO custom_timelines VALUES ('themed', 'Themed timeline', 'BlueHour', NULL, 2)",
                parameters = 0,
            )

            ReliveDatabase.Schema.migrate(driver, oldVersion = 3L, newVersion = 4L)
            val repository = SqlDelightTimelineRepository(
                ReliveDatabaseFactory.create(driver),
                Dispatchers.Unconfined,
            )

            val restored = repository.findCustom(TimelineId("old"))
            assertEquals("Old timeline", restored?.name)
            assertEquals(TimelineAppearance(), restored?.appearance)
            assertEquals(
                TimelineAppearance(momentTheme = MomentTheme.Ocean),
                repository.findCustom(TimelineId("themed"))?.appearance,
            )
        } finally {
            driver.close()
        }
    }

    @Test fun timelineAppearanceSurvivesRepositoryAndDatabaseReload() = runTest {
        val file = File.createTempFile("relive-timeline-appearance", ".db")
        file.delete()
        val url = "jdbc:sqlite:${file.absolutePath}"
        try {
            JdbcSqliteDriver(url).use { driver ->
                ReliveDatabase.Schema.create(driver)
                val repository = SqlDelightTimelineRepository(
                    ReliveDatabaseFactory.create(driver),
                    Dispatchers.Unconfined,
                )
                repository.createCustom(
                    sampleCustomTimeline(
                        "reload",
                        "Reload",
                        TimelineAppearance(
                            wallpaper = TimelineWallpaper.WarmCream,
                            momentTheme = MomentTheme.Rose,
                        ),
                    ),
                    Instant(1L),
                )
            }

            JdbcSqliteDriver(url).use { driver ->
                val repository = SqlDelightTimelineRepository(
                    ReliveDatabaseFactory.create(driver),
                    Dispatchers.Unconfined,
                )
                assertEquals(
                    TimelineAppearance(
                        wallpaper = TimelineWallpaper.WarmCream,
                        momentTheme = MomentTheme.Rose,
                    ),
                    repository.findCustom(TimelineId("reload"))?.appearance,
                )
            }
        } finally {
            file.delete()
        }
    }

    @Test fun dateNavigationSelectsFirstMomentOnRequestedDay() = runTest {
        fx.moments.insert(sampleMoment("later", createdAtMs = 1_500L))
        fx.moments.insert(sampleMoment("first", createdAtMs = 1_000L))

        val target = fx.moments.findDateNavigationTarget(
            MomentDateNavigationScope.All,
            dayStart = Instant(1_000L),
            nextDayStart = Instant(2_000L),
        )

        assertEquals("first", target?.id?.value)
    }

    @Test fun dateNavigationFallsForwardThenBackwardAndScopesCustomTimeline() = runTest {
        val family = TimelineId("family")
        fx.timelines.createCustom(sampleCustomTimeline(family.value), Instant(1L))
        fx.moments.insert(sampleMoment("before", createdAtMs = 1_000L), setOf(family))
        fx.moments.insert(sampleMoment("all-after", createdAtMs = 3_000L))

        val allForward = fx.moments.findDateNavigationTarget(
            MomentDateNavigationScope.All,
            Instant(2_000L),
            Instant(2_500L),
        )
        val familyBackward = fx.moments.findDateNavigationTarget(
            MomentDateNavigationScope.Custom(family),
            Instant(2_000L),
            Instant(2_500L),
        )

        assertEquals("all-after", allForward?.id?.value)
        assertEquals("before", familyBackward?.id?.value)
    }

    @Test fun dateNavigationReturnsNullForEmptyScope() = runTest {
        assertNull(
            fx.moments.findDateNavigationTarget(
                MomentDateNavigationScope.All,
                Instant(1_000L),
                Instant(2_000L),
            ),
        )
    }
}
