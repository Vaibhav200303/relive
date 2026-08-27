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

internal data class TimelineListPosition(
    val index: Int,
    val scrollOffset: Int,
) {
    fun movedTowardOlderThan(previous: TimelineListPosition): Boolean =
        index < previous.index || (index == previous.index && scrollOffset < previous.scrollOffset)
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
