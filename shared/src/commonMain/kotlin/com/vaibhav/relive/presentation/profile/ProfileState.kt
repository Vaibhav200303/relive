package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.model.ProfileSnapshot
import com.vaibhav.relive.domain.time.Instant

data class ProfileState(
    val displayName: String = "Your Relive",
    val joiningDate: Instant? = null,
    val momentCount: Long = 0,
    val customTimelineCount: Long = 0,
    val placeCount: Long = 0,
)

internal fun ProfileSnapshot.toProfileState(): ProfileState = ProfileState(
    joiningDate = createdAt,
    momentCount = momentCount,
    customTimelineCount = customTimelineCount,
    placeCount = placeCount,
)
