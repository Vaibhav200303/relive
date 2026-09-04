package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ReliveSpacing(
    val none: Dp = 0.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
)

/**
 * Corner radius scale — the full M3 10-step system.
 *
 * Names match the M3 spec so components can pick the tier they actually mean instead of
 * a generic sm/md/lg. Legacy names remain as `@Deprecated` computed aliases so nothing
 * breaks; migrate them opportunistically. Note: the old `lg` was **20dp** — that is M3
 * `largeIncreased`, not M3 `large` (16dp). A later audit step confirms per call site.
 */
@Immutable
data class ReliveRadii(
    val none: Dp = 0.dp,
    val xs: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val largeIncreased: Dp = 20.dp,
    val xl: Dp = 28.dp,
    val xlIncreased: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    /** Fully rounded — value large enough to fully round any realistic container. */
    val full: Dp = 999.dp,
) {
    @Deprecated("use small (M3 8dp)", ReplaceWith("small"))
    val sm: Dp get() = small

    @Deprecated("use medium (M3 12dp)", ReplaceWith("medium"))
    val md: Dp get() = medium

    @Deprecated(
        "use largeIncreased (M3 20dp); audit call site — M3 `large` is 16dp",
        ReplaceWith("largeIncreased"),
    )
    val lg: Dp get() = largeIncreased

    @Deprecated("use xl (M3 28dp)", ReplaceWith("xl"))
    val dialog: Dp get() = xl

    @Deprecated("use medium (M3 12dp)", ReplaceWith("medium"))
    val menu: Dp get() = medium

    @Deprecated("use full (fully rounded)", ReplaceWith("full"))
    val pill: Dp get() = full
}

@Immutable
data class ReliveIconSizes(
    val sm: Dp = 12.dp,
    val md: Dp = 20.dp,
    val lg: Dp = 24.dp,
)

@Immutable
data class ReliveStrokes(
    val hairline: Dp = 1.dp,
    val cardOuter: Dp = 1.dp,
    val icon: Dp = 1.5.dp,
    val iconBold: Dp = 2.dp,
)

@Immutable
data class ReliveTimelineDimensions(
    val railWidth: Dp = 1.dp,
    val dotSize: Dp = 10.dp,
    val plusSize: Dp = 32.dp,
    val coverHeroHeight: Dp = 300.dp,
    val itemGap: Dp = 48.dp,
    val contentInset: Dp = 32.dp,
    val horizontalPadding: Dp = 24.dp,
)

@Immutable
data class ReliveTimelineHomeDimensions(
    val allMediaHeight: Dp = 300.dp,
    val customMediaHeight: Dp = 232.dp,
    val infoAreaMinHeight: Dp = 112.dp,
    val createTimelineGlyphSize: Dp = 32.dp,
    /** Resting elevation for a timeline card so it lifts off the canvas as a distinct plane. */
    val cardElevation: Dp = 8.dp,
)

@Immutable
data class ReliveRediscoverDimensions(
    val cardOuterRadius: Dp = 20.dp,
    /** Resting elevation for a Rediscover card, lifting it off the backdrop like a print. */
    val cardElevation: Dp = 8.dp,
    val heroMediaHeight: Dp = 280.dp,
    val compactMediaHeight: Dp = 144.dp,
    val compactInfoAreaHeight: Dp = 128.dp,
    /**
     * The Home row's full-bleed collection card (ADR-0064): one cover surface with the title
     * overlaid, sized to what the former media area plus info area occupied so the row's
     * footprint on Home is unchanged.
     */
    val compactCardHeight: Dp = 272.dp,
    val heroInfoAreaMinHeight: Dp = 128.dp,
    val favoriteShelfCardHeight: Dp = 272.dp,
    val compactCardWidth: Dp = 208.dp,
    val favoriteShelfCardWidthFraction: Float = 0.68f,
    val onThisDayShelfCardWidthFraction: Float = 0.82f,
    val waveformHeight: Dp = 44.dp,
)

@Immutable
data class ReliveProfileDimensions(
    val avatarSize: Dp = 80.dp,
    val appearancePreviewSize: Dp = 44.dp,
    val appearanceSelectionSize: Dp = 56.dp,
    val appearanceItemWidth: Dp = 80.dp,
)

@Immutable
data class ReliveSearchDimensions(
    val containerHeight: Dp = 56.dp,
)

