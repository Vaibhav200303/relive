package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.MomentDateNavigationScope
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.policy.EditWindow
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.presentation.date.editorialDayMonth
import com.vaibhav.relive.domain.model.LocalCalendarDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val momentRepository: MomentRepository,
    private val timelineRepository: TimelineRepository,
    private val rediscoverRepository: RediscoverRepository,
    private val clock: Clock,
    private val scope: CoroutineScope,
    initialTimeline: CurrentTimeline = CurrentTimeline.All,
    private val mode: TimelineMode = TimelineMode.Editable,
    /**
     * The tapped card's own timeline, when the screen is entered through a card (ADR-0063). It
     * seeds the very first frame — real wallpaper, cover, name — so the container transform never
     * shows the default appearance popping into the timeline's mid-morph. [observeCustom]'s first
     * emission then overwrites it with identical database data.
     */
    seedTimelines: List<Timeline.Custom> = emptyList(),
) {

    private val _state = MutableStateFlow(
        TimelineScreenState(
            currentTimeline = initialTimeline,
            customTimelines = seedTimelines,
            appearance = initialTimeline.appearanceFrom(seedTimelines),
        ),
    )
    val state: StateFlow<TimelineScreenState> = _state.asStateFlow()

    private var momentsJob: Job? = null

    /**
     * How many of All's newest moments the Home feed currently holds. Grows one page at a time as
     * the person scrolls toward older moments; the root never reads the whole archive (ADR-0061).
     * Only [CurrentTimeline.All] is windowed — custom timelines and the read-only collections are
     * already bounded by their own scope.
     */
    private val allWindowLimit = MutableStateFlow(HOME_FEED_PAGE_SIZE)
    init {
        scope.launch {
            timelineRepository.observeCustom().collect { timelines ->
                _state.update { current ->
                    current.copy(
                        customTimelines = timelines,
                        appearance = current.currentTimeline.appearanceFrom(timelines),
                    )
                }
            }
        }
        observe(initialTimeline)
    }

    fun selectTimeline(timeline: CurrentTimeline) {
        if (timeline == _state.value.currentTimeline) return
        _state.update {
            it.copy(
                currentTimeline = timeline,
                appearance = timeline.appearanceFrom(it.customTimelines),
                moments = TimelineMomentsState.Loading,
            )
        }
        observe(timeline)
    }

    fun setFavorite(id: MomentId, isFavorite: Boolean) {
        if (!mode.allowsMutations) return
        scope.launch { momentRepository.setFavorite(id, isFavorite) }
    }

    fun updateCurrentTimelineAppearance(
        appearance: TimelineAppearance,
        onResult: (Boolean) -> Unit,
    ) {
        val timeline = _state.value.currentTimeline as? CurrentTimeline.Custom ?: return
        scope.launch {
            onResult(runCatching { timelineRepository.updateAppearance(timeline.id, appearance) }.isSuccess)
        }
    }

    fun updateCurrentTimelineCover(coverPhotoRef: MediaStorageRef?, onResult: (Boolean) -> Unit) {
        val timeline = _state.value.currentTimeline as? CurrentTimeline.Custom ?: return onResult(false)
        scope.launch { onResult(runCatching { timelineRepository.updateCoverPhoto(timeline.id, coverPhotoRef) }.isSuccess) }
    }

    /** Removes only the custom collection and its membership rows, never its Moments. */
    fun deleteCurrentCustomTimeline(onResult: (Boolean) -> Unit) {
        val timeline = _state.value.currentTimeline as? CurrentTimeline.Custom ?: return
        scope.launch {
            onResult(runCatching { timelineRepository.deleteCustom(timeline.id) }.isSuccess)
        }
    }

    fun jumpToDate(date: LocalCalendarDate) {
        val targetScope = when (val timeline = _state.value.currentTimeline) {
            CurrentTimeline.All -> MomentDateNavigationScope.All
            is CurrentTimeline.Custom -> MomentDateNavigationScope.Custom(timeline.id)
            else -> return
        }
        scope.launch {
            val target = momentRepository.findDateNavigationTarget(
                scope = targetScope,
                dayStart = RediscoverCalendar.startOfDay(date),
                nextDayStart = RediscoverCalendar.nextDayStart(date),
            )
            val message = target?.let {
                val shownDate = RediscoverCalendar.localDate(it.createdAt)
                if (shownDate == date) null else "No moments on ${date.editorialDayMonth()} — showing ${shownDate.editorialDayMonth()}."
            } ?: "No moments in this timeline yet."
            _state.update { it.copy(dateNavigation = DateNavigationState(target?.id, message)) }
        }
    }

    fun consumeDateNavigation() {
        _state.update { it.copy(dateNavigation = null) }
    }

    fun canEditOrForget(moment: Moment): Boolean = EditWindow.isEditable(moment, clock)

    /** Opens All's single-Moment contextual action mode and loads current memberships. */
    fun selectMomentForActions(momentId: MomentId, onAssignmentLoadFailure: () -> Unit) {
        if (!mode.allowsMutations || _state.value.currentTimeline != CurrentTimeline.All) return
        _state.update {
            it.copy(
                momentActions = MomentContextualActionState(
                    selectedMomentId = momentId,
                    isLoadingAssignments = true,
                ),
            )
        }
        scope.launch {
            val assigned = runCatching { timelineRepository.timelinesFor(momentId) }
            _state.update { current ->
                if (current.momentActions.selectedMomentId != momentId) {
                    current
                } else {
                    current.copy(
                        momentActions = current.momentActions.copy(
                            assignedTimelineIds = assigned.getOrDefault(emptyList()).toSet(),
                            isLoadingAssignments = false,
                            hasAssignmentLoadFailed = assigned.isFailure,
                        ),
                    )
                }
            }
            if (assigned.isFailure) onAssignmentLoadFailure()
        }
    }

    fun clearMomentActionSelection() {
        _state.update { it.copy(momentActions = MomentContextualActionState()) }
    }

    fun showTimelineAssignmentPicker() {
        _state.update { current ->
            if (
                current.currentTimeline == CurrentTimeline.All &&
                current.momentActions.selectedMomentId != null &&
                !current.momentActions.isLoadingAssignments &&
                !current.momentActions.hasAssignmentLoadFailed
            ) {
                current.copy(momentActions = current.momentActions.copy(isAssignmentPickerVisible = true))
            } else {
                current
            }
        }
    }

    fun dismissTimelineAssignmentPicker() {
        _state.update { current ->
            current.copy(momentActions = current.momentActions.copy(isAssignmentPickerVisible = false))
        }
    }

    /** Adds the selected Moment to one custom timeline without modifying existing memberships. */
    fun addSelectedMomentToTimeline(timelineId: TimelineId, onResult: (Boolean) -> Unit) {
        val selected = _state.value.momentActions.selectedMomentId ?: run {
            onResult(false)
            return
        }
        if (!mode.allowsMutations || _state.value.currentTimeline != CurrentTimeline.All) {
            onResult(false)
            return
        }
        _state.update { current ->
            current.copy(momentActions = current.momentActions.copy(isAssigning = true))
        }
        scope.launch {
            val succeeded = runCatching { timelineRepository.addMembership(selected, timelineId) }.isSuccess
            if (succeeded) {
                _state.update { current ->
                    if (current.momentActions.selectedMomentId == selected) {
                        current.copy(momentActions = MomentContextualActionState())
                    } else {
                        current
                    }
                }
            } else {
                _state.update { current ->
                    if (current.momentActions.selectedMomentId == selected) {
                        current.copy(momentActions = current.momentActions.copy(isAssigning = false))
                    } else {
                        current
                    }
                }
            }
            onResult(succeeded)
        }
    }

    /** Checks the policy again at the destructive boundary before touching persistence. */
    fun forget(moment: Moment, onDeleted: (Moment) -> Unit, onFailure: () -> Unit) {
        if (!mode.allowsMutations) {
            onFailure()
            return
        }
        if (!EditWindow.isForgettable(moment, clock)) {
            onFailure()
            return
        }
        scope.launch {
            try {
                momentRepository.delete(moment.id)
                onDeleted(moment)
            } catch (_: Throwable) {
                onFailure()
            }
        }
    }

    private fun observe(timeline: CurrentTimeline) {
        momentsJob?.cancel()
        momentsJob = scope.launch {
            timeline.momentsFlow().collect { moments ->
                // The repository reads newest-first and presentation keeps that order everywhere:
                // the All moments feed on Home (ADR-0061), custom timeline detail (ADR-0062), and
                // the read-only system collections, which are sliding-cover surfaces opening at
                // their newest moment too (ADR-0065). From Your Past's emission is its curated
                // daily order rather than chronology, kept the same way.
                val presentation = moments.map { it.toPresentation() }
                // A full window means the archive may hold older moments still to load.
                val windowIsFull = timeline is CurrentTimeline.All &&
                    moments.size >= allWindowLimit.value
                _state.update { current ->
                    if (current.currentTimeline != timeline) {
                        current
                    } else {
                        current.copy(
                            moments = if (presentation.isEmpty()) {
                                TimelineMomentsState.Empty
                            } else {
                                TimelineMomentsState.Loaded(presentation)
                            },
                            momentActions = current.momentActions.takeIf { actions ->
                                actions.selectedMomentId == null || presentation.any { it.id == actions.selectedMomentId }
                            } ?: MomentContextualActionState(),
                            hasOlderMoments = windowIsFull,
                        )
                    }
                }
            }
        }
    }

    /**
     * Grows the Home feed by one page. Safe to call repeatedly while a load is in flight and once
     * the whole archive is loaded; both are no-ops.
     */
    fun loadOlderMoments() {
        if (_state.value.currentTimeline !is CurrentTimeline.All) return
        if (!_state.value.hasOlderMoments) return
        allWindowLimit.update { it + HOME_FEED_PAGE_SIZE }
    }

    private fun CurrentTimeline.momentsFlow(): Flow<List<Moment>> = when (this) {
        CurrentTimeline.All -> allWindowLimit.flatMapLatest { limit ->
            momentRepository.observeAllWindow(limit)
        }
        CurrentTimeline.Favorites -> rediscoverRepository.observeFavoriteMoments()
        CurrentTimeline.AllPhotos -> rediscoverRepository.observeAllPhotosMoments()
        is CurrentTimeline.OnThisDay -> rediscoverRepository.observeOnThisDayMoments(
            today = date,
            startOfToday = RediscoverCalendar.startOfDay(date),
        )
        is CurrentTimeline.FromYourPast -> rediscoverRepository.observeFromYourPastMoments(query)
        is CurrentTimeline.Custom -> momentRepository.observeInTimeline(id)
    }

    private fun CurrentTimeline.appearanceFrom(timelines: List<Timeline.Custom>): TimelineAppearance =
        (this as? CurrentTimeline.Custom)
            ?.let { current -> timelines.firstOrNull { it.id == current.id }?.appearance }
            ?: TimelineAppearance()
}
