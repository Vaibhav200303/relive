package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.TimelineAppearance
import kotlinx.coroutines.flow.StateFlow

interface AppearanceRepository {
    val preferences: StateFlow<AppearancePreferences>

    suspend fun setMode(mode: AppearanceMode): Result<Unit>

    suspend fun setDefaultTheme(theme: ThemeReference): Result<Unit>

    /** Persists the appearance of logical All independently from the app palette. */
    suspend fun setAllTimelineAppearance(appearance: TimelineAppearance): Result<Unit>
}
