package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.time.Instant

sealed interface CurrentTimeline {
    data object All : CurrentTimeline

    data class Custom(val id: TimelineId) : CurrentTimeline
}

data class TimelineScreenState(
    val customTimelines: List<Timeline.Custom> = emptyList(),
    val currentTimeline: CurrentTimeline = CurrentTimeline.All,
    val moments: TimelineMomentsState = TimelineMomentsState.Loading,
    val creation: TimelineCreationState = TimelineCreationState(),
)

data class TimelineCreationState(
    val isVisible: Boolean = false,
    val name: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface TimelineMomentsState {
    data object Loading : TimelineMomentsState
    data object Empty : TimelineMomentsState
    data class Loaded(val moments: List<MomentPresentation>) : TimelineMomentsState
}

data class MomentPresentation(
    val id: MomentId,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val formattedDate: String,
    val formattedTime: String,
    val title: String,
    val content: String,
    val locationLabel: String?,
    val location: ReliveLocation?,
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
    val id: String = "",
)
