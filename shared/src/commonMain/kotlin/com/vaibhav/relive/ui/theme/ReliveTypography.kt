package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class ReliveTypography(
    val display: TextStyle,
    val wordmark: TextStyle,
    val coverTitle: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val eyebrow: TextStyle,
    val tag: TextStyle,
    val action: TextStyle,
    val prominentAction: TextStyle,
)

/**
 * Build the Relive typography scale from two family bindings. `ReliveTheme` supplies the
 * locally bundled serif/sans families; the fallback default below uses platform families so
 * a `ReliveThemeTokens` value can exist outside a Composable scope.
 *
 * The scale is one modular editorial system, not a bag of per-screen sizes. It follows the
 * Material 3 brand/plain split — the expressive serif carries the large brand roles
 * ([display]/[wordmark]/[coverTitle]/[title]) and the readable sans carries every text and
 * label role — and each role sets size, line height, tracking, and weight explicitly so the
 * vertical rhythm stays consistent everywhere.
 *
 * Optical sizing: the app satisfies optical-size intent structurally rather than with an
 * `opsz` axis (the bundled cuts are static, not variable). The serif is a high-contrast
 * DISPLAY face used only at large sizes (24–38sp) and the sans is a TEXT face used only at
 * small sizes (11–16sp), so each role already carries the contrast appropriate to its size.
 * Tracking is tuned per size the way an optical axis would tune it — tight on the large
 * serif, open on the small-caps roles ([eyebrow]/[tag]). See Google Fonts, "Choosing
 * typefaces that have optical sizes."
 *
 * @param isDark applies halation compensation for dark canvases — see [labelWeightFor].
 */
fun reliveTypography(
    serif: FontFamily,
    sans: FontFamily,
    isDark: Boolean = false,
): ReliveTypography {
    val labelWeight = labelWeightFor(isDark)
    return ReliveTypography(
        display = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.Normal,
            fontSize = 38.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.5).sp,
        ),
        wordmark = TextStyle(
            fontFamily = serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.25).sp,
        ),
        coverTitle = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.Normal,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.25).sp,
        ),
        title = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.25).sp,
        ),
        subtitle = TextStyle(
            fontFamily = sans,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        ),
        body = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp,
        ),
        caption = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp,
        ),
        eyebrow = TextStyle(
            fontFamily = sans,
            fontWeight = labelWeight,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.5.sp,
        ),
        tag = TextStyle(
            fontFamily = sans,
            fontWeight = labelWeight,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.8.sp,
        ),
        action = TextStyle(
            fontFamily = sans,
            fontWeight = labelWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        prominentAction = TextStyle(
            fontFamily = sans,
            fontWeight = labelWeight,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp,
        ),
    )
}

/**
 * Weight used for the heavy label roles ([ReliveTypography.eyebrow], [ReliveTypography.tag],
 * [ReliveTypography.action], [ReliveTypography.prominentAction]).
 *
 * Halation compensation: on a dark canvas, light-on-dark text glares and its strokes
 * visually bloat, so a weight that looks right on a light canvas reads too heavy on dark
 * (Google Fonts, "Exploring typefaces with multiple weights or grades"). We step these
 * labels down one bundled weight — SemiBold on light, Medium on dark — so they carry the
 * same typographic color in both modes. The lighter body/serif roles are left untouched:
 * no lighter cut is bundled, and lightening 15sp+ text would trade legibility for a glare
 * benefit that is negligible at those sizes.
 */
fun labelWeightFor(isDark: Boolean): FontWeight =
    if (isDark) FontWeight.Medium else FontWeight.SemiBold

/**
 * Structural default used when a token bundle is materialized outside a Composable scope.
 * Uses platform families so no font resource is loaded here; `ReliveTheme` swaps in the
 * bundled Playfair Display + Inter families during composition.
 */
val DefaultReliveTypography: ReliveTypography =
    reliveTypography(serif = FontFamily.Serif, sans = FontFamily.SansSerif)
