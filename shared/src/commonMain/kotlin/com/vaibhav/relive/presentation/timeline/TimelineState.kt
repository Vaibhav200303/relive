package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.time.Instant

sealed interface CurrentTimeline {
    data object All : CurrentTimeline

    /** A derived scope, never stored as a custom timeline or membership. */
    data object Favorites : CurrentTimeline

    /** Read-only collection of every Moment carrying at least one image or video (ADR-0061). */
    data object AllPhotos : CurrentTimeline

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
    val appearance: TimelineAppearance = TimelineAppearance(),
    val moments: TimelineMomentsState = TimelineMomentsState.Loading,
    val dateNavigation: DateNavigationState? = null,
    val momentActions: MomentContextualActionState = MomentContextualActionState(),
    /**
     * True when the Home feed's bounded window may still be grown — there are older moments in the
     * archive that have not been loaded. Always false for surfaces that are not windowed.
     */
    val hasOlderMoments: Boolean = false,
)

/** How many moments the Home feed loads at a time (ADR-0061). */
const val HOME_FEED_PAGE_SIZE: Int = 30

/** How close to the oldest loaded moment the feed grows its window, so paging stays invisible. */
const val HOME_FEED_PREFETCH: Int = 8

/** UI state for the single selected Moment in All's contextual action app bar. */
data class MomentContextualActionState(
    val selectedMomentId: MomentId? = null,
    val assignedTimelineIds: Set<TimelineId> = emptySet(),
    val isLoadingAssignments: Boolean = false,
    val hasAssignmentLoadFailed: Boolean = false,
    val isAssignmentPickerVisible: Boolean = false,
    val isAssigning: Boolean = false,
)

data class MomentContextualActionAvailability(
    val canEnter: Boolean,
    val canEdit: Boolean,
    val canAddToTimeline: Boolean,
    val canForget: Boolean,
)

/** Contextual actions are intentionally an All-timeline-only interaction. */
fun resolveMomentContextualActionAvailability(
    mode: TimelineMode,
    currentTimeline: CurrentTimeline,
    isWithinEditWindow: Boolean,
    hasCustomTimelines: Boolean,
): MomentContextualActionAvailability {
    val isEditableAll = mode.allowsMutations && currentTimeline == CurrentTimeline.All
    val canAddToTimeline = isEditableAll && hasCustomTimelines
    val canEditOrForget = isEditableAll && isWithinEditWindow
    return MomentContextualActionAvailability(
        canEnter = canAddToTimeline || canEditOrForget,
        canEdit = canEditOrForget,
        canAddToTimeline = canAddToTimeline,
        canForget = canEditOrForget,
    )
}

data class DateNavigationState(
    val momentId: MomentId?,
    val message: String?,
)

data class TimelineCreationState(
    val isVisible: Boolean = false,
    val name: String = "",
    val coverPhotoRef: MediaStorageRef? = null,
    val isProcessingCover: Boolean = false,
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
    val feeling: MomentFeeling? = null,
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
