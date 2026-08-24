package com.vaibhav.relive.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.vaibhav.relive.domain.model.ThemeReference

/** Identifier for a selectable Relive palette. Appearance mode is resolved separately. */
enum class ReliveThemeId {
    WarmJournal,
    Evergreen,
    LilacDusk,
    CrimsonKeepsake,
    BlueHour,
    Rosewood,
}

fun ThemeReference.toReliveThemeId(): ReliveThemeId = when (this) {
    ThemeReference.WarmJournal -> ReliveThemeId.WarmJournal
    ThemeReference.Evergreen -> ReliveThemeId.Evergreen
    ThemeReference.LilacDusk -> ReliveThemeId.LilacDusk
    ThemeReference.CrimsonKeepsake -> ReliveThemeId.CrimsonKeepsake
    ThemeReference.BlueHour -> ReliveThemeId.BlueHour
    ThemeReference.Rosewood -> ReliveThemeId.Rosewood
}

/**
 * A fully resolved token bundle for one Relive theme variant. Consuming UI must read from
 * this bundle (via `ReliveTheme.colors` etc.) rather than referring to raw values, so a
 * future theme swap requires no component rewrites.
 */
data class ReliveThemeTokens(
    val id: ReliveThemeId,
    val colors: ReliveColors,
    val typography: ReliveTypography,
    val dimensions: ReliveDimensions,
    val motion: ReliveMotion,
    val generatedCoverPalette: ReliveGeneratedCoverPalette,
    val isDark: Boolean,
    /**
     * When true, the platform status/navigation bar icons should render in a DARK
     * appearance (for use over light Relive canvases like Warm Journal). Future dark
     * themes set this to false so the platform draws light icons instead. Platform
     * bar wiring lives in [ApplyReliveSystemBars]; shared code stays platform-free.
     */
    val systemBarIconsDark: Boolean,
)

val WarmJournalTokens: ReliveThemeTokens = ReliveThemeTokens(
    id = ReliveThemeId.WarmJournal,
    colors = WarmJournalColors,
    typography = DefaultReliveTypography,
    dimensions = DefaultReliveDimensions,
    motion = DefaultReliveMotion,
    generatedCoverPalette = WarmJournalGeneratedCoverPalette,
    isDark = false,
    systemBarIconsDark = true,
)

fun reliveTokensFor(
    id: ReliveThemeId,
    isDark: Boolean = false,
): ReliveThemeTokens {
    if (id == ReliveThemeId.WarmJournal && !isDark) return WarmJournalTokens
    val anchors = paletteAnchorsFor(id.toThemeReference())
    return WarmJournalTokens.copy(
        id = id,
        colors = reliveColorsFor(anchors, isDark),
        generatedCoverPalette = generatedCoverPaletteFor(anchors, isDark),
        isDark = isDark,
        systemBarIconsDark = !isDark,
    )
}

private fun ReliveThemeId.toThemeReference(): ThemeReference = when (this) {
    ReliveThemeId.WarmJournal -> ThemeReference.WarmJournal
    ReliveThemeId.Evergreen -> ThemeReference.Evergreen
    ReliveThemeId.LilacDusk -> ThemeReference.LilacDusk
    ReliveThemeId.CrimsonKeepsake -> ThemeReference.CrimsonKeepsake
    ReliveThemeId.BlueHour -> ThemeReference.BlueHour
    ReliveThemeId.Rosewood -> ThemeReference.Rosewood
}

private val LocalReliveTokens = staticCompositionLocalOf { WarmJournalTokens }

