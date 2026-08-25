package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.domain.model.StartDestination
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.settings.BehaviorPreferencesViewModel
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.ForwardGlyph
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader

@Composable
fun PreferencesScreen(
    viewModel: BehaviorPreferencesViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val preferences = state.preferences
    val dims = ReliveTheme.dimensions
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberReliveHaptics()
    var showStartDestinationDialog by remember { mutableStateOf(false) }

    ReliveBackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas)) {
        Column(Modifier.fillMaxSize()) {
            PreferencesHeader(onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = dims.spacing.huge),
            ) {
                Text(
                    text = "Choose how Relive behaves.",
                    style = ReliveTheme.typography.subtitle,
                    color = ReliveTheme.colors.textSecondary,
                    modifier = Modifier.padding(
                        start = dims.spacing.xl,
                        end = dims.spacing.xl,
                        top = dims.spacing.lg,
                        bottom = dims.spacing.xl,
                    ),
                )
                PreferenceSectionHeading("GENERAL")
                StartDestinationRow(
                    selected = preferences.startDestination,
                    onClick = { showStartDestinationDialog = true },
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    label = "Confirm before discarding",
                    checked = preferences.confirmBeforeDiscarding,
                    onCheckedChange = viewModel::setConfirmBeforeDiscarding,
                )

                PreferenceSectionHeading("TIMELINE")
                PreferenceSwitchRow(
                    label = "Show locations",
                    checked = preferences.showLocations,
                    onCheckedChange = viewModel::setShowLocations,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    label = "Show tags",
                    checked = preferences.showTags,
                    onCheckedChange = viewModel::setShowTags,
                )

                PreferenceSectionHeading("REDISCOVER")
                PreferenceSwitchRow(
                    label = "On This Day",
                    checked = preferences.showOnThisDay,
                    onCheckedChange = viewModel::setShowOnThisDay,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    label = "Favorites",
                    checked = preferences.showFavorites,
                    onCheckedChange = viewModel::setShowFavorites,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(dims.spacing.lg),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = ReliveTheme.colors.accent,
                contentColor = ReliveTheme.colors.textOnAccent,
            )
        }
    }

    if (showStartDestinationDialog) {
        StartDestinationDialog(
            selected = preferences.startDestination,
            onDismiss = { showStartDestinationDialog = false },
            onSelect = { destination ->
                haptics.perform(ReliveHapticCue.Selection)
                viewModel.setStartDestination(destination)
                showStartDestinationDialog = false
            },
        )
    }
}

@Composable
private fun PreferencesHeader(onBack: () -> Unit) {
    ProfilePageHeader("Preferences", onBack)
}

@Composable
private fun PreferenceSectionHeading(label: String) {
    val dims = ReliveTheme.dimensions
    Text(
        text = label,
        style = ReliveTheme.typography.eyebrow,
        color = ReliveTheme.colors.accentMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dims.spacing.xl,
                end = dims.spacing.xl,
                top = dims.spacing.xxl,
                bottom = dims.spacing.sm,
            )
            .semantics { heading() },
    )
}

@Composable
private fun StartDestinationRow(
    selected: StartDestination,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = "Start Relive on, ${selected.displayName()}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Start Relive on",
            style = ReliveTheme.typography.body,
            color = ReliveTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = selected.displayName(),
            style = ReliveTheme.typography.subtitle,
            color = ReliveTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = dims.spacing.sm),
        )
        ForwardGlyph(dims.icon.sm, ReliveTheme.colors.textMuted, dims.stroke.icon)
    }
}

@Composable
private fun PreferenceSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.minTouchTarget)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { enabled ->
                    haptics.perform(
                        if (enabled) ReliveHapticCue.ToggleOn else ReliveHapticCue.ToggleOff,
                    )
                    onCheckedChange(enabled)
                },
            )
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.sm)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = ReliveTheme.typography.body,
            color = ReliveTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun PreferenceDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = ReliveTheme.dimensions.spacing.xl),
        thickness = ReliveTheme.dimensions.stroke.hairline,
        color = ReliveTheme.colors.borderMuted,
    )
}

@Composable
private fun StartDestinationDialog(
    selected: StartDestination,
    onDismiss: () -> Unit,
    onSelect: (StartDestination) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ReliveTheme.colors.surfaceOverlay,
        title = {
            Text("Start Relive on", style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
                StartDestination.entries.forEach { destination ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = dims.minTouchTarget)
                            .selectable(
                                selected = destination == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(destination) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = destination == selected, onClick = null)
                        Text(
                            destination.displayName(),
                            style = ReliveTheme.typography.body,
                            color = ReliveTheme.colors.textPrimary,
                            modifier = Modifier.padding(start = dims.spacing.sm),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ReliveTheme.colors.accent)
            }
        },
    )
}

private fun StartDestination.displayName(): String = when (this) {
    StartDestination.Timelines -> "Timelines"
    StartDestination.Rediscover -> "Rediscover"
}
