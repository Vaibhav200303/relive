package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.time.Instant

sealed interface CurrentTimeline {
    data object All : CurrentTimeline

    /** A derived scope, never stored as a custom timeline or membership. */
    data object Favorites : CurrentTimeline

    /** A derived, local-calendar collection; never stored as membership. */
    data class OnThisDay(val date: LocalCalendarDate) : CurrentTimeline

    /** A derived, deterministic daily-resurfacing collection; never stored as membership. */
    data class FromYourPast(val query: RediscoverQuery) : CurrentTimeline

    data class Custom(val id: TimelineId) : CurrentTimeline
}

/** Semantic viewing capability for normal timelines and future system collections. */
sealed interface TimelineMode {
    val allowsMutations: Boolean

    data object Editable : TimelineMode {
        override val allowsMutations: Boolean = true
    }

    data class ReadOnlySystemCollection(val title: String) : TimelineMode {
        override val allowsMutations: Boolean = false
    }
}

data class TimelineScreenState(
    val customTimelines: List<Timeline.Custom> = emptyList(),
    val currentTimeline: CurrentTimeline = CurrentTimeline.All,
    val moments: TimelineMomentsState = TimelineMomentsState.Loading,
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
