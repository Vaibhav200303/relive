package com.vaibhav.relive.presentation.rediscover

internal enum class RediscoverSectionSpacing {
    Normal,
    Expanded,
}

internal data class RediscoverSectionLayout(
    val showFavorites: Boolean,
    val showFavoritesEmptyState: Boolean,
    val showOnThisDay: Boolean,
    val showFromYourPast: Boolean,
    val fromYourPastSpacing: RediscoverSectionSpacing,
)

internal fun rediscoverSectionLayout(
    favoriteMomentCount: Int,
    onThisDayMomentCount: Int,
    fromYourPastMomentCount: Int,
    showFavorites: Boolean = true,
    showOnThisDay: Boolean = true,
): RediscoverSectionLayout {
    require(favoriteMomentCount >= 0) { "favoriteMomentCount must not be negative" }
    require(onThisDayMomentCount >= 0) { "onThisDayMomentCount must not be negative" }
    require(fromYourPastMomentCount >= 0) { "fromYourPastMomentCount must not be negative" }
    val shouldShowOnThisDay = showOnThisDay && onThisDayMomentCount > 0
    return RediscoverSectionLayout(
        showFavorites = showFavorites,
        showFavoritesEmptyState = showFavorites && favoriteMomentCount == 0,
        showOnThisDay = shouldShowOnThisDay,
        showFromYourPast = fromYourPastMomentCount > 0,
        fromYourPastSpacing = if (shouldShowOnThisDay) {
            RediscoverSectionSpacing.Expanded
        } else {
            RediscoverSectionSpacing.Normal
        },
    )
}
