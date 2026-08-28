package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.TimelineAppearance
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

    override suspend fun setAllTimelineAppearance(appearance: TimelineAppearance): Result<Unit> = runCatching {
        storage.setObject(appearance.wallpaper.encodePreference(), forKey = ALL_TIMELINE_WALLPAPER_KEY)
        storage.setObject(appearance.momentTheme.encodePreference(), forKey = ALL_TIMELINE_MOMENT_THEME_KEY)
        mutablePreferences.update { it.copy(allTimelineAppearance = appearance) }
    }

    private fun readPreferences(): AppearancePreferences = decodeAppearancePreferences(
        mode = storage.stringForKey(APPEARANCE_MODE_KEY),
        theme = storage.stringForKey(APPEARANCE_THEME_KEY),
        allTimelineWallpaper = storage.stringForKey(ALL_TIMELINE_WALLPAPER_KEY),
        allTimelineMomentTheme = storage.stringForKey(ALL_TIMELINE_MOMENT_THEME_KEY),
    )
}
