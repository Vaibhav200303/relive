package com.vaibhav.relive.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class MomentId(val value: String) {
    init { require(value.isNotBlank()) { "MomentId must be non-blank" } }
}

@JvmInline
value class TimelineId(val value: String) {
    init { require(value.isNotBlank()) { "TimelineId must be non-blank" } }
}

@JvmInline
value class MediaAttachmentId(val value: String) {
    init { require(value.isNotBlank()) { "MediaAttachmentId must be non-blank" } }
}
