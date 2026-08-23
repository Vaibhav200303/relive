package com.vaibhav.relive.presentation.cardcover

import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Timeline

enum class CardCoverState { GeneratedCover, Media }

fun cardCoverState(attachments: List<MediaAttachment>): CardCoverState =
    if (attachments.any { it.type.isVisualPreviewMedia }) CardCoverState.Media else CardCoverState.GeneratedCover

fun List<MediaAttachment>.firstVisualPreviewAttachment(): MediaAttachment? =
    asSequence()
        .filter { it.type.isVisualPreviewMedia }
        .minByOrNull { it.sortIndex }

fun Timeline.cardCoverStableKey(): String = when (this) {
    Timeline.All -> "timeline-all"
    is Timeline.Custom -> "timeline-${id.value}"
}

private val MediaType.isVisualPreviewMedia: Boolean
    get() = this == MediaType.Image || this == MediaType.Video
