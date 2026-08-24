package com.vaibhav.relive.ui.feedback

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals

class ReliveHapticsTest {
    @Test
    fun semanticCuesMapToDistinctComposeFeedbackTypes() {
        val mapped = ReliveHapticCue.entries.associateWith { it.toComposeType() }

        assertEquals(HapticFeedbackType.VirtualKey, mapped[ReliveHapticCue.Action])
        assertEquals(HapticFeedbackType.SegmentTick, mapped[ReliveHapticCue.Selection])
        assertEquals(HapticFeedbackType.ToggleOn, mapped[ReliveHapticCue.ToggleOn])
        assertEquals(HapticFeedbackType.ToggleOff, mapped[ReliveHapticCue.ToggleOff])
        assertEquals(HapticFeedbackType.ContextClick, mapped[ReliveHapticCue.Context])
        assertEquals(HapticFeedbackType.Confirm, mapped[ReliveHapticCue.Confirm])
        assertEquals(HapticFeedbackType.Reject, mapped[ReliveHapticCue.Reject])
    }
}
