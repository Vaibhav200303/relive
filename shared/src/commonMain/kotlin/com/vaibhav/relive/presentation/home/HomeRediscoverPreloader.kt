package com.vaibhav.relive.presentation.home

import com.vaibhav.relive.domain.model.AllPhotosCollectionSummary
import com.vaibhav.relive.domain.model.FavoritesCollectionSummary
import com.vaibhav.relive.domain.model.FromYourPastMomentPreview
import com.vaibhav.relive.domain.model.OnThisDayMomentPreview
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Duration
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/** One complete, bounded Rediscover row ready for Home's first visible frame. */
data class HomeRediscoverSnapshot(
    val favorites: FavoritesCollectionSummary,
    val allPhotos: AllPhotosCollectionSummary,
    val onThisDay: List<OnThisDayMomentPreview>,
    val fromYourPast: List<FromYourPastMomentPreview>,
)

/**
 * Loads the four bounded Home projections together. This intentionally does not read the complete
 * archive; it is only a first-frame seed, after which Home's existing reactive collectors take
 * over.
 */
suspend fun preloadHomeRediscover(
    repository: RediscoverRepository,
    clock: Clock,
): HomeRediscoverSnapshot = coroutineScope {
    val now = clock.now()
    val today = RediscoverCalendar.localDate(now)
    val startOfToday = RediscoverCalendar.startOfDay(today)
    val query = RediscoverQuery(
        today = today,
        startOfToday = startOfToday,
        recentCutoff = now - Duration.ofDays(90),
        dailySeed = today.year.toLong() * 10_000L + today.month * 100L + today.day,
    )
    val favorites = async { repository.observeFavoritesSummary().first() }
    val allPhotos = async { repository.observeAllPhotosSummary().first() }
    val onThisDay = async { repository.observeOnThisDayPreviews(today, startOfToday).first() }
    val fromYourPast = async { repository.observeFromYourPastPreviews(query).first() }

    HomeRediscoverSnapshot(
        favorites = favorites.await(),
        allPhotos = allPhotos.await(),
        onThisDay = onThisDay.await(),
        fromYourPast = fromYourPast.await(),
    )
}
