package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.model.ProfileSnapshot
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.ProfileSettings

data class ProfileState(
    val displayName: String = "Your Relive",
    val joiningDate: Instant? = null,
    val momentCount: Long = 0,
    val customTimelineCount: Long = 0,
    val placeCount: Long = 0,
    val profilePhoto: MediaStorageRef? = null,
)

internal fun ProfileSnapshot.toProfileState(settings: ProfileSettings = ProfileSettings()): ProfileState = ProfileState(
    displayName = settings.displayName ?: "Your Relive",
    joiningDate = createdAt,
    momentCount = momentCount,
    customTimelineCount = customTimelineCount,
    placeCount = placeCount,
    profilePhoto = settings.profilePhoto,
)

fun pluralizedStat(value: Long, singular: String): String = "$value ${if (value == 1L) singular else "${singular}s"}"
