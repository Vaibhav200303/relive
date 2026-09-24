package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.vaibhav.relive.domain.entitlement.EntitlementPolicy
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.repository.AppearanceRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.presentation.timeline.TimelineThemeDestination
import com.vaibhav.relive.presentation.timeline.TimelineThemeViewModel
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.ForwardGlyph
import com.vaibhav.relive.ui.components.timeline.HeartGlyph
import com.vaibhav.relive.ui.components.timeline.LocalTimelineWallpaperPalette
import com.vaibhav.relive.ui.components.timeline.TimelineWallpaperSurface
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.rememberReliveHandwritingFamily
import com.vaibhav.relive.ui.theme.timelineMomentForegroundColors
import com.vaibhav.relive.ui.theme.timelineThemePickerPalette

private val FeaturedTimelineWallpapers = listOf(
    TimelineWallpaper.WarmCream,
    TimelineWallpaper.BlushPink,
    TimelineWallpaper.SageGreen,
    TimelineWallpaper.Lavender,
    TimelineWallpaper.SoftPeach,
    TimelineWallpaper.Evergreen,
)

@Composable
fun TimelineThemeScreen(
    timelineRepository: TimelineRepository,
    appearanceRepository: AppearanceRepository,
    destination: TimelineThemeDestination,
    onBack: () -> Unit,
    entitlementProvider: EntitlementProvider,
    onUpgrade: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(timelineRepository, appearanceRepository, destination, scope) {
        TimelineThemeViewModel(timelineRepository, appearanceRepository, destination, scope)
    }
    val state by viewModel.state.collectAsState()
    val entitlement by entitlementProvider.state.collectAsState()
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val policy = EntitlementPolicy(entitlement)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvasBrush()),
    ) {
        TimelineThemeHeader(onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.spacing.xl)
                .padding(bottom = dims.spacing.xxl),
        ) {
            TimelineThemeIntro()

            TimelineThemePreview(
                wallpaper = state.appearance.wallpaper,
                modifier = Modifier.padding(top = dims.spacing.xl),
            )

            Text(
                text = "Wallpaper",
                style = ReliveTheme.typography.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = dims.spacing.lg),
            )
            Text(
                text = "Choose a backdrop for this timeline.",
                style = ReliveTheme.typography.caption,
                color = colors.textSecondary,
            )

            TimelineWallpaperGrid(
                wallpapers = FeaturedTimelineWallpapers,
                selectedWallpaper = state.appearance.wallpaper,
                isLocked = { !policy.maySelectWallpaper(it) },
                onWallpaperClick = { wallpaper ->
                    if (policy.maySelectWallpaper(wallpaper)) {
                        viewModel.selectWallpaper(wallpaper)
                    } else {
                        onUpgrade()
                    }
                },
                modifier = Modifier.padding(top = dims.spacing.md),
            )

            val additionalWallpapers =
                TimelineWallpaper.entries.filterNot(FeaturedTimelineWallpapers::contains)
            TimelineWallpaperGrid(
                wallpapers = additionalWallpapers,
                selectedWallpaper = state.appearance.wallpaper,
                isLocked = { !policy.maySelectWallpaper(it) },
                onWallpaperClick = { wallpaper ->
                    if (policy.maySelectWallpaper(wallpaper)) {
                        viewModel.selectWallpaper(wallpaper)
                    } else {
                        onUpgrade()
                    }
                },
                modifier = Modifier.padding(top = dims.spacing.lg),
            )

            if (!entitlement.isPro) {
                ReliveProThemeBanner(
                    onClick = onUpgrade,
                    modifier = Modifier.padding(top = dims.spacing.lg),
                )
            }
        }
    }
}

@Composable
private fun TimelineThemeHeader(onBack: () -> Unit) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors

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
            modifier = Modifier.padding(start = dims.spacing.xs),
        )
    }
}

@Composable
private fun TimelineThemeIntro() {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val handwriting = rememberReliveHandwritingFamily()

    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Give this timeline its own look and feel.",
            style = ReliveTheme.typography.body,
            color = colors.textSecondary,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            text = "Same moments,\nnew colors ♡",
            style = ReliveTheme.typography.action.copy(fontFamily = handwriting),
            color = colors.accent,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = dims.spacing.md)
                .rotate(dims.timelineTheme.noteRotationDegrees),
        )
    }
}

