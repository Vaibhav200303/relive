package com.vaibhav.relive.presentation.profile

/** Auxiliary Profile navigation; Timeline Home remains the retained parent. */
data class ProfileNavigationState(val isOpen: Boolean = false) {
    fun openProfile(): ProfileNavigationState = copy(isOpen = true)
    fun returnToTimelineHome(): ProfileNavigationState = copy(isOpen = false)
}
