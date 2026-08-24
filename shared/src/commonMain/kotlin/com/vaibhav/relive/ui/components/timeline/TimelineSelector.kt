package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.presentation.timeline.TimelineCreationState
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
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
    val dims = ReliveTheme.dimensions
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.isVisible) {
        if (state.isVisible) focusRequester.requestFocus()
    }
    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        shape = RoundedCornerShape(dims.radii.dialog),
        containerColor = colors.surfaceOverlay,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = { Text("New timeline", style = ReliveTheme.typography.title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    enabled = !state.isSaving,
                    singleLine = true,
                    label = { Text("Timeline name") },
                    isError = state.errorMessage != null,
                    supportingText = {
                        Text(
                            text = state.errorMessage
                                ?: "${state.name.length}/${Timeline.Custom.MAX_NAME_LENGTH}",
                            style = ReliveTheme.typography.subtitle,
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (!state.isSaving) onCreate() },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        cursorColor = colors.accent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedLabelColor = colors.accent,
                        unfocusedLabelColor = colors.textMuted,
                        errorBorderColor = colors.actionDestructive,
                        errorLabelColor = colors.actionDestructive,
                        errorSupportingTextColor = colors.actionDestructive,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.textOnAccent,
                    disabledContainerColor = colors.surfaceCard,
                    disabledContentColor = colors.textMuted,
                ),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = colors.textOnAccent,
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
    val haptics = rememberReliveHaptics()
    AlertDialog(
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
