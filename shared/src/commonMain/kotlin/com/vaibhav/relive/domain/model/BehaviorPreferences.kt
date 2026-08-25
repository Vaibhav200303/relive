package com.vaibhav.relive.domain.model

enum class StartDestination {
    Timelines,
    Rediscover,
}

data class BehaviorPreferences(
    val startDestination: StartDestination = StartDestination.Timelines,
    val confirmBeforeDiscarding: Boolean = true,
    val showLocations: Boolean = true,
    val showTags: Boolean = true,
    val showOnThisDay: Boolean = true,
    val showFavorites: Boolean = true,
)