@Immutable
data class ReliveComposerDimensions(
    /** Visible tag field height; its containing touch target remains 48dp. */
    val tagVisibleHeight: Dp = 36.dp,
)

@Immutable
data class ReliveFloatingToolbarDimensions(
    val height: Dp = 64.dp,
    val compactWidth: Dp = 64.dp,
    val newExpandedWidth: Dp = 136.dp,
    val newLabelMinimumWidth: Dp = 88.dp,
    val controlGap: Dp = 8.dp,
    val indicatorHeight: Dp = 48.dp,
)

@Immutable
data class ReliveMediaDimensions(
    val ratioWide: Float = 2f,
    val sepiaAmount: Float = 0.3f,
    // Adaptive collage (ADR-0019). Multi-media collages only: outer border
    // and internal tile dividers share the same weight so adjacent tiles
    // yield ONE ~4dp separator rather than two overlapping strokes.
    val collageGap: Dp = 4.dp,
    val collageBorder: Dp = 4.dp,
    val collageSingleMaxHeight: Dp = 420.dp,
    val collageTileAspectSquare: Float = 1f,
    val collageDominantAspect: Float = 4f / 3f,
    val collageVideoAspect: Float = 16f / 9f,
    val collageAudioAspect: Float = 4f / 3f,
    // Composer (new-moment) adaptive preview. Both are MAXIMUMS — the
    // preview shrink-wraps around media that would otherwise render
    // smaller. Never used as forced dimensions.
    val composerPreviewMaxHeight: Dp = 420.dp,
    val composerPlaceholderFallbackHeight: Dp = 180.dp,
    val composerPlaceholderFallbackAspect: Float = 4f / 3f,
    // Timeline single-media adaptive preview. MAX bounds only — the tile
    // shrink-wraps around media that fits, and scales proportionally when
    // either bound is exceeded. Multi-media collages continue to use the
    // collage* tokens above.
    val timelineSinglePreviewMaxHeight: Dp = 420.dp,
    val timelineSingleAudioHeight: Dp = 200.dp,
    val timelineSingleFallbackHeight: Dp = 180.dp,
    val timelineSingleFallbackAspect: Float = 4f / 3f,
)

/** Layout tokens for loading silhouettes that reserve the same visual space as their content. */
@Immutable
data class ReliveSkeletonDimensions(
    val fullWidthFraction: Float = 1f,
    val lineHeight: Dp = 12.dp,
    val titleLineHeight: Dp = 24.dp,
    val shortLineWidthFraction: Float = 0.35f,
    val mediumLineWidthFraction: Float = 0.6f,
    val longLineWidthFraction: Float = 0.82f,
    val timelineDetailMediaHeight: Dp = 180.dp,
    val timelineDetailRailWidth: Dp = 1.dp,
    val timelineDetailDotSize: Dp = 10.dp,
    val rediscoverShelfCardWidthFraction: Float = 0.68f,
)

@Immutable
data class ReliveDimensions(
    val spacing: ReliveSpacing = ReliveSpacing(),
    val radii: ReliveRadii = ReliveRadii(),
    val icon: ReliveIconSizes = ReliveIconSizes(),
    val stroke: ReliveStrokes = ReliveStrokes(),
    val timeline: ReliveTimelineDimensions = ReliveTimelineDimensions(),
    val timelineHome: ReliveTimelineHomeDimensions = ReliveTimelineHomeDimensions(),
    val rediscover: ReliveRediscoverDimensions = ReliveRediscoverDimensions(),
    val profile: ReliveProfileDimensions = ReliveProfileDimensions(),
    val search: ReliveSearchDimensions = ReliveSearchDimensions(),
    val composer: ReliveComposerDimensions = ReliveComposerDimensions(),
    val floatingToolbar: ReliveFloatingToolbarDimensions = ReliveFloatingToolbarDimensions(),
    val media: ReliveMediaDimensions = ReliveMediaDimensions(),
    val skeleton: ReliveSkeletonDimensions = ReliveSkeletonDimensions(),
    val minTouchTarget: Dp = 48.dp,
)

val DefaultReliveDimensions: ReliveDimensions = ReliveDimensions()

object ReliveOpacity {
    const val Full: Float = 1.0f
    const val VeryHigh: Float = 0.9f
    const val High: Float = 0.7f
    const val Medium: Float = 0.5f
    const val Low: Float = 0.4f
}
