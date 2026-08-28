package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.TimelineId

/** An editable timeline scope with its own saved appearance. */
sealed interface TimelineThemeDestination {
    data object All : TimelineThemeDestination

    data class Custom(val timelineId: TimelineId) : TimelineThemeDestination
}

fun CurrentTimeline.timelineThemeDestinationOrNull(): TimelineThemeDestination? =
    when (this) {
        CurrentTimeline.All -> TimelineThemeDestination.All
        is CurrentTimeline.Custom -> TimelineThemeDestination.Custom(id)
        else -> null
    }
