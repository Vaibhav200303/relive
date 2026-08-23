package com.vaibhav.relive.domain.model

import com.vaibhav.relive.domain.time.Instant

/**
 * Bounded read model for the Timeline Home. It deliberately contains only
 * the information needed to render a collection card, not full Moments.
 */
data class TimelineHomeSummary(
    val timeline: Timeline,
    val momentCount: Long,
    val previewAttachments: List<MediaAttachment>,
    val createdAt: Instant? = null,
) {
    init {
        require(momentCount >= 0) { "momentCount must not be negative" }
        require(previewAttachments.size <= MAX_PREVIEW_ATTACHMENTS) {
            "Timeline Home previews are bounded to $MAX_PREVIEW_ATTACHMENTS attachments"
        }
        require(previewAttachments.all { it.type != MediaType.Audio }) {
            "Timeline Home previews include visual media only"
        }
        require((timeline is Timeline.Custom) == (createdAt != null)) {
            "Only custom timelines expose a persisted creation time"
        }
    }

    val name: String get() = when (val value = timeline) {
        Timeline.All -> "All"
        is Timeline.Custom -> value.name
    }

    companion object {
        const val MAX_PREVIEW_ATTACHMENTS: Int = 4
    }
}
