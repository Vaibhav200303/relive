package com.vaibhav.relive.ui.components.viewer

/**
 * Pure math for the full-screen image viewer (ADR-0019 §5). Separated from
 * the Composable so fit / zoom / pan clamping can be verified deterministically
 * without a UI toolkit dependency.
 */
object ZoomableImageMath {

    const val MIN_SCALE: Float = 1f
    const val MAX_SCALE: Float = 4f
    const val DOUBLE_TAP_SCALE: Float = 2.5f
    const val ZOOM_EPSILON: Float = 0.01f

    /**
     * Fitted image size inside a viewport, preserving [imageAspect]
     * (width/height). Returns 0f/0f on any degenerate input.
     */
    fun fittedSize(
        viewportW: Float,
        viewportH: Float,
        imageAspect: Float,
    ): Pair<Float, Float> {
        if (viewportW <= 0f || viewportH <= 0f || imageAspect <= 0f) return 0f to 0f
        val viewportAspect = viewportW / viewportH
        return if (imageAspect >= viewportAspect) {
            viewportW to viewportW / imageAspect
        } else {
            viewportH * imageAspect to viewportH
        }
    }

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /**
     * Clamp translation so the scaled image cannot be dragged past its own
     * bounds. When the scaled image is smaller than the viewport on an axis,
     * that axis is locked to 0 (centered).
     */
    fun clampTranslation(
        scale: Float,
        viewportW: Float,
        viewportH: Float,
        fittedW: Float,
        fittedH: Float,
        translationX: Float,
        translationY: Float,
    ): Pair<Float, Float> {
        val scaledW = fittedW * scale
        val scaledH = fittedH * scale
        val maxX = ((scaledW - viewportW) / 2f).coerceAtLeast(0f)
        val maxY = ((scaledH - viewportH) / 2f).coerceAtLeast(0f)
        // `+ 0f` normalises -0f to +0f so bit-exact equality checks stay clean.
        val tx = (translationX.coerceIn(-maxX, maxX)) + 0f
        val ty = (translationY.coerceIn(-maxY, maxY)) + 0f
        return tx to ty
    }

    fun isZoomed(scale: Float): Boolean = scale > MIN_SCALE + ZOOM_EPSILON

    /** Double-tap toggles between fitted and a useful zoom level. */
    fun doubleTapTarget(current: Float): Float =
        if (isZoomed(current)) MIN_SCALE else DOUBLE_TAP_SCALE
}
