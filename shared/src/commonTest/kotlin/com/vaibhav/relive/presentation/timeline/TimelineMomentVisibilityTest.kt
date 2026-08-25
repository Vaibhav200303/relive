package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.BehaviorPreferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimelineMomentVisibilityTest {
    @Test
    fun editableTimelineUsesLocationAndTagPreferences() {
        val visibility = resolveTimelineMomentVisibility(
            mode = TimelineMode.Editable,
            preferences = BehaviorPreferences(showLocations = false, showTags = false),
        )

        assertFalse(visibility.showLocations)
        assertFalse(visibility.showTags)
    }

    @Test
    fun readOnlyCollectionsKeepMetadataVisible() {
        val visibility = resolveTimelineMomentVisibility(
            mode = TimelineMode.ReadOnlySystemCollection("Favorites"),
            preferences = BehaviorPreferences(showLocations = false, showTags = false),
        )

        assertTrue(visibility.showLocations)
        assertTrue(visibility.showTags)
    }
}
