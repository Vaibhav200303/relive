package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.platform.media.ImageSourceThumbnail
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RawMedia
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.VideoSourceThumbnail
import com.vaibhav.relive.platform.share.IncomingSharePayload
import com.vaibhav.relive.platform.share.IncomingShareState
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveCoverLabelScrim
import com.vaibhav.relive.ui.theme.ReliveGeneratedCover
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.rememberGrainBrush
import com.vaibhav.relive.ui.theme.spec

/**
 * The surface shown when content is shared INTO Relive: the shared media itself as a hero, and
 * the timelines it could live in as the same full-bleed cover carousel Home's Rediscover row
 * browses with — picking a destination should feel like the rest of the app, not a file dialog.
 */
@Composable
fun ShareTimelinePickerScreen(
    shareState: IncomingShareState,
    summaries: List<TimelineHomeSummary>,
    mediaStore: MediaStore,
    hasDraft: (CurrentTimeline) -> Boolean,
    onSelect: (Timeline) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val isDark = ReliveTheme.isDark
    Box(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        // The same film grain the Timeline Home canvas wears, at the edge of perception.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = rememberGrainBrush(isDark),
                    alpha = if (isDark) 0.07f else 0.05f,
                ),
        )
        AnimatedContent(
            targetState = shareState,
            transitionSpec = {
                fadeIn(
                    animationSpec = motion.spec(
                        reduceMotion = reduceMotion,
                        full = tween(
                            durationMillis = motion.durations.medium2,
                            easing = motion.easings.emphasizedDecelerate,
                        ),
                    ),
                ) togetherWith fadeOut(
                    animationSpec = motion.spec(
                        reduceMotion = reduceMotion,
                        full = tween(
                            durationMillis = motion.durations.short4,
                            easing = motion.easings.emphasizedAccelerate,
                        ),
                    ),
                )
            },
            label = "incoming share state",
        ) { state ->
            when (state) {
                IncomingShareState.Idle, IncomingShareState.Reading -> ShareLoading(onCancel)
                is IncomingShareState.Error -> ShareError(state.message, onRetry, onCancel)
                is IncomingShareState.Ready -> ShareReadyContent(
                    payload = state.payload,
                    summaries = summaries,
                    mediaStore = mediaStore,
                    hasDraft = hasDraft,
                    onSelect = onSelect,
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun ShareLoading(onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        ShareHeader(onCancel)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ReliveTheme.colors.accent)
        }
    }
}

@Composable
private fun ShareError(message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        ShareHeader(onCancel)
        Column(
            modifier = Modifier.fillMaxSize().padding(ReliveTheme.dimensions.spacing.xl),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Couldn't read what you shared",
                style = type.title,
                color = colors.textPrimary,
            )
            Text(
                text = message,
                style = type.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = ReliveTheme.dimensions.spacing.sm),
            )
            Row(modifier = Modifier.padding(top = ReliveTheme.dimensions.spacing.lg)) {
                TextButton(onClick = onCancel) {
                    Text("Close", style = type.action, color = colors.textSecondary)
                }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.textOnAccent,
                    ),
                ) { Text("Try again", style = type.prominentAction) }
            }
        }
    }
}

