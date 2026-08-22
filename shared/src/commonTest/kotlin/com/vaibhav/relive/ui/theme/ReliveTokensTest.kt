package com.vaibhav.relive.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReliveTokensTest {

    @Test
    fun warmJournalColorsMatchDesignSystem() {
        val c = WarmJournalColors
        assertEquals(Color(0xFFF6F4F0), c.bgCanvas)
        assertEquals(Color(0xFF3C3633), c.textPrimary)
        assertEquals(Color(0xFF6F4E37), c.accent)
        assertEquals(Color(0xFFEFECE5), c.surfaceCard)
        assertEquals(Color(0xFFD5CDBF), c.border)
        assertEquals(Color.White, c.textOnAccent)
    }

    @Test
    fun opacityDerivedColorsUseSharedScale() {
        // Color.copy quantizes alpha to 8 bits, so compare within one channel step.
        val tolerance = 1f / 255f
        val c = WarmJournalColors
        assertEquals(ReliveOpacity.VeryHigh, c.bgHeader.alpha, tolerance)
        assertEquals(ReliveOpacity.High, c.textSecondary.alpha, tolerance)
        assertEquals(ReliveOpacity.Medium, c.textMuted.alpha, tolerance)
        assertEquals(ReliveOpacity.High, c.accentMuted.alpha, tolerance)
        assertEquals(ReliveOpacity.Medium, c.borderMuted.alpha, tolerance)
    }

    @Test
    fun opacityScaleIsMonotonic() {
        assertTrue(ReliveOpacity.Full > ReliveOpacity.VeryHigh)
        assertTrue(ReliveOpacity.VeryHigh > ReliveOpacity.High)
        assertTrue(ReliveOpacity.High > ReliveOpacity.Medium)
        assertTrue(ReliveOpacity.Medium > ReliveOpacity.Low)
        assertEquals(1.0f, ReliveOpacity.Full)
        assertEquals(0.9f, ReliveOpacity.VeryHigh)
    }

    @Test
    fun spacingScaleUsesDesignSystemSteps() {
        val s = DefaultReliveDimensions.spacing
        assertEquals(0.dp, s.none)
        assertEquals(4.dp, s.xs)
        assertEquals(8.dp, s.sm)
        assertEquals(12.dp, s.md)
        assertEquals(16.dp, s.lg)
        assertEquals(24.dp, s.xl)
        assertEquals(32.dp, s.xxl)
        assertEquals(48.dp, s.huge)
    }

    @Test
    fun radiiTokensMatchDesignSystem() {
        val r = DefaultReliveDimensions.radii
        assertEquals(8.dp, r.sm)
        assertEquals(12.dp, r.md)
        assertEquals(999.dp, r.pill)
    }

    @Test
    fun iconSizesMatchDesignSystem() {
        val i = DefaultReliveDimensions.icon
        assertEquals(12.dp, i.sm)
        assertEquals(20.dp, i.md)
        assertEquals(24.dp, i.lg)
    }

    @Test
    fun timelineDimensionsMatchDesignSystem() {
        val t = DefaultReliveDimensions.timeline
        assertEquals(1.dp, t.railWidth)
        assertEquals(10.dp, t.dotSize)
        assertEquals(32.dp, t.plusSize)
        assertEquals(48.dp, t.itemGap)
        assertEquals(32.dp, t.contentInset)
    }

    @Test
    fun minimumTouchTargetMeetsAccessibilityRule() {
        assertEquals(48.dp, DefaultReliveDimensions.minTouchTarget)
    }

    @Test
    fun mediaTokensMatchDesignSystem() {
        val m = DefaultReliveDimensions.media
        assertEquals(2f, m.ratioWide)
        assertEquals(4.dp, m.collageGap)
        assertEquals(4.dp, m.collageBorder)
        assertEquals(420.dp, m.collageSingleMaxHeight)
        assertEquals(1f, m.collageTileAspectSquare)
        assertEquals(4f / 3f, m.collageDominantAspect)
        assertEquals(16f / 9f, m.collageVideoAspect)
        assertEquals(4f / 3f, m.collageAudioAspect)
    }

    @Test
    fun defaultTypographySizesAndStylesMatchDesignSystem() {
        val t = DefaultReliveTypography
        assertEquals(30.sp, t.wordmark.fontSize)
        assertEquals(FontStyle.Italic, t.wordmark.fontStyle)
        assertEquals(24.sp, t.title.fontSize)
        assertEquals(FontStyle.Italic, t.subtitle.fontStyle)
        assertEquals(15.sp, t.body.fontSize)
        assertEquals(10.sp, t.eyebrow.fontSize)
        assertEquals(FontWeight.SemiBold, t.eyebrow.fontWeight)
        assertEquals(10.sp, t.tag.fontSize)
        assertEquals(FontWeight.SemiBold, t.tag.fontWeight)
        assertEquals(14.sp, t.action.fontSize)
        assertEquals(FontWeight.SemiBold, t.action.fontWeight)
    }

    @Test
    fun typographyFactoryAssignsSerifAndSansToTheCorrectRoles() {
        val serif = FontFamily.Cursive
        val sans = FontFamily.Monospace
        val t = reliveTypography(serif = serif, sans = sans)
        assertEquals(serif, t.wordmark.fontFamily)
        assertEquals(serif, t.title.fontFamily)
        assertEquals(sans, t.subtitle.fontFamily)
        assertEquals(sans, t.body.fontFamily)
        assertEquals(sans, t.eyebrow.fontFamily)
        assertEquals(sans, t.tag.fontFamily)
        assertEquals(sans, t.action.fontFamily)
    }

    @Test
    fun motionDurationsAreOrdered() {
        val d = DefaultReliveMotion.durations
        assertTrue(d.fastMillis < d.standardMillis)
        assertTrue(d.standardMillis < d.slowMillis)
        assertEquals(120, d.fastMillis)
        assertEquals(240, d.standardMillis)
        assertEquals(360, d.slowMillis)
        assertNotNull(DefaultReliveMotion.easings.standard)
    }

    @Test
    fun warmJournalRequestsDarkSystemBarIcons() {
        // Warm Journal is a light canvas; platform status/nav icons must render dark
        // for readability. Future dark themes flip this to false.
        assertTrue(WarmJournalTokens.systemBarIconsDark)
    }

    @Test
    fun warmJournalIsTheOnlyResolvedThemeInPhase0() {
        assertEquals(ReliveThemeId.WarmJournal, WarmJournalTokens.id)
        assertSame(WarmJournalTokens, reliveTokensFor(ReliveThemeId.WarmJournal))
        // Placeholder theme ids resolve to Warm Journal until their token sets exist.
        assertSame(WarmJournalTokens, reliveTokensFor(ReliveThemeId.MonochromeArchive))
        assertSame(WarmJournalTokens, reliveTokensFor(ReliveThemeId.FilmMemory))
    }
}
