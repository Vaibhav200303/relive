package com.vaibhav.relive.data.settings

import android.content.Context
import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.repository.AppearanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class AndroidAppearanceRepository(context: Context) : AppearanceRepository {
    private val storage = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val mutablePreferences = MutableStateFlow(readPreferences())

    override val preferences: StateFlow<AppearancePreferences> = mutablePreferences.asStateFlow()

    override suspend fun setMode(mode: AppearanceMode): Result<Unit> = persist(
        key = APPEARANCE_MODE_KEY,
        value = mode.encodePreference(),
        update = { it.copy(mode = mode) },
    )

    override suspend fun setDefaultTheme(theme: ThemeReference): Result<Unit> = persist(
        key = APPEARANCE_THEME_KEY,
        value = theme.encodePreference(),
        update = { it.copy(defaultTheme = theme) },
    )

    private suspend fun persist(
        key: String,
        value: String,
        update: (AppearancePreferences) -> AppearancePreferences,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            check(storage.edit().putString(key, value).commit()) { "Preference commit failed" }
            mutablePreferences.update(update)
        }
    }

    private fun readPreferences(): AppearancePreferences = decodeAppearancePreferences(
        mode = storage.getString(APPEARANCE_MODE_KEY, null),
        theme = storage.getString(APPEARANCE_THEME_KEY, null),
    )

    private companion object {
        const val FILE_NAME: String = "relive_appearance"
    }
}
