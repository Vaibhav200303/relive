package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.FavoritesCollectionSummary
import com.vaibhav.relive.domain.model.FavoriteMomentPreview
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.RediscoverOverview
import com.vaibhav.relive.domain.model.RediscoverQuery
import kotlinx.coroutines.flow.Flow

/** Read-only, bounded local projections used by the Rediscover destination. */
interface RediscoverRepository {
    fun observeOverview(query: RediscoverQuery): Flow<RediscoverOverview>
    fun observeFavoritesSummary(): Flow<FavoritesCollectionSummary>
    fun observeFavoriteMoments(): Flow<List<Moment>>
    fun observeFavoritePreviews(limit: Int = MAX_FAVORITE_PREVIEWS): Flow<List<FavoriteMomentPreview>>

    companion object {
        const val MAX_FAVORITE_PREVIEWS = 10
    }
}
