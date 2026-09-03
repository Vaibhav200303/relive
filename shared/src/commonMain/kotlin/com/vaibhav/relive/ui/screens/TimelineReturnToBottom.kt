package com.vaibhav.relive.ui.screens

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Presentation state for the timeline's return-to-newest affordance. */
internal data class TimelineReturnToBottomState(
    val revealedByManualScroll: Boolean = false,
    val isAutoScrolling: Boolean = false,
) {
    fun onManualPositionChanged(
        movedTowardOlderMoments: Boolean,
        canScrollForward: Boolean,
    ): TimelineReturnToBottomState = when {
        !canScrollForward -> TimelineReturnToBottomState()
        movedTowardOlderMoments -> copy(revealedByManualScroll = true)
        else -> this
    }

    fun onPositionChanged(
        isProgrammaticScroll: Boolean,
        movedTowardOlderMoments: Boolean,
        canScrollForward: Boolean,
    ): TimelineReturnToBottomState = when {
        !canScrollForward -> TimelineReturnToBottomState()
        isProgrammaticScroll -> this
        else -> onManualPositionChanged(
            movedTowardOlderMoments = movedTowardOlderMoments,
            canScrollForward = true,
        )
    }

    fun onAutoScrollStarted(): TimelineReturnToBottomState = copy(isAutoScrolling = true)

    fun onAutoScrollStopped(canScrollForward: Boolean): TimelineReturnToBottomState =
        if (canScrollForward) copy(isAutoScrolling = false) else TimelineReturnToBottomState()

    fun isVisible(canScrollForward: Boolean): Boolean =
        revealedByManualScroll && canScrollForward && !isAutoScrolling
}

/**
 * Presentation state for Home's return-to-top affordance.
 *
 * Home's feed is newest-first, so the end people scroll back to is the top of All moments and the
 * gesture that asks for it is an upward drag. Unlike the timeline's return-to-newest control, this
 * one withdraws again on the first downward movement: it is a passing offer, not a fixed control.
 */
internal data class TimelineReturnToTopState(
    val revealedByManualScroll: Boolean = false,
) {
    fun onManualPositionChanged(
        movedTowardTop: Boolean,
        canReturnToTop: Boolean,
    ): TimelineReturnToTopState = when {
        !canReturnToTop -> TimelineReturnToTopState()
        movedTowardTop -> copy(revealedByManualScroll = true)
        else -> TimelineReturnToTopState()
    }

    fun onPositionChanged(
        isProgrammaticScroll: Boolean,
        movedTowardTop: Boolean,
        canReturnToTop: Boolean,
    ): TimelineReturnToTopState = when {
        !canReturnToTop -> TimelineReturnToTopState()
        isProgrammaticScroll -> this
        else -> onManualPositionChanged(movedTowardTop = movedTowardTop, canReturnToTop = true)
    }

    fun isVisible(canReturnToTop: Boolean): Boolean = revealedByManualScroll && canReturnToTop
}

internal data class TimelineListPosition(
    val index: Int,
    val scrollOffset: Int,
) {
    fun movedTowardTopOf(previous: TimelineListPosition): Boolean =
        index < previous.index || (index == previous.index && scrollOffset < previous.scrollOffset)

    /** On an oldest-first timeline the top of the list is its oldest end. */
    fun movedTowardOlderThan(previous: TimelineListPosition): Boolean = movedTowardTopOf(previous)
}

/** Owns at most one cancellable, user-initiated return-to-bottom animation. */
internal class TimelineAutoScrollController {
    private var job: Job? = null

    val isRunning: Boolean
        get() = job?.isActive == true

    fun start(
        scope: CoroutineScope,
        scroll: suspend () -> Unit,
        onStopped: () -> Unit,
    ) {
        if (isRunning) return
        job = scope.launch {
            try {
                scroll()
            } finally {
                onStopped()
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}
