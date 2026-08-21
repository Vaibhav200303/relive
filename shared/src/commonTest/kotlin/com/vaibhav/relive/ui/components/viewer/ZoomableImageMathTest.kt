package com.vaibhav.relive.ui.components.viewer

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZoomableImageMathTest {

    private fun approx(expected: Float, actual: Float, tol: Float = 0.01f) {
        assertTrue(abs(expected - actual) <= tol, "expected≈$expected got=$actual")
    }

    @Test
    fun `portrait image fills viewport height`() {
        val (w, h) = ZoomableImageMath.fittedSize(1000f, 2000f, 0.5f)
        approx(1000f, w); approx(2000f, h)
    }

    @Test
    fun `landscape image fills viewport width`() {
        val (w, h) = ZoomableImageMath.fittedSize(1000f, 2000f, 2f)
        approx(1000f, w); approx(500f, h)
    }

    @Test
    fun `square image in portrait viewport fills width`() {
        val (w, h) = ZoomableImageMath.fittedSize(1000f, 2000f, 1f)
        approx(1000f, w); approx(1000f, h)
    }

    @Test
    fun `square image in landscape viewport fills height`() {
        val (w, h) = ZoomableImageMath.fittedSize(2000f, 1000f, 1f)
        approx(1000f, w); approx(1000f, h)
    }

    @Test
    fun `viewport matching image aspect uses full area`() {
        val (w, h) = ZoomableImageMath.fittedSize(1600f, 900f, 1600f / 900f)
        approx(1600f, w); approx(900f, h)
    }

    @Test
    fun `degenerate inputs return zero`() {
        val (w, h) = ZoomableImageMath.fittedSize(0f, 100f, 1f)
        assertEquals(0f, w); assertEquals(0f, h)
    }

    @Test
    fun `clampScale enforces min and max`() {
        assertEquals(ZoomableImageMath.MIN_SCALE, ZoomableImageMath.clampScale(0.1f))
        assertEquals(ZoomableImageMath.MAX_SCALE, ZoomableImageMath.clampScale(99f))
        assertEquals(2f, ZoomableImageMath.clampScale(2f))
    }

    @Test
    fun `translation clamped to image bounds when zoomed`() {
        val (tx, ty) = ZoomableImageMath.clampTranslation(
            scale = 2f, viewportW = 1000f, viewportH = 2000f,
            fittedW = 1000f, fittedH = 2000f,
            translationX = 5000f, translationY = -5000f,
        )
        approx(500f, tx); approx(-1000f, ty)
    }

    @Test
    fun `translation locked to zero at fitted scale`() {
        val (tx, ty) = ZoomableImageMath.clampTranslation(
            1f, 1000f, 2000f, 1000f, 2000f, 500f, -900f,
        )
        assertEquals(0f, tx); assertEquals(0f, ty)
    }

    @Test
    fun `isZoomed true above threshold false at fit`() {
        assertFalse(ZoomableImageMath.isZoomed(1f))
        assertFalse(ZoomableImageMath.isZoomed(1.005f))
        assertTrue(ZoomableImageMath.isZoomed(1.5f))
    }

    @Test
    fun `double tap toggles fit and zoomed states`() {
        assertEquals(ZoomableImageMath.DOUBLE_TAP_SCALE, ZoomableImageMath.doubleTapTarget(1f))
        assertEquals(ZoomableImageMath.MIN_SCALE, ZoomableImageMath.doubleTapTarget(2.5f))
        assertEquals(ZoomableImageMath.MIN_SCALE, ZoomableImageMath.doubleTapTarget(4f))
    }

    @Test
    fun `pager can scroll only when image at fitted scale`() {
        // Encodes the pager-vs-image gesture rule: userScrollEnabled = !isZoomed.
        assertTrue(!ZoomableImageMath.isZoomed(1f))
        assertFalse(!ZoomableImageMath.isZoomed(1.5f))
    }
}
