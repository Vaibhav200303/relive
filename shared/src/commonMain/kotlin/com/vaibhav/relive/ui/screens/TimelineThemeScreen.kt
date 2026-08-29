package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.repository.AppearanceRepository
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.presentation.timeline.TimelineThemeDestination
import com.vaibhav.relive.presentation.timeline.TimelineThemeViewModel
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.TimelineWallpaperSurface
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun TimelineThemeScreen(
    timelineRepository: TimelineRepository,
    appearanceRepository: AppearanceRepository,
    destination: TimelineThemeDestination,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(timelineRepository, appearanceRepository, destination, scope) {
        TimelineThemeViewModel(timelineRepository, appearanceRepository, destination, scope)
    }
    val state by viewModel.state.collectAsState()
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bgCanvas),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .semantics { contentDescription = "Back from timeline theme" },
            ) {
                BackGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
            }
            Text(
                text = "Timeline theme",
                style = ReliveTheme.typography.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = dims.spacing.sm),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.xxl),
        ) {
            TimelineThemePreview(state.appearance.wallpaper)
            Text("Wallpaper", style = ReliveTheme.typography.subtitle, color = colors.textPrimary)
            Text("Choose a backdrop for this timeline.", style = ReliveTheme.typography.body, color = colors.textSecondary)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                userScrollEnabled = true,
                horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
                verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
                modifier = Modifier.height(420.dp),
            ) {
                items(TimelineWallpaper.entries) { wallpaper ->
                    TimelineWallpaperOption(
                        wallpaper = wallpaper,
                        selected = wallpaper == state.appearance.wallpaper,
                        onClick = { viewModel.selectWallpaper(wallpaper) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineThemePreview(wallpaper: TimelineWallpaper) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val shape = RoundedCornerShape(dims.radii.lg)
    TimelineWallpaperSurface(
        wallpaper = wallpaper,
        modifier = Modifier
            .fillMaxWidth()
            .border(dims.stroke.hairline, colors.borderMuted, shape)
            .clip(shape),
    ) {
    Column(
        modifier = Modifier.padding(dims.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        Text("A small preview", style = ReliveTheme.typography.eyebrow, color = colors.textMuted)
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(top = dims.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("14", style = ReliveTheme.typography.title, color = colors.textPrimary)
                Text("JUN", style = ReliveTheme.typography.eyebrow, color = colors.textMuted)
                Box(
                    modifier = Modifier
                        .padding(top = dims.spacing.sm)
                        .size(dims.timeline.dotSize)
                        .background(colors.accent, RoundedCornerShape(dims.radii.pill)),
                )
                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .padding(top = dims.spacing.xs)
                        .size(dims.timeline.railWidth, 116.dp)
                        .background(colors.border),
                )
            }
            Column(
                modifier = Modifier.padding(start = dims.spacing.md).weight(1f),
                verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
            ) {
                Text("An unhurried afternoon", style = ReliveTheme.typography.subtitle, color = colors.textPrimary)
                Text("A quiet walk, a little sunlight, and a moment worth keeping.", style = ReliveTheme.typography.body, color = colors.textSecondary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .background(colors.surfaceFloating, RoundedCornerShape(dims.radii.md)),
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("♡", style = ReliveTheme.typography.subtitle, color = colors.accent)
                    Text("  Favourite · 2:40 PM", style = ReliveTheme.typography.eyebrow, color = colors.textMuted)
                }
            }
        }
    }
    }
}

@Composable
private fun TimelineWallpaperOption(
    wallpaper: TimelineWallpaper,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val shape = RoundedCornerShape(dims.radii.md)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .border(if (selected) dims.stroke.cardOuter * 2 else dims.stroke.hairline, if (selected) colors.accent else colors.borderMuted, shape)
            .background(colors.surfaceCard, shape)
            .clickable(onClick = onClick)
            .padding(dims.spacing.sm)
            .semantics { contentDescription = "${wallpaper.label()}${if (selected) ", selected" else ""}" },
    ) {
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(dims.radii.sm))) { TimelineWallpaperThumbnail(wallpaper) }
        Column(modifier = Modifier.padding(top = dims.spacing.sm)) {
            Text(wallpaper.label(), style = ReliveTheme.typography.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (selected) Text("Selected", style = ReliveTheme.typography.eyebrow, color = colors.textSecondary)
        }
    }
}

@Composable
private fun TimelineWallpaperThumbnail(wallpaper: TimelineWallpaper) {
    TimelineWallpaperSurface(wallpaper = wallpaper, modifier = Modifier.fillMaxSize()) {}
}

private fun TimelineWallpaper.label(): String = when (this) {
    TimelineWallpaper.WarmCream -> "Warm Cream"
    TimelineWallpaper.BlushPink -> "Blush Pink"
    TimelineWallpaper.SageGreen -> "Sage Green"
    TimelineWallpaper.Lavender -> "Lavender"
    TimelineWallpaper.PowderBlue -> "Powder Blue"
    TimelineWallpaper.SoftPeach -> "Soft Peach"
    TimelineWallpaper.MidnightNavy -> "Midnight Navy"
    TimelineWallpaper.Evergreen -> "Evergreen"
    TimelineWallpaper.MauveDusk -> "Mauve Dusk"
    TimelineWallpaper.TerracottaGlow -> "Terracotta Glow"
    TimelineWallpaper.CharcoalMist -> "Charcoal Mist"
    TimelineWallpaper.CoralBloom -> "Coral Bloom"
    TimelineWallpaper.AquaSky -> "Aqua Sky"
    TimelineWallpaper.GoldenHour -> "Golden Hour"
    TimelineWallpaper.VioletHaze -> "Violet Haze"
    TimelineWallpaper.SapphireBlue -> "Sapphire Blue"
}
