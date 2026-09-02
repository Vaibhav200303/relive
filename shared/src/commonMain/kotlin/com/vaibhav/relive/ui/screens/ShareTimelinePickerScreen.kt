package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.share.IncomingSharePayload
import com.vaibhav.relive.platform.share.IncomingShareState
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme

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
    Box(Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas)) {
        AnimatedContent(
            targetState = shareState,
            transitionSpec = {
                fadeIn(tween(motion.durations.standardMillis, easing = motion.easings.standard)) togetherWith
                    fadeOut(tween(motion.durations.fastMillis, easing = motion.easings.standard))
            },
            label = "incoming share state",
        ) { state ->
            when (state) {
                IncomingShareState.Idle, IncomingShareState.Reading -> ShareLoading(onCancel)
                is IncomingShareState.Error -> ShareError(state.message, onRetry, onCancel)
                is IncomingShareState.Ready -> ShareTimelineGrid(
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
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        ShareHeader(onCancel)
        Column(
            modifier = Modifier.fillMaxSize().padding(ReliveTheme.dimensions.spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message, style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
            Row(modifier = Modifier.padding(top = ReliveTheme.dimensions.spacing.lg)) {
                TextButton(onClick = onCancel) { Text("Close") }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReliveTheme.colors.accent,
                        contentColor = ReliveTheme.colors.textOnAccent,
                    ),
                ) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun ShareTimelineGrid(
    payload: IncomingSharePayload,
    summaries: List<TimelineHomeSummary>,
    mediaStore: MediaStore,
    hasDraft: (CurrentTimeline) -> Boolean,
    onSelect: (Timeline) -> Unit,
    onCancel: () -> Unit,
) {
    val haptics = rememberReliveHaptics()
    Column(Modifier.fillMaxSize()) {
        ShareHeader(onCancel)
        Text(
            text = "Choose a timeline",
            style = ReliveTheme.typography.title,
            color = ReliveTheme.colors.textPrimary,
            modifier = Modifier.padding(
                start = ReliveTheme.dimensions.spacing.xl,
                top = ReliveTheme.dimensions.spacing.lg,
                end = ReliveTheme.dimensions.spacing.xl,
            ),
        )
        Text(
            text = shareItemLabel(payload),
            style = ReliveTheme.typography.subtitle,
            color = ReliveTheme.colors.textSecondary,
            modifier = Modifier.padding(
                start = ReliveTheme.dimensions.spacing.xl,
                top = ReliveTheme.dimensions.spacing.xs,
                end = ReliveTheme.dimensions.spacing.xl,
            ),
        )
        if (summaries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ReliveTheme.colors.accent)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(ReliveTheme.dimensions.spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.md),
            ) {
                summaries.firstOrNull { it.timeline == Timeline.All }?.let { all ->
                    item(
                        key = "all",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        ShareTimelineCard(
                            summary = all,
                            mediaStore = mediaStore,
                            showDraftIndicator = hasDraft(CurrentTimeline.All),
                            onClick = {
                                haptics.perform(ReliveHapticCue.Selection)
                                onSelect(Timeline.All)
                            },
                        )
                    }
                }
                if (summaries.any { it.timeline is Timeline.Custom }) {
                    item(
                        key = "all-to-custom-gap",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Spacer(Modifier.height(ReliveTheme.dimensions.shareTimelinePicker.allToCustomGap))
                    }
                }
                items(
                    items = summaries.filter { it.timeline is Timeline.Custom },
                    key = { summary -> (summary.timeline as Timeline.Custom).id.value },
                ) { summary ->
                    val timeline = summary.timeline as Timeline.Custom
                    ShareTimelineCard(
                        summary = summary,
                        mediaStore = mediaStore,
                        showDraftIndicator = hasDraft(CurrentTimeline.Custom(timeline.id)),
                        onClick = {
                            haptics.perform(ReliveHapticCue.Selection)
                            onSelect(timeline)
                        },
                    )
                }
            }
        }
    }
}

/** Compact share-target card: the footer deliberately contains only the timeline name. */
@Composable
private fun ShareTimelineCard(
    summary: TimelineHomeSummary,
    mediaStore: MediaStore,
    showDraftIndicator: Boolean,
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val picker = dims.shareTimelinePicker
    val cardShape = RoundedCornerShape(dims.radii.largeIncreased)
    val mediaHeight = if (summary.timeline == Timeline.All) picker.allMediaHeight else picker.customMediaHeight
    Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surfaceCard)
                .border(dims.stroke.hairline, colors.borderMuted, cardShape)
                .combinedClickable(onClick = onClick),
        ) {
            Box {
                TimelineHomeMediaPreview(
                    summary = summary,
                    mediaStore = mediaStore,
                    mediaHeight = mediaHeight,
                    allCollageBucket = 0L,
                    allCollageCandidates = null,
                )
                if (showDraftIndicator) {
                    Text(
                        text = "DRAFT",
                        style = ReliveTheme.typography.tag,
                        color = colors.accentMuted,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(dims.spacing.sm),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = picker.nameAreaMinHeight)
                    .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = summary.name,
                    style = ReliveTheme.typography.body,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
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

private fun Timeline.toCurrentTimeline(): CurrentTimeline = when (this) {
    Timeline.All -> CurrentTimeline.All
    is Timeline.Custom -> CurrentTimeline.Custom(id)
}

private fun shareItemLabel(payload: IncomingSharePayload): String {
    val count = payload.media.size + listOfNotNull(payload.subject, payload.text).size
    return "$count ${if (count == 1) "item" else "items"} ready for your moment"
}
