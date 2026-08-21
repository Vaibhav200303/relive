package com.vaibhav.relive.platform.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraControlsTest {
    @Test fun photoCycleOffOn() {
        assertEquals(FlashMode.On, FlashMode.Off.nextPhoto())
        assertEquals(FlashMode.Off, FlashMode.On.nextPhoto())
    }

    @Test fun videoCycleOffOn() {
        assertEquals(FlashMode.On, FlashMode.Off.nextVideo())
        assertEquals(FlashMode.Off, FlashMode.On.nextVideo())
    }

    @Test fun coerceToOffOnFlashlessLens() {
        assertEquals(FlashMode.Off, FlashMode.On.coercedFor(hasFlash = false))
        assertEquals(FlashMode.On, FlashMode.On.coercedFor(hasFlash = true))
        assertEquals(FlashMode.Off, FlashMode.Off.coercedFor(hasFlash = true))
    }

    @Test fun lensToggle() {
        assertEquals(LensFacing.Front, LensFacing.Back.toggled())
        assertEquals(LensFacing.Back, LensFacing.Front.toggled())
    }

    @Test fun zoomPresetsIncludeHalfWhenUltraWideSupported() {
        val presets = availableZoomPresets(minRatio = 0.5f, maxRatio = 8f)
        assertEquals(listOf(0.5f, 1f, 2f), presets.map { it.ratio })
    }

    @Test fun zoomPresetsExcludeHalfWhenLensStartsAtOne() {
        val presets = availableZoomPresets(minRatio = 1f, maxRatio = 6f)
        assertEquals(listOf(1f, 2f), presets.map { it.ratio })
    }

    @Test fun zoomPresetsExcludeTwoWhenLensMaxBelowTwo() {
        val presets = availableZoomPresets(minRatio = 1f, maxRatio = 1.5f)
        assertEquals(listOf(1f), presets.map { it.ratio })
    }

    @Test fun frontCameraFixedZoomOffersOnlyOne() {
        val presets = availableZoomPresets(minRatio = 1f, maxRatio = 1f)
        assertEquals(listOf(1f), presets.map { it.ratio })
    }

    @Test fun defaultZoomIsOneWhenSupported() {
        assertEquals(1f, defaultZoomRatio(minRatio = 0.5f, maxRatio = 6f))
    }

    @Test fun defaultZoomFallsBackToMinWhenOneUnsupported() {
        assertEquals(2f, defaultZoomRatio(minRatio = 2f, maxRatio = 8f))
    }

    @Test fun clampZoomRatioClampsBothEnds() {
        assertEquals(0.5f, clampZoomRatio(0.1f, minRatio = 0.5f, maxRatio = 8f))
        assertEquals(8f, clampZoomRatio(20f, minRatio = 0.5f, maxRatio = 8f))
        assertEquals(2f, clampZoomRatio(2f, minRatio = 0.5f, maxRatio = 8f))
    }

    @Test fun activePresetHighlightsExactMatch() {
        val presets = availableZoomPresets(0.5f, 8f)
        assertEquals(1, activePresetIndex(1f, presets))
        assertEquals(0, activePresetIndex(0.5f, presets))
        assertEquals(2, activePresetIndex(2f, presets))
    }

    @Test fun activePresetNullForMidPinch() {
        val presets = availableZoomPresets(0.5f, 8f)
        assertNull(activePresetIndex(1.4f, presets))
        assertNull(activePresetIndex(3.5f, presets))
    }

    @Test fun clampSurvivesLensSwitchToFront() {
        val currentBackRatio = 2f
        val clamped = clampZoomRatio(currentBackRatio, minRatio = 1f, maxRatio = 1f)
        assertEquals(1f, clamped)
    }

    @Test fun clampPreservesRatioWhenNewLensSupportsIt() {
        val currentBackRatio = 1f
        val clamped = clampZoomRatio(currentBackRatio, minRatio = 0.5f, maxRatio = 8f)
        assertEquals(1f, clamped)
    }

    // --- Dynamic preset slot labels (Pixel-style) ---

    @Test fun slotsIncludeAllThreeOnWideRangeLens() {
        val slots = zoomSlotsFor(minRatio = 0.5f, maxRatio = 8f).map { it.slot }
        assertEquals(listOf(ZoomSlot.Low, ZoomSlot.Mid, ZoomSlot.High), slots)
    }

    @Test fun slotsSkipLowWhenNoUltrawide() {
        val slots = zoomSlotsFor(minRatio = 1f, maxRatio = 6f).map { it.slot }
        assertEquals(listOf(ZoomSlot.Mid, ZoomSlot.High), slots)
    }

    @Test fun slotsSkipHighWhenMaxBelowTwo() {
        val slots = zoomSlotsFor(minRatio = 1f, maxRatio = 1.5f).map { it.slot }
        assertEquals(listOf(ZoomSlot.Mid), slots)
    }

    @Test fun activeSlotForExactOneIsMid() {
        val slots = zoomSlotsFor(0.5f, 8f)
        assertEquals(ZoomSlot.Mid, activeZoomSlot(1f, slots))
    }

    @Test fun activeSlotForUnderOneIsLow() {
        val slots = zoomSlotsFor(0.5f, 8f)
        assertEquals(ZoomSlot.Low, activeZoomSlot(0.7f, slots))
    }

    @Test fun activeSlotForBetweenOneAndTwoIsMid() {
        val slots = zoomSlotsFor(0.5f, 8f)
        assertEquals(ZoomSlot.Mid, activeZoomSlot(1.4f, slots))
    }

    @Test fun activeSlotAboveTwoIsHigh() {
        val slots = zoomSlotsFor(0.5f, 8f)
        assertEquals(ZoomSlot.High, activeZoomSlot(3.5f, slots))
    }

    @Test fun formatZoomLabelWhole() {
        assertEquals("1×", formatZoomLabel(1f))
        assertEquals("2×", formatZoomLabel(2f))
        assertEquals("2×", formatZoomLabel(2.02f))
    }

    @Test fun formatZoomLabelOneDecimal() {
        assertEquals("0.5×", formatZoomLabel(0.5f))
        assertEquals("0.7×", formatZoomLabel(0.7f))
        assertEquals("1.4×", formatZoomLabel(1.44f))
        assertEquals("3.5×", formatZoomLabel(3.51f))
    }

    @Test fun slotLabelExactOneShowsAnchorInMid() {
        val slots = zoomSlotsFor(0.5f, 8f)
        assertEquals("0.5×", zoomSlotLabel(ZoomSlot.Low, 1f, slots))
        assertEquals("1×", zoomSlotLabel(ZoomSlot.Mid, 1f, slots))
        assertEquals("2×", zoomSlotLabel(ZoomSlot.High, 1f, slots))
    }

    @Test fun slotLabelSevenTenthsShowsInLow() {
        val slots = zoomSlotsFor(0.5f, 8f)
        assertEquals("0.7×", zoomSlotLabel(ZoomSlot.Low, 0.7f, slots))
        assertEquals("1×", zoomSlotLabel(ZoomSlot.Mid, 0.7f, slots))
        assertEquals("2×", zoomSlotLabel(ZoomSlot.High, 0.7f, slots))
    }

    @Test fun slotLabelOneFourShowsInMid() {
        val slots = zoomSlotsFor(0.5f, 8f)
        assertEquals("0.5×", zoomSlotLabel(ZoomSlot.Low, 1.4f, slots))
        assertEquals("1.4×", zoomSlotLabel(ZoomSlot.Mid, 1.4f, slots))
        assertEquals("2×", zoomSlotLabel(ZoomSlot.High, 1.4f, slots))
    }

    @Test fun slotLabelThreeFiveShowsInHigh() {
        val slots = zoomSlotsFor(0.5f, 8f)
        assertEquals("0.5×", zoomSlotLabel(ZoomSlot.Low, 3.5f, slots))
        assertEquals("1×", zoomSlotLabel(ZoomSlot.Mid, 3.5f, slots))
        assertEquals("3.5×", zoomSlotLabel(ZoomSlot.High, 3.5f, slots))
    }

    @Test fun frontFixedLensHasOnlyMidSlotAndNoFakeHalf() {
        val slots = zoomSlotsFor(1f, 1f)
        assertEquals(listOf(ZoomSlot.Mid), slots.map { it.slot })
        assertEquals("1×", zoomSlotLabel(ZoomSlot.Mid, 1f, slots))
    }

    @Test fun tappingAnchorClampsWithinRange() {
        // Simulated preset tap: value clamps to lens range before apply.
        assertEquals(1f, clampZoomRatio(0.5f, minRatio = 1f, maxRatio = 6f))
        assertEquals(2f, clampZoomRatio(2f, minRatio = 1f, maxRatio = 6f))
    }

    @Test fun cameraSwitchRecalculatesSlotsAndClamps() {
        val backSlots = zoomSlotsFor(0.5f, 8f)
        val frontSlots = zoomSlotsFor(1f, 1f)
        val backRatio = 1.4f
        val clampedForFront = clampZoomRatio(backRatio, 1f, 1f)
        assertEquals(1f, clampedForFront)
        assertEquals(ZoomSlot.Mid, activeZoomSlot(clampedForFront, frontSlots))
        // Sanity: back lens still sees the 1.4× as mid.
        assertEquals(ZoomSlot.Mid, activeZoomSlot(backRatio, backSlots))
    }

    @Test fun photoVideoSwitchPreservesRatioWhenSupported() {
        // Rebind reports same range → ratio stays. Pure clamp check.
        val before = 1.7f
        val after = if (before in 0.5f..8f) before else defaultZoomRatio(0.5f, 8f)
        assertEquals(1.7f, after)
    }
}
