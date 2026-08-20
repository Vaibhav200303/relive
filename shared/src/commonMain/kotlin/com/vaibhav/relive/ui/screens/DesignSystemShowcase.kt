package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Temporary Phase 0 showcase. Renders each core token so the theme foundation can be
 * validated on device. NOT the real timeline screen; delete once real screens land.
 */
@Composable
fun DesignSystemShowcase() {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = dims.timeline.horizontalPadding,
                vertical = dims.spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.lg),
    ) {
        Text(text = "Relive", style = type.wordmark, color = colors.accent)

        Text(
            text = "SEPTEMBER 28, 2023",
            style = type.eyebrow,
            color = colors.accentMuted,
        )

        Text(text = "Quiet morning light", style = type.title, color = colors.textPrimary)

        Text(
            text = "Peaceful moments before the day begins.",
            style = type.subtitle,
            color = colors.textSecondary,
        )

        Text(
            text = "Phase 0 design-system showcase. Verifies canvas, typography, accent, " +
                "borders, surfaces, spacing rhythm, icon sizing and 48dp touch targets. " +
                "Not the real timeline.",
            style = type.body,
            color = colors.textPrimary,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TagChip("FAMILY")
            TagChip("TRAVEL")
            TagChip("KIDS")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dims.radii.md))
                .background(colors.surfaceCard)
                .border(
                    width = dims.stroke.hairline,
                    color = colors.borderMuted,
                    shape = RoundedCornerShape(dims.radii.md),
                )
                .padding(dims.spacing.lg),
        ) {
            Text(
                text = "Surface / card token",
                style = type.body,
                color = colors.textSecondary,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(dims.timeline.dotSize)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .semantics { contentDescription = "Timeline dot sample" },
            )
            Text(
                text = "timeline dot",
                style = type.eyebrow,
                color = colors.textMuted,
            )
        }

        // Icon-sized swatch inside a 48dp touch target proves min-target compliance without
        // pulling in the extended icons dependency.
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(dims.minTouchTarget)
                .semantics { contentDescription = "Favorite" },
        ) {
            Box(
                modifier = Modifier
                    .size(dims.icon.lg)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }

        Text(text = "Action label", style = type.action, color = colors.accent)

        Spacer(Modifier.height(dims.spacing.huge))
    }
}

@Composable
private fun TagChip(label: String) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dims.radii.pill))
            .background(colors.surfaceCard)
            .border(
                width = dims.stroke.hairline,
                color = colors.borderMuted,
                shape = RoundedCornerShape(dims.radii.pill),
            )
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.xs),
    ) {
        Text(text = label, style = type.tag, color = colors.textSecondary)
    }
}
