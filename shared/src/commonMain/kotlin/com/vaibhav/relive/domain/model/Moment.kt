package com.vaibhav.relive.domain.model

import com.vaibhav.relive.domain.time.Instant

/**
 * A single stored memory. Stored once regardless of how many custom timelines it
 * belongs to (see PRODUCT_SPEC §2, ADR-0004). Title and content are preserved
 * verbatim as entered — no silent trimming beyond the emptiness check documented in
 * [MomentValidation].
 *
 * [createdAt] is set at construction and must never change afterwards. [updatedAt]
 * reflects the last edit and must never be used to compute the 4-day edit/forget
 * window (ADR-0005).
 */
data class Moment(
    val id: MomentId,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val title: String = "",
    val content: String = "",
    val isFavorite: Boolean = false,
    /** Optional post-save reflection; absent forever unless the person chooses one (ADR-0066). */
    val feeling: MomentFeeling? = null,
    val location: ReliveLocation? = null,
    val tags: List<Tag> = emptyList(),
    val attachments: List<MediaAttachment> = emptyList(),
) {
    init {
        updatedAt?.let {
            require(it >= createdAt) { "updatedAt must not precede createdAt" }
        }
        require(attachments.map { it.sortIndex }.toSet().size == attachments.size) {
            "attachment sortIndex values must be unique within a moment"
        }
        require(attachments.map { it.id }.toSet().size == attachments.size) {
            "attachment ids must be unique within a moment"
        }
        require(tags.toSet().size == tags.size) { "tags must not repeat (canonical form)" }
    }
}
