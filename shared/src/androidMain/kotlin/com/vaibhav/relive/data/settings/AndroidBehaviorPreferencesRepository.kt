package com.vaibhav.relive.data.settings

import android.content.Context
import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.repository.BehaviorPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class AndroidBehaviorPreferencesRepository(context: Context) : BehaviorPreferencesRepository {
    private val storage = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val mutablePreferences = MutableStateFlow(readPreferences())

    override val preferences: StateFlow<BehaviorPreferences> = mutablePreferences.asStateFlow()

    override suspend fun setConfirmBeforeDiscarding(enabled: Boolean): Result<Unit> = persistBoolean(
        key = CONFIRM_BEFORE_DISCARDING_KEY,
        value = enabled,
        update = { it.copy(confirmBeforeDiscarding = enabled) },
    )

    override suspend fun setShowLocations(enabled: Boolean): Result<Unit> = persistBoolean(
        key = SHOW_LOCATIONS_KEY,
        value = enabled,
        update = { it.copy(showLocations = enabled) },
    )

    override suspend fun setShowTags(enabled: Boolean): Result<Unit> = persistBoolean(
        key = SHOW_TAGS_KEY,
        value = enabled,
        update = { it.copy(showTags = enabled) },
    )

    override suspend fun setShowOnThisDay(enabled: Boolean): Result<Unit> = persistBoolean(
        key = SHOW_ON_THIS_DAY_KEY,
        value = enabled,
        update = { it.copy(showOnThisDay = enabled) },
    )

    override suspend fun setShowFavorites(enabled: Boolean): Result<Unit> = persistBoolean(
        key = SHOW_FAVORITES_KEY,
        value = enabled,
        update = { it.copy(showFavorites = enabled) },
    )

    private suspend fun persistBoolean(
        key: String,
        value: Boolean,
        update: (BehaviorPreferences) -> BehaviorPreferences,
    ): Result<Unit> = persist(key, value.encodeBehaviorPreference(), update)

    private suspend fun persist(
        key: String,
        value: String,
        update: (BehaviorPreferences) -> BehaviorPreferences,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            check(storage.edit().putString(key, value).commit()) { "Preference commit failed" }
            mutablePreferences.update(update)
        }
    }

    private fun readPreferences(): BehaviorPreferences = decodeBehaviorPreferences(
        confirmBeforeDiscarding = storage.getString(CONFIRM_BEFORE_DISCARDING_KEY, null),
        showLocations = storage.getString(SHOW_LOCATIONS_KEY, null),
        showTags = storage.getString(SHOW_TAGS_KEY, null),
        showOnThisDay = storage.getString(SHOW_ON_THIS_DAY_KEY, null),
        showFavorites = storage.getString(SHOW_FAVORITES_KEY, null),
    )

    private companion object {
        const val FILE_NAME: String = "relive_behavior_preferences"
    }
}