@Composable
private fun TimelineThemePreview(
    wallpaper: TimelineWallpaper,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    val shape = RoundedCornerShape(dims.radii.largeIncreased)

    TimelineWallpaperSurface(
        wallpaper = wallpaper,
        modifier = modifier
            .fillMaxWidth()
            .height(dims.timelineTheme.previewHeight)
            .clip(shape),
    ) {
        val momentColors = timelineMomentForegroundColors(
            colors = ReliveTheme.colors,
            wallpaper = LocalTimelineWallpaperPalette.current,
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(dims.spacing.lg),
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("14", style = ReliveTheme.typography.title, color = momentColors.textPrimary)
                Text("JUN", style = ReliveTheme.typography.eyebrow, color = momentColors.textMuted)
                Box(
                    modifier = Modifier
                        .padding(top = dims.spacing.sm)
                        .size(dims.timeline.dotSize)
                        .background(momentColors.accent, RoundedCornerShape(dims.radii.full)),
                )
                Box(
                    modifier = Modifier
                        .padding(top = dims.spacing.xs)
                        .weight(1f)
                        .width(dims.timeline.railWidth)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ReliveTheme.colors.accent,
                                    ReliveTheme.colors.spark,
                                    ReliveTheme.colors.accent,
                                ),
                            ),
                        ),
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = dims.spacing.lg)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
            ) {
                Text(
                    text = "An unhurried afternoon",
                    style = ReliveTheme.typography.subtitle,
                    color = momentColors.textPrimary,
                )
                Text(
                    text = "A quiet walk, a little sunlight,\nand a moment worth keeping.",
                    style = ReliveTheme.typography.body,
                    color = momentColors.textSecondary,
                )
                PreviewLandscape(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dims.timelineTheme.previewMediaHeight)
                        .clip(RoundedCornerShape(dims.radii.small)),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeartGlyph(
                        size = dims.icon.sm,
                        color = momentColors.accent,
                        strokeWidth = dims.stroke.icon,
                        filled = true,
                    )
                    Text(
                        text = "  Favourite · 2:40 PM",
                        style = ReliveTheme.typography.eyebrow,
                        color = momentColors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewLandscape(modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors

    Canvas(modifier = modifier) {
        drawRect(Brush.verticalGradient(listOf(colors.accentMuted, colors.bgCanvasGlow)))
        drawPreviewMountain(
            color = colors.tint,
            peaks = listOf(0.64f, 0.48f, 0.66f, 0.52f, 0.68f),
        )
        drawPreviewMountain(
            color = colors.textMuted.copy(alpha = 0.30f),
            peaks = listOf(0.76f, 0.58f, 0.73f, 0.64f, 0.78f),
        )
        drawPreviewMountain(
            color = colors.textPrimary.copy(alpha = 0.66f),
            peaks = listOf(0.90f, 0.54f, 0.88f, 0.62f, 0.92f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewMountain(
    color: Color,
    peaks: List<Float>,
) {
    val path = Path().apply {
        moveTo(0f, size.height)
        peaks.forEachIndexed { index, peak ->
            lineTo(
                x = size.width * index / peaks.lastIndex.coerceAtLeast(1),
                y = size.height * peak,
            )
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

@Composable
private fun TimelineWallpaperGrid(
    wallpapers: List<TimelineWallpaper>,
    selectedWallpaper: TimelineWallpaper,
    isLocked: (TimelineWallpaper) -> Boolean,
    onWallpaperClick: (TimelineWallpaper) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        wallpapers.chunked(2).forEach { rowWallpapers ->
            Row(horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm)) {
                rowWallpapers.forEach { wallpaper ->
                    TimelineWallpaperOption(
                        wallpaper = wallpaper,
                        selected = wallpaper == selectedWallpaper,
                        locked = isLocked(wallpaper),
                        onClick = { onWallpaperClick(wallpaper) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowWallpapers.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TimelineWallpaperOption(
    wallpaper: TimelineWallpaper,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val pickerColors = timelineThemePickerPalette(ReliveTheme.isDark)
    val shape = RoundedCornerShape(dims.radii.medium)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dims.timelineTheme.optionHeight)
            .clip(shape)
            .background(colors.surfaceCard)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(wallpaper.label())
                    if (selected) append(", selected")
                    if (locked) append(", Relive Pro")
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.timelineTheme.optionArtworkHeight),
            ) {
                TimelineWallpaperThumbnail(wallpaper)
                when {
                    selected -> TimelineWallpaperSelectedBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(dims.spacing.sm),
                    )
                    locked -> TimelineWallpaperLockBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(dims.spacing.sm),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = dims.spacing.md, vertical = dims.spacing.xs),
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = wallpaper.label(),
                        style = ReliveTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = wallpaper.description(),
                        style = ReliveTheme.typography.eyebrow,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (locked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .height(dims.timelineTheme.proBadgeHeight)
                            .background(
                                pickerColors.proBadgeBackground,
                                RoundedCornerShape(dims.radii.full),
                            )
                            .padding(horizontal = dims.spacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Pro",
                            style = ReliveTheme.typography.eyebrow,
                            color = pickerColors.proBadgeForeground,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = if (selected) {
                        dims.stroke.cardOuter * 2
                    } else {
                        dims.stroke.hairline
                    },
                    color = if (selected) colors.accent else colors.borderMuted,
                    shape = shape,
                ),
        )
    }
}

@Composable
private fun TimelineWallpaperSelectedBadge(modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors

    Box(
        modifier = modifier
            .size(dims.timelineTheme.selectionBadgeSize)
            .background(colors.accent, RoundedCornerShape(dims.radii.full)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(dims.icon.md)) {
            val stroke = dims.stroke.iconBold.toPx()
            drawLine(
                color = colors.textOnAccent,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height * 0.52f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.70f),
                strokeWidth = stroke,
            )
            drawLine(
                color = colors.textOnAccent,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.70f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.31f),
                strokeWidth = stroke,
            )
        }
    }
}

@Composable
private fun TimelineWallpaperLockBadge(modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors

    Box(
        modifier = modifier
            .size(dims.timelineTheme.lockBadgeSize)
            .background(colors.surfaceCard, RoundedCornerShape(dims.radii.full)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ProfileIcons.Lock,
            contentDescription = null,
            modifier = Modifier.size(dims.icon.sm),
            tint = colors.textPrimary,
        )
    }
}

@Composable
private fun ReliveProThemeBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val pickerColors = timelineThemePickerPalette(ReliveTheme.isDark)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = dims.timelineTheme.promoMinHeight)
            .clip(RoundedCornerShape(dims.radii.large))
            .background(pickerColors.bannerBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = dims.spacing.lg, vertical = dims.spacing.md)
            .semantics { contentDescription = "More themes with Relive Pro" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(dims.timelineTheme.promoIconSize)
                .background(
                    pickerColors.promotionalAccent,
                    RoundedCornerShape(dims.radii.small),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ProfileIcons.Crown,
                contentDescription = null,
                modifier = Modifier.size(dims.icon.md),
                tint = pickerColors.onPromotionalAccent,
            )
        }
        Column(
            modifier = Modifier
                .padding(start = dims.spacing.lg)
                .weight(1f),
        ) {
            Text(
                text = "More themes with Relive Pro",
                style = ReliveTheme.typography.body,
                color = colors.textPrimary,
            )
            Text(
                text = "Unlock all backgrounds and make every timeline uniquely yours.",
                style = ReliveTheme.typography.caption,
                color = colors.textSecondary,
            )
        }
        ForwardGlyph(dims.icon.md, colors.textSecondary, dims.stroke.icon)
    }
}

@Composable
private fun TimelineWallpaperThumbnail(wallpaper: TimelineWallpaper) {
    TimelineWallpaperSurface(
        wallpaper = wallpaper,
        modifier = Modifier.fillMaxSize(),
    ) {}
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

private fun TimelineWallpaper.description(): String = when (this) {
    TimelineWallpaper.WarmCream -> "Soft and timeless"
    TimelineWallpaper.BlushPink -> "Bright and cheerful"
    TimelineWallpaper.SageGreen -> "Calm and natural"
    TimelineWallpaper.Lavender -> "Peaceful and cozy"
    TimelineWallpaper.PowderBlue -> "Light and airy"
    TimelineWallpaper.SoftPeach -> "Warm and gentle"
    TimelineWallpaper.MidnightNavy -> "Deep and reflective"
    TimelineWallpaper.Evergreen -> "Fresh and serene"
    TimelineWallpaper.MauveDusk -> "Quiet and romantic"
    TimelineWallpaper.TerracottaGlow -> "Earthy and warm"
    TimelineWallpaper.CharcoalMist -> "Soft and dramatic"
    TimelineWallpaper.CoralBloom -> "Lively and tender"
    TimelineWallpaper.AquaSky -> "Clear and refreshing"
    TimelineWallpaper.GoldenHour -> "Glowing and nostalgic"
    TimelineWallpaper.VioletHaze -> "Dreamy and gentle"
    TimelineWallpaper.SapphireBlue -> "Rich and tranquil"
}
