package com.vaibhav.relive.presentation.profile

/** Auxiliary Profile navigation; Timeline Home remains the retained parent. */
data class ProfileNavigationState(
    val destination: ProfileDestination = ProfileDestination.Closed,
    private val upgradeReturnDestination: ProfileDestination = ProfileDestination.Profile,
) {
    val isOpen: Boolean get() = destination != ProfileDestination.Closed

    fun openProfile(): ProfileNavigationState = copy(destination = ProfileDestination.Profile)
    fun openPreferences(): ProfileNavigationState = copy(destination = ProfileDestination.Preferences)
    fun openMediaStorage(): ProfileNavigationState = copy(destination = ProfileDestination.MediaStorage)
    fun openBackupRestore(): ProfileNavigationState = copy(destination = ProfileDestination.BackupRestore)
    /** Opens Pro without losing the screen that presented the upgrade gate. */
    fun openUpgrade(
        returnTo: ProfileDestination = ProfileDestination.Profile,
    ): ProfileNavigationState = copy(
        destination = ProfileDestination.Upgrade,
        upgradeReturnDestination = returnTo,
    )
    fun openLocation() = copy(destination = ProfileDestination.Location)
    fun openNotifications() = copy(destination = ProfileDestination.RediscoverNotifications)
    fun openPrivacy() = copy(destination = ProfileDestination.PrivacySecurity)
    fun openHelp() = copy(destination = ProfileDestination.HelpFeedback)
    fun openAbout() = copy(destination = ProfileDestination.AboutRelive)
    fun openLicenses() = copy(destination = ProfileDestination.Licenses)
    fun returnToProfile(): ProfileNavigationState = copy(destination = ProfileDestination.Profile)
    fun returnFromUpgrade(): ProfileNavigationState = copy(destination = upgradeReturnDestination)
    fun returnToTimelineHome(): ProfileNavigationState = copy(destination = ProfileDestination.Closed)
}

enum class ProfileDestination { Closed, Profile, Preferences, MediaStorage, BackupRestore, Upgrade, Location, RediscoverNotifications, PrivacySecurity, HelpFeedback, AboutRelive, Licenses }
