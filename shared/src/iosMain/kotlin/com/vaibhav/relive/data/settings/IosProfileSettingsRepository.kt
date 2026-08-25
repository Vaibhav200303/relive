package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.LockAfter
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.ProfileSettings
import com.vaibhav.relive.domain.repository.ProfileSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSUserDefaults

class IosProfileSettingsRepository : ProfileSettingsRepository {
    private val store = NSUserDefaults.standardUserDefaults
    private val mutable = MutableStateFlow(read())
    override val settings = mutable.asStateFlow()
    override suspend fun setDisplayName(value: String) = save({ store.setObject(value, PROFILE_NAME) }) { it.copy(displayName = value) }
    override suspend fun setProfilePhoto(value: MediaStorageRef?) = save({ store.setObject(value?.value, PROFILE_PHOTO) }) { it.copy(profilePhoto = value) }
    override suspend fun setAppLockEnabled(value: Boolean) = save({ store.setBool(value, APP_LOCK); if (!value) store.setBool(false, BIOMETRICS) }) { it.copy(appLockEnabled = value, biometricUnlockEnabled = if (value) it.biometricUnlockEnabled else false) }
    override suspend fun setBiometricUnlockEnabled(value: Boolean) = save({ store.setBool(value, BIOMETRICS) }) { it.copy(biometricUnlockEnabled = value) }
    override suspend fun setLockAfter(value: LockAfter) = save({ store.setObject(value.name, LOCK_AFTER) }) { it.copy(lockAfter = value) }
    override suspend fun setRediscoverRemindersEnabled(value: Boolean) = save({ store.setBool(value, REMINDERS) }) { it.copy(rediscoverRemindersEnabled = value) }
    private fun save(write: () -> Unit, update: (ProfileSettings) -> ProfileSettings) = runCatching { write(); mutable.update(update) }
    private fun read() = decodeProfileSettings(store.stringForKey(PROFILE_NAME), store.stringForKey(PROFILE_PHOTO), store.boolOrNull(APP_LOCK), store.boolOrNull(BIOMETRICS), store.stringForKey(LOCK_AFTER), store.boolOrNull(REMINDERS))
    private fun NSUserDefaults.boolOrNull(key: String) = if (objectForKey(key) == null) null else boolForKey(key)
}
