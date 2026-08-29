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
    val dateLarge: TextStyle,
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
 * The scale is one modular editorial system — the "Kept" direction — not a bag of per-screen
 * sizes. It follows the Material 3 brand/plain split: the expressive serif (Fraunces) carries
 * the large brand/emotional roles ([display]/[wordmark]/[coverTitle]/[title]/[dateLarge]) and
 * the readable sans (Inter) carries every text and label role. Each role sets size, line
 * height, tracking, and weight explicitly so the vertical rhythm stays consistent everywhere.
 *
 * Weight is used intentionally, not "everything bold": brand roles are serif Medium; the
 * moment [title] is serif SemiBold (the strongest text element — the opening sentence of a
 * journal); reading [body]/[subtitle]/[caption] are sans Regular; metadata and controls
 * ([eyebrow]/[tag]/[action]) are sans Medium; only the primary call to action
 * ([prominentAction]) is sans SemiBold.
 *
 * Optical sizing: the app satisfies optical-size intent structurally rather than with a live
 * `opsz` axis (the bundled cuts are static instances). The serif is Fraunces' 72pt display
 * optical cut, used only at large sizes (24–34sp), and the sans is a text face used only at
 * small sizes (11–16sp), so each role carries the contrast appropriate to its size. Tracking
 * is tuned per size the way an optical axis would tune it — tight (negative) on the large
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
    val prominentWeight = prominentLabelWeightFor(isDark)
    return ReliveTypography(
        display = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.Medium,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp,
        ),
        wordmark = TextStyle(
            fontFamily = serif,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Medium,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.25).sp,
        ),
        coverTitle = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.Medium,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.25).sp,
        ),
        title = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.25).sp,
        ),
        dateLarge = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.Medium,
            fontSize = 28.sp,
            lineHeight = 32.sp,
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
            lineHeight = 26.sp,
            letterSpacing = 0.sp,
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
            letterSpacing = 1.2.sp,
        ),
        tag = TextStyle(
            fontFamily = sans,
            fontWeight = labelWeight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
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
            fontWeight = prominentWeight,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp,
        ),
    )
}

/**
 * Weight used for the standard label roles ([ReliveTypography.eyebrow],
 * [ReliveTypography.tag], [ReliveTypography.action]) — the calm metadata and control text.
 *
 * Halation compensation: on a dark canvas, light-on-dark text glares and its strokes
 * visually bloat, so a weight that looks right on a light canvas reads too heavy on dark
 * (Google Fonts, "Exploring typefaces with multiple weights or grades"). We step these
 * labels down one bundled weight — **Medium on light, Regular on dark** — so they carry the
 * same typographic color in both modes. This is the same one-step mechanism the system has
 * always used, rebased one weight lighter than the previous SemiBold/Medium baseline because
 * the "Kept" scale deliberately makes metadata and controls calmer (Medium, not SemiBold).
 * The lighter body/serif roles are left untouched: no lighter serif cut is bundled, and
 * lightening 15sp+ reading text would trade legibility for a negligible glare benefit.
 */
fun labelWeightFor(isDark: Boolean): FontWeight =
    if (isDark) FontWeight.Normal else FontWeight.Medium

/**
 * Weight used for the single heaviest label role, the primary call to action
 * ([ReliveTypography.prominentAction], e.g. "Save moment" / "+ New").
 *
 * It keeps the original SemiBold-on-light / Medium-on-dark halation step so the one genuinely
 * emphatic control retains its presence while still shedding a weight on dark canvases. Only
 * the CTA is this heavy; ordinary buttons use the lighter [labelWeightFor].
 */
fun prominentLabelWeightFor(isDark: Boolean): FontWeight =
    if (isDark) FontWeight.Medium else FontWeight.SemiBold

/**
 * Structural default used when a token bundle is materialized outside a Composable scope.
 * Uses platform families so no font resource is loaded here; `ReliveTheme` swaps in the
 * bundled Fraunces + Inter families during composition.
 */
val DefaultReliveTypography: ReliveTypography =
    reliveTypography(serif = FontFamily.Serif, sans = FontFamily.SansSerif)
