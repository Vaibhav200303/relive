package com.vaibhav.relive.presentation.rediscover

internal enum class RediscoverSectionSpacing {
    Normal,
    Expanded,
}

internal data class RediscoverSectionLayout(
    val showOnThisDay: Boolean,
    val fromYourPastSpacing: RediscoverSectionSpacing,
)

internal fun rediscoverSectionLayout(onThisDayMomentCount: Int): RediscoverSectionLayout {
    require(onThisDayMomentCount >= 0) { "onThisDayMomentCount must not be negative" }
    val showOnThisDay = onThisDayMomentCount > 0
    return RediscoverSectionLayout(
        showOnThisDay = showOnThisDay,
        fromYourPastSpacing = if (showOnThisDay) {
            RediscoverSectionSpacing.Expanded
        } else {
            RediscoverSectionSpacing.Normal
        },
    )
}
