package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import relive.shared.generated.resources.Res
import relive.shared.generated.resources.fraunces_medium
import relive.shared.generated.resources.fraunces_semibold
import relive.shared.generated.resources.inter_italic
import relive.shared.generated.resources.inter_medium
import relive.shared.generated.resources.inter_regular
import relive.shared.generated.resources.inter_semibold

/**
 * Locally bundled serif family. Fraunces, licensed under SIL Open Font License 1.1
 * (see `shared/licenses/fonts/Fraunces-OFL.txt`).
 *
 * A warm old-style soft-serif that carries every Relive brand/emotional role. The bundled
 * cuts are static instances taken from the Fraunces variable font at a fixed 72pt optical
 * size (`opsz=72`, the display optical cut), moderate softness (`SOFT=40`) and no wonk
 * (`WONK=0`) — a controlled, intimate-journal character rather than a decorative one. Both
 * cuts are roman; the serif is never set italic (the only italic role, `subtitle`, is sans).
 * Only the two weights referenced by `reliveTypography` are bundled: Medium (500) for the
 * brand roles and SemiBold (600) for the strongest text element, the moment title.
 */
@Composable
internal fun rememberReliveSerifFamily(): FontFamily {
    val medium = Font(Res.font.fraunces_medium, FontWeight.Medium, FontStyle.Normal)
    val semibold = Font(Res.font.fraunces_semibold, FontWeight.SemiBold, FontStyle.Normal)
    return remember(medium, semibold) { FontFamily(medium, semibold) }
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
