package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.presentation.timeline.TimelineCreationState
import com.vaibhav.relive.ui.theme.ReliveTheme

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