@Composable
private fun ShareReadyContent(
    payload: IncomingSharePayload,
    summaries: List<TimelineHomeSummary>,
    mediaStore: MediaStore,
    hasDraft: (CurrentTimeline) -> Boolean,
    onSelect: (Timeline) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion

    // One entrance progress drives a soft cascade — voice, then the shared content, then the
    // timelines to keep it in — settling downward-to-still. Reduced motion collapses the whole
    // cascade to the short shared fade with no travel.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = motion.spec(
                reduceMotion = reduceMotion,
                full = tween(
                    durationMillis = motion.durations.extraLong1,
                    easing = motion.easings.emphasizedDecelerate,
                ),
            ),
        )
    }
    fun Modifier.cascade(start: Float): Modifier = graphicsLayer {
        val visible = ((entrance.value - start) / 0.4f).coerceIn(0f, 1f)
        alpha = visible
        if (!reduceMotion) translationY = (1f - visible) * 20.dp.toPx()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ShareHeader(onCancel)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cascade(0f)
                .padding(horizontal = dims.spacing.xl),
        ) {
            Text(
                text = "SHARED WITH RELIVE",
                style = type.eyebrow,
                color = colors.textMuted,
            )
            Text(
                text = "Keep this moment",
                style = type.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = dims.spacing.xs),
            )
            Text(
                text = payloadMetaLabel(payload),
                style = type.subtitle,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = dims.spacing.xs),
            )
        }
        Spacer(Modifier.height(dims.spacing.lg))
        Box(Modifier.cascade(0.15f)) {
            SharedPayloadHero(payload)
        }
        Column(Modifier.cascade(0.3f)) {
            Text(
                text = "Choose a timeline",
                style = type.title,
                color = colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md),
            )
            if (summaries.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = dims.rediscover.compactCardHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else {
                TimelinePickerCarousel(
                    summaries = summaries,
                    mediaStore = mediaStore,
                    hasDraft = hasDraft,
                    onSelect = onSelect,
                )
                if (summaries.none { it.timeline is Timeline.Custom }) {
                    Text(
                        text = "It will be kept in All moments — you can make timelines later.",
                        style = type.subtitle,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(
                            horizontal = dims.spacing.xl,
                            vertical = dims.spacing.md,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(dims.spacing.xl))
    }
}

/**
 * The shared content itself, shown before any of it is processed into the archive: one
 * full-width tile for a single item, the multi-browse carousel for a batch, and a quote card
 * for a text-only share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharedPayloadHero(payload: IncomingSharePayload) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val media = payload.media
    when {
        media.size > 1 -> HorizontalMultiBrowseCarousel(
            state = rememberCarouselState(itemCount = { media.size }),
            preferredItemWidth = dims.rediscover.compactCardWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.shareTimelinePicker.heroHeight),
            itemSpacing = dims.spacing.md,
            contentPadding = PaddingValues(horizontal = dims.spacing.xl),
        ) { index ->
            Box(
                Modifier
                    .fillMaxSize()
                    .maskClip(RoundedCornerShape(dims.rediscover.cardOuterRadius))
                    .background(colors.surfaceCard),
            ) {
                SharedMediaTile(media[index])
            }
        }
        media.size == 1 -> {
            val heroShape = RoundedCornerShape(dims.radii.largeIncreased)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.spacing.xl)
                    .height(dims.shareTimelinePicker.heroHeight)
                    .shadow(
                        elevation = dims.timelineHome.cardElevation,
                        shape = heroShape,
                        clip = false,
                        ambientColor = colors.shadow,
                        spotColor = colors.shadow,
                    )
                    .clip(heroShape)
                    .background(colors.surfaceCard)
                    .border(dims.stroke.hairline, colors.borderMuted, heroShape),
            ) {
                SharedMediaTile(media.first())
            }
        }
        else -> {
            val quoteShape = RoundedCornerShape(dims.radii.largeIncreased)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.spacing.xl)
                    .heightIn(min = dims.shareTimelinePicker.heroTextMinHeight)
                    .shadow(
                        elevation = dims.timelineHome.cardElevation,
                        shape = quoteShape,
                        clip = false,
                        ambientColor = colors.shadow,
                        spotColor = colors.shadow,
                    )
                    .clip(quoteShape)
                    .background(colors.surfaceCard)
                    .border(dims.stroke.hairline, colors.borderMuted, quoteShape)
                    .padding(dims.spacing.lg),
            ) {
                payload.subject?.takeIf { it.isNotBlank() }?.let { subject ->
                    Text(
                        text = subject,
                        style = ReliveTheme.typography.title,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(dims.spacing.xs))
                }
                Text(
                    text = payload.text?.takeIf { it.isNotBlank() } ?: "A note for your archive",
                    style = ReliveTheme.typography.body,
                    color = colors.textSecondary,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One incoming item rendered from its pre-processing temp file. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.SharedMediaTile(item: RawMedia) {
    val dims = ReliveTheme.dimensions
    when (item.type) {
        MediaType.Image -> ImageSourceThumbnail(item.sourcePath, Modifier.matchParentSize())
        MediaType.Video -> {
            VideoSourceThumbnail(item.sourcePath, Modifier.matchParentSize())
            MediaKindTag("VIDEO", Modifier.align(Alignment.BottomStart).padding(dims.spacing.sm))
        }
        MediaType.Audio -> {
            Box(Modifier.matchParentSize().background(ReliveTheme.colors.surfaceAudio))
            MediaKindTag("AUDIO", Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun MediaKindTag(label: String, modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    Text(
        text = label,
        style = ReliveTheme.typography.tag,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(dims.radii.full))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = dims.spacing.sm, vertical = dims.spacing.xs),
    )
}

/**
 * The destinations, browsed exactly the way Home's Rediscover row is: full-bleed cover cards in
 * a multi-browse carousel, the focal card carrying its name over the shared label scrim. All
 * moments leads; custom timelines follow in their summary order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelinePickerCarousel(
    summaries: List<TimelineHomeSummary>,
    mediaStore: MediaStore,
    hasDraft: (CurrentTimeline) -> Boolean,
    onSelect: (Timeline) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val ordered = remember(summaries) {
        buildList {
            summaries.firstOrNull { it.timeline == Timeline.All }?.let(::add)
            addAll(summaries.filter { it.timeline is Timeline.Custom })
        }
    }
    if (ordered.isEmpty()) return
    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState(itemCount = { ordered.size }),
        preferredItemWidth = dims.rediscover.compactCardWidth,
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.rediscover.compactCardHeight),
        itemSpacing = dims.spacing.md,
        contentPadding = PaddingValues(horizontal = dims.spacing.xl),
    ) { index ->
        val summary = ordered[index]
        val currentTimeline = when (val timeline = summary.timeline) {
            Timeline.All -> CurrentTimeline.All
            is Timeline.Custom -> CurrentTimeline.Custom(timeline.id)
        }
        ShareTimelineCard(
            summary = summary,
            mediaStore = mediaStore,
            showDraftIndicator = hasDraft(currentTimeline),
            onClick = { onSelect(summary.timeline) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarouselItemScope.ShareTimelineCard(
    summary: TimelineHomeSummary,
    mediaStore: MediaStore,
    showDraftIndicator: Boolean,
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    val shape = RoundedCornerShape(dims.rediscover.cardOuterRadius)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .maskClip(shape)
            .background(colors.surfaceCard)
            .clickable {
                haptics.perform(ReliveHapticCue.Selection)
                onClick()
            }
            .semantics { contentDescription = "Add to ${summary.name}" },
    ) {
        // A custom timeline wears its user-chosen cover photo; All — like every collection
        // cover — wears an accent-derived gradient.
        val coverPhoto = (summary.timeline as? Timeline.Custom)?.coverPhotoRef
        if (coverPhoto != null) {
            RelivedImageTile(coverPhoto, mediaStore, Modifier.matchParentSize())
        } else {
            ReliveGeneratedCover(
                stableKey = when (val timeline = summary.timeline) {
                    Timeline.All -> "share-cover-all"
                    is Timeline.Custom -> "share-cover-${timeline.id.value}"
                },
                modifier = Modifier.matchParentSize(),
            )
        }
        // The label belongs to the focal card only, exactly as on Home's Rediscover row: scrim
        // and text share one layer whose alpha rises as the card grows into the large slot.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val info = carouselItemDrawInfo
                    val range = info.maxSize - info.minSize
                    val grown = if (range > 0f) {
                        ((info.size - info.minSize) / range).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    alpha = ((grown - 0.8f) / 0.2f).coerceIn(0f, 1f)
                }
                .background(ReliveCoverLabelScrim),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        // The mask is centred in the item, so pin the label block to its visible
                        // left edge instead of letting letters get sliced mid-glyph.
                        translationX = carouselItemDrawInfo.maskRect.left
                    }
                    .padding(dims.spacing.lg),
            ) {
                if (showDraftIndicator) {
                    Text(
                        text = "DRAFT",
                        style = type.tag,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                Text(
                    text = summary.name,
                    style = type.title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (summary.momentCount == 1L) "1 moment" else "${summary.momentCount} moments",
                    style = type.caption,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun ShareHeader(onCancel: () -> Unit) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(dims.minTouchTarget).semantics { contentDescription = "Cancel shared moment" },
        ) {
            BackGlyph(dims.icon.lg, ReliveTheme.colors.textSecondary, dims.stroke.icon)
        }
        Text(
            text = "Relive",
            style = ReliveTheme.typography.title,
            color = ReliveTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun payloadMetaLabel(payload: IncomingSharePayload): String {
    val images = payload.media.count { it.type == MediaType.Image }
    val videos = payload.media.count { it.type == MediaType.Video }
    val audio = payload.media.count { it.type == MediaType.Audio }
    val hasNote = !payload.text.isNullOrBlank() || !payload.subject.isNullOrBlank()
    val parts = buildList {
        if (images > 0) add(if (images == 1) "1 photo" else "$images photos")
        if (videos > 0) add(if (videos == 1) "1 video" else "$videos videos")
        if (audio > 0) add(if (audio == 1) "1 audio clip" else "$audio audio clips")
        if (hasNote) add("a note")
    }
    return if (parts.isEmpty()) "Ready for your moment" else parts.joinToString(" · ") + " ready to keep"
}
