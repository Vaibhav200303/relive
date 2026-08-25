package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.repository.ProfileRepository
import com.vaibhav.relive.domain.repository.ProfileSettingsRepository
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.platform.media.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    repository: ProfileRepository,
    private val settingsRepository: ProfileSettingsRepository,
    private val mediaStore: MediaStore,
    private val scope: CoroutineScope,
) {
    val state: StateFlow<ProfileState> = combine(repository.observeProfile(), settingsRepository.settings) { profile, settings ->
        profile.toProfileState(settings)
    }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), ProfileState())

    fun saveDisplayName(value: String, onResult: (Boolean) -> Unit = {}) {
        val valid = validatedDisplayName(value) ?: run { onResult(false); return }
        scope.launch { onResult(settingsRepository.setDisplayName(valid).isSuccess) }
    }

    fun setProfilePhoto(value: MediaStorageRef?, onResult: (Boolean) -> Unit = {}) {
        val previous = settingsRepository.settings.value.profilePhoto
        scope.launch {
            val success = settingsRepository.setProfilePhoto(value).isSuccess
            if (success && previous != null && previous != value) mediaStore.delete(previous)
            if (!success && value != null && value != previous) mediaStore.delete(value)
            onResult(success)
        }
    }

    companion object { const val MAX_NAME_LENGTH = 60 }
}

internal fun validatedDisplayName(value: String): String? = value.trim().takeIf { it.isNotEmpty() && it.length <= ProfileViewModel.MAX_NAME_LENGTH }
