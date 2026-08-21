package com.vaibhav.relive.presentation.viewer

import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation

/**
 * Dedicated multi-media gallery for one Moment. Timeline collage taps
 * route through here when the Moment has 2+ attachments; a single-
 * attachment Moment bypasses the gallery and opens the viewer directly.
 * Order matches the attachment list (ADR-0013 `sort_index`).
 */
data class MomentMediaGalleryState(
    val attachments: List<MomentAttachmentPresentation>,
) {
    init {
        require(attachments.size >= 2) {
            "gallery requires 2+ attachments; single-attachment moments open the viewer directly"
        }
    }

    val size: Int get() = attachments.size
}

/**
 * Navigation state overlaying the timeline: at most one of [gallery] and
 * [viewer] originates a tap, but both may be present when the viewer was
 * opened from a gallery item (viewer overlays gallery so gallery scroll
 * position is preserved when the viewer closes).
 */
data class TimelineMediaNavState(
    val gallery: MomentMediaGalleryState? = null,
    val viewer: MediaViewerState? = null,
) {
    companion object {
        val Idle: TimelineMediaNavState = TimelineMediaNavState()
    }
}

/**
 * Timeline collage tap. 1 attachment → viewer directly. 2+ attachments
 * (including a +N tile) → gallery for that Moment; the tapped index is
 * discarded because the gallery is the required intermediate surface.
 */
fun TimelineMediaNavState.openFromCollage(
    attachments: List<MomentAttachmentPresentation>,
    @Suppress("UNUSED_PARAMETER") tappedIndex: Int,
): TimelineMediaNavState {
    require(attachments.isNotEmpty()) { "collage tap requires at least one attachment" }
    return if (attachments.size == 1) {
        copy(viewer = openAt(attachments, 0))
    } else {
        copy(gallery = MomentMediaGalleryState(attachments), viewer = null)
    }
}

/** Gallery tile tap: open viewer at the tapped attachment. */
fun TimelineMediaNavState.openFromGallery(index: Int): TimelineMediaNavState {
    val g = gallery ?: return this
    return copy(viewer = openAt(g.attachments, index))
}

/** Viewer Back: return to the gallery (if any) at the same scroll position. */
fun TimelineMediaNavState.closeViewer(): TimelineMediaNavState = copy(viewer = null)

/** Gallery Back: return to the timeline. Also clears any viewer overlay. */
fun TimelineMediaNavState.closeGallery(): TimelineMediaNavState = TimelineMediaNavState.Idle
