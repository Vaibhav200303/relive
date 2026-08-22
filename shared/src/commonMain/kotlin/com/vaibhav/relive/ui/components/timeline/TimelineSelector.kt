package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.presentation.timeline.TimelineCreationState
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun TimelineSelector(
    timelines: List<Timeline.Custom>,
    selected: CurrentTimeline,
    enabled: Boolean,
    onSelect: (CurrentTimeline) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(ReliveTheme.colors.bgCanvas),
        contentPadding = PaddingValues(
            horizontal = dims.timeline.horizontalPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "all") {
            TimelineSelectorItem(
                label = "All",
                selected = selected == CurrentTimeline.All,
                enabled = enabled,
                onClick = { onSelect(CurrentTimeline.All) },
            )
        }
        items(timelines, key = { it.id.value }) { timeline ->
            TimelineSelectorItem(
                label = timeline.name,
                selected = selected == CurrentTimeline.Custom(timeline.id),
                enabled = enabled,
                onClick = { onSelect(CurrentTimeline.Custom(timeline.id)) },
            )
        }
        item(key = "add-timeline") {
            IconButton(
                onClick = onAdd,
                enabled = enabled,
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .semantics { contentDescription = "Create timeline" },
            ) {
                Text(
                    text = "+",
                    style = ReliveTheme.typography.title,
                    color = ReliveTheme.colors.accent,
                )
            }
        }
    }
}

@Composable
private fun TimelineSelectorItem(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = dims.minTouchTarget)
            .clip(CircleShape)
            .background(if (selected) colors.surfaceCard else colors.bgCanvas)
            .border(dims.stroke.hairline, colors.border, CircleShape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = dims.spacing.md),
    ) {
        Text(
            text = if (selected) "✓ $label" else label,
            style = ReliveTheme.typography.action,
            color = if (selected) colors.accent else colors.textSecondary,
        )
    }
}

@Composable
fun TimelineCreationDialog(
    state: TimelineCreationState,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.isVisible) return
    val colors = ReliveTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.bgCanvas,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = { Text("New timeline", style = ReliveTheme.typography.title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.sm),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    enabled = !state.isSaving,
                    singleLine = true,
                    label = { Text("Timeline name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        cursorColor = colors.accent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedLabelColor = colors.accent,
                        unfocusedLabelColor = colors.textMuted,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.errorMessage?.let { message ->
                    Text(message, style = ReliveTheme.typography.subtitle, color = colors.textSecondary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                enabled = !state.isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.accent),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = colors.accent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun DiscardTimelineDraftDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    val colors = ReliveTheme.colors
    AlertDialog(
        onDismissRequest = onKeepEditing,
        containerColor = colors.bgCanvas,
        title = { Text("Leave this draft?", style = ReliveTheme.typography.title) },
        text = {
            Text(
                "Switching timelines will discard this unfinished Moment.",
                style = ReliveTheme.typography.body,
                color = colors.textSecondary,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.accent),
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
