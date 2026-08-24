package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.vaibhav.relive.domain.model.ThemeReference

@Immutable
data class RelivePaletteAnchors(
    val light: Color,
    val mid: Color,
    val strong: Color,
    val dark: Color,
)

@Immutable
data class RelivePaletteOption(
    val theme: ThemeReference,
    val label: String,
    val anchors: RelivePaletteAnchors,
)

val OriginalPaletteAnchors = RelivePaletteAnchors(
    light = Color(0xFFF6F4F0),
    mid = Color(0xFFD5CDBF),
    strong = Color(0xFF6F4E37),
    dark = Color(0xFF3C3633),
)

val EvergreenPaletteAnchors = RelivePaletteAnchors(
    light = Color(0xFFD1F2EB),
    mid = Color(0xFF50C878),
    strong = Color(0xFF0B6E4F),
    dark = Color(0xFF013220),
)

val LilacDuskPaletteAnchors = RelivePaletteAnchors(
    light = Color(0xFFE6C7E6),
    mid = Color(0xFFA3779D),
    strong = Color(0xFF663399),
    dark = Color(0xFF2E1A47),
)

val CrimsonKeepsakePaletteAnchors = RelivePaletteAnchors(
    light = Color(0xFFFBE4E3),
    mid = Color(0xFFD72638),
    strong = Color(0xFF98111E),
    dark = Color(0xFF3F0D12),
)

val BlueHourPaletteAnchors = RelivePaletteAnchors(
    light = Color(0xFFD6E6F3),
    mid = Color(0xFFA6C5D7),
    strong = Color(0xFF0F52BA),
    dark = Color(0xFF000926),
)

val RosewoodPaletteAnchors = RelivePaletteAnchors(
    light = Color(0xFFFADADD),
    mid = Color(0xFFB66E79),
    strong = Color(0xFF8C4E4F),
    dark = Color(0xFF3B1F1B),
)

val RelivePaletteOptions: List<RelivePaletteOption> = listOf(
    RelivePaletteOption(ThemeReference.WarmJournal, "Original", OriginalPaletteAnchors),
    RelivePaletteOption(ThemeReference.Evergreen, "Evergreen", EvergreenPaletteAnchors),
    RelivePaletteOption(ThemeReference.LilacDusk, "Lilac Dusk", LilacDuskPaletteAnchors),
    RelivePaletteOption(ThemeReference.CrimsonKeepsake, "Crimson Keepsake", CrimsonKeepsakePaletteAnchors),
    RelivePaletteOption(ThemeReference.BlueHour, "Blue Hour", BlueHourPaletteAnchors),
    RelivePaletteOption(ThemeReference.Rosewood, "Rosewood", RosewoodPaletteAnchors),
)

fun paletteAnchorsFor(theme: ThemeReference): RelivePaletteAnchors =
    RelivePaletteOptions.first { it.theme == theme }.anchors

fun paletteLabelFor(theme: ThemeReference): String =
    RelivePaletteOptions.first { it.theme == theme }.label

fun previewGradientFor(
    anchors: RelivePaletteAnchors,
    isDark: Boolean,
): List<Color> = if (isDark) {
    listOf(anchors.dark, anchors.mid)
} else {
    listOf(anchors.light, anchors.strong)
}
