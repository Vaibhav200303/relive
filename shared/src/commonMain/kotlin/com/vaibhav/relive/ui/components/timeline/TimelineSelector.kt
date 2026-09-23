package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.presentation.timeline.TimelineCreationState
import com.vaibhav.relive.ui.components.ReliveAlertDialog
import com.vaibhav.relive.ui.components.composer.CloseGlyph
import com.vaibhav.relive.ui.components.composer.ImageGlyph
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveOpacity
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun TimelineCreationDialog(
    state: TimelineCreationState,
    onNameChange: (String) -> Unit,
    mediaStore: MediaStore,
    onChooseCover: () -> Unit,
    onClearCover: () -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.isVisible) return
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val creation = dims.timelineCreation
    val scrollState = rememberScrollState()
    val zoneShape = RoundedCornerShape(dims.radii.xl)
    Dialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.spacing.md)
                .widthIn(max = creation.maxWidth)
                .heightIn(max = creation.maxHeight),
            shape = ReliveTheme.shapes.dialog,
            color = colors.surfaceOverlay,
            contentColor = colors.textPrimary,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(creation.contentInset),
                verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(end = creation.headerCloseSize)) {
                        Text("New timeline", style = ReliveTheme.typography.display)
                        Spacer(Modifier.height(dims.spacing.sm))
                        Text(
                            "Give your timeline a name and a cover photo to get started.",
                            style = ReliveTheme.typography.body,
                            color = colors.textSecondary,
                        )
                    }
                    IconButton(
                        onClick = { if (!state.isSaving) onDismiss() },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(creation.headerCloseSize)
                            .semantics { contentDescription = "Close new timeline dialog" },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(creation.headerCloseVisualSize)
                                .background(colors.tint, RoundedCornerShape(dims.radii.full)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CloseGlyph(creation.headerCloseGlyphSize, colors.textPrimary, dims.stroke.icon)
                        }
                    }
                }
                Spacer(Modifier.height(dims.spacing.md))
                TimelineCreationLabel("Timeline name")
                BasicTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    enabled = !state.isSaving,
                    singleLine = true,
                    textStyle = ReliveTheme.typography.body.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!state.isSaving) onCreate() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(creation.fieldHeight)
                        .border(
                            width = dims.stroke.cardOuter,
                            color = if (state.errorMessage != null) colors.actionDestructive else colors.border,
                            shape = RoundedCornerShape(dims.radii.medium),
                        ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dims.spacing.lg),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (state.name.isEmpty()) {
                                Text(
                                    "E.g. Summer 2025",
                                    style = ReliveTheme.typography.body,
                                    color = colors.textMuted,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    state.errorMessage?.let {
                        Text(
                            it,
                            style = ReliveTheme.typography.caption,
                            color = colors.actionDestructive,
                        )
                    } ?: Spacer(Modifier.weight(1f))
                    Text(
                        "${state.name.length}/${Timeline.Custom.MAX_NAME_LENGTH}",
                        style = ReliveTheme.typography.caption,
                        color = colors.textMuted,
                    )
                }
                Spacer(Modifier.height(dims.spacing.sm))
                TimelineCreationLabel("Cover photo")
                TimelineCoverDropZone(
                    state = state,
                    mediaStore = mediaStore,
                    shape = zoneShape,
                    onChooseCover = onChooseCover,
                    onClearCover = onClearCover,
                    enabled = !state.isSaving && !state.isProcessingCover,
                )
                Spacer(Modifier.height(dims.spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !state.isSaving,
                        modifier = Modifier.sizeIn(
                            minWidth = dims.minTouchTarget,
                            minHeight = dims.minTouchTarget,
                        ),
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
                    ) { Text("Cancel", style = ReliveTheme.typography.prominentAction) }
                    Button(
                        onClick = onCreate,
                        enabled = !state.isSaving && !state.isProcessingCover,
                        modifier = Modifier
                            .height(creation.primaryActionHeight)
                            .weight(1f)
                            .widthIn(max = creation.primaryActionMaxWidth),
                        shape = RoundedCornerShape(dims.radii.full),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.textOnAccent,
                            disabledContainerColor = colors.surfaceCard,
                            disabledContentColor = colors.textMuted,
                        ),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dims.icon.md),
                                color = colors.textOnAccent,
                                strokeWidth = dims.stroke.icon,
                            )
                        } else {
                            Text(
                                if (state.isProcessingCover) "Preparing photo…" else "Create timeline",
                                style = ReliveTheme.typography.prominentAction,
                            )
                            Spacer(Modifier.size(dims.spacing.sm))
                            TimelineArrowGlyph(dims.icon.lg, colors.textOnAccent, dims.stroke.icon)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCreationLabel(text: String) {
    Text(text, style = ReliveTheme.typography.prominentAction, color = ReliveTheme.colors.textPrimary)
}

@Composable
private fun TimelineCoverDropZone(
    state: TimelineCreationState,
    mediaStore: MediaStore,
    shape: RoundedCornerShape,
    onChooseCover: () -> Unit,
    onClearCover: () -> Unit,
    enabled: Boolean,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val creation = dims.timelineCreation
    val cover = state.coverPhotoRef
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(creation.coverZoneHeight)
            .clip(shape)
            .background(colors.surfaceCard.copy(alpha = ReliveOpacity.Medium))
            .drawBehind {
                drawRoundRect(
                    color = colors.border,
                    style = Stroke(
                        width = dims.stroke.cardOuter.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(dims.spacing.sm.toPx(), dims.spacing.sm.toPx()),
                        ),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(dims.radii.xl.toPx()),
                )
            }
            .clickable(enabled = enabled, onClick = onChooseCover)
            .semantics {
                contentDescription = if (cover != null) {
                    "Replace cover photo"
                } else {
                    "Choose cover photo"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null && mediaStore.exists(cover)) {
            RelivedImageTile(cover, mediaStore, Modifier.matchParentSize())
            TextButton(
                onClick = onClearCover,
                enabled = enabled,
                modifier = Modifier.align(Alignment.TopEnd),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.textOnAccent),
            ) { Text("Remove", style = ReliveTheme.typography.action) }
        } else if (state.isProcessingCover) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = colors.accent)
                Spacer(Modifier.height(dims.spacing.sm))
                Text("Preparing photo…", style = ReliveTheme.typography.body, color = colors.textSecondary)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(creation.emptyIconSurfaceSize)
                        .background(colors.tint, RoundedCornerShape(dims.radii.full)),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageGlyph(creation.emptyIconSize, colors.accent, dims.stroke.iconBold)
                }
                Spacer(Modifier.height(dims.spacing.md))
                Text("No cover photo", style = ReliveTheme.typography.body, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun TimelineArrowGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(Modifier.size(size)) {
        val y = this.size.height / 2f
        val inset = this.size.width * .12f
        val sw = strokeWidth.toPx()
        drawLine(
            color,
            androidx.compose.ui.geometry.Offset(inset, y),
            androidx.compose.ui.geometry.Offset(this.size.width - inset, y),
            sw,
        )
        drawLine(
            color,
            androidx.compose.ui.geometry.Offset(this.size.width * .58f, y - this.size.height * .25f),
            androidx.compose.ui.geometry.Offset(this.size.width - inset, y),
            sw,
        )
        drawLine(
            color,
            androidx.compose.ui.geometry.Offset(this.size.width * .58f, y + this.size.height * .25f),
            androidx.compose.ui.geometry.Offset(this.size.width - inset, y),
            sw,
        )
    }
}

@Composable
fun DiscardTimelineDraftDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val haptics = rememberReliveHaptics()
    ReliveAlertDialog(
        onDismissRequest = onKeepEditing,
        shape = RoundedCornerShape(ReliveTheme.dimensions.radii.dialog),
        containerColor = colors.surfaceOverlay,
        title = { Text("Leave this draft?", style = ReliveTheme.typography.title) },
        text = {
            Text(
                "Switching timelines will discard this unfinished Moment.",
                style = ReliveTheme.typography.body,
                color = colors.textSecondary,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.perform(ReliveHapticCue.Action)
                    onDiscard()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.actionDestructive,
                    contentColor = colors.textOnDestructive,
                ),
            ) {
                Text("Discard and switch")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onKeepEditing,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
            ) {
                Text("Keep editing")
            }
        },
    )
}
