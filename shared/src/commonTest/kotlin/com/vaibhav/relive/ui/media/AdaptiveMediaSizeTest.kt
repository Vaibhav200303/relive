package com.vaibhav.relive.ui.media

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.platform.media.NaturalSizePx
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure host-JVM coverage of the adaptive media sizing math shared by the
 * composer and the timeline. Uses density 1.0 so 1 px == 1 dp — the
 * assertions read as size comparisons rather than density arithmetic.
 */
class AdaptiveMediaSizeTest {

    private val density = Density(density = 1f, fontScale = 1f)
    private val maxW = 360.dp
    private val maxH = 420.dp

    @Test
    fun portraitInsideBoundsShrinkWraps() {
        val size = computeAdaptiveMediaPreviewSize(NaturalSizePx(200, 300), maxW, maxH, density)
        assertEquals(200.dp, size.width)
        assertEquals(300.dp, size.height)
    }

    @Test
    fun landscapeInsideBoundsShrinkWraps() {
        val size = computeAdaptiveMediaPreviewSize(NaturalSizePx(320, 180), maxW, maxH, density)
        assertEquals(320.dp, size.width)
        assertEquals(180.dp, size.height)
    }

    @Test
    fun squareInsideBoundsShrinkWraps() {
        val size = computeAdaptiveMediaPreviewSize(NaturalSizePx(250, 250), maxW, maxH, density)
        assertEquals(250.dp, size.width)
        assertEquals(250.dp, size.height)
    }

    @Test
    fun smallMediaIsNotUpscaled() {
        val size = computeAdaptiveMediaPreviewSize(NaturalSizePx(80, 60), maxW, maxH, density)
        assertEquals(80.dp, size.width)
        assertEquals(60.dp, size.height)
    }

    @Test
    fun landscapeWiderThanMaxWidthScalesDownPreservingAspect() {
        val natural = NaturalSizePx(1600, 900)
        val size = computeAdaptiveMediaPreviewSize(natural, maxW, maxH, density)
        assertEquals(maxW, size.width)
        assertAspectPreserved(natural, size.width.value, size.height.value)
        assertTrue(size.height.value <= maxH.value + EPS)
    }

    @Test
    fun tallPortraitTallerThanMaxHeightScalesDownPreservingAspect() {
        // Portrait natural size wider than maxW AND taller than the aspect-derived height —
        // second-pass clamp should bring height down to maxH.
        val natural = NaturalSizePx(600, 2000)
        val size = computeAdaptiveMediaPreviewSize(natural, maxW, maxH, density)
        assertEquals(maxH, size.height)
        assertAspectPreserved(natural, size.width.value, size.height.value)
        assertTrue(size.width.value <= maxW.value + EPS)
    }

    @Test
    fun exactlyBoundedByBothMaxesIsKept() {
        val natural = NaturalSizePx(maxW.value.toInt(), maxH.value.toInt())
        val size = computeAdaptiveMediaPreviewSize(natural, maxW, maxH, density)
        assertEquals(maxW, size.width)
        assertEquals(maxH, size.height)
    }

    @Test
    fun fallbackHonorsAspectWhenWidthFits() {
        val size = fallbackAdaptivePreviewSize(maxWidth = maxW, maxHeight = maxH, fallbackAspect = 4f / 3f, fallbackHeight = 180.dp)
        assertEquals(180.dp, size.height)
        assertEquals(240.dp, size.width) // 180 * 4/3
    }

    @Test
    fun fallbackClampsWidthAndRecomputesHeight() {
        // aspect 4:1 forces wFromAspect = 180*4 = 720dp > maxW(360). Clamp to maxW, recompute h = 90dp.
        val size = fallbackAdaptivePreviewSize(maxWidth = maxW, maxHeight = maxH, fallbackAspect = 4f, fallbackHeight = 180.dp)
        assertEquals(maxW, size.width)
        assertEquals(90.dp, size.height)
    }

    @Test
    fun fallbackHeightCappedByMaxHeight() {
        val size = fallbackAdaptivePreviewSize(maxWidth = maxW, maxHeight = 120.dp, fallbackAspect = 1f, fallbackHeight = 500.dp)
        assertEquals(120.dp, size.height)
        assertEquals(120.dp, size.width)
    }

