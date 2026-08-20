package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.MomentId

/**
 * A single moment prepared for the All timeline view. The presentation layer
 * decides the displayed date string here so the UI stays platform-free.
 */
data class MomentPresentation(
    val id: MomentId,
    val formattedDate: String,
    val title: String,
    val content: String,
    val locationLabel: String?,
    val isFavorite: Boolean,
) {
    val hasTitle: Boolean get() = title.isNotBlank()
    val hasContent: Boolean get() = content.isNotBlank()
}

sealed interface AllTimelineUiState {
    /** Persistence has not yet produced its first snapshot. */
    data object Loading : AllTimelineUiState

    /** Persistence produced an empty list. */
    data object Empty : AllTimelineUiState

    data class Loaded(val moments: List<MomentPresentation>) : AllTimelineUiState
}
