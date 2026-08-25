package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.LockAfter
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.ProfileSettings

internal const val PROFILE_NAME = "profile.name"
internal const val PROFILE_PHOTO = "profile.photo"
internal const val APP_LOCK = "profile.app_lock"
internal const val BIOMETRICS = "profile.biometrics"
internal const val LOCK_AFTER = "profile.lock_after"
internal const val REMINDERS = "profile.rediscover_reminders"

internal fun decodeProfileSettings(
    name: String?, photo: String?, appLock: Boolean?, biometrics: Boolean?, lockAfter: String?, reminders: Boolean?,
) = ProfileSettings(
    displayName = name?.trim()?.takeIf { it.isNotEmpty() },
    profilePhoto = photo?.takeIf { it.isNotBlank() }?.let(::MediaStorageRef),
    appLockEnabled = appLock ?: false,
    biometricUnlockEnabled = biometrics ?: false,
    lockAfter = LockAfter.entries.firstOrNull { it.name == lockAfter } ?: LockAfter.Immediately,
    rediscoverRemindersEnabled = reminders ?: false,
)
