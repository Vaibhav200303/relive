package com.vaibhav.relive.ui.components.composer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentValidation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.permission.MicPermissionAdapter
import com.vaibhav.relive.platform.permission.MicPermissionResult
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.composer.LiveRecording
import com.vaibhav.relive.presentation.composer.MicPermissionUiState
import com.vaibhav.relive.presentation.composer.MomentComposerState
import com.vaibhav.relive.presentation.composer.SaveState
import com.vaibhav.relive.presentation.date.EditorialDateFormatter
import com.vaibhav.relive.presentation.date.EditorialTimeFormatter
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.components.timeline.LocalTimelineWallpaperPalette
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics

/**
 * Inline composer article. Extends the Phase 3 shape with the Phase 4
 * media surface: draft attachments stack above `Add Media`; while
 * recording, a compact recorder replaces the Add Media reveal.
 *
 * Add Media reveals exactly three actions: Voice, Camera, Media. Photo/Video
 * selection is presented inside the Media and Camera flows, not as top-level
 * composer actions.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MomentComposer(
    state: MomentComposerState,
    customTimelines: List<Timeline.Custom>,
    clock: Clock,
    mediaStore: MediaStore,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onPendingTagChange: (String) -> Unit,
    onCommitPendingTag: () -> Unit,
    onRemoveTag: (Tag) -> Unit,
    onToggleTimelineAssignment: (TimelineId) -> Unit,
    onToggleAddMedia: () -> Unit,
    onMicTap: () -> Unit,
    onCameraTap: () -> Unit,
    onLibraryTap: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onRetryAttachment: (String) -> Unit,
    onReset: () -> Unit,
    onKeepMoment: () -> Unit,
    onMicPermissionResult: (MicPermissionResult) -> Unit,
    onDismissMicPermissionMessage: () -> Unit,
    onOpenAppSettings: () -> Unit,
    requestInitialFocus: Boolean = false,
    onInitialFocusHandled: () -> Unit = {},
    /**
     * Which way the timeline rail leaves the composer's plus marker.
     *
     * The composer always sits at the chronological end of its feed, so the rail approaches it from
     * the direction the older moments lie and stops at the marker's centre. On an oldest-first
     * timeline that end is the tail, so the rail arrives from above (the default). On the Home
     * surface the feed is newest-first, so the same chronological end renders at the head and the
     * rail instead leaves the marker downward toward the first moment's dot (ADR-0061).
     */
    railContinuesBelow: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Bridge composer state → platform mic-permission prompt.
    MicPermissionAdapter(
        pending = state.pendingMicPermissionRequest,
        onResult = onMicPermissionResult,
    )
    // Back collapses the Add Media reveal without exiting the app.
    ReliveBackHandler(enabled = state.addMediaExpanded, onBack = onToggleAddMedia)
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val wallpaper = LocalTimelineWallpaperPalette.current
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            titleFocusRequester.requestFocus()
            onInitialFocusHandled()
        }
    }

    val now = state.editingMoment?.createdAt ?: clock.now()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind { drawComposerRail(colors.borderMuted, dims, dims.minTouchTarget, railContinuesBelow) }
            .padding(vertical = dims.spacing.xl),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(dims.timeline.contentInset)
                .heightIn(min = dims.minTouchTarget),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(dims.timeline.plusSize)
                    .clip(CircleShape)
                    .background(wallpaper.backgroundColor)
                    .border(
                        width = dims.stroke.hairline,
                        color = colors.border,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                PlusGlyph(
                    size = dims.icon.md,
                    color = colors.textSecondary,
                    strokeWidth = dims.stroke.icon,
                )
            }
        }
        Spacer(Modifier.width(dims.spacing.sm))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = dims.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dims.minTouchTarget),
            ) {
                ComposerDateTimeRow(
                    date = EditorialDateFormatter.format(now),
                    time = EditorialTimeFormatter.format(now),
                )
                Spacer(Modifier.weight(1f))
                ResetButton(onReset = onReset, enabled = !state.isSaving)
            }

            Spacer(Modifier.height(dims.spacing.sm))

            ComposerLocationField(
                value = state.location.readableComposerLabel(),
                enabled = !state.isSaving,
                onValueChange = onLocationChange,
            )

            Spacer(Modifier.height(dims.spacing.sm))

            ComposerTitleField(
                value = state.title,
                enabled = !state.isSaving,
                onValueChange = onTitleChange,
                focusRequester = titleFocusRequester,
            )

            Spacer(Modifier.height(dims.spacing.sm))

            ComposerContentField(
                value = state.content,
                enabled = !state.isSaving,
                onValueChange = onContentChange,
            )

            Spacer(Modifier.height(dims.spacing.lg))

            ComposerTagsRow(
                tags = state.tags,
                pendingInput = state.pendingTagInput,
                enabled = !state.isSaving,
                onPendingChange = onPendingTagChange,
                onCommit = onCommitPendingTag,
                onRemove = onRemoveTag,
            )

            Spacer(Modifier.height(dims.spacing.lg))

            if (!state.isEditing && state.timelineContext == CurrentTimeline.All && customTimelines.isNotEmpty()) {
                ComposerTimelineAssignments(
                    timelines = customTimelines,
                    selectedIds = state.selectedTimelineIds,
                    enabled = !state.isSaving,
                    onToggle = onToggleTimelineAssignment,
                )
                Spacer(Modifier.height(dims.spacing.lg))
            }

            // Attachments (if any) — above the recorder & Add Media.
            if (state.attachments.isNotEmpty()) {
                DraftAttachmentColumn(
                    attachments = state.attachments,
                    mediaStore = mediaStore,
                    onRemove = onRemoveAttachment,
                    onRetry = onRetryAttachment,
                )
                Spacer(Modifier.height(dims.spacing.lg))
            }

            // Live recorder replaces the Add Media reveal while active.
            if (state.recording != null) {
                LiveRecorderCard(
                    recording = state.recording,
                    onStop = onStopRecording,
                    onCancel = onCancelRecording,
                )
            } else {
                AddMediaShell(
                    expanded = state.addMediaExpanded,
                    enabled = !state.isSaving,
                    onToggle = onToggleAddMedia,
                    onMicTap = onMicTap,
                    onCameraTap = onCameraTap,
                    onLibraryTap = onLibraryTap,
                )
            }

            Spacer(Modifier.height(dims.spacing.lg))

            MicPermissionHint(
                state = state.micPermission,
                onOpenSettings = onOpenAppSettings,
                onDismiss = onDismissMicPermissionMessage,
            )
            MediaErrorText(state.mediaError)
            SaveErrorLine(state.saveState)

            KeepMomentAction(
                enabled = !state.isSaving && !state.isRecording && !state.hasProcessingAttachments,
                isSaving = state.isSaving,
                isProcessingMedia = state.hasProcessingAttachments,
                isEditing = state.isEditing,
                onClick = onKeepMoment,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComposerTimelineAssignments(
    timelines: List<Timeline.Custom>,
    selectedIds: Set<TimelineId>,
    enabled: Boolean,
    onToggle: (TimelineId) -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
        Text(
            text = "TIMELINES · OPTIONAL",
            style = type.eyebrow,
            color = colors.textMuted,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
        ) {
            timelines.forEach { timeline ->
                val selected = timeline.id in selectedIds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = dims.minTouchTarget)
                        .clip(CircleShape)
                        .background(if (selected) colors.surfaceCard else colors.bgCanvas)
                        .border(dims.stroke.hairline, colors.border, CircleShape)
                        .toggleable(
                            value = selected,
                            enabled = enabled,
                            role = Role.Checkbox,
                            onValueChange = {
                                haptics.perform(
                                    if (selected) ReliveHapticCue.ToggleOff else ReliveHapticCue.ToggleOn,
                                )
                                onToggle(timeline.id)
                            },
                        )
                        .padding(horizontal = dims.spacing.md),
                ) {
                    Text(
                        text = if (selected) "✓ ${timeline.name}" else timeline.name,
                        style = type.tag,
                        color = if (selected) colors.accent else colors.textSecondary,
                    )
                }
            }
        }
    }
}