    /**
     * A rotated portrait video is delivered by [rememberVideoNaturalSizeFor]
     * with swapped dimensions (rotation already applied). This test locks
     * that contract: adaptive sizing operates on the swapped natural size,
     * so a 1080x1920 portrait clip is treated as portrait, not landscape.
     */
    @Test
    fun rotatedVideoIsSizedAsPortrait() {
        val portrait = NaturalSizePx(1080, 1920)
        val size = computeAdaptiveMediaPreviewSize(portrait, maxW, maxH, density)
        assertTrue(size.height.value >= size.width.value, "expected portrait bounds, got $size")
        assertAspectPreserved(portrait, size.width.value, size.height.value)
        assertTrue(size.width.value <= maxW.value + EPS)
        assertTrue(size.height.value <= maxH.value + EPS)
    }

    // --- Inline-video eligibility (Case A vs Case B) --------------------

    @Test
    fun previewIsUnconstrainedWhenNaturalFitsBothBounds() {
        val preview = computeAdaptiveMediaPreview(NaturalSizePx(320, 240), maxW, maxH, density)
        assertFalse(preview.wasConstrained)
        assertEquals(320.dp, preview.size.width)
        assertEquals(240.dp, preview.size.height)
    }

    @Test
    fun previewIsConstrainedWhenWidthExceedsMax() {
        val preview = computeAdaptiveMediaPreview(NaturalSizePx(1600, 400), maxW, maxH, density)
        assertTrue(preview.wasConstrained)
        assertEquals(maxW, preview.size.width)
    }

    @Test
    fun previewIsConstrainedWhenHeightExceedsMax() {
        val preview = computeAdaptiveMediaPreview(NaturalSizePx(200, 1200), maxW, maxH, density)
        assertTrue(preview.wasConstrained)
        assertTrue(preview.size.height.value <= maxH.value + EPS)
    }

    @Test
    fun inlineAllowedWhenLandscapeFitsBothBounds() {
        // 320x180 sits inside both 360dp and 420dp at density 1 — no clamp.
        assertTrue(inlinePlaybackAllowed(NaturalSizePx(320, 180), maxW, maxH, density))
    }

    @Test
    fun inlineAllowedWhenPortraitFitsBothBounds() {
        // Rotation-corrected portrait size — same helper caller uses for images.
        assertTrue(inlinePlaybackAllowed(NaturalSizePx(240, 400), maxW, maxH, density))
    }

    @Test
    fun inlineDeniedWhenWidthExceedsMax() {
        assertFalse(inlinePlaybackAllowed(NaturalSizePx(1920, 200), maxW, maxH, density))
    }

    @Test
    fun inlineDeniedWhenHeightExceedsMax() {
        assertFalse(inlinePlaybackAllowed(NaturalSizePx(200, 800), maxW, maxH, density))
    }

    @Test
    fun inlineDeniedWhenPortraitTallerThanMaxHeight() {
        // Rotated portrait: rememberVideoNaturalSizeFor already swaps.
        assertFalse(inlinePlaybackAllowed(NaturalSizePx(1080, 1920), maxW, maxH, density))
    }

    @Test
    fun inlineDeniedWhenLandscapeWiderThanMaxWidth() {
        assertFalse(inlinePlaybackAllowed(NaturalSizePx(1920, 1080), maxW, maxH, density))
    }

    @Test
    fun inlineDeniedWhenNaturalSizeUnknown() {
        assertFalse(inlinePlaybackAllowed(null, maxW, maxH, density))
        assertNull(null) // trivial to prove import used; keeps failure surface obvious
    }

    @Test
    fun inlineDeniedForDegenerateDimensions() {
        assertFalse(inlinePlaybackAllowed(NaturalSizePx(0, 0), maxW, maxH, density))
        assertFalse(inlinePlaybackAllowed(NaturalSizePx(320, 0), maxW, maxH, density))
    }

    @Test
    fun inlineAllowedAtExactMaximumBounds() {
        val exact = NaturalSizePx(maxW.value.toInt(), maxH.value.toInt())
        assertTrue(inlinePlaybackAllowed(exact, maxW, maxH, density))
    }

    private fun assertAspectPreserved(natural: NaturalSizePx, wDp: Float, hDp: Float) {
        val naturalAspect = natural.widthPx.toFloat() / natural.heightPx.toFloat()
        val actualAspect = wDp / hDp
        assertTrue(
            abs(naturalAspect - actualAspect) < ASPECT_TOLERANCE,
            "aspect drift: natural=$naturalAspect actual=$actualAspect",
        )
    }

    companion object {
        private const val EPS = 0.01f
        private const val ASPECT_TOLERANCE = 0.005f
    }
}
