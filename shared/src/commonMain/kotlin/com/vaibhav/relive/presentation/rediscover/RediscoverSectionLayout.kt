package com.vaibhav.relive.presentation.rediscover

internal enum class RediscoverSectionSpacing {
    Normal,
    Expanded,
}

internal data class RediscoverSectionLayout(
    val showFavorites: Boolean,
    val showOnThisDay: Boolean,
    val fromYourPastSpacing: RediscoverSectionSpacing,
)

internal fun rediscoverSectionLayout(
    onThisDayMomentCount: Int,
    showFavorites: Boolean = true,
    showOnThisDay: Boolean = true,
): RediscoverSectionLayout {
    require(onThisDayMomentCount >= 0) { "onThisDayMomentCount must not be negative" }
    val shouldShowOnThisDay = showOnThisDay && onThisDayMomentCount > 0
    return RediscoverSectionLayout(
        showFavorites = showFavorites,
        showOnThisDay = shouldShowOnThisDay,
        fromYourPastSpacing = if (shouldShowOnThisDay) {
            RediscoverSectionSpacing.Expanded
        } else {
            RediscoverSectionSpacing.Normal
        },
    )
}
