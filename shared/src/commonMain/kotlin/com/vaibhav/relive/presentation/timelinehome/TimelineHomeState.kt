package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary

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
        get() {
            val normalizedQuery = query.trim()
            return if (normalizedQuery.isEmpty()) {
                customSummaries
            } else {
                customSummaries.filter { it.name.contains(normalizedQuery, ignoreCase = true) }
            }
        }
}
