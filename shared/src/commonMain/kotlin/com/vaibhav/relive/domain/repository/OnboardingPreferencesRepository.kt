package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.OnboardingPreferences
import kotlinx.coroutines.flow.StateFlow

interface OnboardingPreferencesRepository {
    val preferences: StateFlow<OnboardingPreferences>

    /** Completion is monotonic: an older caller can never downgrade a newer introduction. */
    suspend fun complete(version: Int): Result<Unit>
}
