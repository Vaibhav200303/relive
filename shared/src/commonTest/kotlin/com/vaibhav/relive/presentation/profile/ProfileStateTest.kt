package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.model.ProfileSnapshot
import com.vaibhav.relive.domain.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileStateTest {
    @Test fun snapshotMapsToProfileState() {
        val state = ProfileSnapshot(Instant(123L), 4, 2, 3).toProfileState()

        assertEquals(Instant(123L), state.joiningDate)
        assertEquals(4, state.momentCount)
        assertEquals(2, state.customTimelineCount)
        assertEquals(3, state.placeCount)
    }

    @Test fun unknownMigratedJoiningDateStaysAbsent() {
        assertNull(ProfileSnapshot(null, 0, 0, 0).toProfileState().joiningDate)
    }
}
