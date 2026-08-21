package com.vaibhav.relive.platform.media

/**
 * Two-state flash / torch used by [CameraCaptureSurface]. Photo mode and video
 * mode share the same control: Off is the default, tap toggles Off <-> On.
 * There is no Auto state — the user picks explicitly, matching messaging-app
 * cameras.
 */
enum class FlashMode { Off, On }

/** Photo-mode toggle: Off <-> On. */
fun FlashMode.nextPhoto(): FlashMode = when (this) {
    FlashMode.Off -> FlashMode.On
    FlashMode.On -> FlashMode.Off
}

/** Video-mode torch toggle: Off <-> On. */
fun FlashMode.nextVideo(): FlashMode = when (this) {
    FlashMode.Off -> FlashMode.On
    FlashMode.On -> FlashMode.Off
}

/**
 * Normalize flash state after switching to a lens that may not have a flash
 * unit. Any non-Off state on a flashless lens is coerced to Off so the UI and
 * hardware bindings stay in sync.
 */
fun FlashMode.coercedFor(hasFlash: Boolean): FlashMode =
    if (hasFlash) this else FlashMode.Off

enum class LensFacing { Back, Front }

fun LensFacing.toggled(): LensFacing =
    if (this == LensFacing.Back) LensFacing.Front else LensFacing.Back

enum class CameraMode { Photo, Video }

/**
 * Zoom preset offered by the camera control row. Only presets the current
 * lens actually supports are surfaced — no fake 0.5× on a lens whose real
 * min-zoom-ratio is 1.0×, no fake 2× on a lens whose max-zoom-ratio is
 * below 2.0×.
 */
data class ZoomPreset(val ratio: Float, val label: String)

private val ALL_PRESETS = listOf(
    ZoomPreset(0.5f, "0.5×"),
    ZoomPreset(1f, "1×"),
    ZoomPreset(2f, "2×"),
)

/**
 * Filter the fixed preset set to what the active lens supports. Requires the
 * ratio to lie within the reported CameraX zoom range with a small tolerance
 * to account for float noise on some devices reporting e.g. 0.4999.
 */
fun availableZoomPresets(minRatio: Float, maxRatio: Float): List<ZoomPreset> {
    val tolerance = 0.01f
    return ALL_PRESETS.filter { it.ratio in (minRatio - tolerance)..(maxRatio + tolerance) }
}

/** Default zoom is 1× when the lens supports it, otherwise the reported min. */
fun defaultZoomRatio(minRatio: Float, maxRatio: Float): Float {
    if (1f in (minRatio - 0.01f)..(maxRatio + 0.01f)) return 1f
    return minRatio.coerceAtMost(maxRatio)
}

/** Clamp any zoom value (preset tap or pinch) to the lens's real range. */
fun clampZoomRatio(ratio: Float, minRatio: Float, maxRatio: Float): Float =
    ratio.coerceIn(minRatio, maxRatio)

/**
 * Which preset (if any) should render as active for the current zoom ratio.
 * A ratio close enough to a preset value snaps that preset visually; a ratio
 * mid-way (e.g. from a pinch) yields null so no preset is highlighted.
 */
fun activePresetIndex(ratio: Float, presets: List<ZoomPreset>): Int? {
    val tolerance = 0.05f
    val index = presets.indexOfFirst { kotlin.math.abs(it.ratio - ratio) <= tolerance }
    return if (index >= 0) index else null
}

/**
 * Three fixed zoom "slots" occupying the quick-zoom row. A slot represents a
 * ratio range; whichever slot the current camera ratio falls into shows the
 * exact ratio, the others show their anchor label.
 *
 *   Low  = below 1× (ultrawide)
 *   Mid  = 1× until below 2×
 *   High = 2× and above
 */
enum class ZoomSlot { Low, Mid, High }

/** Anchor value shown when a slot is inactive. */
data class ZoomSlotSpec(val slot: ZoomSlot, val anchorRatio: Float, val anchorLabel: String)

private val LOW_SPEC = ZoomSlotSpec(ZoomSlot.Low, 0.5f, "0.5×")
private val MID_SPEC = ZoomSlotSpec(ZoomSlot.Mid, 1f, "1×")
private val HIGH_SPEC = ZoomSlotSpec(ZoomSlot.High, 2f, "2×")

/**
 * Slots the current lens can actually reach. Low is offered only when the
 * lens reports minZoomRatio ≤ ~0.5; High only when maxZoomRatio ≥ ~2. Mid is
 * offered whenever the lens can hit 1× (essentially always).
 */
fun zoomSlotsFor(minRatio: Float, maxRatio: Float): List<ZoomSlotSpec> {
    val tolerance = 0.01f
    val out = mutableListOf<ZoomSlotSpec>()
    if (LOW_SPEC.anchorRatio in (minRatio - tolerance)..(maxRatio + tolerance)) out += LOW_SPEC
    if (MID_SPEC.anchorRatio in (minRatio - tolerance)..(maxRatio + tolerance)) out += MID_SPEC
    if (HIGH_SPEC.anchorRatio in (minRatio - tolerance)..(maxRatio + tolerance)) out += HIGH_SPEC
    return out
}

/**
 * Which slot the current ratio belongs to, restricted to slots actually
 * present. Returns null when no slots exist (fixed-zoom lens with no anchors
 * in range — impossible in practice because Mid always fits).
 */
fun activeZoomSlot(ratio: Float, slots: List<ZoomSlotSpec>): ZoomSlot? {
    if (slots.isEmpty()) return null
    val tolerance = 0.02f
    val hasLow = slots.any { it.slot == ZoomSlot.Low }
    val hasHigh = slots.any { it.slot == ZoomSlot.High }
    val hasMid = slots.any { it.slot == ZoomSlot.Mid }
    return when {
        ratio >= 2f - tolerance && hasHigh -> ZoomSlot.High
        ratio < 1f - tolerance && hasLow -> ZoomSlot.Low
        hasMid -> ZoomSlot.Mid
        hasHigh -> ZoomSlot.High
        else -> ZoomSlot.Low
    }
}

/**
 * Human-readable zoom label: one decimal, no trailing zero, always followed
 * by "×". Examples: 1.0 → "1×", 1.4 → "1.4×", 0.7 → "0.7×", 3.5 → "3.5×",
 * 2.03 → "2×" (rounds to nearest 0.1).
 */
fun formatZoomLabel(ratio: Float): String {
    val rounded = kotlin.math.round(ratio * 10f) / 10f
    val whole = rounded.toInt()
    return if (kotlin.math.abs(rounded - whole) < 0.05f) {
        "${whole}×"
    } else {
        // one-decimal string without relying on JVM-only String.format
        val tenths = kotlin.math.round(rounded * 10f).toInt()
        val w = tenths / 10
        val d = kotlin.math.abs(tenths % 10)
        "${w}.${d}×"
    }
}

/**
 * Label to show inside a given slot. The active slot displays the exact
 * current ratio (unless that ratio matches its anchor); every other slot
 * shows its anchor label.
 */
fun zoomSlotLabel(
    slot: ZoomSlot,
    ratio: Float,
    slots: List<ZoomSlotSpec>,
): String {
    val spec = slots.firstOrNull { it.slot == slot } ?: return ""
    val active = activeZoomSlot(ratio, slots) == slot
    if (!active) return spec.anchorLabel
    val onAnchor = kotlin.math.abs(ratio - spec.anchorRatio) < 0.05f
    return if (onAnchor) spec.anchorLabel else formatZoomLabel(ratio)
}
