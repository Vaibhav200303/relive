package com.vaibhav.relive.ui.components.timeline

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * A single moment entry rendered on the All timeline. The parent lays out the rail
 * as a continuous background line; this row draws only its own dot and content.
 */
@Composable
fun MomentCard(
    moment: MomentPresentation,
    onToggleFavorite: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dims.spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        // Rail gutter with a centered dot aligned to the eyebrow row.
        Box(
            modifier = Modifier
                .width(dims.timeline.contentInset)
                .padding(top = dims.spacing.xs),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(dims.timeline.dotSize)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = dims.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
            ) {
                Text(
                    text = moment.formattedDate,
                    style = type.eyebrow,
                    color = colors.accentMuted,
                )
                if (moment.locationLabel != null) {
                    Text(
                        text = "· ${moment.locationLabel.uppercase()}",
                        style = type.eyebrow,
                        color = colors.textMuted,
                    )
                }
            }
            if (moment.hasTitle) {
                Spacer(Modifier.height(dims.spacing.xs))
                Text(
                    text = moment.title,
                    style = type.title,
                    color = colors.textPrimary,
                )
            }
            if (moment.hasContent) {
                Spacer(Modifier.height(dims.spacing.sm))
                ExpandableContent(text = moment.content)
            }
        }
        FavoriteHeart(
            isFavorite = moment.isFavorite,
            onToggle = { onToggleFavorite(!moment.isFavorite) },
        )
    }
}

@Composable
private fun ExpandableContent(text: String) {
    val type = ReliveTheme.typography
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    var expanded by remember(text) { mutableStateOf(false) }
    val collapsedLines = 3

    Column(modifier = Modifier.animateContentSize()) {
        Text(
            text = text,
            style = type.body,
            color = colors.textSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
        )
        // Only offer expansion when the collapsed view would truncate. A safe
        // approximation: text long enough to overflow three lines at the body
        // style. Presentation prefers a low-risk heuristic over a re-measurement
        // pass; matches the reference "... more"/"less" behaviour visually.
        if (text.length > MinExpandThreshold) {
            Spacer(Modifier.height(dims.spacing.xs))
            Text(
                text = if (expanded) "less" else "... more",
                style = type.action,
                color = colors.accent,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { expanded = !expanded }
                    .semantics {
                        contentDescription = if (expanded) "Show less" else "Show more"
                    },
            )
        }
    }
}

private const val MinExpandThreshold = 140

@Composable
private fun FavoriteHeart(isFavorite: Boolean, onToggle: () -> Unit) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val tint = if (isFavorite) colors.accent else colors.textMuted
    val description = if (isFavorite) "Unfavorite moment" else "Favorite moment"
    IconButton(
        onClick = onToggle,
        modifier = Modifier
            .size(dims.minTouchTarget)
            .semantics { contentDescription = description },
    ) {
        HeartGlyph(
            size = dims.icon.md,
            color = tint,
            strokeWidth = dims.stroke.icon,
            filled = isFavorite,
        )
    }
}
