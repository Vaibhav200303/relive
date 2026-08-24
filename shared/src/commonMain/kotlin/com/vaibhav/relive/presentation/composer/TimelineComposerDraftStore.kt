package com.vaibhav.relive.presentation.composer

import com.vaibhav.relive.presentation.timeline.CurrentTimeline

/**
 * App-session store for unfinished, timeline-scoped composer work.
 *
 * This deliberately stores only stable draft state. Camera/picker overlays,
 * recording and in-flight media processing are screen-bound and must not be
 * resurrected after navigation.
 */
class TimelineComposerDraftStore {
    private val drafts = mutableMapOf<CurrentTimeline, MomentComposerState>()

    fun restore(timeline: CurrentTimeline): MomentComposerState? = drafts[timeline]

    fun preserve(state: MomentComposerState) {
        if (state.hasUserDraft) {
            drafts[state.timelineContext] = state.copy(
                addMediaExpanded = false,
                recording = null,
                pendingMediaAction = null,
                pendingMicPermissionRequest = false,
                micPermission = MicPermissionUiState.Idle,
                overlay = ComposerOverlay.None,
                saveState = SaveState.Idle,
                mediaError = null,
                attachments = state.attachments.filter { it.status is DraftMediaStatus.Ready },
            )
        } else {
            drafts.remove(state.timelineContext)
        }
    }

    fun clear(timeline: CurrentTimeline) {
        drafts.remove(timeline)
    }
}
