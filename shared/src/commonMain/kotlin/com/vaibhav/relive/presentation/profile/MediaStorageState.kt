package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.model.ArchiveInsights

sealed interface MediaStorageState {
    data object Loading : MediaStorageState
    data class Loaded(val insights: ArchiveInsights) : MediaStorageState
    data object Error : MediaStorageState
}
