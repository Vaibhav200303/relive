package com.vaibhav.relive.presentation.viewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaSelectionStateTest {
    @Test
    fun toggleStartsAndEndsSelectionMode() {
        val selected = MediaSelectionState().toggle(2)
        assertTrue(selected.isActive)
        assertEquals(setOf(2), selected.selectedIndices)
        assertFalse(selected.toggle(2).isActive)
    }

    @Test
    fun selectAllUsesAvailableIndices() {
        assertEquals(setOf(0, 1, 2), MediaSelectionState().selectAll(3).selectedIndices)
    }
}
