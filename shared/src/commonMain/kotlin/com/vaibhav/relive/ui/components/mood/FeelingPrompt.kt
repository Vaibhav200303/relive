package com.vaibhav.relive.ui.components.mood

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.presentation.insights.moodVerdictLabel
import com.vaibhav.relive.ui.components.composer.CloseGlyph
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * The post-save reflection prompt (PRODUCT_SPEC §10A.1). Renders inline beneath the
 * Moment that was just kept — never a modal, sheet or screen — and is entirely
 * skippable: dismissing writes nothing, and a Moment with no feeling is normal.
 */
@Composable
fun FeelingPrompt(
    onChoose: (MomentFeeling) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val shape = RoundedCornerShape(dims.radii.large)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceCard)
            .border(BorderStroke(dims.stroke.hairline, colors.borderMuted), shape)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "How does this moment feel?",
                style = type.subtitle,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .semantics { contentDescription = "Skip this question" },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CloseGlyph(
                    size = dims.icon.md,
                    color = colors.accentMuted,
                    strokeWidth = dims.stroke.iconBold,
                )
            }
        }
        Spacer(Modifier.height(dims.spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm)) {
            listOf(MomentFeeling.Great, MomentFeeling.Good, MomentFeeling.Low).forEach { feeling ->
                FeelingChip(feeling = feeling, onChoose = onChoose)
            }
        }
        Spacer(Modifier.height(dims.spacing.xs))
    }
}

@Composable
private fun FeelingChip(
    feeling: MomentFeeling,
    onChoose: (MomentFeeling) -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    val label = moodVerdictLabel(feeling)
    Row(
        modifier = Modifier
            .heightIn(min = dims.minTouchTarget)
            .clip(CircleShape)
            .background(colors.bgCanvas)
            .border(BorderStroke(dims.stroke.hairline, colors.borderMuted), CircleShape)
            .clickable {
                haptics.perform(ReliveHapticCue.Confirm)
                onChoose(feeling)
            }
            .semantics { contentDescription = "This moment felt $label" }
            .padding(horizontal = dims.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeelingFace(feeling = feeling, size = 24.dp)
        Spacer(Modifier.width(dims.spacing.sm))
        Text(text = label, style = type.action, color = colors.accent)
    }
}

/** The feeling a Moment carries, shown at the bottom-left of its print card (§10A.2). */
@Composable
fun MomentFeelingMark(
    feeling: MomentFeeling,
    modifier: Modifier = Modifier,
) {
    val label = moodVerdictLabel(feeling)
    FeelingFace(
        feeling = feeling,
        size = 22.dp,
        modifier = modifier.semantics { contentDescription = "Felt $label" },
    )
}
