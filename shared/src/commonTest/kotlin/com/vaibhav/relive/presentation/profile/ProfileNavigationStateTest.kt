package com.vaibhav.relive.presentation.profile

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileNavigationStateTest {
    @Test fun timelineHomeOpensProfile() {
        assertTrue(ProfileNavigationState().openProfile().isOpen)
    }

    @Test fun backFromProfileReturnsToTimelineHome() {
        assertFalse(ProfileNavigationState(ProfileDestination.Profile).returnToTimelineHome().isOpen)
    }

    @Test fun media_storage_returns_to_profile_before_timeline_home() {
        val mediaStorage = ProfileNavigationState().openProfile().openMediaStorage()

        assertEquals(ProfileDestination.MediaStorage, mediaStorage.destination)
        assertEquals(ProfileDestination.Profile, mediaStorage.returnToProfile().destination)
    }

    @Test fun preferences_returns_to_profile_before_timeline_home() {
        val preferences = ProfileNavigationState().openProfile().openPreferences()

        assertEquals(ProfileDestination.Preferences, preferences.destination)
        assertEquals(ProfileDestination.Profile, preferences.returnToProfile().destination)
    }
}
