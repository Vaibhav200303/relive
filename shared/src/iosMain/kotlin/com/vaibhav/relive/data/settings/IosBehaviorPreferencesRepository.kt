package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.repository.BehaviorPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSUserDefaults

class IosBehaviorPreferencesRepository : BehaviorPreferencesRepository {
    private val storage = NSUserDefaults.standardUserDefaults
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
    ): Result<Unit> = runCatching {
        storage.setObject(value, forKey = key)
        mutablePreferences.update(update)
    }

    private fun readPreferences(): BehaviorPreferences = decodeBehaviorPreferences(
        confirmBeforeDiscarding = storage.stringForKey(CONFIRM_BEFORE_DISCARDING_KEY),
        showLocations = storage.stringForKey(SHOW_LOCATIONS_KEY),
        showTags = storage.stringForKey(SHOW_TAGS_KEY),
        showOnThisDay = storage.stringForKey(SHOW_ON_THIS_DAY_KEY),
        showFavorites = storage.stringForKey(SHOW_FAVORITES_KEY),
    )
}
