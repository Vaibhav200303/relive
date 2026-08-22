package com.vaibhav.relive.ui.media

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.vaibhav.relive.platform.media.NaturalSizePx

/**
 * Pure adaptive media sizing shared by the composer and the timeline.
 *
 *   Rule A: displayWidth <= maxWidth, displayHeight <= maxHeight.
 *   Rule B: aspect ratio preserved — no stretch, no crop.
 *   Rule C: if the media's natural dp-equivalent size already fits inside
 *           both bounds, shrink-wrap around it (never enlarge to fill).
 *   Rule D: maxWidth/maxHeight are ceilings, not targets.
 *
 * Timeline- and composer-specific maximums stay at the call site; this
 * helper does not know which surface owns it.
 */

/**
 * Result of the adaptive sizing pass. [wasConstrained] is true when the
 * natural dp-equivalent size did NOT fit inside both bounds and had to be
 * scaled down to respect them. Callers use the flag to gate behavior like
 * inline timeline video playback (only allowed when the media fits
 * without clamping).
 */
data class AdaptiveMediaPreview(
    val size: DpSize,
    val wasConstrained: Boolean,
)

fun computeAdaptiveMediaPreview(
    natural: NaturalSizePx,
    maxWidth: Dp,
    maxHeight: Dp,
    density: Density,
): AdaptiveMediaPreview {
    val naturalW = with(density) { natural.widthPx.toDp() }
    val naturalH = with(density) { natural.heightPx.toDp() }
    val aspect = natural.widthPx.toFloat() / natural.heightPx.toFloat()

    if (naturalW <= maxWidth && naturalH <= maxHeight) {
        return AdaptiveMediaPreview(DpSize(naturalW, naturalH), wasConstrained = false)
    }

    var w = if (naturalW > maxWidth) maxWidth else naturalW
    var h = w / aspect
    if (h > maxHeight) {
        h = maxHeight
        w = h * aspect
    }
    return AdaptiveMediaPreview(DpSize(w, h), wasConstrained = true)
}

fun computeAdaptiveMediaPreviewSize(
    natural: NaturalSizePx,
    maxWidth: Dp,
    maxHeight: Dp,
    density: Density,
): DpSize = computeAdaptiveMediaPreview(natural, maxWidth, maxHeight, density).size

/**
 * Whether a video with the given rotation-corrected [natural] pixel size
 * is eligible for inline timeline playback. Uses the SAME adaptive sizing
 * math as the preview layout so the decision cannot drift from what the
 * user sees. Null [natural] (probe not finished / failed) returns false so
 * the tile falls back to full-screen-only playback until dimensions are
 * known.
 */
fun inlinePlaybackAllowed(
    natural: NaturalSizePx?,
    maxWidth: Dp,
    maxHeight: Dp,
    density: Density,
): Boolean {
    if (natural == null) return false
    if (natural.widthPx <= 0 || natural.heightPx <= 0) return false
    return !computeAdaptiveMediaPreview(natural, maxWidth, maxHeight, density).wasConstrained
}

/**
 * Restrained fallback bounds used while natural dimensions are still being
 * probed. Fits inside both ceilings using [fallbackAspect]; if the aspect-
 * derived width would overflow, it is clamped and the height is recomputed
 * so the box stays proportional.
 */
fun fallbackAdaptivePreviewSize(
    maxWidth: Dp,
    maxHeight: Dp,
    fallbackAspect: Float,
    fallbackHeight: Dp,
): DpSize {
    val h = fallbackHeight.coerceAtMost(maxHeight)
    val wFromAspect = h * fallbackAspect
    return if (wFromAspect > maxWidth) {
        val clampedH = (maxWidth / fallbackAspect).coerceAtMost(maxHeight)
        DpSize(maxWidth, clampedH)
    } else {
        DpSize(wFromAspect, h)
    }
}
