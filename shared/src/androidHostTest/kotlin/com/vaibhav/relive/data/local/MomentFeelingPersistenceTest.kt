package com.vaibhav.relive.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Feeling persistence and its bounded insights projection (PRODUCT_SPEC §10A, ADR-0066). */
class MomentFeelingPersistenceTest {

    private val fixture = TestFixture()

    @AfterTest
    fun tearDown() = fixture.close()

    @Test
    fun `a moment saves and reloads without a feeling`() = runTest {
        fixture.moments.insert(sampleMoment(id = "m1", title = "Unfelt"))

        assertNull(fixture.moments.findById(MomentId("m1"))?.feeling)
    }

    @Test
    fun `an inserted feeling round trips`() = runTest {
        MomentFeeling.entries.forEachIndexed { index, feeling ->
            val id = "m-$index"
            fixture.moments.insert(sampleMoment(id = id, title = "Felt").copy(feeling = feeling))
            assertEquals(feeling, fixture.moments.findById(MomentId(id))?.feeling)
        }
    }

    @Test
    fun `setFeeling writes and clears without touching the rest of the moment`() = runTest {
        val original = sampleMoment(
            id = "m1",
            title = "Evening at the ghat",
            content = "The lamps came out one by one.",
            isFavorite = true,
        )
        fixture.moments.insert(original)

        fixture.moments.setFeeling(MomentId("m1"), MomentFeeling.Great)
        val felt = fixture.moments.findById(MomentId("m1"))
        assertEquals(MomentFeeling.Great, felt?.feeling)
        assertEquals(original.title, felt?.title)
        assertEquals(original.content, felt?.content)
        assertEquals(original.createdAt, felt?.createdAt)
        assertEquals(true, felt?.isFavorite)
        // The feeling write is not an edit, so it must not stamp updatedAt.
        assertNull(felt?.updatedAt)

        fixture.moments.setFeeling(MomentId("m1"), null)
        assertNull(fixture.moments.findById(MomentId("m1"))?.feeling)
    }

    @Test
    fun `updateEditable preserves an existing feeling`() = runTest {
        fixture.moments.insert(sampleMoment(id = "m1", title = "Before"))
        fixture.moments.setFeeling(MomentId("m1"), MomentFeeling.Good)

        val stored = fixture.moments.findById(MomentId("m1"))!!
        fixture.moments.updateEditable(
            stored.copy(title = "After", updatedAt = Instant(stored.createdAt.epochMilliseconds + 1)),
        )

        val edited = fixture.moments.findById(MomentId("m1"))
        assertEquals("After", edited?.title)
        assertEquals(MomentFeeling.Good, edited?.feeling, "editing must not clear the feeling")
    }

    @Test
    fun `the sample projection is bounded by the cutoff and carries only the two scalars`() = runTest {
        fixture.moments.insert(
            sampleMoment(id = "old", createdAtMs = 1_000L, title = "Old").copy(feeling = MomentFeeling.Great),
        )
        fixture.moments.insert(
            sampleMoment(id = "recent", createdAtMs = 5_000L, title = "Recent").copy(feeling = MomentFeeling.Low),
        )
        fixture.moments.insert(sampleMoment(id = "unfelt", createdAtMs = 6_000L, title = "Unfelt"))

        val samples = fixture.moments.observeFeelingSamplesSince(Instant(5_000L)).first()

        assertEquals(2, samples.size, "the pre-cutoff moment is excluded")
        assertEquals(listOf(5_000L, 6_000L), samples.map { it.createdAt.epochMilliseconds })
        assertEquals(listOf(MomentFeeling.Low, null), samples.map { it.feeling })
    }

    @Test
    fun `an unfelt archive still projects its moments`() = runTest {
        fixture.moments.insert(sampleMoment(id = "m1", createdAtMs = 2_000L, title = "Unfelt"))

        val samples = fixture.moments.observeFeelingSamplesSince(Instant(0L)).first()

        assertEquals(1, samples.size)
        assertNull(samples.single().feeling)
    }
}

/** Migration coverage: an older database gains the column and keeps every Moment unfelt. */
class MomentFeelingMigrationTest {

    @Test
    fun `migrating a pre-feeling database leaves existing moments unfelt`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            // Build the schema as it stood before this feature, then seed a Moment into it.
            ReliveDatabase.Schema.create(driver)
            driver.execute(
                identifier = null,
                sql = "ALTER TABLE moments DROP COLUMN feeling",
                parameters = 0,
            )
            driver.execute(
                identifier = null,
                sql = "INSERT INTO moments (id, created_at, title, content, is_favorite) " +
                    "VALUES ('legacy', 1000, 'Legacy', 'Saved before feelings existed', 0)",
                parameters = 0,
            )

            // The migration must add the column without disturbing the row.
            driver.execute(identifier = null, sql = "ALTER TABLE moments ADD COLUMN feeling TEXT", parameters = 0)

            val database = ReliveDatabaseFactory.create(driver)
            val repository = SqlDelightMomentRepository(database, Dispatchers.Unconfined)
            val migrated = repository.findById(MomentId("legacy"))

            assertTrue(migrated != null, "the legacy moment survived the migration")
            assertEquals("Legacy", migrated.title)
            assertNull(migrated.feeling, "existing moments are never backfilled")

            // And the new write path works on the migrated database.
            repository.setFeeling(MomentId("legacy"), MomentFeeling.Good)
            assertEquals(MomentFeeling.Good, repository.findById(MomentId("legacy"))?.feeling)
        } finally {
            driver.close()
        }
    }
}
