package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.pow

@Immutable
data class ReliveColors(
    val bgCanvas: Color,
    /**
     * A warmer, slightly more saturated tint of [bgCanvas] used as the light source in the
     * canvas gradient (see [reliveCanvasBrush]). Reads as afternoon light falling across the
     * page rather than a flat fill; kept close to [bgCanvas] so the effect stays subtle.
     */
    val bgCanvasGlow: Color,
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
    /**
     * Ambient/spot color for card elevation shadows. A warm dark tone (not neutral black) so
     * lifted surfaces cast a shadow that belongs to the palette. Compose scales its alpha by
     * the elevation, so this is supplied fully opaque.
     */
    val shadow: Color,
)

private val WarmJournalInk = Color(0xFF3C3633)
private val WarmJournalAccent = Color(0xFF6F4E37)
private val WarmJournalCanvas = Color(0xFFF6F4F0)
private val WarmJournalCard = Color(0xFFEFECE5)
private val WarmJournalFloatingSurface = Color(0xFFE1D8CB)
private val WarmJournalBorder = Color(0xFFD5CDBF)
private val WarmJournalDestructive = Color(0xFF98111E)
private val WarmJournalCanvasGlow = Color(0xFFF2E7D5)
private val WarmJournalShadow = Color(0xFF3C2A1A)

val WarmJournalColors: ReliveColors = ReliveColors(
    bgCanvas = WarmJournalCanvas,
    bgCanvasGlow = WarmJournalCanvasGlow,
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
    shadow = WarmJournalShadow,
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
    // Light source is the canvas pulled toward the palette's own mid hue. Dark canvases need a
    // much stronger lift than light ones: a subtle hue mix is invisible against near-black, so the
    // dark glow leans harder on the mid anchor to read as an actual light source at the top edge.
    val canvasGlow = if (isDark) mix(canvas, anchors.mid, 0.22f) else mix(canvas, anchors.mid, 0.16f)
    val shadow = mix(anchors.dark, Color.Black, 0.35f)

    return ReliveColors(
        bgCanvas = canvas,
        bgCanvasGlow = canvasGlow,
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
        shadow = shadow,
    )
}

/**
 * Vertical light-source gradient for screen canvases: a warm glow ([ReliveColors.bgCanvasGlow])
 * concentrated at the top that settles into the flat [ReliveColors.bgCanvas] by the upper
 * third. Applied at the screen root so content scrolls over a single, calm light source rather
 * than a flat fill. Kept intentionally subtle — one hue, low contrast.
 */
fun ReliveColors.canvasBrush(): Brush = Brush.verticalGradient(
    0.0f to bgCanvasGlow,
    0.35f to bgCanvas,
    1.0f to bgCanvas,
)

/**
 * Rich accent gradient for the single emotional hero surface (the "On This Day" card's caption
 * block). Runs from [accent] into a deepened accent so the block reads as a warm, lit panel
 * rather than a flat fill. This is the one place saturated brand color is used at size — the rest
 * of the app stays calm around it. Pair with [ReliveColors.textOnAccent] for legible content.
 */
fun ReliveColors.accentHeroBrush(): Brush = Brush.verticalGradient(
    listOf(accent, lerp(accent, Color.Black, 0.24f)),
)

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
