package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.MomentTheme
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.repository.TimelineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimelineThemeState(
    val appearance: TimelineAppearance = TimelineAppearance(),
    val isLoading: Boolean = true,
)

/** State for the timeline-owned appearance editor. It deliberately has no app-theme dependency. */
class TimelineThemeViewModel(
    private val timelineRepository: TimelineRepository,
    private val timelineId: TimelineId,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TimelineThemeState())
    val state: StateFlow<TimelineThemeState> = _state.asStateFlow()

    init {
        scope.launch {
            timelineRepository.observeCustom().collect { timelines ->
                val appearance = timelines.firstOrNull { it.id == timelineId }?.appearance ?: return@collect
                _state.update { TimelineThemeState(appearance = appearance, isLoading = false) }
            }
        }
    }

    fun selectWallpaper(wallpaper: TimelineWallpaper) = updateAppearance { copy(wallpaper = wallpaper) }

    fun selectMomentTheme(momentTheme: MomentTheme) = updateAppearance { copy(momentTheme = momentTheme) }

    private fun updateAppearance(transform: TimelineAppearance.() -> TimelineAppearance) {
        val updated = transform(_state.value.appearance)
        if (updated == _state.value.appearance) return
        _state.update { it.copy(appearance = updated, isLoading = false) }
        scope.launch { timelineRepository.updateAppearance(timelineId, updated) }
    }
}
