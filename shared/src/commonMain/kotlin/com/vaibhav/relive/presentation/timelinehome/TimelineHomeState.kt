package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.model.TimelineHomeSummary

sealed interface TimelineHomeContent {
    data object Loading : TimelineHomeContent
    data class Loaded(val summaries: List<TimelineHomeSummary>) : TimelineHomeContent
}

data class TimelineHomeState(
    val content: TimelineHomeContent = TimelineHomeContent.Loading,
)
