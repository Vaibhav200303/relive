package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.model.StartDestination
import kotlinx.coroutines.flow.StateFlow

interface BehaviorPreferencesRepository {
    val preferences: StateFlow<BehaviorPreferences>

    suspend fun setStartDestination(destination: StartDestination): Result<Unit>

    suspend fun setConfirmBeforeDiscarding(enabled: Boolean): Result<Unit>

    suspend fun setShowLocations(enabled: Boolean): Result<Unit>

    suspend fun setShowTags(enabled: Boolean): Result<Unit>

    suspend fun setShowOnThisDay(enabled: Boolean): Result<Unit>

    suspend fun setShowFavorites(enabled: Boolean): Result<Unit>
}
