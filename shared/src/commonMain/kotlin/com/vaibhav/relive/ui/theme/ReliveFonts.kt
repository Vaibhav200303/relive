package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import relive.shared.generated.resources.Res
import relive.shared.generated.resources.inter_italic
import relive.shared.generated.resources.inter_medium
import relive.shared.generated.resources.inter_regular
import relive.shared.generated.resources.inter_semibold
import relive.shared.generated.resources.playfair_display_italic
import relive.shared.generated.resources.playfair_display_regular

/**
 * Locally bundled serif family. Playfair Display, licensed under SIL Open Font License 1.1
 * (see `shared/licenses/fonts/PlayfairDisplay-OFL.txt`).
 * Only the weights/styles actually referenced by `reliveTypography` are bundled.
 */
@Composable
internal fun rememberReliveSerifFamily(): FontFamily {
    val regular = Font(Res.font.playfair_display_regular, FontWeight.Normal, FontStyle.Normal)
    val italic = Font(Res.font.playfair_display_italic, FontWeight.Normal, FontStyle.Italic)
    return remember(regular, italic) { FontFamily(regular, italic) }
}

/**
 * Locally bundled sans family. Inter, licensed under SIL Open Font License 1.1
 * (see `shared/licenses/fonts/Inter-OFL.txt`).
 */
@Composable
internal fun rememberReliveSansFamily(): FontFamily {
    val regular = Font(Res.font.inter_regular, FontWeight.Normal, FontStyle.Normal)
    val italic = Font(Res.font.inter_italic, FontWeight.Normal, FontStyle.Italic)
    val medium = Font(Res.font.inter_medium, FontWeight.Medium, FontStyle.Normal)
    val semibold = Font(Res.font.inter_semibold, FontWeight.SemiBold, FontStyle.Normal)
    return remember(regular, italic, medium, semibold) {
        FontFamily(regular, italic, medium, semibold)
    }
}

@Composable
internal fun rememberReliveTypography(isDark: Boolean): ReliveTypography {
    val serif = rememberReliveSerifFamily()
    val sans = rememberReliveSansFamily()
    return remember(serif, sans, isDark) {
        reliveTypography(serif = serif, sans = sans, isDark = isDark)
    }
}
