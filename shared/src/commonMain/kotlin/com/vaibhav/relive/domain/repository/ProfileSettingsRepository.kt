package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.LockAfter
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.ProfileSettings
import kotlinx.coroutines.flow.StateFlow

interface ProfileSettingsRepository {
    val settings: StateFlow<ProfileSettings>
    suspend fun setDisplayName(value: String): Result<Unit>
    suspend fun setProfilePhoto(value: MediaStorageRef?): Result<Unit>
    suspend fun setAppLockEnabled(value: Boolean): Result<Unit>
    suspend fun setBiometricUnlockEnabled(value: Boolean): Result<Unit>
    suspend fun setLockAfter(value: LockAfter): Result<Unit>
    suspend fun setRediscoverRemindersEnabled(value: Boolean): Result<Unit>
}
