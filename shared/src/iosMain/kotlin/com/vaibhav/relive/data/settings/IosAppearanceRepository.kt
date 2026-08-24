package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.repository.AppearanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSUserDefaults

class IosAppearanceRepository : AppearanceRepository {
    private val storage = NSUserDefaults.standardUserDefaults
    private val mutablePreferences = MutableStateFlow(readPreferences())

    override val preferences: StateFlow<AppearancePreferences> = mutablePreferences.asStateFlow()

    override suspend fun setMode(mode: AppearanceMode): Result<Unit> = runCatching {
        storage.setObject(mode.encodePreference(), forKey = APPEARANCE_MODE_KEY)
        mutablePreferences.update { it.copy(mode = mode) }
    }

    override suspend fun setDefaultTheme(theme: ThemeReference): Result<Unit> = runCatching {
        storage.setObject(theme.encodePreference(), forKey = APPEARANCE_THEME_KEY)
        mutablePreferences.update { it.copy(defaultTheme = theme) }
    }

    private fun readPreferences(): AppearancePreferences = decodeAppearancePreferences(
        mode = storage.stringForKey(APPEARANCE_MODE_KEY),
        theme = storage.stringForKey(APPEARANCE_THEME_KEY),
    )
}