/**
 * Collapsed placeholder for the inline composer. Renders only the `+` rail
 * marker in the same timeline column position the expanded composer uses, so
 * tapping it feels like the composer opens in place.
 */
@Composable
fun CollapsedComposerMarker(
    onExpand: () -> Unit,
    /** See `MomentComposer`'s `railContinuesBelow`. */
    railContinuesBelow: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val wallpaper = LocalTimelineWallpaperPalette.current
    val haptics = rememberReliveHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind { drawComposerRail(colors.borderMuted, dims, dims.minTouchTarget, railContinuesBelow) }
            .padding(vertical = dims.spacing.xl),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(dims.timeline.contentInset),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptics.perform(ReliveHapticCue.Action)
                            onExpand()
                        },
                    )
                    .semantics { contentDescription = "Add a new moment" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(dims.timeline.plusSize)
                        .clip(CircleShape)
                        .background(wallpaper.backgroundColor)
                        .border(
                            width = dims.stroke.hairline,
                            color = colors.border,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    PlusGlyph(
                        size = dims.icon.md,
                        color = colors.textSecondary,
                        strokeWidth = dims.stroke.icon,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawComposerRail(
    color: androidx.compose.ui.graphics.Color,
    dims: com.vaibhav.relive.ui.theme.ReliveDimensions,
    markerSize: androidx.compose.ui.unit.Dp,
    continuesBelow: Boolean,
) {
    val axis = dims.timeline.contentInset.toPx() / 2f
    val markerCenter = (dims.spacing.xl.toPx() + markerSize.toPx() / 2f)
        .coerceAtMost(size.height)
    // The rail always terminates at the marker's centre; only the side it arrives from changes.
    val start = if (continuesBelow) markerCenter else 0f
    val end = if (continuesBelow) size.height else markerCenter
    drawLine(
        color = color,
        start = androidx.compose.ui.geometry.Offset(axis, start),
        end = androidx.compose.ui.geometry.Offset(axis, end),
        strokeWidth = dims.timeline.railWidth.toPx(),
    )
}

@Composable
private fun ComposerDateTimeRow(date: String, time: String, modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        modifier = modifier.heightIn(min = dims.timeline.plusSize),
    ) {
        Text(text = date, style = type.eyebrow, color = colors.textMuted)
        Box(
            modifier = Modifier
                .size(3.dp)
                .clip(CircleShape)
                .background(colors.textMuted),
        )
        Text(text = time, style = type.eyebrow, color = colors.textMuted)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComposerTitleField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        textStyle = type.title.copy(color = colors.textPrimary),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .bringIntoViewRequester(requester)
            .onFocusEvent { if (it.isFocused) scope.launch { requester.bringIntoView() } }
            .semantics { contentDescription = "Memory title" },
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(text = "Memory Title", style = type.title, color = colors.textMuted)
            }
            inner()
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComposerLocationField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        PinGlyph(
            size = dims.icon.sm,
            color = colors.textMuted,
            strokeWidth = dims.stroke.icon,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = ReliveTheme.typography.body.copy(color = colors.textSecondary),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .weight(1f)
                .bringIntoViewRequester(requester)
                .onFocusEvent { if (it.isFocused) scope.launch { requester.bringIntoView() } }
                .semantics { contentDescription = "Moment location" },
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "Add location",
                        style = ReliveTheme.typography.body,
                        color = colors.textMuted,
                    )
                }
                inner()
            },
        )
    }
}

