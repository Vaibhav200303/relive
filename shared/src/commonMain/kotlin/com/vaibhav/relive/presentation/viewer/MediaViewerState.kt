package com.vaibhav.relive.presentation.viewer

import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation

/**
 * State for the full-screen media viewer (ADR-0019 §5). The viewer is a
 * single unified page-per-attachment surface: [attachments] is the full
 * ordered list from the tapped Moment (order = ADR-0013 `sort_index`),
 * [currentIndex] is the page currently shown. Opening at the tapped tile
 * uses [openAt]; +N tiles open at index 3 with all remaining attachments
 * accessible by swipe.
 */
data class MediaViewerState(
    val attachments: List<MomentAttachmentPresentation>,
    val initialIndex: Int,
    val currentIndex: Int,
) {
    init {
        require(attachments.isNotEmpty()) { "viewer requires at least one attachment" }
        require(initialIndex in attachments.indices) { "initialIndex $initialIndex out of bounds" }
        require(currentIndex in attachments.indices) { "currentIndex $currentIndex out of bounds" }
    }

    val size: Int get() = attachments.size
    val current: MomentAttachmentPresentation get() = attachments[currentIndex]

    fun withCurrent(index: Int): MediaViewerState =
        copy(currentIndex = index.coerceIn(0, attachments.lastIndex))
}

/**
 * Open the viewer at the exact tapped attachment. [tappedIndex] is the
 * index inside the tile that was tapped; caller passes the full attachment
 * list so hidden `+N` attachments become swipe-reachable.
 */
fun openAt(
    attachments: List<MomentAttachmentPresentation>,
    tappedIndex: Int,
): MediaViewerState {
    require(attachments.isNotEmpty()) { "cannot open viewer with no attachments" }
    val idx = tappedIndex.coerceIn(0, attachments.lastIndex)
    return MediaViewerState(attachments = attachments, initialIndex = idx, currentIndex = idx)
}
