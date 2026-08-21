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

@Immutable
data class ReliveRadii(
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val pill: Dp = 999.dp,
)

@Immutable
data class ReliveIconSizes(
    val sm: Dp = 12.dp,
    val md: Dp = 20.dp,
    val lg: Dp = 24.dp,
)

@Immutable
data class ReliveStrokes(
    val hairline: Dp = 1.dp,
    val icon: Dp = 1.5.dp,
    val iconBold: Dp = 2.dp,
)

@Immutable
data class ReliveTimelineDimensions(
    val railWidth: Dp = 1.dp,
    val dotSize: Dp = 10.dp,
    val plusSize: Dp = 32.dp,
    val itemGap: Dp = 48.dp,
    val contentInset: Dp = 32.dp,
    val horizontalPadding: Dp = 24.dp,
)

@Immutable
data class ReliveMediaDimensions(
    val ratioWide: Float = 2f,
    val ratioSquare: Float = 1f,
    val carouselPeekFraction: Float = 0.85f,
    val sepiaAmount: Float = 0.3f,
    val carouselHeight: Dp = 240.dp,
    val indicatorDotActive: Dp = 6.dp,
    val indicatorDotInactive: Dp = 5.dp,
    val indicatorSpacing: Dp = 3.dp,
)

@Immutable
data class ReliveDimensions(
    val spacing: ReliveSpacing = ReliveSpacing(),
    val radii: ReliveRadii = ReliveRadii(),
    val icon: ReliveIconSizes = ReliveIconSizes(),
    val stroke: ReliveStrokes = ReliveStrokes(),
    val timeline: ReliveTimelineDimensions = ReliveTimelineDimensions(),
    val media: ReliveMediaDimensions = ReliveMediaDimensions(),
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
