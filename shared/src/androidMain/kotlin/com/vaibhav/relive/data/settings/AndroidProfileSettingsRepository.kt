package com.vaibhav.relive.data.settings

import android.content.Context
import com.vaibhav.relive.domain.model.LockAfter
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.ProfileSettings
import com.vaibhav.relive.domain.repository.ProfileSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class AndroidProfileSettingsRepository(context: Context) : ProfileSettingsRepository {
    private val store = context.applicationContext.getSharedPreferences("relive_profile", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())
    override val settings = mutable.asStateFlow()

    override suspend fun setDisplayName(value: String) = persist({ putString(PROFILE_NAME, value) }) { it.copy(displayName = value) }
    override suspend fun setProfilePhoto(value: MediaStorageRef?) = persist({ putString(PROFILE_PHOTO, value?.value) }) { it.copy(profilePhoto = value) }
    override suspend fun setAppLockEnabled(value: Boolean) = persist({ putBoolean(APP_LOCK, value); if (!value) putBoolean(BIOMETRICS, false) }) { it.copy(appLockEnabled = value, biometricUnlockEnabled = if (value) it.biometricUnlockEnabled else false) }
    override suspend fun setBiometricUnlockEnabled(value: Boolean) = persist({ putBoolean(BIOMETRICS, value) }) { it.copy(biometricUnlockEnabled = value) }
    override suspend fun setLockAfter(value: LockAfter) = persist({ putString(LOCK_AFTER, value.name) }) { it.copy(lockAfter = value) }
    override suspend fun setRediscoverRemindersEnabled(value: Boolean) = persist({ putBoolean(REMINDERS, value) }) { it.copy(rediscoverRemindersEnabled = value) }

    private suspend fun persist(edit: android.content.SharedPreferences.Editor.() -> Unit, update: (ProfileSettings) -> ProfileSettings): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(store.edit().apply(edit).commit()) { "Preference commit failed" }
            mutable.update(update)
        }
    }

    private fun read() = decodeProfileSettings(
        store.getString(PROFILE_NAME, null), store.getString(PROFILE_PHOTO, null),
        store.booleanOrNull(APP_LOCK), store.booleanOrNull(BIOMETRICS), store.getString(LOCK_AFTER, null), store.booleanOrNull(REMINDERS),
    )
    private fun android.content.SharedPreferences.booleanOrNull(key: String) = if (contains(key)) getBoolean(key, false) else null
}
