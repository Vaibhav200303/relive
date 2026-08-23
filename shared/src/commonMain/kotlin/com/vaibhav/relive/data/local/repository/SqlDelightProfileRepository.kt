package com.vaibhav.relive.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.coroutines.mapToOne
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.domain.model.ProfileSnapshot
import com.vaibhav.relive.domain.repository.ProfileRepository
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SqlDelightProfileRepository(
    private val database: ReliveDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ProfileRepository {
    override fun observeProfile(): Flow<ProfileSnapshot> = combine(
        database.profileQueries.selectProfileCreatedAt().asFlow().mapToOneOrNull(dispatcher),
        database.profileQueries.selectProfileStatistics().asFlow().mapToOne(dispatcher),
    ) { createdAt, statistics ->
        ProfileSnapshot(
            createdAt = createdAt?.let(::Instant),
            momentCount = statistics.moment_count,
            customTimelineCount = statistics.timeline_count,
            placeCount = statistics.place_count,
        )
    }
}