private fun com.vaibhav.relive.domain.model.ReliveLocation?.readableComposerLabel(): String =
    this?.let { location ->
        listOfNotNull(location.placeName, location.locality, location.region, location.country)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(", ")
    }.orEmpty()

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComposerContentField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val bodyItalic = type.subtitle.copy(color = colors.textSecondary, fontStyle = FontStyle.Italic)
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        textStyle = bodyItalic,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
            .onFocusEvent { if (it.isFocused) scope.launch { requester.bringIntoView() } }
            .semantics { contentDescription = "Memory content" },
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(text = "What do you want to remember?", style = bodyItalic, color = colors.textMuted)
            }
            inner()
        },
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ComposerTagsRow(
    tags: List<Tag>,
    pendingInput: String,
    enabled: Boolean,
    onPendingChange: (String) -> Unit,
    onCommit: () -> Unit,
    onRemove: (Tag) -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        tags.forEach { tag ->
            TagPill(tag = tag, enabled = enabled, onRemove = { onRemove(tag) })
        }
        val tagFocusRequester = remember { FocusRequester() }
        Box(
            modifier = Modifier
                .heightIn(min = dims.minTouchTarget)
                .clickable(enabled = enabled, role = Role.Button) { tagFocusRequester.requestFocus() }
                .semantics { contentDescription = "Add tag" },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
                modifier = Modifier
                    .height(dims.composer.tagVisibleHeight)
                    .clip(CircleShape)
                    .border(
                        width = dims.stroke.hairline,
                        color = colors.borderMuted,
                        shape = CircleShape,
                    )
                    .padding(horizontal = dims.spacing.md),
            ) {
                Text(text = "#", style = type.tag, color = colors.textMuted)
                val tagRequester = remember { BringIntoViewRequester() }
                val tagScope = rememberCoroutineScope()
                Box(modifier = Modifier.widthIn(min = 40.dp)) {
                    BasicTextField(
                        value = pendingInput,
                        onValueChange = onPendingChange,
                        enabled = enabled,
                        singleLine = true,
                        textStyle = type.tag.copy(color = colors.textSecondary),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onCommit() }),
                        modifier = Modifier
                            .focusRequester(tagFocusRequester)
                            .bringIntoViewRequester(tagRequester)
                            .onFocusEvent { if (it.isFocused) tagScope.launch { tagRequester.bringIntoView() } }
                            .semantics { contentDescription = "Add tag input" },
                        decorationBox = { inner ->
                            if (pendingInput.isEmpty()) {
                                Text(text = "tag", style = type.tag, color = colors.textMuted)
                            }
                            inner()
                        },
                    )
                }
                IconButton(
                    onClick = {
                        haptics.perform(ReliveHapticCue.Action)
                        onCommit()
                    },
                    enabled = enabled && pendingInput.isNotBlank(),
                    modifier = Modifier
                        .size(dims.composer.tagVisibleHeight)
                        .semantics { contentDescription = "Commit tag" },
                ) {
                    PlusGlyph(size = dims.icon.sm, color = colors.textSecondary, strokeWidth = dims.stroke.icon)
                }
            }
        }
    }
}

