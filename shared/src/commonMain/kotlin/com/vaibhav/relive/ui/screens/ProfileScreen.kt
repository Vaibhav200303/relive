package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.date.ProfileSinceFormatter
import com.vaibhav.relive.presentation.profile.ProfileViewModel
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.ForwardGlyph
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val dims = ReliveTheme.dimensions
    ReliveBackHandler(enabled = true, onBack = onBack)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas),
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
        item(key = "profile-statistics") { ProfileStatistics(state.momentCount, state.customTimelineCount, state.placeCount) }
        item(key = "personalize") {
            ProfileSection("PERSONALIZE", listOf("Appearance & themes"))
        }
        item(key = "your-memories") {
            ProfileSection("YOUR MEMORIES", listOf("Media & storage", "Backup"))
        }
        item(key = "preferences") {
            ProfileSection("PREFERENCES", listOf("Location", "Rediscover notifications", "Privacy & security"))
        }
        item(key = "relive") {
            ProfileSection("RELIVE", listOf("Help & feedback", "About Relive"), last = true)
        }
    }
}

@Composable
private fun ProfileHeader(onBack: () -> Unit) {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
private fun ProfileSection(title: String, labels: List<String>, last: Boolean = false) {
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
        labels.forEach { label -> ProfileSettingRow(label) }
    }
}

@Composable
private fun ProfileSettingRow(label: String) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.minTouchTarget)
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
