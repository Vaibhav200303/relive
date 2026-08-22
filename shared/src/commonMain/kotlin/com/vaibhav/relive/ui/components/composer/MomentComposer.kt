package com.vaibhav.relive.ui.components.composer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentValidation
import com.vaibhav.relive.domain.model.Tag
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
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Inline composer article. Extends the Phase 3 shape with the Phase 4
 * media surface: draft attachments stack above `Add Media`; while
 * recording, a compact recorder replaces the Add Media reveal.
 *
 * Add Media reveals exactly three actions: Mic, Camera, Library — per the
 * Phase 4 spec. Photo/Video selection is presented inside the Library and
 * Camera flows, not as top-level composer actions.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MomentComposer(
    state: MomentComposerState,
    clock: Clock,
    mediaStore: MediaStore,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onPendingTagChange: (String) -> Unit,
    onCommitPendingTag: () -> Unit,
    onRemoveTag: (Tag) -> Unit,
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

    val now = clock.now()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dims.spacing.xl),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(dims.timeline.contentInset)
                .padding(top = dims.spacing.xs),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(dims.timeline.plusSize)
                    .clip(CircleShape)
                    .background(colors.bgCanvas)
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

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = dims.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ComposerDateTimeRow(
                    date = EditorialDateFormatter.format(now),
                    time = EditorialTimeFormatter.format(now),
                    modifier = Modifier.weight(1f),
                )
                ResetButton(onReset = onReset, enabled = !state.isSaving)
            }

            Spacer(Modifier.height(dims.spacing.sm))

            ComposerTitleField(
                value = state.title,
                enabled = !state.isSaving,
                onValueChange = onTitleChange,
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
                onClick = onKeepMoment,
            )
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
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dims.spacing.xl),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(dims.timeline.contentInset)
                .padding(top = dims.spacing.xs),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExpand,
                    )
                    .semantics { contentDescription = "Add a new moment" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(dims.timeline.plusSize)
                        .clip(CircleShape)
                        .background(colors.bgCanvas)
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

@Composable
private fun ComposerDateTimeRow(date: String, time: String, modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        modifier = modifier,
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
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        tags.forEach { tag ->
            TagPill(tag = tag, enabled = enabled, onRemove = { onRemove(tag) })
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
            modifier = Modifier
                .clip(CircleShape)
                .border(
                    width = dims.stroke.hairline,
                    color = colors.borderMuted,
                    shape = CircleShape,
                )
                .padding(horizontal = dims.spacing.md, vertical = dims.spacing.xs),
        ) {
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
                        .bringIntoViewRequester(tagRequester)
                        .onFocusEvent { if (it.isFocused) tagScope.launch { tagRequester.bringIntoView() } }
                        .semantics { contentDescription = "Add tag" },
                    decorationBox = { inner ->
                        if (pendingInput.isEmpty()) {
                            Text(text = "TAG", style = type.tag, color = colors.textMuted)
                        }
                        inner()
                    },
                )
            }
            IconButton(
                onClick = onCommit,
                enabled = enabled && pendingInput.isNotBlank(),
                modifier = Modifier
                    .size(dims.minTouchTarget / 2)
                    .semantics { contentDescription = "Commit tag" },
            ) {
                PlusGlyph(size = dims.icon.sm, color = colors.textSecondary, strokeWidth = dims.stroke.icon)
            }
        }
    }
}

@Composable
private fun TagPill(tag: Tag, enabled: Boolean, onRemove: () -> Unit) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
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
            .padding(start = dims.spacing.md, end = dims.spacing.sm, top = dims.spacing.xs, bottom = dims.spacing.xs),
    ) {
        Text(text = tag.label.uppercase(), style = type.tag, color = colors.textSecondary)
        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier
                .size(dims.minTouchTarget / 3)
                .semantics { contentDescription = "Remove tag ${tag.label}" },
        ) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.radii.md))
            .background(colors.surfaceCardTranslucent)
            .border(
                width = dims.stroke.hairline,
                color = colors.borderMuted,
                shape = RoundedCornerShape(dims.radii.md),
            )
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(dims.spacing.lg)
            .semantics { contentDescription = if (expanded) "Hide media choices" else "Add media" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            ImageGlyph(size = dims.icon.md, color = colors.textPrimary, strokeWidth = dims.stroke.icon)
            Text(
                text = "Add a New Moment",
                style = type.title.copy(fontSize = 14.sp),
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(dims.spacing.xs))
        Text(
            text = "Add photos, videos, or voice",
            style = type.subtitle,
            color = colors.textMuted,
        )
        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dims.spacing.lg),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MediaChoice(label = "Mic", contentDesc = "Record audio", onClick = onMicTap) {
                    MicGlyph(size = dims.icon.lg, color = colors.textPrimary, strokeWidth = dims.stroke.icon)
                }
                MediaChoice(label = "Camera", contentDesc = "Open camera", onClick = onCameraTap) {
                    VideoGlyph(size = dims.icon.lg, color = colors.textPrimary, strokeWidth = dims.stroke.icon)
                }
                MediaChoice(label = "Library", contentDesc = "Choose from library", onClick = onLibraryTap) {
                    ImageGlyph(size = dims.icon.lg, color = colors.textPrimary, strokeWidth = dims.stroke.icon)
                }
            }
        }
    }
}

@Composable
private fun MediaChoice(
    label: String,
    contentDesc: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(dims.spacing.sm)
            .semantics { contentDescription = contentDesc },
    ) {
        icon()
        Text(text = label, style = type.tag, color = colors.textSecondary)
    }
}

@Composable
private fun ResetButton(onReset: () -> Unit, enabled: Boolean) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    IconButton(
        onClick = onReset,
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
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val label = when {
        isSaving -> "Keeping…"
        isProcessingMedia -> "Processing media…"
        else -> "Keep Moment"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = type.action,
            color = if (enabled) colors.accent else colors.textMuted,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(dims.spacing.sm)
                .semantics { contentDescription = "Keep moment" },
        )
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenSettings,
                    )
                    .padding(dims.spacing.xs)
                    .semantics { contentDescription = "Open app settings" },
            )
        } else {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(dims.minTouchTarget / 2)
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
