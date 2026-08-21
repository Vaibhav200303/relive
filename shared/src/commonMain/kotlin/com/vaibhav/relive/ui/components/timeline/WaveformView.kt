package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.vaibhav.relive.platform.media.WaveformProcessor
import kotlin.math.max

/**
 * Compact rounded-capsule waveform for a saved audio recording (ADR-0019
 * §4, revised). White vertical capsule segments centered on the black
 * media canvas — symmetric around the horizontal midline, uniform width,
 * amplitude drives height only. Silence renders as a tiny centered dot
 * (never blank), loud peaks fill the vertical space.
 *
 * Playback is expressed as a horizontal window into the real envelope:
 * [progress] selects which contiguous slice of buckets is visible.
 * Advancing playback shifts the visible slice left, so past waveform
 * exits toward the left and future waveform enters from the right. When
 * paused the window is frozen at the last progress. No per-segment
 * animation, no equalizer bounce — segment heights come only from real
 * envelope data.
 *
 * [envelope] null/empty → all segments render at their minimum height so
 * the tile still reads as an audio surface without fabricating amplitude.
 */
@Composable
fun WaveformView(
    envelope: FloatArray?,
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    preferredSegments: Int = 15,
    segmentWidthPx: Float = 4f,
    segmentGapPx: Float = 5f,
    minSegmentHeightPx: Float = 4f,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val n = WaveformProcessor.fitSegments(w, segmentWidthPx, segmentGapPx, preferredSegments)
        if (n <= 0) return@Canvas
        val window = WaveformProcessor.window(envelope, n, progress)
        val mid = h / 2f
        val halfCap = minSegmentHeightPx / 2f
        val fullHalf = max(halfCap, h / 2f - halfCap)
        val totalW = n * segmentWidthPx + (n - 1) * segmentGapPx
        val startX = (w - totalW) / 2f
        val corner = CornerRadius(segmentWidthPx / 2f, segmentWidthPx / 2f)
        for (i in 0 until n) {
            val amp = window[i].coerceIn(0f, 1f)
            val segHalf = max(halfCap, amp * fullHalf)
            val segH = segHalf * 2f
            val x = startX + i * (segmentWidthPx + segmentGapPx)
            val y = mid - segHalf
            drawRoundRect(
                color = color.copy(alpha = edgeAlpha(i, n)),
                topLeft = Offset(x, y),
                size = Size(segmentWidthPx, segH),
                cornerRadius = corner,
            )
        }
    }
}

/**
 * Subtle fade toward the left/right edges of the window so the flowing
 * waveform feels like segments enter and exit rather than snapping at the
 * viewport boundary. Uniform alpha across the middle band.
 */
private fun edgeAlpha(i: Int, n: Int): Float {
    if (n <= 1) return 1f
    val t = i.toFloat() / (n - 1).toFloat()
    val fadeZone = 0.18f
    val floor = 0.35f
    return when {
        t < fadeZone -> floor + (1f - floor) * (t / fadeZone)
        t > 1f - fadeZone -> floor + (1f - floor) * ((1f - t) / fadeZone)
        else -> 1f
    }
}
