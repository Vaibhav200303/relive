package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.TimelineId

/** A custom timeline is the only timeline scope with its own saved appearance. */
data class TimelineThemeDestination(val timelineId: TimelineId)

fun CurrentTimeline.timelineThemeDestinationOrNull(): TimelineThemeDestination? =
    (this as? CurrentTimeline.Custom)?.let { TimelineThemeDestination(it.id) }
