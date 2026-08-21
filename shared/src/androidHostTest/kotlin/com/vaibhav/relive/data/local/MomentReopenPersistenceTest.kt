package com.vaibhav.relive.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.domain.model.MomentId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Regression: a manually created Moment must survive the SQLDelight driver being
 * torn down and reopened over the same on-disk database file. Simulates the real
 * Android lifecycle where the process is killed (Recents swipe / debug reinstall)
 * and the app is relaunched against the same persistent `relive.db`.
 */
class MomentReopenPersistenceTest {
    private lateinit var dbFile: File

    @BeforeTest fun setup() {
        dbFile = Files.createTempFile("relive-reopen-", ".db").toFile().also { it.delete() }
    }

    @AfterTest fun tearDown() {
        dbFile.delete()
    }

    @Test fun momentSurvivesDriverReopen() = runTest {
        val inserted = sampleMoment(
            id = "user-created-1",
            title = "Kyoto morning",
            content = "Rain on stone",
        )

        openDriver(create = true).use { driver ->
            val db = ReliveDatabaseFactory.create(driver)
            SqlDelightMomentRepository(db, Dispatchers.Unconfined).insert(inserted)
        }

        openDriver(create = false).use { driver ->
            val db = ReliveDatabaseFactory.create(driver)
            val loaded = SqlDelightMomentRepository(db, Dispatchers.Unconfined)
                .findById(MomentId("user-created-1"))
            assertNotNull(loaded, "moment lost after driver reopen")
            assertEquals(inserted, loaded)
        }
    }

    @Test fun schemaCreationDoesNotWipeExistingData() = runTest {
        openDriver(create = true).use { driver ->
            val db = ReliveDatabaseFactory.create(driver)
            SqlDelightMomentRepository(db, Dispatchers.Unconfined)
                .insert(sampleMoment(id = "persist-me", title = "persist"))
        }

        // Second open: schema already present; do NOT call Schema.create again.
        // If startup ever wiped/recreated the DB, this row would vanish.
        openDriver(create = false).use { driver ->
            val db = ReliveDatabaseFactory.create(driver)
            val all = SqlDelightMomentRepository(db, Dispatchers.Unconfined).listAll()
            assertEquals(1, all.size)
            assertEquals("persist-me", all.single().id.value)
        }
    }

    private fun openDriver(create: Boolean): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (create) ReliveDatabase.Schema.create(driver)
        return driver
    }
}
