package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.share.IncomingSharePayload
import com.vaibhav.relive.platform.share.IncomingShareState
import com.vaibhav.relive.presentation.share.SharePickerTapOutcome
import com.vaibhav.relive.presentation.share.resolveSharePickerTap
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.presentation.timelinehome.matchingTimelineQuery
import com.vaibhav.relive.ui.components.ReliveSkeletonContent
import com.vaibhav.relive.ui.components.TimelineHomeSkeleton
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.EmptyShareTimelinesPlaceholder
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.rememberGrainBrush
import com.vaibhav.relive.ui.theme.spec

/**
 * The surface shown when content is shared INTO Relive: the Timelines screen itself, minus every
 * control that does not choose a destination (ADR-0069). Same canvas, same search field, same
 * cards in the same order, and the same container transform into the timeline that receives the
 * share. The only differences are the heading naming the choice and a Back that returns to the app
 * the content came from — there is no profile affordance, no create control, and no long-press
 * rename/delete selection here.
 *
 * The shared content itself is not previewed: choosing a timeline lands directly in that
 * timeline's composer with the media already attached, which is where it is reviewed before Keep
 * Moment saves it.
 */
@Composable
fun ShareTimelinePickerScreen(
    shareState: IncomingShareState,
    summaries: List<TimelineHomeSummary>,
    timelinesLoading: Boolean,
    mediaStore: MediaStore,
    hasDraft: (CurrentTimeline) -> Boolean,
    onSelect: (Timeline.Custom, IncomingSharePayload) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    /** Container-transform bounds for the card that opens a timeline, supplied by the host. */
    cardContainerModifier: @Composable (Timeline.Custom) -> Modifier = { Modifier },
) {
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val isDark = ReliveTheme.isDark
    var query by remember { mutableStateOf("") }
    // The list is on screen from the first frame, so a timeline can be chosen while the payload is
    // still being read; that choice is held here and commits the moment the payload lands.
    var pending by remember { mutableStateOf<Timeline.Custom?>(null) }
    LaunchedEffect(shareState, pending) {
        val timeline = pending ?: return@LaunchedEffect
        when (val outcome = resolveSharePickerTap(shareState)) {
            is SharePickerTapOutcome.Commit -> {
                pending = null
                onSelect(timeline, outcome.payload)
            }
            SharePickerTapOutcome.Hold -> Unit
            SharePickerTapOutcome.Drop -> pending = null
        }
    }

    Box(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        // The same film grain the Timelines canvas wears, at the edge of perception.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = rememberGrainBrush(isDark),
                    alpha = if (isDark) 0.07f else 0.05f,
                ),
        )
        Column(Modifier.fillMaxSize()) {
            ShareTimelinePickerHeader(onCancel)
            AnimatedContent(
                targetState = shareState as? IncomingShareState.Error,
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
            ) { error ->
                if (error != null) {
                    ShareError(error.message, onRetry, onCancel)
                } else {
                    Column(Modifier.fillMaxSize()) {
                        TimelineHomeSearchBar(query = query, onQueryChange = { query = it })
                        ReliveSkeletonContent(
                            isLoading = timelinesLoading,
                            skeleton = { TimelineHomeSkeleton() },
                        ) {
                            ShareTimelineList(
                                summaries = remember(summaries, query) {
                                    summaries.matchingTimelineQuery(query)
                                },
                                hasCustomTimelines = summaries.isNotEmpty(),
                                query = query,
                                mediaStore = mediaStore,
                                hasDraft = hasDraft,
                                pendingTimeline = pending,
                                onSelect = { pending = it },
                                cardContainerModifier = cardContainerModifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The Timelines list exactly: the same eyebrow heading, the same card in the same order, the same
 * empty and no-match states. It carries no selection mode, so a card has only one gesture — the
 * tap that sends the share to that timeline.
 */
@Composable
private fun ShareTimelineList(
    summaries: List<TimelineHomeSummary>,
    hasCustomTimelines: Boolean,
    query: String,
    mediaStore: MediaStore,
    hasDraft: (CurrentTimeline) -> Boolean,
    pendingTimeline: Timeline.Custom?,
    onSelect: (Timeline.Custom) -> Unit,
    cardContainerModifier: @Composable (Timeline.Custom) -> Modifier,
) {
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    LazyColumn(
        // No floating navigation bar over this surface, so the list ends at the normal margin
        // rather than reserving room for chrome that is not there.
        contentPadding = PaddingValues(
            start = dims.spacing.xl,
            top = dims.spacing.md,
            end = dims.spacing.xl,
            bottom = dims.spacing.huge,
        ),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.xl),
    ) {
        item(key = "your-timeline-heading") {
            Text(
                text = "YOUR TIMELINE",
                style = ReliveTheme.typography.title,
                color = ReliveTheme.colors.textPrimary,
            )
        }
        if (summaries.isEmpty()) {
            if (query.isNotBlank() || hasCustomTimelines) {
                item(key = "no-matching-custom-timelines") {
                    Text(
                        text = "No timelines found.",
                        style = ReliveTheme.typography.subtitle,
                        color = ReliveTheme.colors.textSecondary,
                    )
                }
            } else {
                item(key = "empty-custom-timelines") {
                    EmptyShareTimelinesPlaceholder(
                        modifier = Modifier.padding(vertical = dims.spacing.lg),
                    )
                }
            }
        } else {
            items(summaries, key = { (it.timeline as Timeline.Custom).id.value }) { summary ->
                val timeline = summary.timeline as Timeline.Custom
                TimelineHomeCard(
                    summary,
                    mediaStore,
                    onClick = {
                        haptics.perform(ReliveHapticCue.Selection)
                        onSelect(timeline)
                    },
                    // A held tap keeps the card lit while the payload finishes being read, so a
                    // choice made early is visibly taken rather than silently swallowed.
                    isSelected = pendingTimeline?.id == timeline.id,
                    showDraftIndicator = hasDraft(CurrentTimeline.Custom(timeline.id)),
                    modifier = cardContainerModifier(timeline),
                )
            }
        }
    }
}

/**
 * The Timelines header geometry exactly — no app-bar band, controls floating over the canvas
 * gradient — with the wordmark replaced by the heading that names the choice, and Back where the
 * profile affordance sits. Nothing occupies the trailing slot.
 */
@Composable
private fun ShareTimelinePickerHeader(onBack: () -> Unit) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(dims.minTouchTarget)
                .semantics { contentDescription = "Cancel shared moment" },
        ) {
            BackGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
        }
        Text(
            text = "Choose a timeline",
            style = ReliveTheme.typography.title,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                // Kept clear of the Back control on the narrowest screens.
                .padding(horizontal = dims.minTouchTarget),
        )
    }
}

@Composable
private fun ShareError(message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier.fillMaxSize().padding(dims.spacing.xl),
        verticalArrangement = Arrangement.Center,
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
            modifier = Modifier.padding(top = dims.spacing.sm),
        )
        Row(modifier = Modifier.padding(top = dims.spacing.lg)) {
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
