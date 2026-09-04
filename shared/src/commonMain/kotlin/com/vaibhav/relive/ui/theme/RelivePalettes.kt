package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.vaibhav.relive.domain.model.ThemeReference

/**
 * The seven semantic roles that make up one palette in one mode. Every screen reads the app
 * theme through [ReliveColors] tokens derived from these roles, so a palette applies uniformly
 * across the whole app once selected — timeline, rediscover, profile and every subscreen.
 *
 * The rules the whole system obeys:
 *  - [ink] is the single font colour for a mode: wordmark, headers, card titles and body all use
 *    it; hierarchy comes from size/weight/family, never hue. [inkSoft] is the muted sibling for
 *    secondary and meta text.
 *  - [primary] is the calm workhorse accent (buttons, pills, active nav, selection). [spark] is
 *    the loud highlight used only as small marks (dots, sparkles, active indicators). [tint] is a
 *    soft supporting fill.
 *  - [canvas] is the screen background; in dark mode it stays a deep, *chromatic* tone of the
 *    palette's own hue (lifted off black), never charcoal. [surface] is one value-step off canvas.
 */
@Immutable
data class RelivePaletteRoles(
    val canvas: Color,
    val surface: Color,
    val ink: Color,
    val inkSoft: Color,
    val primary: Color,
    val spark: Color,
    val tint: Color,
)

@Immutable
data class RelivePalette(
    val theme: ThemeReference,
    val label: String,
    val light: RelivePaletteRoles,
    val dark: RelivePaletteRoles,
) {
    fun roles(isDark: Boolean): RelivePaletteRoles = if (isDark) dark else light
}

// 01 · Ink & Lilac — editorial, premium (the reference palette).
val InkLilacPalette = RelivePalette(
    theme = ThemeReference.InkLilac,
    label = "Original",
    light = RelivePaletteRoles(
        canvas = Color(0xFFF0EEE9),
        surface = Color(0xFFFFFFFF),
        ink = Color(0xFF17184B),
        inkSoft = Color(0xFF55567A),
        // Deepened from the airy reference lilac so a filled control clears WCAG non-text
        // contrast (>= 3:1) against a white card while staying unmistakably lilac.
        primary = Color(0xFF7E5BC6),
        spark = Color(0xFFCDE24A),
        tint = Color(0xFFD3DDE7),
    ),
    dark = RelivePaletteRoles(
        canvas = Color(0xFF1D2049),
        surface = Color(0xFF282B57),
        ink = Color(0xFFF1EFF7),
        inkSoft = Color(0xFFADAAC9),
        primary = Color(0xFFC4A9F2),
        spark = Color(0xFFD8E63C),
        tint = Color(0xFF34386A),
    ),
)

// 02 · Teal & Saffron — oceanic, calm, quietly premium.
val TealSaffronPalette = RelivePalette(
    theme = ThemeReference.TealSaffron,
    label = "Evergreen",
    light = RelivePaletteRoles(
        canvas = Color(0xFFF0F5F3),
        surface = Color(0xFFFFFFFF),
        ink = Color(0xFF0F3A43),
        inkSoft = Color(0xFF476A70),
        primary = Color(0xFF2E8079),
        spark = Color(0xFFFFC24D),
        tint = Color(0xFFD7E7E2),
    ),
    dark = RelivePaletteRoles(
        canvas = Color(0xFF163640),
        surface = Color(0xFF1F4550),
        ink = Color(0xFFE9F3F0),
        inkSoft = Color(0xFF9CBDB7),
        primary = Color(0xFF5FC2B6),
        spark = Color(0xFFFFC94F),
        tint = Color(0xFF244E58),
    ),
)

// 03 · Ember & Aqua — warm, nostalgic, with a cool spark.
val EmberAquaPalette = RelivePalette(
    theme = ThemeReference.EmberAqua,
    label = "Ember & Aqua",
    light = RelivePaletteRoles(
        canvas = Color(0xFFF5F2EB),
        surface = Color(0xFFFFFFFF),
        ink = Color(0xFF3A231E),
        inkSoft = Color(0xFF6E534B),
        primary = Color(0xFFB4573C),
        spark = Color(0xFFFF6B3D),
        tint = Color(0xFFA9CDCE),
    ),
    dark = RelivePaletteRoles(
        canvas = Color(0xFF34231E),
        surface = Color(0xFF452F28),
        ink = Color(0xFFF4EAE3),
        inkSoft = Color(0xFFC3A89D),
        primary = Color(0xFFE08A6C),
        spark = Color(0xFFFF7A4D),
        tint = Color(0xFF6FA6A8),
    ),
)

