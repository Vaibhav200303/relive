package com.vaibhav.relive.ui.components.timeline

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MomentCard(
    moment: MomentPresentation,
    mediaStore: MediaStore,
    onToggleFavorite: ((Boolean) -> Unit)?,
    onOpenMedia: (List<MomentAttachmentPresentation>, Int) -> Unit,
    canEditOrForget: Boolean,
    onEdit: () -> Unit,
    onForget: () -> Unit,
    hasPreviousMoment: Boolean,
    showLocation: Boolean = true,
    showTags: Boolean = true,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()

    var actionsOpen by remember(moment.id, canEditOrForget) { mutableStateOf(false) }
    Box {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier.border(
                        width = dims.stroke.hairline,
                        color = colors.accentMuted,
                        shape = RoundedCornerShape(dims.radii.md),
                    )
                } else {
                    Modifier
                },
            )
            .drawBehind {
                val axis = dims.timeline.contentInset.toPx() / 2f
                val markerCenter = dims.spacing.xl.toPx() + dims.minTouchTarget.toPx() / 2f
                drawLine(
                    color = colors.borderMuted,
                    start = androidx.compose.ui.geometry.Offset(axis, if (hasPreviousMoment) 0f else markerCenter),
                    end = androidx.compose.ui.geometry.Offset(axis, size.height),
                    strokeWidth = dims.timeline.railWidth.toPx(),
                )
            }
            .padding(vertical = dims.spacing.xl)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    if (canEditOrForget) {
                        haptics.perform(ReliveHapticCue.Context)
                        actionsOpen = true
                    }
                },
            )
            .semantics {
                if (canEditOrForget) {
                    customActions = listOf(
                        CustomAccessibilityAction("Edit moment") {
                            haptics.perform(ReliveHapticCue.Action)
                            onEdit()
                            true
                        },
                        CustomAccessibilityAction("Forget moment") {
                            haptics.perform(ReliveHapticCue.Action)
                            onForget()
                            true
                        },
                    )
                }
            },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(dims.timeline.contentInset)
                .heightIn(min = dims.minTouchTarget),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(dims.timeline.dotSize)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
        Spacer(Modifier.width(dims.spacing.sm))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = dims.spacing.sm),
        ) {
            // The saved location is a second metadata line, aligned with DATE • TIME.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dims.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
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
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(colors.accentMuted),
                        )
                        Text(
                            text = moment.formattedTime,
                            style = type.eyebrow,
                            color = colors.accentMuted,
                        )
                    }
                    if (showLocation && moment.locationLabel != null) {
                        Text(
                            text = moment.locationLabel,
                            style = type.eyebrow,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = dims.spacing.xs),
                        )
                    }
                }
                onToggleFavorite?.let { toggle ->
                    FavoriteHeart(
                        isFavorite = moment.isFavorite,
                        onToggle = { toggle(!moment.isFavorite) },
                    )
                }
            }

            // TITLE
            if (moment.hasTitle) {
                Spacer(Modifier.height(dims.spacing.sm))
                Text(
                    text = moment.title,
                    style = type.title,
                    color = colors.textPrimary,
                )
            }

            // CONTENT
            if (moment.hasContent) {
                Spacer(Modifier.height(dims.spacing.sm))
                ExpandableContent(text = moment.content)
            }

            // MEDIA
            if (moment.hasAttachments) {
                Spacer(Modifier.height(dims.spacing.lg))
                TimelineMediaSection(
                    attachments = moment.attachments,
                    mediaStore = mediaStore,
                    onOpen = onOpenMedia,
                )
            }

            // TAGS
            if (showTags && moment.hasTags) {
                Spacer(Modifier.height(dims.spacing.md))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                ) {
                    moment.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.surfaceCard)
                                .border(
                                    width = dims.stroke.hairline,
                                    color = colors.borderMuted,
                                    shape = CircleShape,
                                )
                                .padding(horizontal = dims.spacing.md, vertical = dims.spacing.xs),
                        ) {
                            Text(
                                text = "#" + tag.label.lowercase(),
                                style = type.tag,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
        DropdownMenu(
            expanded = actionsOpen,
            onDismissRequest = { actionsOpen = false },
            shape = RoundedCornerShape(dims.radii.menu),
            containerColor = colors.surfaceOverlay,
            tonalElevation = 0.dp,
            shadowElevation = dims.spacing.xs,
            border = BorderStroke(dims.stroke.hairline, colors.borderMuted),
        ) {
            DropdownMenuItem(
                text = { Text("Edit", style = type.action, color = colors.textPrimary) },
                leadingIcon = { MomentMenuGlyph(forget = false) },
                modifier = Modifier.heightIn(min = dims.minTouchTarget),
                onClick = {
                    actionsOpen = false
                    haptics.perform(ReliveHapticCue.Action)
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("Forget", style = type.action, color = colors.actionDestructive) },
                leadingIcon = { MomentMenuGlyph(forget = true) },
                modifier = Modifier.heightIn(min = dims.minTouchTarget),
                onClick = {
                    actionsOpen = false
                    haptics.perform(ReliveHapticCue.Action)
                    onForget()
                },
            )
        }
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
        if (text.length > MinExpandThreshold) {
            Spacer(Modifier.height(dims.spacing.xs))
            Text(
                text = if (expanded) "less" else "... more",
                style = type.action,
                color = colors.accent,
                modifier = Modifier
                    .heightIn(min = dims.minTouchTarget)
                    .wrapContentHeight(Alignment.CenterVertically)
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
    val haptics = rememberReliveHaptics()
    IconButton(
        onClick = {
            haptics.perform(if (isFavorite) ReliveHapticCue.ToggleOff else ReliveHapticCue.ToggleOn)
            onToggle()
        },
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

@Composable
private fun MomentMenuGlyph(forget: Boolean) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val tint = if (forget) colors.actionDestructive else colors.textSecondary
    Canvas(Modifier.size(dims.icon.md)) {
        val stroke = dims.stroke.icon.toPx()
        if (forget) {
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.3f),
                androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.3f),
                stroke,
            )
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.38f),
                androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.82f),
                stroke,
            )
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.38f),
                androidx.compose.ui.geometry.Offset(size.width * 0.58f, size.height * 0.82f),
                stroke,
            )
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.2f),
                androidx.compose.ui.geometry.Offset(size.width * 0.58f, size.height * 0.2f),
                stroke,
            )
        } else {
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.76f),
                androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.28f),
                stroke,
            )
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.82f),
                androidx.compose.ui.geometry.Offset(size.width * 0.76f, size.height * 0.34f),
                stroke,
            )
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.76f),
                androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.82f),
                stroke,
            )
        }
    }
}