@Composable
private fun TagPill(tag: Tag, enabled: Boolean, onRemove: () -> Unit) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Box(
        modifier = Modifier
            .heightIn(min = dims.minTouchTarget)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.perform(ReliveHapticCue.Action)
                    onRemove()
                },
            )
            .semantics { contentDescription = "Remove tag ${tag.label}" },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.surfaceCard)
                .border(
                    width = dims.stroke.hairline,
                    color = colors.borderMuted,
                    shape = CircleShape,
                )
                .padding(
                    start = dims.spacing.md,
                    end = dims.spacing.sm,
                    top = dims.spacing.xs,
                    bottom = dims.spacing.xs,
                ),
        ) {
            Text(text = "#" + tag.label.lowercase(), style = type.tag, color = colors.textSecondary)
            CloseGlyph(size = dims.icon.sm, color = colors.textMuted, strokeWidth = dims.stroke.icon)
            }
        }
    }

@Composable
private fun AddMediaShell(
    expanded: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onMicTap: () -> Unit,
    onCameraTap: () -> Unit,
    onLibraryTap: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val haptics = rememberReliveHaptics()
    val shape = RoundedCornerShape(dims.radii.largeIncreased)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.minTouchTarget)
            .clip(shape)
            .background(colors.surfaceCard)
            .border(
                width = dims.stroke.hairline,
                color = colors.borderMuted,
                shape = shape,
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.perform(if (expanded) ReliveHapticCue.ToggleOff else ReliveHapticCue.ToggleOn)
                    onToggle()
                },
            )
            .padding(horizontal = dims.spacing.lg, vertical = dims.spacing.md)
            .semantics { contentDescription = if (expanded) "Hide media choices" else "Add media" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            ImageGlyph(size = dims.icon.md, color = colors.textPrimary, strokeWidth = dims.stroke.icon)
            Text(
                text = "Add media",
                style = type.action,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(dims.spacing.xs))
        Text(
            text = "Voice, camera, or library",
            style = type.subtitle,
            color = colors.textMuted,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(motion.durations.standardMillis, easing = motion.easings.standard),
                expandFrom = Alignment.Top,
            ) + fadeIn(
                animationSpec = tween(motion.durations.standardMillis, easing = motion.easings.standard),
            ),
            exit = shrinkVertically(
                animationSpec = tween(motion.durations.standardMillis, easing = motion.easings.standard),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(
                animationSpec = tween(motion.durations.fastMillis, easing = motion.easings.standard),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dims.spacing.md),
            ) {
                MediaChoice(
                    label = "Voice",
                    contentDesc = "Record audio",
                    enabled = enabled,
                    onClick = onMicTap,
                    modifier = Modifier.weight(1f),
                ) {
                    MicGlyph(size = dims.icon.lg, color = colors.textPrimary, strokeWidth = dims.stroke.icon)
                }
                MediaChoice(
                    label = "Camera",
                    contentDesc = "Open camera",
                    enabled = enabled,
                    onClick = onCameraTap,
                    modifier = Modifier.weight(1f),
                ) {
                    CameraGlyph(size = dims.icon.lg, color = colors.textPrimary, strokeWidth = dims.stroke.icon)
                }
                MediaChoice(
                    label = "Media",
                    contentDesc = "Choose media",
                    enabled = enabled,
                    onClick = onLibraryTap,
                    modifier = Modifier.weight(1f),
                ) {
                    GalleryGlyph(size = dims.icon.lg, color = colors.textPrimary, strokeWidth = dims.stroke.icon)
                }
            }
        }
    }
}

