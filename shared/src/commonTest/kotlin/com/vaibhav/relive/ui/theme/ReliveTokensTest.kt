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
import kotlin.test.assertTrue

class ReliveTokensTest {

    @Test
    fun warmJournalColorsMatchDesignSystem() {
        val c = WarmJournalColors
        assertEquals(Color(0xFFF6F4F0), c.bgCanvas)
        assertEquals(Color(0xFF3C3633), c.textPrimary)
        assertEquals(Color(0xFF6F4E37), c.accent)
        assertEquals(Color(0xFFEFECE5), c.surfaceCard)
        assertEquals(Color(0xFFF6F4F0), c.surfaceOverlay)
        assertEquals(c.accent, c.actionDestructive)
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
        assertEquals(28.dp, r.dialog)
        assertEquals(r.md, r.menu)
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
    fun rediscoverShelfWidthsPreserveTheFeaturedHierarchy() {
        val rediscover = DefaultReliveDimensions.rediscover
        assertEquals(20.dp, rediscover.cardOuterRadius)
        assertEquals(0.68f, rediscover.favoriteShelfCardWidthFraction)
        assertTrue(rediscover.onThisDayShelfCardWidthFraction > rediscover.favoriteShelfCardWidthFraction)
        assertEquals(272.dp, rediscover.favoriteShelfCardHeight)
        assertEquals(128.dp, rediscover.compactInfoAreaHeight)
        assertEquals(128.dp, rediscover.heroInfoAreaMinHeight)
    }

    @Test
    fun compactTagFieldKeepsASeparateAccessibleTouchTarget() {
        assertEquals(36.dp, DefaultReliveDimensions.composer.tagVisibleHeight)
        assertTrue(DefaultReliveDimensions.composer.tagVisibleHeight < DefaultReliveDimensions.minTouchTarget)
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
        assertTrue(d.fastMillis < d.timelineReturnMillis)
        assertTrue(d.timelineReturnMillis < d.standardMillis)
        assertTrue(d.standardMillis < d.slowMillis)
        assertEquals(120, d.fastMillis)
        assertEquals(145, d.timelineReturnMillis)
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
    fun generatedCoverPalettesRespectTheActiveTheme() {
        assertEquals(ReliveThemeId.WarmJournal, WarmJournalTokens.id)
        assertEquals(WarmJournalGeneratedCoverPalette, reliveTokensFor(ReliveThemeId.WarmJournal).generatedCoverPalette)
        ReliveThemeId.entries.drop(1).forEach { theme ->
            assertTrue(reliveTokensFor(theme).generatedCoverPalette.covers.isNotEmpty())
            assertTrue(reliveTokensFor(theme, isDark = true).generatedCoverPalette.covers.isNotEmpty())
        }
    }

    @Test
    fun suppliedPaletteAnchorsAreExact() {
        assertEquals(Color(0xFFD1F2EB), EvergreenPaletteAnchors.light)
        assertEquals(Color(0xFF50C878), EvergreenPaletteAnchors.mid)
        assertEquals(Color(0xFF0B6E4F), EvergreenPaletteAnchors.strong)
        assertEquals(Color(0xFF013220), EvergreenPaletteAnchors.dark)
        assertEquals(Color(0xFF98111E), CrimsonKeepsakePaletteAnchors.strong)
        assertEquals(Color(0xFF3F0D12), CrimsonKeepsakePaletteAnchors.dark)
        assertEquals(Color(0xFF000926), BlueHourPaletteAnchors.dark)
        assertEquals(Color(0xFF3B1F1B), RosewoodPaletteAnchors.dark)
    }

    @Test
    fun previewsUseTheLiteralModeSpecificAnchors() {
        RelivePaletteOptions.forEach { option ->
            assertEquals(
                listOf(option.anchors.light, option.anchors.strong),
                previewGradientFor(option.anchors, isDark = false),
            )
            assertEquals(
                listOf(option.anchors.dark, option.anchors.mid),
                previewGradientFor(option.anchors, isDark = true),
            )
        }
    }

    @Test
    fun everyPaletteHasDistinctLightAndDarkAccessibleContentPairs() {
        ReliveThemeId.entries.forEach { theme ->
            val light = reliveTokensFor(theme, isDark = false)
            val dark = reliveTokensFor(theme, isDark = true)
            assertTrue(light.colors.bgCanvas != dark.colors.bgCanvas)
            assertTrue(light.systemBarIconsDark)
            assertTrue(!dark.systemBarIconsDark)
            listOf(light, dark).forEach { tokens ->
                assertTrue(
                    contrastRatio(tokens.colors.textPrimary, tokens.colors.bgCanvas) >= 4.5f,
                    "$theme ${tokens.isDark} primary text contrast",
                )
                assertTrue(
                    contrastRatio(tokens.colors.textSecondary, tokens.colors.bgCanvas) >= 4.5f,
                    "$theme ${tokens.isDark} body text contrast",
                )
                assertTrue(
                    contrastRatio(tokens.colors.textOnAccent, tokens.colors.accent) >= 4.5f,
                    "$theme ${tokens.isDark} accent contrast=" +
                        contrastRatio(tokens.colors.textOnAccent, tokens.colors.accent),
                )
                assertTrue(
                    contrastRatio(tokens.colors.textOnDestructive, tokens.colors.actionDestructive) >= 4.5f,
                    "$theme ${tokens.isDark} destructive action contrast",
                )
                assertTrue(
                    contrastRatio(tokens.colors.accent, tokens.colors.surfaceCard) >= 3f,
                    "$theme ${tokens.isDark} selected indicator contrast",
                )
                assertTrue(
                    contrastRatio(tokens.colors.textPrimary, tokens.colors.surfaceCard) >= 4.5f,
                    "$theme ${tokens.isDark} card content contrast",
                )
                assertTrue(
                    contrastRatio(tokens.colors.textSecondary, tokens.colors.surfaceCard) >= 4.5f,
                    "$theme ${tokens.isDark} card body contrast",
                )
                assertTrue(
                    contrastRatio(tokens.colors.textPrimary, tokens.colors.surfaceOverlay) >= 4.5f,
                    "$theme ${tokens.isDark} overlay content contrast",
                )
            }
        }
    }
}
