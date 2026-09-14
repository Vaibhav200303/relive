package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Dusty-rose promotional colors from the approved Timeline theme screen reference. */
@Immutable
data class TimelineThemePickerPalette(
    val bannerBackground: Color,
    val promotionalAccent: Color,
    val onPromotionalAccent: Color,
    val proBadgeBackground: Color,
    val proBadgeForeground: Color,
)

fun timelineThemePickerPalette(isDark: Boolean): TimelineThemePickerPalette =
    if (isDark) {
        TimelineThemePickerPalette(
            bannerBackground = Color(0xFF422D35),
            promotionalAccent = Color(0xFFD99AAF),
            onPromotionalAccent = Color(0xFF2A1820),
            proBadgeBackground = Color(0xFF573643),
            proBadgeForeground = Color(0xFFF7DCE5),
        )
    } else {
        TimelineThemePickerPalette(
            bannerBackground = Color(0xFFF5E7E8),
            promotionalAccent = Color(0xFFB96883),
            onPromotionalAccent = Color(0xFFFFF9FA),
            proBadgeBackground = Color(0xFFF4E3E7),
            proBadgeForeground = Color(0xFF754257),
        )
    }
