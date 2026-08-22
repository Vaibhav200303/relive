package com.vaibhav.relive.presentation.timelinehome

import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary

/**
 * Editorial copy for a Timeline Home card that has no visual preview media.
 * A null result leaves the existing image/video preview untouched.
 */
fun TimelineHomeSummary.emptyPreviewPlaceholderText(): String? {
    if (previewAttachments.isNotEmpty()) return null

    val subject = when (val value = timeline) {
        Timeline.All -> "Your story"
        is Timeline.Custom -> value.name
    }
    val momentDescription = if (momentCount == 0L) "first" else "new"
    return if (momentDescription == "first") {
        "$subject is waiting for its first moment."
    } else {
        "$subject is waiting for a new moment."
    }
}
