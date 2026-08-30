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
    fun defaultColorsMatchInkLilacLight() {
        val c = DefaultReliveColors
        assertEquals(Color(0xFFF0EEE9), c.bgCanvas)
        assertEquals(Color(0xFF17184B), c.textPrimary)
        assertEquals(Color(0xFF55567A), c.textSecondary)
        assertEquals(Color(0xFF7E5BC6), c.accent)
        assertEquals(Color(0xFFCDE24A), c.spark)
        assertEquals(Color(0xFFD3DDE7), c.tint)
        assertEquals(Color(0xFFFFFFFF), c.surfaceCard)
        assertEquals(Color(0xFFFFFFFF), c.surfaceOverlay)
        assertEquals(Color(0xFF98111E), c.actionDestructive)
    }

    @Test
    fun opacityDerivedColorsUseSharedScale() {
        // Color.copy quantizes alpha to 8 bits, so compare within one channel step.
        val tolerance = 1f / 255f
        val c = DefaultReliveColors
        assertEquals(ReliveOpacity.VeryHigh, c.bgHeader.alpha, tolerance)
        assertEquals(ReliveOpacity.Medium, c.surfaceCardTranslucent.alpha, tolerance)
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
        assertEquals(34.sp, t.display.fontSize)
        assertEquals(30.sp, t.wordmark.fontSize)
        // The wordmark is now a roman Medium masthead, not the previous fashion-italic.
        assertEquals(FontStyle.Normal, t.wordmark.fontStyle)
        assertEquals(FontWeight.Medium, t.wordmark.fontWeight)
        assertEquals(30.sp, t.coverTitle.fontSize)
        // The moment title is the strongest text element: serif SemiBold.
        assertEquals(24.sp, t.title.fontSize)
        assertEquals(FontWeight.SemiBold, t.title.fontWeight)
        // Optional editorial day-header role (not yet wired into the timeline UI).
        assertEquals(28.sp, t.dateLarge.fontSize)
        assertEquals(FontWeight.Medium, t.dateLarge.fontWeight)
        assertEquals(FontStyle.Italic, t.subtitle.fontStyle)
        assertEquals(15.sp, t.subtitle.fontSize)
        assertEquals(16.sp, t.body.fontSize)
        assertEquals(26.sp, t.body.lineHeight)
        assertEquals(13.sp, t.caption.fontSize)
        assertEquals(11.sp, t.eyebrow.fontSize)
        assertEquals(FontWeight.Medium, t.eyebrow.fontWeight)
        assertEquals(12.sp, t.tag.fontSize)
        assertEquals(FontWeight.Medium, t.tag.fontWeight)
        assertEquals(14.sp, t.action.fontSize)
        assertEquals(FontWeight.Medium, t.action.fontWeight)
        assertEquals(16.sp, t.prominentAction.fontSize)
        assertEquals(FontWeight.SemiBold, t.prominentAction.fontWeight)
    }

    @Test
    fun typographyScaleHasConsistentVerticalRhythmAndDistinctSteps() {
        val t = DefaultReliveTypography
        // Every role sets an explicit line height (no silent per-role default).
        listOf(
            t.display, t.wordmark, t.coverTitle, t.title, t.dateLarge, t.subtitle,
            t.body, t.caption, t.eyebrow, t.tag, t.action, t.prominentAction,
        ).forEach { style ->
            assertTrue(style.lineHeight.value > 0f, "line height set for $style")
        }
        // The former muddy middle now steps clearly: caption < subtitle < body.
        assertTrue(t.caption.fontSize.value < t.subtitle.fontSize.value)
        assertTrue(t.subtitle.fontSize.value < t.body.fontSize.value)
        // Large serif carries tight optical tracking; small caps carry open tracking.
        assertTrue(t.title.letterSpacing.value < 0f)
        assertTrue(t.eyebrow.letterSpacing.value > 1f)
    }

    @Test
    fun typographyFactoryAssignsSerifAndSansToTheCorrectRoles() {
        val serif = FontFamily.Cursive
        val sans = FontFamily.Monospace
        val t = reliveTypography(serif = serif, sans = sans)
        assertEquals(serif, t.display.fontFamily)
        assertEquals(serif, t.wordmark.fontFamily)
        assertEquals(serif, t.coverTitle.fontFamily)
        assertEquals(serif, t.title.fontFamily)
        assertEquals(serif, t.dateLarge.fontFamily)
        assertEquals(sans, t.subtitle.fontFamily)
        assertEquals(sans, t.body.fontFamily)
        assertEquals(sans, t.caption.fontFamily)
        assertEquals(sans, t.eyebrow.fontFamily)
        assertEquals(sans, t.tag.fontFamily)
        assertEquals(sans, t.action.fontFamily)
    }

    @Test
    fun darkModeStepsHeavyLabelsDownToCompensateHalation() {
        val serif = FontFamily.Cursive
        val sans = FontFamily.Monospace
        val light = reliveTypography(serif = serif, sans = sans, isDark = false)
        val dark = reliveTypography(serif = serif, sans = sans, isDark = true)

        // Light: the calm "Kept" scale sets standard labels at Medium and the single
        // heaviest control (the primary CTA) at SemiBold.
        assertEquals(FontWeight.Medium, light.eyebrow.fontWeight)
        assertEquals(FontWeight.Medium, light.tag.fontWeight)
        assertEquals(FontWeight.Medium, light.action.fontWeight)
        assertEquals(FontWeight.SemiBold, light.prominentAction.fontWeight)

        // Dark steps each label one bundled weight lighter (standard Medium -> Regular,
        // CTA SemiBold -> Medium) so glare-bloated light-on-dark labels carry the same
        // typographic color as light mode.
        assertEquals(FontWeight.Normal, dark.eyebrow.fontWeight)
        assertEquals(FontWeight.Normal, dark.tag.fontWeight)
        assertEquals(FontWeight.Normal, dark.action.fontWeight)
        assertEquals(FontWeight.Medium, dark.prominentAction.fontWeight)

        // Body/serif roles are identical across modes (no lighter cut is bundled).
        assertEquals(light.body.fontWeight, dark.body.fontWeight)
        assertEquals(light.title.fontWeight, dark.title.fontWeight)
        assertEquals(light.body.fontSize, dark.body.fontSize)
    }

    @Test
    fun materialTypographyFillsEveryRoleWithBrandedFamilies() {
        val serif = FontFamily.Cursive
        val sans = FontFamily.Monospace
        val t = reliveTypography(serif = serif, sans = sans)
        val m = reliveMaterialTypography(t)

        // Display/headline had no Relive binding and previously fell back to the
        // Material default; they must now use the bundled serif family.
        assertEquals(serif, m.displayLarge.fontFamily)
        assertEquals(serif, m.displayMedium.fontFamily)
        assertEquals(serif, m.displaySmall.fontFamily)
        assertEquals(serif, m.headlineLarge.fontFamily)
        assertEquals(serif, m.headlineMedium.fontFamily)
        assertEquals(serif, m.headlineSmall.fontFamily)

        // The previously-mapped roles keep their exact Relive bindings.
        assertEquals(t.title, m.titleLarge)
        assertEquals(t.body, m.bodyLarge)
        assertEquals(t.action, m.labelLarge)
        assertEquals(t.eyebrow, m.labelMedium)
        assertEquals(t.tag, m.labelSmall)
        assertEquals(t.display, m.displayLarge)
        assertEquals(t.caption, m.bodySmall)
        assertEquals(t.subtitle, m.titleSmall)

        // Optical tracking: display sizes are set tighter than headlineSmall.
        assertTrue(m.displayLarge.letterSpacing.value < 0f)
        assertTrue(m.displayLarge.fontSize > m.headlineLarge.fontSize)
    }

    @Suppress("DEPRECATION")
    @Test
    fun motionDurationsAreOrdered() {
        val d = DefaultReliveMotion.durations
        assertTrue(d.fastMillis < d.standardMillis)
        assertTrue(d.timelineReturnMillis < d.fastMillis)
        assertTrue(d.timelineReturnMillis < d.standardMillis)
        assertTrue(d.standardMillis < d.slowMillis)
        assertEquals(150, d.fastMillis)
        assertEquals(100, d.timelineReturnMillis)
        assertEquals(300, d.standardMillis)
        assertEquals(500, d.slowMillis)
        assertNotNull(DefaultReliveMotion.easings.standard)
    }

    @Test
    fun motionDurationsExposeFullM3Scale() {
        val d = DefaultReliveMotion.durations
        assertEquals(50, d.short1)
        assertEquals(100, d.short2)
        assertEquals(150, d.short3)
        assertEquals(200, d.short4)
        assertEquals(250, d.medium1)
        assertEquals(300, d.medium2)
        assertEquals(350, d.medium3)
        assertEquals(400, d.medium4)
        assertEquals(450, d.long1)
        assertEquals(500, d.long2)
        assertEquals(550, d.long3)
        assertEquals(600, d.long4)
        assertEquals(700, d.extraLong1)
    }

    @Test
    fun motionEasingsExposeAllSixM3Curves() {
        val e = DefaultReliveMotion.easings
        assertNotNull(e.standard)
        assertNotNull(e.standardDecelerate)
        assertNotNull(e.standardAccelerate)
        assertNotNull(e.emphasizedDecelerate)
        assertNotNull(e.emphasizedAccelerate)
        // The cubic curves must round-trip 0 and 1. `emphasized` is a PathEasing whose
        // backing `Path` needs an Android runtime, so its transform is exercised in a
        // Compose UI test, not this host-JVM unit test.
        listOf(
            e.standard, e.standardDecelerate, e.standardAccelerate,
            e.emphasizedDecelerate, e.emphasizedAccelerate,
        ).forEach { easing ->
            assertEquals(0f, easing.transform(0f))
            assertEquals(1f, easing.transform(1f))
        }
    }

    @Test
    fun defaultThemeRequestsDarkSystemBarIcons() {
        // The default palette is a light canvas; platform status/nav icons must render dark
        // for readability. Dark mode flips this to false.
        assertTrue(DefaultReliveTokens.systemBarIconsDark)
    }

    @Test
    fun generatedCoverPalettesRespectTheActiveTheme() {
        assertEquals(ReliveThemeId.InkLilac, DefaultReliveTokens.id)
        assertEquals(DefaultGeneratedCoverPalette, reliveTokensFor(ReliveThemeId.InkLilac).generatedCoverPalette)
        ReliveThemeId.entries.drop(1).forEach { theme ->
            assertTrue(reliveTokensFor(theme).generatedCoverPalette.covers.isNotEmpty())
            assertTrue(reliveTokensFor(theme, isDark = true).generatedCoverPalette.covers.isNotEmpty())
        }
    }

    @Test
    fun suppliedPaletteRolesAreExact() {
        assertEquals(Color(0xFF17184B), InkLilacPalette.light.ink)
        assertEquals(Color(0xFFC4A9F2), InkLilacPalette.dark.primary)
        assertEquals(Color(0xFF2E8079), TealSaffronPalette.light.primary)
        assertEquals(Color(0xFFFF6B3D), EmberAquaPalette.light.spark)
        assertEquals(Color(0xFF6E3F97), PlumGoldPalette.light.primary)
        assertEquals(Color(0xFF63C6A0), RoseSagePalette.light.spark)
    }

    @Test
    fun previewsUseTheModeCanvasIntoPrimary() {
        RelivePaletteOptions.forEach { option ->
            assertEquals(
                listOf(option.light.canvas, option.light.primary),
                previewGradientFor(option, isDark = false),
            )
            assertEquals(
                listOf(option.dark.canvas, option.dark.primary),
                previewGradientFor(option, isDark = true),
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
