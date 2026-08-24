package com.vaibhav.relive.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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

val WarmJournalGeneratedCoverPalette = ReliveGeneratedCoverPalette(
    covers = listOf(
        ReliveGeneratedCoverColors(Color(0xFF241332), Color(0xFF5A2748)),
        ReliveGeneratedCoverColors(Color(0xFF111C35), Color(0xFF244D73)),
        ReliveGeneratedCoverColors(Color(0xFF102D2A), Color(0xFF275B4F)),
        ReliveGeneratedCoverColors(Color(0xFF321A17), Color(0xFF70402F)),
        ReliveGeneratedCoverColors(Color(0xFF191631), Color(0xFF443A70)),
        ReliveGeneratedCoverColors(Color(0xFF16272E), Color(0xFF285967)),
        ReliveGeneratedCoverColors(Color(0xFF321522), Color(0xFF653048)),
        ReliveGeneratedCoverColors(Color(0xFF222817), Color(0xFF4B5932)),
    ),
)

fun generatedCoverPaletteFor(
    anchors: RelivePaletteAnchors,
    isDark: Boolean,
): ReliveGeneratedCoverPalette {
    if (!isDark && anchors == OriginalPaletteAnchors) return WarmJournalGeneratedCoverPalette
    return ReliveGeneratedCoverPalette(
        covers = if (isDark) {
            listOf(
                ReliveGeneratedCoverColors(anchors.dark, anchors.mid),
                ReliveGeneratedCoverColors(anchors.dark, anchors.strong),
                ReliveGeneratedCoverColors(anchors.strong, anchors.dark),
                ReliveGeneratedCoverColors(anchors.strong, anchors.mid),
            )
        } else {
            listOf(
                ReliveGeneratedCoverColors(anchors.strong, anchors.dark),
                ReliveGeneratedCoverColors(anchors.dark, anchors.mid),
                ReliveGeneratedCoverColors(anchors.mid, anchors.strong),
                ReliveGeneratedCoverColors(anchors.dark, anchors.strong),
            )
        },
    )
}
