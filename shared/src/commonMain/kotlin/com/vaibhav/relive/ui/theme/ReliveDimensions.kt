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
    val cardOuterRadius: Dp = 28.dp,
    /** Resting elevation for a Rediscover card, lifting it off the backdrop like a print. */
    val cardElevation: Dp = 20.dp,
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
    val compactCardWidth: Dp = 240.dp,
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

/** Layout tokens for the compact, editorial timeline appearance picker. */
@Immutable
data class ReliveTimelineThemeDimensions(
    val previewHeight: Dp = 220.dp,
    val previewMediaHeight: Dp = 76.dp,
    val optionHeight: Dp = 132.dp,
    val optionArtworkHeight: Dp = 84.dp,
    val selectionBadgeSize: Dp = 28.dp,
    val lockBadgeSize: Dp = 28.dp,
    val proBadgeHeight: Dp = 20.dp,
    val promoIconSize: Dp = 40.dp,
    val promoMinHeight: Dp = 72.dp,
    val noteRotationDegrees: Float = -7f,
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

/** Layout tokens for the custom-timeline creation dialog. */
@Immutable
data class ReliveTimelineCreationDimensions(
    val maxWidth: Dp = 640.dp,
    val maxHeight: Dp = 640.dp,
    val contentInset: Dp = 24.dp,
    val headerCloseSize: Dp = 48.dp,
    val headerCloseVisualSize: Dp = 36.dp,
    val headerCloseGlyphSize: Dp = 16.dp,
    val fieldHeight: Dp = 56.dp,
    val coverZoneHeight: Dp = 160.dp,
    val emptyIconSurfaceSize: Dp = 64.dp,
    val emptyIconSize: Dp = 24.dp,
    val primaryActionHeight: Dp = 56.dp,
    val primaryActionMaxWidth: Dp = 240.dp,
)

@Immutable
data class ReliveOnboardingDimensions(
    val pageHorizontalPadding: Dp = 24.dp,
    val numberedPageTopPadding: Dp = 16.dp,
    val headerHeight: Dp = 48.dp,
    val bottomControlsHeight: Dp = 72.dp,
    val progressWidth: Dp = 92.dp,
    val progressHeight: Dp = 16.dp,
    val progressDot: Dp = 6.dp,
    val progressDotActive: Dp = 8.dp,
    val arrowActionSize: Dp = 56.dp,
    val primaryActionWidth: Dp = 280.dp,
    val primaryActionHeight: Dp = 64.dp,
    val copyMaxWidth: Dp = 440.dp,
    val compactHeightThreshold: Dp = 760.dp,
    val captureFeatureHeight: Dp = 96.dp,
    val captureIconMedallionSize: Dp = 48.dp,
    val captureIconSize: Dp = 24.dp,
    val featureTileRadius: Dp = 18.dp,
    val notificationCardHeight: Dp = 92.dp,
    val rediscoverHeroHeight: Dp = 260.dp,
    val privacyEmblemSize: Dp = 218.dp,
    val privacyFeatureHeight: Dp = 86.dp,
    val finalSheetHeight: Dp = 176.dp,
    val finalSheetRadius: Dp = 36.dp,
    val pressedScale: Float = 0.96f,
)

/** Layout values unique to the Relive Pro editorial paywall. */
@Immutable
data class ReliveProDimensions(
    val heroArtworkWidth: Dp = 112.dp,
    val heroArtworkHeight: Dp = 136.dp,
    val heroPhotoWidth: Dp = 72.dp,
    val heroPhotoHeight: Dp = 88.dp,
    val heroPhotoImageHeight: Dp = 60.dp,
    val crownBadgeSize: Dp = 44.dp,
    val featureStageHeight: Dp = 244.dp,
    val featurePeekInset: Dp = 96.dp,
    val featureFocusInset: Dp = 12.dp,
    val featureRestingScale: Float = 0.86f,
    val featureRestingAlpha: Float = 0.72f,
    val featureStoryHeight: Dp = 58.dp,
    val featureStoryTravel: Dp = 12.dp,
    val featureStoryBubble: Dp = 52.dp,
    val featureStoryBubbleSmall: Dp = 44.dp,
    val featureStoryCardWidth: Dp = 48.dp,
    val featureIconSurfaceSize: Dp = 56.dp,
    val featureIconSize: Dp = 32.dp,
    val planRowMinHeight: Dp = 64.dp,
    val primaryActionHeight: Dp = 56.dp,
    val selectionIndicatorSize: Dp = 20.dp,
    val selectionIndicatorDotSize: Dp = 10.dp,
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
    val timelineTheme: ReliveTimelineThemeDimensions = ReliveTimelineThemeDimensions(),
    val search: ReliveSearchDimensions = ReliveSearchDimensions(),
    val composer: ReliveComposerDimensions = ReliveComposerDimensions(),
    val timelineCreation: ReliveTimelineCreationDimensions = ReliveTimelineCreationDimensions(),
    val onboarding: ReliveOnboardingDimensions = ReliveOnboardingDimensions(),
    val pro: ReliveProDimensions = ReliveProDimensions(),
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
