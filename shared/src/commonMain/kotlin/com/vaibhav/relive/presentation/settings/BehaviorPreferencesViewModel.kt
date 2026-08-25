package com.vaibhav.relive.presentation.settings

import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.model.StartDestination
import com.vaibhav.relive.domain.repository.BehaviorPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BehaviorPreferencesState(
    val preferences: BehaviorPreferences = BehaviorPreferences(),
    val errorMessage: String? = null,
)

class BehaviorPreferencesViewModel(
    private val repository: BehaviorPreferencesRepository,
    private val scope: CoroutineScope,
) {
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<BehaviorPreferencesState> = combine(repository.preferences, error) { preferences, message ->
        BehaviorPreferencesState(preferences = preferences, errorMessage = message)
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5_000),
        BehaviorPreferencesState(preferences = repository.preferences.value),
    )

    fun setStartDestination(destination: StartDestination) {
        persist { repository.setStartDestination(destination) }
    }

    fun setConfirmBeforeDiscarding(enabled: Boolean) {
        persist { repository.setConfirmBeforeDiscarding(enabled) }
    }

    fun setShowLocations(enabled: Boolean) {
        persist { repository.setShowLocations(enabled) }
    }

    fun setShowTags(enabled: Boolean) {
        persist { repository.setShowTags(enabled) }
    }

    fun setShowOnThisDay(enabled: Boolean) {
        persist { repository.setShowOnThisDay(enabled) }
    }

    fun setShowFavorites(enabled: Boolean) {
        persist { repository.setShowFavorites(enabled) }
    }

    fun clearError() {
        error.value = null
    }

    private fun persist(write: suspend () -> Result<Unit>) {
        scope.launch {
            write().onFailure { error.value = "Could not save preferences." }
        }
    }
}
