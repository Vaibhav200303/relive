package com.vaibhav.relive.presentation.profile

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileNavigationStateTest {
    @Test fun timelineHomeOpensProfile() {
        assertTrue(ProfileNavigationState().openProfile().isOpen)
    }

    @Test fun backFromProfileReturnsToTimelineHome() {
        assertFalse(ProfileNavigationState(isOpen = true).returnToTimelineHome().isOpen)
    }
}
