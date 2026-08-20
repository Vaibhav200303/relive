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
 * The repository contract is newest-first (see [MomentRepository.observeAll]).
 * The UI wants a WhatsApp-style chronological order (oldest at the top, newest
 * just above the composer), so this presentation layer reverses the emitted
 * list. The persistence contract is preserved untouched.
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
                    AllTimelineUiState.Loaded(
                        moments.asReversed().map { it.toPresentation() },
                    )
                }
            }
        }
    }

    fun setFavorite(id: MomentId, isFavorite: Boolean) {
        scope.launch { momentRepository.setFavorite(id, isFavorite) }
    }
}
