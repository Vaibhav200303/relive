package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.repository.ArchiveInsightsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaStorageViewModel(
    private val repository: ArchiveInsightsRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<MediaStorageState>(MediaStorageState.Loading)
    val state: StateFlow<MediaStorageState> = mutableState.asStateFlow()

    fun refresh() {
        if (mutableState.value is MediaStorageState.Loading) return
        mutableState.value = MediaStorageState.Loading
        scope.launch { load() }
    }

    fun loadOnEntry() {
        if (mutableState.value !is MediaStorageState.Loading) refresh() else scope.launch { load() }
    }

    private suspend fun load() {
        mutableState.value = runCatching { repository.load() }
            .fold(
                onSuccess = { MediaStorageState.Loaded(it) },
                onFailure = { MediaStorageState.Error },
            )
    }
}
