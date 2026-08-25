package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.BehaviorPreferences

data class TimelineMomentVisibility(
    val showLocations: Boolean,
    val showTags: Boolean,
)

fun resolveTimelineMomentVisibility(
    mode: TimelineMode,
    preferences: BehaviorPreferences,
): TimelineMomentVisibility = if (mode.allowsMutations) {
    TimelineMomentVisibility(
        showLocations = preferences.showLocations,
        showTags = preferences.showTags,
    )
} else {
    TimelineMomentVisibility(showLocations = true, showTags = true)
}
