package com.vaibhav.relive.presentation.profile

/** Auxiliary Profile navigation; Timeline Home remains the retained parent. */
data class ProfileNavigationState(val destination: ProfileDestination = ProfileDestination.Closed) {
    val isOpen: Boolean get() = destination != ProfileDestination.Closed

    fun openProfile(): ProfileNavigationState = copy(destination = ProfileDestination.Profile)
    fun openMediaStorage(): ProfileNavigationState = copy(destination = ProfileDestination.MediaStorage)
    fun returnToProfile(): ProfileNavigationState = copy(destination = ProfileDestination.Profile)
    fun returnToTimelineHome(): ProfileNavigationState = copy(destination = ProfileDestination.Closed)
}

enum class ProfileDestination { Closed, Profile, MediaStorage }
