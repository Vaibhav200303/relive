package com.vaibhav.relive.domain.model

import kotlin.jvm.JvmInline

enum class MediaType { Image, Video, Audio }

/**
 * Persistence-independent handle to a stored media blob. Concrete meaning (file path,
 * content URI, remote key, ...) is decided by the storage layer; the domain only
 * requires a stable, non-blank string reference.
 */
@JvmInline
value class MediaStorageRef(val value: String) {
    init { require(value.isNotBlank()) { "MediaStorageRef must be non-blank" } }
}

/**
 * A single media entry attached to a moment. [sortIndex] gives a stable presentation
 * order; equal indices between two attachments of the same moment are invalid.
 */
data class MediaAttachment(
    val id: MediaAttachmentId,
    val type: MediaType,
    val storageRef: MediaStorageRef,
    val sortIndex: Int,
) {
    init { require(sortIndex >= 0) { "sortIndex must be >= 0" } }
}
