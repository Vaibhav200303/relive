package com.vaibhav.relive.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** A dark, restrained gradient pair used when a collection card has no visual media. */
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
): ReliveGeneratedCoverSelection {
    require(stableKey.isNotBlank()) { "Generated cover stable key must not be blank" }
    val hash = stableCoverHash(stableKey)
    return ReliveGeneratedCoverSelection(
        paletteIndex = (hash % palette.covers.size.toUInt()).toInt(),
    )
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
 * Media-less covers are always deep tiles carrying light-on-dark content, so they are seeded from
 * the palette's dark roles regardless of the active mode — a card without a photo reads the same
 * whether the app is light or dark, and stays tied to the selected palette's hue.
 */
fun generatedCoverPaletteFor(palette: RelivePalette): ReliveGeneratedCoverPalette {
    val d = palette.dark
    return ReliveGeneratedCoverPalette(
        covers = listOf(
            ReliveGeneratedCoverColors(d.canvas, d.surface),
            ReliveGeneratedCoverColors(lerp(d.canvas, d.primary, 0.30f), d.canvas),
            ReliveGeneratedCoverColors(d.surface, lerp(d.surface, d.primary, 0.42f)),
            ReliveGeneratedCoverColors(lerp(d.canvas, Color.Black, 0.12f), d.tint),
            ReliveGeneratedCoverColors(d.tint, d.canvas),
            ReliveGeneratedCoverColors(lerp(d.primary, Color.Black, 0.45f), d.canvas),
        ),
    )
}

/** The app-wide default cover palette (Ink &amp; Lilac). */
val DefaultGeneratedCoverPalette: ReliveGeneratedCoverPalette = generatedCoverPaletteFor(DefaultRelivePalette)
