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
import com.vaibhav.relive.ui.components.ReliveAlertDialog
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
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.settings.BehaviorPreferencesViewModel
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.ForwardGlyph
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader
import com.vaibhav.relive.ui.components.ReliveSnackbarHost

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
        ReliveSnackbarHost(
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
