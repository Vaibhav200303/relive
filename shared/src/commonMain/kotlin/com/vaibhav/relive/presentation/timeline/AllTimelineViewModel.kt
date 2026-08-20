package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.repository.MomentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the built-in All timeline. Observes [MomentRepository.observeAll]
 * and republishes the derived UI state; favorite toggles delegate straight to the
 * repository so the observed flow re-emits with the updated row.
 *
 * The state holder receives its own [CoroutineScope] rather than extending
 * `androidx.lifecycle.ViewModel`, so it composes cleanly with `rememberCoroutineScope`
 * in Compose Multiplatform without pulling the AndroidX ViewModel factory plumbing
 * into shared code. Cancelling [scope] tears down the observation.
 */
class AllTimelineViewModel(
    private val momentRepository: MomentRepository,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<AllTimelineUiState>(AllTimelineUiState.Loading)
    val state: StateFlow<AllTimelineUiState> = _state.asStateFlow()

    init {
        scope.launch {
            momentRepository.observeAll().collect { moments ->
                _state.value = if (moments.isEmpty()) {
                    AllTimelineUiState.Empty
                } else {
                    AllTimelineUiState.Loaded(moments.map { it.toPresentation() })
                }
            }
        }
    }

    fun setFavorite(id: MomentId, isFavorite: Boolean) {
        scope.launch { momentRepository.setFavorite(id, isFavorite) }
    }
}
