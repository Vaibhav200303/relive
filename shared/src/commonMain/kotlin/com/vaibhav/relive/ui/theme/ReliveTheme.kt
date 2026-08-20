package com.vaibhav.relive.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Identifier for a Relive theme variant. Only Warm Journal is implemented in Phase 0;
 * Monochrome Archive and Film Memory are placeholders reserved for later phases
 * (see docs/PRODUCT_SPEC.md §11 and docs/DESIGN_SYSTEM.md §19).
 */
enum class ReliveThemeId {
    WarmJournal,
    MonochromeArchive,
    FilmMemory,
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
    systemBarIconsDark = true,
)

fun reliveTokensFor(id: ReliveThemeId): ReliveThemeTokens = when (id) {
    ReliveThemeId.WarmJournal -> WarmJournalTokens
    ReliveThemeId.MonochromeArchive,
    ReliveThemeId.FilmMemory -> WarmJournalTokens
}

private val LocalReliveTokens = staticCompositionLocalOf { WarmJournalTokens }

@Composable
fun ReliveTheme(
    themeId: ReliveThemeId = ReliveThemeId.WarmJournal,
    content: @Composable () -> Unit,
) {
    val baseTokens = reliveTokensFor(themeId)
    val bundledTypography = rememberReliveTypography()
    val tokens = remember(baseTokens, bundledTypography) {
        baseTokens.copy(typography = bundledTypography)
    }
    val c = tokens.colors
    val materialScheme = lightColorScheme(
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
        outline = c.border,
        outlineVariant = c.borderMuted,
    )
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
}
