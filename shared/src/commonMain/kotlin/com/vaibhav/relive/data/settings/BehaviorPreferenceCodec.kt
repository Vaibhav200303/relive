package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.model.StartDestination

internal const val START_DESTINATION_KEY: String = "relive.behavior.start_destination"
internal const val CONFIRM_BEFORE_DISCARDING_KEY: String = "relive.behavior.confirm_before_discarding"
internal const val SHOW_LOCATIONS_KEY: String = "relive.behavior.show_locations"
internal const val SHOW_TAGS_KEY: String = "relive.behavior.show_tags"
internal const val SHOW_ON_THIS_DAY_KEY: String = "relive.behavior.show_on_this_day"
internal const val SHOW_FAVORITES_KEY: String = "relive.behavior.show_favorites"

internal fun StartDestination.encodePreference(): String = when (this) {
    StartDestination.Timelines -> "timelines"
    StartDestination.Rediscover -> "rediscover"
}

internal fun Boolean.encodeBehaviorPreference(): String = toString()

internal fun decodeBehaviorPreferences(
    startDestination: String?,
    confirmBeforeDiscarding: String?,
    showLocations: String?,
    showTags: String?,
    showOnThisDay: String?,
    showFavorites: String?,
): BehaviorPreferences = BehaviorPreferences(
    startDestination = when (startDestination) {
        "rediscover" -> StartDestination.Rediscover
        else -> StartDestination.Timelines
    },
    confirmBeforeDiscarding = confirmBeforeDiscarding.decodeBooleanPreference(default = true),
    showLocations = showLocations.decodeBooleanPreference(default = true),
    showTags = showTags.decodeBooleanPreference(default = true),
    showOnThisDay = showOnThisDay.decodeBooleanPreference(default = true),
    showFavorites = showFavorites.decodeBooleanPreference(default = true),
)

private fun String?.decodeBooleanPreference(default: Boolean): Boolean = when (this) {
    "true" -> true
    "false" -> false
    else -> default
}