@Composable
private fun MediaChoice(
    label: String,
    contentDesc: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
        modifier = modifier
            .heightIn(min = dims.minTouchTarget)
            .clip(RoundedCornerShape(dims.radii.md))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.perform(ReliveHapticCue.Action)
                    onClick()
                },
            )
            .padding(horizontal = dims.spacing.xs, vertical = dims.spacing.sm)
            .semantics { contentDescription = contentDesc },
    ) {
        icon()
        Text(text = label, style = type.action, color = colors.textSecondary)
    }
}

@Composable
private fun ResetButton(onReset: () -> Unit, enabled: Boolean) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    IconButton(
        onClick = {
            haptics.perform(ReliveHapticCue.Action)
            onReset()
        },
        enabled = enabled,
        modifier = Modifier
            .size(dims.minTouchTarget)
            .semantics { contentDescription = "Reset composer" },
    ) {
        CloseGlyph(size = dims.icon.md, color = colors.textMuted, strokeWidth = dims.stroke.icon)
    }
}

@Composable
private fun KeepMomentAction(
    enabled: Boolean,
    isSaving: Boolean,
    isProcessingMedia: Boolean,
    isEditing: Boolean,
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val label = when {
        isSaving -> if (isEditing) "Saving…" else "Keeping…"
        isProcessingMedia -> "Processing media…"
        else -> if (isEditing) "Save changes" else "Keep Moment"
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.textOnAccent,
            disabledContainerColor = colors.surfaceCard,
            disabledContentColor = colors.textMuted,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.minTouchTarget)
            .semantics { contentDescription = if (isEditing) "Save changes" else "Keep moment" },
    ) {
        Text(text = label, style = type.action)
    }
}

@Composable
private fun MicPermissionHint(
    state: MicPermissionUiState,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state is MicPermissionUiState.Idle) return
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dims.spacing.sm)
            .semantics { contentDescription = "Microphone permission required" },
    ) {
        Text(
            text = "Microphone access is needed to record audio.",
            style = type.subtitle,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (state is MicPermissionUiState.SettingsRequired) {
            Text(
                text = "Settings",
                style = type.action,
                color = colors.accent,
                modifier = Modifier
                    .heightIn(min = dims.minTouchTarget)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptics.perform(ReliveHapticCue.Action)
                            onOpenSettings()
                        },
                    )
                    .padding(dims.spacing.xs)
                    .semantics { contentDescription = "Open app settings" },
            )
        } else {
            IconButton(
                onClick = {
                    haptics.perform(ReliveHapticCue.Action)
                    onDismiss()
                },
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .semantics { contentDescription = "Dismiss microphone permission message" },
            ) {
                CloseGlyph(size = dims.icon.sm, color = colors.textMuted, strokeWidth = dims.stroke.icon)
            }
        }
    }
}

@Composable
private fun SaveErrorLine(saveState: SaveState) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val message = when (saveState) {
        is SaveState.Invalid -> if (MomentValidation.Reason.Empty in saveState.reasons) {
            "Add a title, a thought, or media before keeping."
        } else {
            "This moment can't be kept yet."
        }
        is SaveState.Failure -> "Couldn't keep this moment. Try again."
        SaveState.AwaitingProcessing -> "Waiting for media to finish processing…"
        SaveState.Idle, SaveState.Saving -> null
    }
    if (message != null) {
        Text(
            text = message,
            style = type.subtitle,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = dims.spacing.sm),
        )
    }
}
