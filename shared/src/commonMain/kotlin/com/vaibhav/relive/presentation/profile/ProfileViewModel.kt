package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    repository: ProfileRepository,
    scope: CoroutineScope,
) {
    val state: StateFlow<ProfileState> = repository.observeProfile()
        .map { it.toProfileState() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), ProfileState())
}
