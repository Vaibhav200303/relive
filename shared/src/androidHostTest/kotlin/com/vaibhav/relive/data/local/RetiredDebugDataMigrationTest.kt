package com.vaibhav.relive.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.repository.SqlDelightMomentRepository
import com.vaibhav.relive.domain.model.MomentId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RetiredDebugDataMigrationTest {

    @Test
    fun `migration removes retired debug records and preserves real moments`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            ReliveDatabase.Schema.create(driver)
            insertMoment(driver, id = "relive-debug-rediscover-qa-photo-v1", title = "Retired")
            insertMoment(driver, id = "debug-1", title = "Retired sample")
            insertMoment(driver, id = "real-memory", title = "Keep me")

            ReliveDatabase.Schema.migrate(driver, oldVersion = 5L, newVersion = 6L)

            val repository = SqlDelightMomentRepository(
                ReliveDatabaseFactory.create(driver),
                Dispatchers.Unconfined,
            )
            assertNull(repository.findById(MomentId("relive-debug-rediscover-qa-photo-v1")))
            assertNull(repository.findById(MomentId("debug-1")))
            assertNotNull(repository.findById(MomentId("real-memory")))
        } finally {
            driver.close()
        }
    }

    private fun insertMoment(driver: JdbcSqliteDriver, id: String, title: String) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO moments (id, created_at, title, content, is_favorite) " +
                "VALUES (?, 1000, ?, '', 0)",
            parameters = 2,
        ) {
            bindString(0, id)
            bindString(1, title)
        }
    }
}
