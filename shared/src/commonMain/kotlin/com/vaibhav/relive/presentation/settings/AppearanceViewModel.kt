package com.vaibhav.relive.presentation.settings

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.repository.AppearanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearanceViewModel(
    private val repository: AppearanceRepository,
    private val scope: CoroutineScope,
) {
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<AppearanceState> = combine(repository.preferences, error) { preferences, message ->
        AppearanceState(preferences = preferences, errorMessage = message)
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5_000),
        AppearanceState(preferences = repository.preferences.value),
    )

    fun setMode(mode: AppearanceMode) {
        scope.launch {
            repository.setMode(mode).onFailure {
                error.value = "Could not save appearance."
            }
        }
    }

    fun setDefaultTheme(theme: ThemeReference) {
        scope.launch {
            repository.setDefaultTheme(theme).onFailure {
                error.value = "Could not save appearance."
            }
        }
    }

    fun clearError() {
        error.value = null
    }
}
