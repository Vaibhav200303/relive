package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.presentation.date.ProfileSinceFormatter
import com.vaibhav.relive.presentation.profile.ProfileViewModel
import com.vaibhav.relive.presentation.settings.AppearanceViewModel
import com.vaibhav.relive.ui.components.settings.AppearanceModeControl
import com.vaibhav.relive.ui.components.settings.RelivePalettePicker
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.ForwardGlyph
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    appearanceViewModel: AppearanceViewModel,
    onBack: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenMediaStorage: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val appearance by appearanceViewModel.state.collectAsState()
    val dims = ReliveTheme.dimensions
    val snackbarHostState = remember { SnackbarHostState() }
    ReliveBackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(appearance.errorMessage) {
        appearance.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            appearanceViewModel.clearError()
        }
    }

    Box(Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "profile-header") { ProfileHeader(onBack) }
            item(key = "profile-identity") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dims.spacing.xl, bottom = dims.spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                ) {
                    ProfileAvatar()
                    Text(state.displayName, style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
                    Text(
                        "Your private memory space",
                        style = ReliveTheme.typography.subtitle,
                        color = ReliveTheme.colors.textSecondary,
                    )
                    state.joiningDate?.let { createdAt ->
                        Text(
                            "Since ${ProfileSinceFormatter.format(createdAt)}",
                            style = ReliveTheme.typography.subtitle,
                            color = ReliveTheme.colors.textMuted,
                        )
                    }
                }
            }
            item(key = "profile-statistics") {
                ProfileStatistics(state.momentCount, state.customTimelineCount, state.placeCount)
            }
            item(key = "appearance") {
                ProfileAppearanceSection(
                    mode = appearance.preferences.mode,
                    theme = appearance.preferences.defaultTheme,
                    onModeChange = appearanceViewModel::setMode,
                    onThemeChange = appearanceViewModel::setDefaultTheme,
                )
            }
            item(key = "your-memories") {
                ProfileSection(
                    title = "YOUR MEMORIES",
                    labels = listOf("Media & storage", "Backup"),
                    onMediaStorage = onOpenMediaStorage,
                )
            }
            item(key = "preferences") {
                ProfileSection(
                    title = "PREFERENCES",
                    labels = listOf("Preferences", "Location", "Rediscover notifications", "Privacy & security"),
                    onPreferences = onOpenPreferences,
                )
            }
            item(key = "relive") {
                ProfileSection("RELIVE", listOf("Help & feedback", "About Relive"), last = true)
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
}

@Composable
private fun ProfileAppearanceSection(
    mode: AppearanceMode,
    theme: ThemeReference,
    onModeChange: (AppearanceMode) -> Unit,
    onThemeChange: (ThemeReference) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
    ) {
        Text("APPEARANCE", style = ReliveTheme.typography.eyebrow, color = colors.accentMuted)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dims.radii.lg))
                .background(colors.surfaceCard)
                .border(dims.stroke.hairline, colors.borderMuted, RoundedCornerShape(dims.radii.lg))
                .padding(dims.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.lg),
        ) {
            AppearanceModeControl(selected = mode, onSelect = onModeChange)
            HorizontalDivider(color = colors.borderMuted, thickness = dims.stroke.hairline)
            RelivePalettePicker(
                selectedTheme = theme,
                globalTheme = theme,
                includeUseAppTheme = false,
                onSelect = { selected -> selected?.let(onThemeChange) },
            )
        }
    }
}

@Composable
private fun ProfileHeader(onBack: () -> Unit) {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(dims.minTouchTarget)
                .align(Alignment.CenterStart)
                .semantics { contentDescription = "Back to Timeline Home" },
        ) {
            BackGlyph(dims.icon.lg, ReliveTheme.colors.textSecondary, dims.stroke.icon)
        }
        Text(
            "PROFILE",
            style = ReliveTheme.typography.eyebrow,
            color = ReliveTheme.colors.accentMuted,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ProfileAvatar() {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Canvas(
        modifier = Modifier
            .size(dims.profile.avatarSize)
            .semantics { contentDescription = "Profile avatar" },
    ) {
        drawCircle(colors.surfaceCard)
        val stroke = Stroke(width = dims.stroke.iconBold.toPx())
        drawCircle(
            color = colors.textSecondary,
            radius = size.minDimension * 0.16f,
            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.37f),
            style = stroke,
        )
        drawArc(
            color = colors.textSecondary,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height * 0.42f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height * 0.42f),
            style = stroke,
        )
    }
}

@Composable
private fun ProfileStatistics(momentCount: Long, timelineCount: Long, placeCount: Long) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md)
            .semantics { contentDescription = "$momentCount moments, $timelineCount timelines, $placeCount places" },
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ProfileStatistic(momentCount, "Moments")
        ProfileStatistic(timelineCount, "Timelines")
        ProfileStatistic(placeCount, "Places")
    }
}

@Composable
private fun ProfileStatistic(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.xs)) {
        Text(value.toString(), style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
        Text(label, style = ReliveTheme.typography.tag, color = ReliveTheme.colors.textMuted)
    }
}

@Composable
private fun ProfileSection(
    title: String,
    labels: List<String>,
    last: Boolean = false,
    onPreferences: (() -> Unit)? = null,
    onMediaStorage: (() -> Unit)? = null,
) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dims.spacing.xl,
                end = dims.spacing.xl,
                top = dims.spacing.xxl,
                bottom = if (last) dims.spacing.huge else dims.spacing.none,
            ),
    ) {
        Text(title, style = ReliveTheme.typography.eyebrow, color = ReliveTheme.colors.accentMuted)
        labels.forEach { label ->
            ProfileSettingRow(
                label = label,
                onClick = when (label) {
                    "Preferences" -> onPreferences
                    "Media & storage" -> onMediaStorage
                    else -> null
                },
            )
        }
    }
}

@Composable
private fun ProfileSettingRow(label: String, onClick: (() -> Unit)? = null) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.minTouchTarget)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileRowGlyph(Modifier.size(dims.icon.md))
        Text(
            label,
            style = ReliveTheme.typography.body,
            color = ReliveTheme.colors.textPrimary,
            modifier = Modifier.weight(1f).padding(start = dims.spacing.md),
        )
        ForwardGlyph(dims.icon.sm, ReliveTheme.colors.textMuted, dims.stroke.icon)
    }
}

@Composable
private fun ProfileRowGlyph(modifier: Modifier) {
    val colors = ReliveTheme.colors
    val strokeWidth = ReliveTheme.dimensions.stroke.icon
    Canvas(modifier) {
        val stroke = Stroke(width = strokeWidth.toPx())
        drawCircle(colors.textMuted, radius = size.minDimension * 0.32f, style = stroke)
        drawLine(
            colors.textMuted,
            androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.5f),
            androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height * 0.5f),
            strokeWidth = stroke.width,
        )
    }
}
