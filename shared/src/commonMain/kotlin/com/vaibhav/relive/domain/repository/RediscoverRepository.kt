package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.AllPhotosCollectionSummary
import com.vaibhav.relive.domain.model.FavoritesCollectionSummary
import com.vaibhav.relive.domain.model.FavoriteMomentPreview
import com.vaibhav.relive.domain.model.FromYourPastMomentPreview
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.OnThisDayMomentPreview
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.domain.model.RediscoverOverview
import com.vaibhav.relive.domain.model.RediscoverQuery
import kotlinx.coroutines.flow.Flow

/** Read-only, bounded local projections used by the Rediscover destination. */
interface RediscoverRepository {
    fun observeOverview(query: RediscoverQuery): Flow<RediscoverOverview>
    fun observeFavoritesSummary(): Flow<FavoritesCollectionSummary>
    fun observeFavoriteMoments(): Flow<List<Moment>>

    /** Cover data for the `All Photos` card in Home's Rediscover row (ADR-0061). */
    fun observeAllPhotosSummary(): Flow<AllPhotosCollectionSummary> =
        throw UnsupportedOperationException("All Photos summary is not implemented by this repository")

    /** Every Moment carrying at least one image or video, newest first. Read-only. */
    fun observeAllPhotosMoments(): Flow<List<Moment>> =
        throw UnsupportedOperationException("All Photos moments are not implemented by this repository")
    fun observeFavoritePreviews(limit: Int = MAX_FAVORITE_PREVIEWS): Flow<List<FavoriteMomentPreview>>
    fun observeOnThisDayPreviews(
        today: LocalCalendarDate,
        startOfToday: Instant,
        limit: Int = MAX_ON_THIS_DAY_PREVIEWS,
    ): Flow<List<OnThisDayMomentPreview>>
    fun observeOnThisDayMoments(today: LocalCalendarDate, startOfToday: Instant): Flow<List<Moment>>
    fun observeFromYourPastPreviews(query: RediscoverQuery): Flow<List<FromYourPastMomentPreview>>
    fun observeFromYourPastMoments(query: RediscoverQuery): Flow<List<Moment>>

    companion object {
        const val MAX_FAVORITE_PREVIEWS = 10
        const val MAX_ON_THIS_DAY_PREVIEWS = 10
        const val MAX_FROM_YOUR_PAST_PREVIEWS = 10
    }
}
