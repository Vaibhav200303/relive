package com.vaibhav.relive.ui.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Calm, semantic haptic vocabulary shared by Relive interactions. */
enum class ReliveHapticCue {
    Action,
    Selection,
    ToggleOn,
    ToggleOff,
    Context,
    Confirm,
    Reject,
}

internal fun ReliveHapticCue.toComposeType(): HapticFeedbackType = when (this) {
    ReliveHapticCue.Action -> HapticFeedbackType.VirtualKey
    ReliveHapticCue.Selection -> HapticFeedbackType.SegmentTick
    ReliveHapticCue.ToggleOn -> HapticFeedbackType.ToggleOn
    ReliveHapticCue.ToggleOff -> HapticFeedbackType.ToggleOff
    ReliveHapticCue.Context -> HapticFeedbackType.ContextClick
    ReliveHapticCue.Confirm -> HapticFeedbackType.Confirm
    ReliveHapticCue.Reject -> HapticFeedbackType.Reject
}

@Stable
class ReliveHaptics internal constructor(private val feedback: HapticFeedback) {
    fun perform(cue: ReliveHapticCue) {
        feedback.performHapticFeedback(cue.toComposeType())
    }
}

@Composable
fun rememberReliveHaptics(): ReliveHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { ReliveHaptics(feedback) }
}
