package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

@Immutable
data class ReliveColors(
    val bgCanvas: Color,
    val bgHeader: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textOnAccent: Color,
    val textOnDestructive: Color,
    val accent: Color,
    val accentMuted: Color,
    val surfaceCard: Color,
    val surfaceFloating: Color,
    val surfaceOverlay: Color,
    val surfaceCardTranslucent: Color,
    val surfaceAudio: Color,
    val actionDestructive: Color,
    val border: Color,
    val borderMuted: Color,
)

private val WarmJournalInk = Color(0xFF3C3633)
private val WarmJournalAccent = Color(0xFF6F4E37)
private val WarmJournalCanvas = Color(0xFFF6F4F0)
private val WarmJournalCard = Color(0xFFEFECE5)
private val WarmJournalFloatingSurface = Color(0xFFE1D8CB)
private val WarmJournalBorder = Color(0xFFD5CDBF)
private val WarmJournalDestructive = Color(0xFF98111E)

val WarmJournalColors: ReliveColors = ReliveColors(
    bgCanvas = WarmJournalCanvas,
    bgHeader = WarmJournalCanvas.copy(alpha = ReliveOpacity.VeryHigh),
    textPrimary = WarmJournalInk,
    textSecondary = WarmJournalInk.copy(alpha = ReliveOpacity.High),
    textMuted = WarmJournalInk.copy(alpha = ReliveOpacity.Medium),
    textOnAccent = Color.White,
    textOnDestructive = Color.White,
    accent = WarmJournalAccent,
    accentMuted = WarmJournalAccent.copy(alpha = ReliveOpacity.High),
    surfaceCard = WarmJournalCard,
    surfaceFloating = WarmJournalFloatingSurface,
    surfaceOverlay = WarmJournalCanvas,
    surfaceCardTranslucent = WarmJournalCard.copy(alpha = ReliveOpacity.Medium),
    surfaceAudio = Color(0xFF171514),
    actionDestructive = WarmJournalDestructive,
    border = WarmJournalBorder,
    borderMuted = WarmJournalBorder.copy(alpha = ReliveOpacity.Medium),
)

private val DarkCanvasNeutral = Color(0xFF0B0A09)
private val DestructiveLight = Color(0xFF98111E)
private val DestructiveDark = Color(0xFFFF8A95)

internal fun reliveColorsFor(
    anchors: RelivePaletteAnchors,
    isDark: Boolean,
): ReliveColors {
    if (!isDark && anchors == OriginalPaletteAnchors) return WarmJournalColors

    val canvas = if (isDark) {
        mix(anchors.dark, DarkCanvasNeutral, 0.35f)
    } else {
        mix(anchors.light, WarmJournalCanvas, 0.72f)
    }
    val surface = if (isDark) {
        mix(anchors.dark, anchors.strong, 0.18f)
    } else {
        mix(anchors.light, Color.White, 0.38f)
    }
    val floating = if (isDark) {
        mix(anchors.dark, anchors.mid, 0.28f)
    } else {
        mix(anchors.light, anchors.mid, 0.35f)
    }
    val overlay = if (isDark) mix(anchors.dark, anchors.strong, 0.25f) else canvas
    val primaryText = if (isDark) mix(anchors.light, Color.White, 0.18f) else anchors.dark
    val accent = if (isDark) anchors.mid else anchors.strong
    val border = if (isDark) {
        mix(anchors.dark, anchors.mid, 0.32f)
    } else {
        mix(anchors.light, anchors.mid, 0.45f)
    }
    val textOnAccent = higherContrastText(accent, listOf(anchors.dark, Color.White, Color.Black))
    val actionDestructive = if (isDark) DestructiveDark else DestructiveLight
    val textOnDestructive = higherContrastText(actionDestructive, listOf(Color.White, Color.Black))

    return ReliveColors(
        bgCanvas = canvas,
        bgHeader = canvas.copy(alpha = ReliveOpacity.VeryHigh),
        textPrimary = primaryText,
        textSecondary = mix(primaryText, canvas, if (isDark) 0.18f else 0.20f),
        textMuted = mix(primaryText, canvas, if (isDark) 0.32f else 0.34f),
        textOnAccent = textOnAccent,
        textOnDestructive = textOnDestructive,
        accent = accent,
        accentMuted = mix(accent, canvas, 0.18f),
        surfaceCard = surface,
        surfaceFloating = floating,
        surfaceOverlay = overlay,
        surfaceCardTranslucent = surface.copy(alpha = ReliveOpacity.Medium),
        surfaceAudio = if (isDark) mix(anchors.dark, Color.Black, 0.45f) else anchors.dark,
        actionDestructive = actionDestructive,
        border = border,
        borderMuted = mix(border, canvas, 0.42f),
    )
}

internal fun contrastRatio(foreground: Color, background: Color): Float {
    val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
    val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun higherContrastText(background: Color, candidates: List<Color>): Color =
    candidates.maxBy { contrastRatio(it, background) }

private fun relativeLuminance(color: Color): Float =
    0.2126f * linearChannel(color.red) +
        0.7152f * linearChannel(color.green) +
        0.0722f * linearChannel(color.blue)

private fun linearChannel(value: Float): Float =
    if (value <= 0.04045f) value / 12.92f else ((value + 0.055f) / 1.055f).pow(2.4f)

private fun mix(from: Color, toward: Color, towardWeight: Float): Color = Color(
    red = from.red + (toward.red - from.red) * towardWeight,
    green = from.green + (toward.green - from.green) * towardWeight,
    blue = from.blue + (toward.blue - from.blue) * towardWeight,
    alpha = from.alpha + (toward.alpha - from.alpha) * towardWeight,
)
