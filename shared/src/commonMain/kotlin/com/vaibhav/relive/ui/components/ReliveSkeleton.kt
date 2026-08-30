package com.vaibhav.relive.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import com.vaibhav.relive.ui.theme.ReliveOpacity
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.durationSpec
import com.vaibhav.relive.ui.theme.spec

/**
 * A warm, token-colored loading placeholder. Its pulse is deliberately disabled when the
 * platform requests reduced motion, leaving the same stable silhouette in place.
 */
@Composable
fun ReliveSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape,
) {
    val colors = ReliveTheme.colors
    val motion = ReliveTheme.motion
    val pulseAlpha = if (ReliveTheme.reduceMotion) {
        ReliveOpacity.Low
    } else {
        val transition = rememberInfiniteTransition(label = "reliveSkeletonPulse")
        val pulseSpec = motion.durationSpec<Float>(
            reduceMotion = ReliveTheme.reduceMotion,
            full = tween(
                durationMillis = motion.durations.long2,
                easing = motion.easings.standard,
            ),
        )
        val alpha by transition.animateFloat(
            initialValue = ReliveOpacity.Low,
            targetValue = ReliveOpacity.Medium,
            animationSpec = infiniteRepeatable(
                animation = pulseSpec,
                repeatMode = RepeatMode.Reverse,
            ),
            label = "reliveSkeletonPulseAlpha",
        )
        alpha
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceCardTranslucent)
            .background(colors.surfaceOverlay.copy(alpha = pulseAlpha)),
    )
}

/**
 * Keeps a loading silhouette present until content is ready, then fades the content in using
 * the Phase 0.4 medium-1 motion token. Screens wire this in during Phase 6.
 */
@Composable
fun ReliveSkeletonContent(
    isLoading: Boolean,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val motion = ReliveTheme.motion
    val fadeSpec = motion.spec<Float>(
        reduceMotion = ReliveTheme.reduceMotion,
        full = tween(
            durationMillis = motion.durations.medium1,
            easing = motion.easings.standard,
        ),
    )
    AnimatedContent(
        targetState = isLoading,
        transitionSpec = { fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec) },
        label = "reliveSkeletonContent",
    ) { loading ->
        if (loading) skeleton() else content()
    }
}

/** Loading silhouette for the timeline-home cover cards and their editorial metadata. */
@Composable
fun TimelineHomeSkeleton(modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier.padding(dims.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
    ) {
        TimelineHomeSkeletonCard()
        TimelineHomeSkeletonCard()
    }
}

@Composable
private fun TimelineHomeSkeletonCard() {
    val dims = ReliveTheme.dimensions
    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.sm)) {
        ReliveSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.timelineHome.allMediaHeight),
            shape = ReliveTheme.shapes.card,
        )
        SkeletonLine(dims.skeleton.mediumLineWidthFraction)
        SkeletonLine(dims.skeleton.longLineWidthFraction)
        SkeletonLine(dims.skeleton.shortLineWidthFraction)
    }
}

/** Loading silhouette for a timeline header, rail, and Moment cards. */
@Composable
fun TimelineDetailSkeleton(modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier.padding(horizontal = dims.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(dims.timeline.itemGap),
    ) {
        ReliveSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.timeline.coverHeroHeight),
            shape = ReliveTheme.shapes.card,
        )
        TimelineMomentSkeleton()
        TimelineMomentSkeleton()
    }
}

@Composable
private fun TimelineMomentSkeleton() {
    val dims = ReliveTheme.dimensions
    Row(horizontalArrangement = Arrangement.spacedBy(dims.timeline.contentInset)) {
        Column(
            modifier = Modifier.width(dims.skeleton.timelineDetailDotSize),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReliveSkeletonBox(
                modifier = Modifier.fillMaxWidth().height(dims.skeleton.timelineDetailDotSize),
                shape = ReliveTheme.shapes.pill,
            )
            ReliveSkeletonBox(
                modifier = Modifier
                    .width(dims.skeleton.timelineDetailRailWidth)
                    .height(dims.skeleton.timelineDetailMediaHeight),
                shape = ReliveTheme.shapes.pill,
            )
        }
        Column(
            modifier = Modifier.weight(dims.skeleton.fullWidthFraction),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            SkeletonLine(dims.skeleton.shortLineWidthFraction)
            SkeletonLine(dims.skeleton.mediumLineWidthFraction, title = true)
            SkeletonLine(dims.skeleton.longLineWidthFraction)
            ReliveSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.skeleton.timelineDetailMediaHeight),
                shape = ReliveTheme.shapes.card,
            )
        }
    }
}

/** Loading silhouette for Rediscover's hero and horizontally scrolling collection cards. */
@Composable
fun RediscoverSkeleton(modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier.padding(vertical = dims.spacing.md),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.xl),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = dims.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            SkeletonLine(dims.skeleton.mediumLineWidthFraction, title = true)
            ReliveSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.rediscover.heroMediaHeight),
                shape = ReliveTheme.shapes.card,
            )
            SkeletonLine(dims.skeleton.longLineWidthFraction)
        }
        Row(
            modifier = Modifier.padding(start = dims.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
        ) {
            RediscoverShelfSkeletonCard()
            RediscoverShelfSkeletonCard()
        }
    }
}

@Composable
private fun RediscoverShelfSkeletonCard() {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier.fillMaxWidth(dims.skeleton.rediscoverShelfCardWidthFraction),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        ReliveSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.rediscover.compactMediaHeight),
            shape = ReliveTheme.shapes.card,
        )
        SkeletonLine(dims.skeleton.mediumLineWidthFraction)
        SkeletonLine(dims.skeleton.shortLineWidthFraction)
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, title: Boolean = false) {
    val dims = ReliveTheme.dimensions
    ReliveSkeletonBox(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(if (title) dims.skeleton.titleLineHeight else dims.skeleton.lineHeight),
        shape = ReliveTheme.shapes.pill,
    )
}

@Preview
@Composable
private fun ReliveSkeletonPreview() {
    ReliveTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            TimelineHomeSkeleton()
            TimelineDetailSkeleton()
            RediscoverSkeleton()
        }
    }
}
