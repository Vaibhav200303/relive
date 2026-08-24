package com.vaibhav.relive.ui.components.composer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.presentation.composer.LiveRecording
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Compact inline recorder shown above the composer while a recording is
 * active. Renders the live amplitude waveform, live duration, a square stop
 * (`■`, icon-only, no "Stop" label per §Mic) and a top-right cancel `×`.
 */
@Composable
internal fun LiveRecorderCard(
    recording: LiveRecording,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.radii.md))
            .background(colors.surfaceCard)
            .border(
                width = dims.stroke.hairline,
                color = colors.borderMuted,
                shape = RoundedCornerShape(dims.radii.md),
            )
            .padding(dims.spacing.md),
    ) {
        StopSquare(onStop = onStop)
        WaveformCanvas(
            amplitudes = recording.amplitudes,
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
        )
        Text(
            text = formatDuration(recording.durationMs),
            style = type.eyebrow,
            color = colors.textSecondary,
        )
        IconButton(
            onClick = {
                haptics.perform(ReliveHapticCue.Action)
                onCancel()
            },
            modifier = Modifier
                .size(dims.minTouchTarget)
                .semantics { contentDescription = "Discard recording" },
        ) {
            CloseGlyph(size = dims.icon.md, color = colors.textMuted, strokeWidth = dims.stroke.icon)
        }
    }
}

@Composable
private fun StopSquare(onStop: () -> Unit) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    IconButton(
        onClick = {
            haptics.perform(ReliveHapticCue.ToggleOff)
            onStop()
        },
        modifier = Modifier
            .size(dims.minTouchTarget)
            .semantics { contentDescription = "Stop recording" },
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.accent),
        )
    }
}

@Composable
private fun WaveformCanvas(amplitudes: List<Float>, modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val stroke = dims.stroke.icon
    Canvas(modifier = modifier) {
        if (amplitudes.isEmpty()) return@Canvas
        val gap = 3f
        val barW = 2f
        val count = ((size.width + gap) / (barW + gap)).toInt().coerceAtMost(amplitudes.size)
        val start = amplitudes.size - count
        val cy = size.height / 2f
        for (i in 0 until count) {
            val amp = amplitudes[start + i].coerceIn(0f, 1f)
            val h = (size.height * amp).coerceAtLeast(2f)
            val x = i * (barW + gap)
            drawRect(
                color = colors.accent,
                topLeft = Offset(x, cy - h / 2f),
                size = Size(barW, h),
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    val ss = if (s < 10) "0$s" else s.toString()
    return "$m:$ss"
}
