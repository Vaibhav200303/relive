package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.settings.BehaviorPreferencesViewModel
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.ForwardGlyph
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
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

    Box(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
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
                PreferencePanel {
                    PreferenceSwitchRow("Confirm before discarding", "Show a reminder before permanently deleting a moment.", preferences.confirmBeforeDiscarding, viewModel::setConfirmBeforeDiscarding)
                }

                PreferenceSectionHeading("TIMELINE")
                PreferencePanel {
                    PreferenceSwitchRow("Show locations", "Display saved locations on moments.", preferences.showLocations, viewModel::setShowLocations)
                    PreferenceDivider()
                    PreferenceSwitchRow("Show tags", "Display tags on moments.", preferences.showTags, viewModel::setShowTags)
                }

                PreferenceSectionHeading("REDISCOVER")
                PreferencePanel {
                    PreferenceSwitchRow("On This Day", "Show memories from the same date in previous years.", preferences.showOnThisDay, viewModel::setShowOnThisDay)
                    PreferenceDivider()
                    PreferenceSwitchRow("Favorites", "Show favorites in Rediscover.", preferences.showFavorites, viewModel::setShowFavorites)
                }
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
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.minTouchTarget + dims.spacing.xs)
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
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = ReliveTheme.typography.action.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = ReliveTheme.colors.textPrimary,
            )
            Text(
                text = supporting,
                style = ReliveTheme.typography.tag.copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = ReliveTheme.colors.textMuted,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun PreferenceDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = ReliveTheme.dimensions.spacing.md),
        thickness = ReliveTheme.dimensions.stroke.hairline,
        color = ReliveTheme.colors.borderMuted,
    )
}

@Composable
private fun PreferencePanel(content: @Composable ColumnScope.() -> Unit) {
    val d = ReliveTheme.dimensions
    val shape = RoundedCornerShape(d.radii.largeIncreased)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = d.spacing.lg)
            .clip(shape)
            .background(ReliveTheme.colors.surfaceCard)
            .border(d.stroke.hairline, ReliveTheme.colors.borderMuted, shape),
        content = content,
    )
}
