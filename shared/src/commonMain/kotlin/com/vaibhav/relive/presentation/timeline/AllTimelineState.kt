package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag

/**
 * A single moment prepared for the All timeline view.
 */
data class MomentPresentation(
    val id: MomentId,
    val formattedDate: String,
    val title: String,
    val content: String,
    val locationLabel: String?,
    val isFavorite: Boolean,
    val tags: List<Tag>,
    val attachments: List<MomentAttachmentPresentation>,
) {
    val hasTitle: Boolean get() = title.isNotBlank()
    val hasContent: Boolean get() = content.isNotBlank()
    val hasTags: Boolean get() = tags.isNotEmpty()
    val hasAttachments: Boolean get() = attachments.isNotEmpty()
}

data class MomentAttachmentPresentation(
    val storageRef: MediaStorageRef,
    val type: MediaType,
)

sealed interface AllTimelineUiState {
    data object Loading : AllTimelineUiState
    data object Empty : AllTimelineUiState
    data class Loaded(val moments: List<MomentPresentation>) : AllTimelineUiState
}