@Composable
fun ReliveTheme(
    themeId: ReliveThemeId = ReliveThemeId.WarmJournal,
    darkMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseTokens = reliveTokensFor(themeId, darkMode)
    val bundledTypography = rememberReliveTypography()
    val animatedColors = animateReliveColors(baseTokens.colors, baseTokens.motion.durations.standardMillis)
    val tokens = baseTokens.copy(typography = bundledTypography, colors = animatedColors)
    val c = tokens.colors
    val materialScheme = if (tokens.isDark) {
        darkColorScheme(
            primary = c.accent,
            onPrimary = c.textOnAccent,
            secondary = c.accent,
            onSecondary = c.textOnAccent,
            background = c.bgCanvas,
            onBackground = c.textPrimary,
            surface = c.surfaceCard,
            onSurface = c.textPrimary,
            surfaceVariant = c.surfaceCard,
            onSurfaceVariant = c.textSecondary,
            surfaceContainerHigh = c.surfaceOverlay,
            error = c.actionDestructive,
            onError = c.textOnDestructive,
            outline = c.border,
            outlineVariant = c.borderMuted,
        )
    } else {
        lightColorScheme(
            primary = c.accent,
            onPrimary = c.textOnAccent,
            secondary = c.accent,
            onSecondary = c.textOnAccent,
            background = c.bgCanvas,
            onBackground = c.textPrimary,
            surface = c.surfaceCard,
            onSurface = c.textPrimary,
            surfaceVariant = c.surfaceCard,
            onSurfaceVariant = c.textSecondary,
            surfaceContainerHigh = c.surfaceOverlay,
            error = c.actionDestructive,
            onError = c.textOnDestructive,
            outline = c.border,
            outlineVariant = c.borderMuted,
        )
    }
    val materialTypography = Typography(
        titleLarge = tokens.typography.title,
        titleMedium = tokens.typography.title,
        bodyLarge = tokens.typography.body,
        bodyMedium = tokens.typography.body,
        bodySmall = tokens.typography.subtitle,
        labelLarge = tokens.typography.action,
        labelMedium = tokens.typography.eyebrow,
        labelSmall = tokens.typography.tag,
    )
    ApplyReliveSystemBars(tokens)
    CompositionLocalProvider(
        LocalReliveTokens provides tokens,
        LocalContentColor provides c.textPrimary,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = materialTypography,
            content = content,
        )
    }
}

@Composable
private fun animateReliveColors(target: ReliveColors, durationMillis: Int): ReliveColors {
    val animation = tween<Color>(durationMillis = durationMillis)
    @Composable
    fun animated(color: Color): Color = animateColorAsState(color, animationSpec = animation).value
    return ReliveColors(
        bgCanvas = animated(target.bgCanvas),
        bgHeader = animated(target.bgHeader),
        textPrimary = animated(target.textPrimary),
        textSecondary = animated(target.textSecondary),
        textMuted = animated(target.textMuted),
        textOnAccent = animated(target.textOnAccent),
        textOnDestructive = animated(target.textOnDestructive),
        accent = animated(target.accent),
        accentMuted = animated(target.accentMuted),
        surfaceCard = animated(target.surfaceCard),
        surfaceFloating = animated(target.surfaceFloating),
        surfaceOverlay = animated(target.surfaceOverlay),
        surfaceCardTranslucent = animated(target.surfaceCardTranslucent),
        surfaceAudio = animated(target.surfaceAudio),
        actionDestructive = animated(target.actionDestructive),
        border = animated(target.border),
        borderMuted = animated(target.borderMuted),
    )
}

/**
 * Configure the host platform's system bars (status/navigation) to match [tokens].
 * Android sets bar icon appearance and keeps bars transparent for edge-to-edge;
 * iOS is a no-op (status-bar style is driven by the UIViewController).
 */
@Composable
expect fun ApplyReliveSystemBars(tokens: ReliveThemeTokens)

object ReliveTheme {
    val tokens: ReliveThemeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalReliveTokens.current

    val colors: ReliveColors
        @Composable
        @ReadOnlyComposable
        get() = LocalReliveTokens.current.colors

    val typography: ReliveTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalReliveTokens.current.typography

    val dimensions: ReliveDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalReliveTokens.current.dimensions

    val motion: ReliveMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalReliveTokens.current.motion

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReliveTokens.current.isDark
}
