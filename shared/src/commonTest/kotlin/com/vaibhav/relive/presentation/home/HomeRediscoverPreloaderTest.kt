package com.vaibhav.relive.presentation.home

import com.vaibhav.relive.domain.model.AllPhotosCollectionSummary
import com.vaibhav.relive.domain.model.FavoriteMomentPreview
import com.vaibhav.relive.domain.model.FavoritesCollectionSummary
import com.vaibhav.relive.domain.model.FromYourPastMomentPreview
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.OnThisDayMomentPreview
import com.vaibhav.relive.domain.model.RediscoverOverview
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeRediscoverPreloaderTest {
    @Test
    fun returnsOneCompleteBoundedFirstFrameSnapshot() = runTest {
        val favorites = FavoritesCollectionSummary(momentCount = 2, previewAttachments = emptyList())
        val allPhotos = AllPhotosCollectionSummary(momentCount = 3, previewAttachments = emptyList())
        val repository = FakeRediscoverRepository(favorites, allPhotos)

        val snapshot = preloadHomeRediscover(repository, Clock { Instant(1_700_000_000_000L) })

        assertEquals(favorites, snapshot.favorites)
        assertEquals(allPhotos, snapshot.allPhotos)
        assertEquals(repository.onThisDay, snapshot.onThisDay)
        assertEquals(repository.fromYourPast, snapshot.fromYourPast)
        assertTrue(repository.requestedPastQuery != null)
    }
}

private class FakeRediscoverRepository(
    private val favorites: FavoritesCollectionSummary,
    private val allPhotos: AllPhotosCollectionSummary,
) : RediscoverRepository {
    val onThisDay = emptyList<OnThisDayMomentPreview>()
    val fromYourPast = emptyList<FromYourPastMomentPreview>()
    var requestedPastQuery: RediscoverQuery? = null

    override fun observeFavoritesSummary(): Flow<FavoritesCollectionSummary> = flowOf(favorites)
    override fun observeAllPhotosSummary(): Flow<AllPhotosCollectionSummary> = flowOf(allPhotos)
    override fun observeOnThisDayPreviews(
        today: LocalCalendarDate,
        startOfToday: Instant,
        limit: Int,
    ): Flow<List<OnThisDayMomentPreview>> = flowOf(onThisDay)

    override fun observeFromYourPastPreviews(query: RediscoverQuery): Flow<List<FromYourPastMomentPreview>> {
        requestedPastQuery = query
        return flowOf(fromYourPast)
    }

    override fun observeOverview(query: RediscoverQuery): Flow<RediscoverOverview> = error("Not used")
    override fun observeFavoriteMoments(): Flow<List<Moment>> = error("Not used")
    override fun observeFavoritePreviews(limit: Int): Flow<List<FavoriteMomentPreview>> = error("Not used")
    override fun observeOnThisDayMoments(
        today: LocalCalendarDate,
        startOfToday: Instant,
    ): Flow<List<Moment>> = error("Not used")

    override fun observeFromYourPastMoments(query: RediscoverQuery): Flow<List<Moment>> = error("Not used")
}
