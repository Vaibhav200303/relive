package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ReliveColors(
    val bgCanvas: Color,
    val bgHeader: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textOnAccent: Color,
    val accent: Color,
    val accentMuted: Color,
    val surfaceCard: Color,
    val surfaceFloating: Color,
    val surfaceCardTranslucent: Color,
    val surfaceAudio: Color,
    val border: Color,
    val borderMuted: Color,
)

private val WarmJournalInk = Color(0xFF3C3633)
private val WarmJournalAccent = Color(0xFF6F4E37)
private val WarmJournalCanvas = Color(0xFFF6F4F0)
private val WarmJournalCard = Color(0xFFEFECE5)
private val WarmJournalFloatingSurface = Color(0xFFE1D8CB)
private val WarmJournalBorder = Color(0xFFD5CDBF)

val WarmJournalColors: ReliveColors = ReliveColors(
    bgCanvas = WarmJournalCanvas,
    bgHeader = WarmJournalCanvas.copy(alpha = ReliveOpacity.VeryHigh),
    textPrimary = WarmJournalInk,
    textSecondary = WarmJournalInk.copy(alpha = ReliveOpacity.High),
    textMuted = WarmJournalInk.copy(alpha = ReliveOpacity.Medium),
    textOnAccent = Color.White,
    accent = WarmJournalAccent,
    accentMuted = WarmJournalAccent.copy(alpha = ReliveOpacity.High),
    surfaceCard = WarmJournalCard,
    surfaceFloating = WarmJournalFloatingSurface,
    surfaceCardTranslucent = WarmJournalCard.copy(alpha = ReliveOpacity.Medium),
    surfaceAudio = Color(0xFF171514),
    border = WarmJournalBorder,
    borderMuted = WarmJournalBorder.copy(alpha = ReliveOpacity.Medium),
)
