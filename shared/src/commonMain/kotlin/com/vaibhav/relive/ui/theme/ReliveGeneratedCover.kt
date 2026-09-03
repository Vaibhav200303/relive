package com.vaibhav.relive.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** An accent-derived gradient pair used when a collection card has no visual media. */
data class ReliveGeneratedCoverColors(
    val start: Color,
    val end: Color,
)

data class ReliveGeneratedCoverPalette(
    val covers: List<ReliveGeneratedCoverColors>,
) {
    init {
        require(covers.isNotEmpty()) { "Generated cover palette must not be empty" }
    }
}

/** Palette selection is deterministic so a cover stays stable across sessions and recomposition. */
data class ReliveGeneratedCoverSelection(
    val paletteIndex: Int,
)

fun generatedCoverSelection(
    stableKey: String,
    palette: ReliveGeneratedCoverPalette,
): ReliveGeneratedCoverSelection = ReliveGeneratedCoverSelection(
    paletteIndex = stableCoverIndex(stableKey, palette.covers.size),
)

/**
 * Map [stableKey] onto one of [optionCount] choices with the shared cover hash. The same function
 * backs gradient selection and the Rediscover row's single-preview-attachment pick, so every cover
 * decision keyed the same way stays put until the key itself changes.
 */
fun stableCoverIndex(stableKey: String, optionCount: Int): Int {
    require(stableKey.isNotBlank()) { "Generated cover stable key must not be blank" }
    require(optionCount > 0) { "Cover selection needs at least one option" }
    return (stableCoverHash(stableKey) % optionCount.toUInt()).toInt()
}

private fun stableCoverHash(value: String): UInt {
    var hash = 2_166_136_261u
    value.forEach { character ->
        hash = (hash xor character.code.toUInt()) * 16_777_619u
    }
    return hash
}

/**
 * A cheap Compose-brush fallback cover for collection cards without an image or video preview.
 * The caller supplies a persisted logical identity; no visual state is retained.
 */
@Composable
fun ReliveGeneratedCover(
    stableKey: String,
    modifier: Modifier = Modifier,
) {
    val palette = ReliveTheme.tokens.generatedCoverPalette
    val colors = palette.covers[generatedCoverSelection(stableKey, palette).paletteIndex]
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(colors.start, colors.end))),
    )
}

/**
 * Media-less covers are cut from the same cloth as the `+ New` button: every pair is the active
 * mode's workhorse accent taken to two different depths — settled toward black in light mode,
 * lifted toward white in dark mode — so generated cards read as siblings of the primary action
 * in whichever palette and mode are active (ADR-0064). All stops stay strictly on the darker
 * (light mode) or lighter (dark mode) side of the accent, and fully opaque.
 */
fun generatedCoverPaletteFor(
    palette: RelivePalette,
    isDark: Boolean,
): ReliveGeneratedCoverPalette {
    val accent = palette.roles(isDark).primary
    val anchor = if (isDark) Color.White else Color.Black
    fun step(depth: Float): Color = lerp(accent, anchor, depth)
    return ReliveGeneratedCoverPalette(
        covers = listOf(
            ReliveGeneratedCoverColors(step(0.10f), step(0.42f)),
            ReliveGeneratedCoverColors(step(0.38f), step(0.12f)),
            ReliveGeneratedCoverColors(step(0.22f), step(0.55f)),
            ReliveGeneratedCoverColors(step(0.55f), step(0.22f)),
            ReliveGeneratedCoverColors(step(0.06f), step(0.30f)),
            ReliveGeneratedCoverColors(step(0.46f), step(0.68f)),
        ),
    )
}

/** The app-wide default cover palette (Ink &amp; Lilac, light). */
val DefaultGeneratedCoverPalette: ReliveGeneratedCoverPalette =
    generatedCoverPaletteFor(DefaultRelivePalette, isDark = false)
