package com.vaibhav.relive.domain.model

import com.vaibhav.relive.domain.time.Instant

/** A compact, reactive read model for the Profile screen. */
data class ProfileSnapshot(
    val createdAt: Instant?,
    val momentCount: Long,
    val customTimelineCount: Long,
    val placeCount: Long,
)