// 04 · Plum & Gold — elegant, dusk, romantic.
val PlumGoldPalette = RelivePalette(
    theme = ThemeReference.PlumGold,
    label = "Plum & Gold",
    light = RelivePaletteRoles(
        canvas = Color(0xFFF3EEF3),
        surface = Color(0xFFFFFFFF),
        ink = Color(0xFF2E1A3F),
        inkSoft = Color(0xFF5E4A70),
        primary = Color(0xFF6E3F97),
        spark = Color(0xFFE4A845),
        tint = Color(0xFFE2D2EB),
    ),
    dark = RelivePaletteRoles(
        canvas = Color(0xFF2A1B3D),
        surface = Color(0xFF382650),
        ink = Color(0xFFF1ECF5),
        inkSoft = Color(0xFFB8A6CC),
        primary = Color(0xFFB98CE0),
        spark = Color(0xFFF0C264),
        tint = Color(0xFF3E2C5B),
    ),
)

// 05 · Rose & Sage — gentle, botanical, warm nostalgia.
val RoseSagePalette = RelivePalette(
    theme = ThemeReference.RoseSage,
    label = "Rose & Sage",
    light = RelivePaletteRoles(
        canvas = Color(0xFFFAF2F0),
        surface = Color(0xFFFFFFFF),
        ink = Color(0xFF46282A),
        inkSoft = Color(0xFF7A5654),
        primary = Color(0xFFA24A56),
        spark = Color(0xFF63C6A0),
        tint = Color(0xFFEAD6D3),
    ),
    dark = RelivePaletteRoles(
        canvas = Color(0xFF3A2523),
        surface = Color(0xFF4B322E),
        ink = Color(0xFFF5E8E6),
        inkSoft = Color(0xFFCBA7A4),
        primary = Color(0xFFD98A93),
        spark = Color(0xFF6FD3AC),
        tint = Color(0xFF523633),
    ),
)

// 06 · Sunrise — first light: pale gold sky, warm earth ink, an amber sun. Light mode is the
// golden hour after dawn; dark mode is the violet pre-dawn sky waiting for it. The atmospheric
// canvas gradient derives from these roles, so the whole app takes on the morning's light.
val SunrisePalette = RelivePalette(
    theme = ThemeReference.Sunrise,
    label = "Sunrise",
    light = RelivePaletteRoles(
        canvas = Color(0xFFFBF2E4),
        surface = Color(0xFFFFFFFF),
        ink = Color(0xFF46280F),
        inkSoft = Color(0xFF6F4E30),
        primary = Color(0xFFB4541A),
        spark = Color(0xFFFFB94E),
        tint = Color(0xFFF3DCC0),
    ),
    dark = RelivePaletteRoles(
        canvas = Color(0xFF32213E),
        surface = Color(0xFF413052),
        ink = Color(0xFFF7EEE3),
        inkSoft = Color(0xFFC7AFA6),
        primary = Color(0xFFF3A356),
        spark = Color(0xFFFFCF7D),
        tint = Color(0xFF473556),
    ),
)

// 07 · Sunset — dusk: rose-washed sky, deep plum ink, a low orange sun. Light mode is the warm
// blush just before the light goes; dark mode is the burgundy afterglow once it has.
val SunsetPalette = RelivePalette(
    theme = ThemeReference.Sunset,
    label = "Sunset",
    light = RelivePaletteRoles(
        canvas = Color(0xFFF9ECE9),
        surface = Color(0xFFFFFFFF),
        ink = Color(0xFF3B1E33),
        inkSoft = Color(0xFF6D4763),
        primary = Color(0xFFAD3F63),
        spark = Color(0xFFFF9E52),
        tint = Color(0xFFF0D3D5),
    ),
    dark = RelivePaletteRoles(
        canvas = Color(0xFF381D2F),
        surface = Color(0xFF4A2940),
        ink = Color(0xFFF8ECEF),
        inkSoft = Color(0xFFCEA9B9),
        primary = Color(0xFFEC8FA3),
        spark = Color(0xFFFFB068),
        tint = Color(0xFF56304A),
    ),
)

val RelivePaletteOptions: List<RelivePalette> = listOf(
    InkLilacPalette,
    TealSaffronPalette,
    EmberAquaPalette,
    PlumGoldPalette,
    RoseSagePalette,
    SunrisePalette,
    SunsetPalette,
)

/** Palettes a person can choose in Appearance; Original remains resolvable for existing settings. */
val ReliveSelectablePaletteOptions: List<RelivePalette> =
    RelivePaletteOptions.filterNot { it.theme == ThemeReference.InkLilac }

/** The app-wide default palette. */
val DefaultRelivePalette: RelivePalette = InkLilacPalette

fun paletteFor(theme: ThemeReference): RelivePalette =
    RelivePaletteOptions.first { it.theme == theme }

fun paletteLabelFor(theme: ThemeReference): String = paletteFor(theme).label

/**
 * Two-stop gradient for the palette picker swatch: the mode's canvas settling into its primary
 * accent, so the swatch previews the actual base-plus-accent pairing a viewer will get.
 */
fun previewGradientFor(
    palette: RelivePalette,
    isDark: Boolean,
): List<Color> = palette.roles(isDark).let { listOf(it.canvas, it.primary) }
