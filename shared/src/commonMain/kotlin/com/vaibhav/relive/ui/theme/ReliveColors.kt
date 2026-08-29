package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
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
    /** Calm workhorse accent: buttons, pills, active nav, selection. Safe at size. */
    val accent: Color,
    val accentMuted: Color,
    /**
     * Loud highlight, small use only: dots, sparkles, active indicators. Never used as a text
     * colour and never as a large fill — it exists to draw the eye to a single point.
     */
    val spark: Color,
    /** Soft supporting fill: chips, quiet backgrounds, the search field. */
    val tint: Color,
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

private val DestructiveLight = Color(0xFF98111E)
private val DestructiveDark = Color(0xFFFF8A95)

/**
 * Build the full semantic token bundle for one palette in one mode from its seven [roles].
 * Derived tokens (borders, muted text, glow, translucency, shadow) are computed the same way for
 * every palette, so all five stay internally consistent and any new palette drops in for free.
 * On-accent and on-destructive text are chosen for guaranteed >= 4.5:1 contrast.
 */
internal fun reliveColorsFor(
    roles: RelivePaletteRoles,
    isDark: Boolean,
): ReliveColors {
    val canvas = roles.canvas
    val surface = roles.surface
    val ink = roles.ink
    val primary = roles.primary
    val destructive = if (isDark) DestructiveDark else DestructiveLight
    return ReliveColors(
        bgCanvas = canvas,
        // A subtle light source at the top edge: dark canvases lift toward their primary hue,
        // light canvases settle toward the soft tint. One hue, low contrast, never neon.
        bgCanvasGlow = if (isDark) mix(canvas, primary, 0.18f) else mix(canvas, roles.tint, 0.45f),
        bgHeader = canvas.copy(alpha = ReliveOpacity.VeryHigh),
        textPrimary = ink,
        textSecondary = roles.inkSoft,
        textMuted = mix(roles.inkSoft, canvas, 0.34f),
        textOnAccent = readableTextOn(primary, ink),
        textOnDestructive = higherContrastText(destructive, listOf(Color.White, Color.Black)),
        accent = primary,
        accentMuted = mix(primary, canvas, 0.18f),
        spark = roles.spark,
        tint = roles.tint,
        surfaceCard = surface,
        surfaceFloating = roles.tint,
        surfaceOverlay = if (isDark) mix(surface, ink, 0.06f) else surface,
        surfaceCardTranslucent = surface.copy(alpha = ReliveOpacity.Medium),
        surfaceAudio = if (isDark) mix(canvas, Color.Black, 0.35f) else mix(ink, Color.Black, 0.08f),
        actionDestructive = destructive,
        border = mix(surface, ink, if (isDark) 0.20f else 0.14f),
        borderMuted = mix(canvas, ink, if (isDark) 0.14f else 0.10f),
        shadow = mix(ink, Color.Black, 0.35f),
    )
}

/** The app-wide default token bundle (Ink &amp; Lilac, light). */
val DefaultReliveColors: ReliveColors = reliveColorsFor(DefaultRelivePalette.light, isDark = false)

/** Prefer the palette ink on a filled accent when it is legible; otherwise fall back to B/W. */
private fun readableTextOn(background: Color, preferredInk: Color): Color =
    if (contrastRatio(preferredInk, background) >= 4.5f) {
        preferredInk
    } else {
        higherContrastText(background, listOf(Color.White, Color.Black))
    }

/**
 * Diagonal light-source gradient for screen canvases: a warm glow ([ReliveColors.bgCanvasGlow])
 * gathered in the top-left corner that settles into the flat [ReliveColors.bgCanvas] toward the
 * bottom-right. `Offset.Infinite` as the end lets the gradient span whatever area it fills, so it
 * runs corner-to-corner on any screen size. Applied at the screen root so content scrolls over a
 * single, calm light source rather than a flat fill. Kept intentionally subtle — one hue, low
 * contrast.
 */
fun ReliveColors.canvasBrush(): Brush = Brush.linearGradient(
    0.0f to bgCanvasGlow,
    0.55f to bgCanvas,
    1.0f to bgCanvas,
    start = Offset.Zero,
    end = Offset.Infinite,
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
