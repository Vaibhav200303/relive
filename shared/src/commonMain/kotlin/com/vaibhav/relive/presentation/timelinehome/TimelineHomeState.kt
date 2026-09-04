package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary

/** One-shot destination information emitted by Timeline Home navigation. */
data class TimelineHomeNavigation(
    val timeline: Timeline,
    val openComposerOnEnter: Boolean = false,
)

sealed interface TimelineHomeContent {
    data object Loading : TimelineHomeContent
    data class Loaded(val summaries: List<TimelineHomeSummary>) : TimelineHomeContent
}

data class TimelineHomeState(
    val content: TimelineHomeContent = TimelineHomeContent.Loading,
    val query: String = "",
) {
    val customSummaries: List<TimelineHomeSummary>
        get() = (content as? TimelineHomeContent.Loaded)
            ?.summaries
            .orEmpty()
            .filter { it.timeline is Timeline.Custom }

    val visibleCustomSummaries: List<TimelineHomeSummary>
        get() = customSummaries.matchingTimelineQuery(query)
}

/**
 * The name filter Timeline Home's search field applies. Shared with the external-share timeline
 * picker, which runs the same field over its own query so both surfaces narrow identically.
 */
fun List<TimelineHomeSummary>.matchingTimelineQuery(query: String): List<TimelineHomeSummary> {
    val normalizedQuery = query.trim()
    return if (normalizedQuery.isEmpty()) this
    else filter { it.name.contains(normalizedQuery, ignoreCase = true) }
}
