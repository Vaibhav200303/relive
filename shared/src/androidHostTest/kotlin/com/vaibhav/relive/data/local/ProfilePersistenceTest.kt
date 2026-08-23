package com.vaibhav.relive.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.data.local.repository.SqlDelightProfileRepository
import com.vaibhav.relive.domain.model.ReliveLocation
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilePersistenceTest {
    private lateinit var fx: TestFixture

    @BeforeTest fun setup() { fx = TestFixture() }
    @AfterTest fun tearDown() { fx.close() }

    @Test fun freshDatabaseCreatesOneStableProfileJoiningDate() = runTest {
        val first = fx.profile.observeProfile().first().createdAt
        val second = fx.profile.observeProfile().first().createdAt

        assertNotNull(first)
        assertEquals(first, second)
    }

    @Test fun freshDatabaseJoiningDateCannotBeMutated() {
        assertFailsWith<Exception> {
            fx.driver.execute(
                identifier = null,
                sql = "UPDATE profile_metadata SET created_at = 0 WHERE singleton = 1",
                parameters = 0,
            )
        }
    }

    @Test fun statisticsUsePersistedMomentsCustomTimelinesAndMeaningfulDistinctPlaces() = runTest {
        fx.moments.insert(sampleMoment(id = "one", location = ReliveLocation(placeName = "Cafe Lune")))
        fx.moments.insert(sampleMoment(id = "two", location = ReliveLocation(placeName = " Cafe Lune ")))
        fx.moments.insert(sampleMoment(id = "three", location = ReliveLocation(locality = "Pune", country = "India")))
        fx.timelines.createCustom(sampleCustomTimeline(), sampleMoment().createdAt)

        val profile = fx.profile.observeProfile().first()

        assertEquals(3, profile.momentCount)
        assertEquals(1, profile.customTimelineCount)
        assertEquals(2, profile.placeCount)
    }

    @Test fun statisticsReactToPersistedMomentChanges() = runTest {
        val observed = async { fx.profile.observeProfile().take(2).toList() }
        runCurrent()

        fx.moments.insert(sampleMoment(id = "reactive", location = ReliveLocation(locality = "Delhi")))

        val snapshots = observed.await()
        assertEquals(0, snapshots.first().momentCount)
        assertEquals(1, snapshots.last().momentCount)
        assertEquals(1, snapshots.last().placeCount)
    }

    @Test fun migratedDatabaseLeavesJoiningDateAbsent() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ReliveDatabase.Schema.create(driver)
        driver.execute(identifier = null, sql = "DROP TABLE profile_metadata", parameters = 0)

        ReliveDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = 2)
        val repository = SqlDelightProfileRepository(
            ReliveDatabaseFactory.create(driver),
            kotlinx.coroutines.Dispatchers.Unconfined,
        )

        assertNull(repository.observeProfile().first().createdAt)
        driver.close()
    }
}
