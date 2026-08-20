package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class ReliveTypography(
    val wordmark: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val eyebrow: TextStyle,
    val tag: TextStyle,
    val action: TextStyle,
)

/**
 * Build the Relive typography scale from two family bindings. `ReliveTheme` supplies the
 * locally bundled serif/sans families; the fallback default below uses platform families so
 * a `ReliveThemeTokens` value can exist outside a Composable scope.
 */
fun reliveTypography(serif: FontFamily, sans: FontFamily): ReliveTypography = ReliveTypography(
    wordmark = TextStyle(
        fontFamily = serif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
    ),
    title = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    subtitle = TextStyle(
        fontFamily = sans,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    body = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    eyebrow = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp,
    ),
    tag = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp,
    ),
    action = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
)

/**
 * Structural default used when a token bundle is materialized outside a Composable scope.
 * Uses platform families so no font resource is loaded here; `ReliveTheme` swaps in the
 * bundled Playfair Display + Inter families during composition.
 */
val DefaultReliveTypography: ReliveTypography =
    reliveTypography(serif = FontFamily.Serif, sans = FontFamily.SansSerif)
