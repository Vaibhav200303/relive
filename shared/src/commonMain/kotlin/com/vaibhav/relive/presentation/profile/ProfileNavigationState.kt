package com.vaibhav.relive.presentation.profile

/** Auxiliary Profile navigation; Timeline Home remains the retained parent. */
data class ProfileNavigationState(val destination: ProfileDestination = ProfileDestination.Closed) {
    val isOpen: Boolean get() = destination != ProfileDestination.Closed

    fun openProfile(): ProfileNavigationState = copy(destination = ProfileDestination.Profile)
    fun openPreferences(): ProfileNavigationState = copy(destination = ProfileDestination.Preferences)
    fun openMediaStorage(): ProfileNavigationState = copy(destination = ProfileDestination.MediaStorage)
    fun openBackupRestore(): ProfileNavigationState = copy(destination = ProfileDestination.BackupRestore)
    fun openLocation() = copy(destination = ProfileDestination.Location)
    fun openNotifications() = copy(destination = ProfileDestination.RediscoverNotifications)
    fun openPrivacy() = copy(destination = ProfileDestination.PrivacySecurity)
    fun openHelp() = copy(destination = ProfileDestination.HelpFeedback)
    fun openAbout() = copy(destination = ProfileDestination.AboutRelive)
    fun openLicenses() = copy(destination = ProfileDestination.Licenses)
    fun returnToProfile(): ProfileNavigationState = copy(destination = ProfileDestination.Profile)
    fun returnToTimelineHome(): ProfileNavigationState = copy(destination = ProfileDestination.Closed)
}

enum class ProfileDestination { Closed, Profile, Preferences, MediaStorage, BackupRestore, Location, RediscoverNotifications, PrivacySecurity, HelpFeedback, AboutRelive, Licenses }
