package com.vaibhav.relive.ui.components.timeline

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
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
import com.vaibhav.relive.ui.theme.TimelineMomentForegroundColors
import com.vaibhav.relive.ui.theme.timelineMomentForegroundColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MomentCard(
    moment: MomentPresentation,
    mediaStore: MediaStore,
    onToggleFavorite: ((Boolean) -> Unit)?,
    onOpenMedia: (List<MomentAttachmentPresentation>, Int) -> Unit,
    sharedTransition: TimelineMediaSharedTransition? = null,
    canEditOrForget: Boolean,
    onEdit: () -> Unit,
    onForget: () -> Unit,
    onShowContextualActions: (() -> Unit)? = null,
    isContextuallySelected: Boolean = false,
    hasPreviousMoment: Boolean,
    showLocation: Boolean = true,
    showTags: Boolean = true,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val momentColors = timelineMomentForegroundColors(
        colors = colors,
        wallpaper = LocalTimelineWallpaperPalette.current,
    )
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    val motion = ReliveTheme.motion
    val selectionColor by animateColorAsState(
        targetValue = if (isContextuallySelected) {
            colors.accent.copy(alpha = com.vaibhav.relive.ui.theme.ReliveOpacity.Low)
        } else {
            Color.Transparent
        },
        animationSpec = tween(motion.durations.fastMillis, easing = motion.easings.emphasized),
        label = "moment contextual selection",
    )

    var actionsOpen by remember(moment.id, canEditOrForget) { mutableStateOf(false) }
    Box {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(selectionColor)
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
                    if (onShowContextualActions != null) {
                        haptics.perform(ReliveHapticCue.Context)
                        onShowContextualActions()
                    } else if (canEditOrForget) {
                        haptics.perform(ReliveHapticCue.Context)
                        actionsOpen = true
                    }
                },
            )
            .semantics {
                if (onShowContextualActions != null) {
                    customActions = listOf(
                        CustomAccessibilityAction("Show moment actions") {
                            haptics.perform(ReliveHapticCue.Action)
                            onShowContextualActions()
                            true
                        },
                    )
                } else if (canEditOrForget) {
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
        Spacer(Modifier.width(dims.spacing.none))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = dims.spacing.none),
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
                            color = momentColors.accentMuted,
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(momentColors.accentMuted),
                        )
                        Text(
                            text = moment.formattedTime,
                            style = type.eyebrow,
                            color = momentColors.accentMuted,
                        )
                    }
                    if (showLocation && moment.locationLabel != null) {
                        Text(
                            text = moment.locationLabel,
                            style = type.eyebrow,
                            color = momentColors.textSecondary,
                            modifier = Modifier.padding(top = dims.spacing.xs),
                        )
                    }
                }
                onToggleFavorite?.let { toggle ->
                    FavoriteHeart(
                        isFavorite = moment.isFavorite,
                        momentColors = momentColors,
                        onToggle = { toggle(!moment.isFavorite) },
                    )
                }
            }

            // The Moment content — title, description, media, tags — now lives inside one
            // physical "print" card pinned to the wallpaper. Metadata above stays on the
            // background; the card carries its own opaque surface, so content is resolved
            // against the app theme rather than the wallpaper.
            Spacer(Modifier.height(dims.spacing.md))
            PinnedMomentCard(
                moment = moment,
                mediaStore = mediaStore,
                onOpenMedia = onOpenMedia,
                sharedTransition = sharedTransition,
                showTags = showTags,
            )
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

/**
 * One Moment rendered as a physical print card pinned to the timeline wallpaper. The card owns a
 * warm, opaque surface with soft corners, a subtle lift, and a Polaroid-style band of whitespace
 * below its content. A tiny accent push-pin sits centred over the top edge so the card reads as
 * pinned to the wallpaper behind it — independent of the timeline rail, which stays the timeline
 * indicator. Content resolves against the app theme because it now sits on the card, not the
 * wallpaper. Wraps its content: text-only Moments stay compact rather than reserving media space.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinnedMomentCard(
    moment: MomentPresentation,
    mediaStore: MediaStore,
    onOpenMedia: (List<MomentAttachmentPresentation>, Int) -> Unit,
    sharedTransition: TimelineMediaSharedTransition?,
    showTags: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val cardShape = RoundedCornerShape(dims.radii.sm)
    val pinSize = dims.icon.lg
    // The print is always white paper, in every theme. Because the card stays a light surface even
    // in dark mode, its text must resolve against white rather than the dark-theme foreground.
    val printSurface = Color.White
    val onCardPrimary = if (ReliveTheme.isDark) OnPrintPrimary else colors.textPrimary
    val onCardSecondary = if (ReliveTheme.isDark) OnPrintSecondary else colors.textSecondary
    val onCardChipFill = if (ReliveTheme.isDark) OnPrintChipFill else colors.tint

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Reserve the pin's upper half so it overhangs the top edge.
                .padding(top = pinSize / 2)
                .shadow(
                    elevation = MomentCardElevation,
                    shape = cardShape,
                    ambientColor = colors.shadow,
                    spotColor = colors.shadow,
                )
                .clip(cardShape)
                .background(printSurface)
                .padding(
                    start = dims.spacing.lg,
                    end = dims.spacing.lg,
                    top = dims.spacing.xl,
                    // Larger bottom band, echoing a Polaroid/Instax print's white margin.
                    bottom = dims.spacing.xxl,
                ),
        ) {
            var precededByText = false

            if (moment.hasTitle) {
                Text(
                    text = moment.title,
                    style = type.title,
                    color = onCardPrimary,
                )
                precededByText = true
            }

            if (moment.hasContent) {
                if (precededByText) Spacer(Modifier.height(dims.spacing.sm))
                ExpandableContent(
                    text = moment.content,
                    contentColor = onCardSecondary,
                    accentColor = colors.accent,
                )
                precededByText = true
            }

            if (moment.hasAttachments) {
                if (precededByText) Spacer(Modifier.height(dims.spacing.lg))
                TimelineMediaSection(
                    attachments = moment.attachments,
                    mediaStore = mediaStore,
                    onOpen = onOpenMedia,
                    sharedTransition = sharedTransition,
                )
            }

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
                                // Soft supporting fill: the chip must read against the white card
                                // it sits on, not blend into it, in either theme.
                                .background(onCardChipFill)
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
                                color = onCardSecondary,
                            )
                        }
                    }
                }
            }
        }

        MomentPin(
            // Matches the floating navigation bar's surface colour.
            color = colors.surfaceFloating,
            shadowColor = colors.shadow,
            size = pinSize,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * A realistic stationery push-pin drawn from primitives so its size and colour stay under our
 * control: a domed grip head, a narrow waist, a rounded barrel, and a metallic needle, shaded with
 * highlights and shade tones and tilted for a three-dimensional look. A soft cast shadow and a
 * punched hole where the needle meets the paper sell the illusion that the card is physically
 * pinned. Purely decorative — hidden from TalkBack. No backing shape; it sits over the card's top
 * edge. [color] is the base plastic tone; every lighter/darker facet is derived from it so the pin
 * follows the theme.
 */
@Composable
private fun MomentPin(
    color: Color,
    shadowColor: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val headLight = lerp(color, Color.White, 0.45f)
    val headShade = lerp(color, Color.Black, 0.28f)
    val bodyShade = lerp(color, Color.Black, 0.42f)
    val needleBase = Color(0xFFAEB3BC)
    val needleLight = Color(0xFFE9ECF1)
    val tiltDeg = -22f

    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics {},
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val pivot = Offset(cx, h * 0.5f)

        // The needle tip in the upright design, rotated by the tilt so the hole and shadow land
        // exactly where the pin actually points.
        val rad = tiltDeg * (kotlin.math.PI.toFloat() / 180f)
        val cos = kotlin.math.cos(rad)
        val sin = kotlin.math.sin(rad)
        val tipUx = cx
        val tipUy = h * 0.98f
        val tip = Offset(
            pivot.x + (tipUx - pivot.x) * cos - (tipUy - pivot.y) * sin,
            pivot.y + (tipUx - pivot.x) * sin + (tipUy - pivot.y) * cos,
        )

        // Cast shadow: a soft blob under the head, lifting the pin off the paper.
        drawOval(
            color = shadowColor.copy(alpha = 0.15f),
            topLeft = Offset(w * 0.24f, h * 0.30f),
            size = Size(w * 0.66f, h * 0.46f),
        )
        // Punched hole in the paper where the needle enters.
        drawOval(
            color = shadowColor.copy(alpha = 0.55f),
            topLeft = Offset(tip.x - w * 0.10f, tip.y - h * 0.04f),
            size = Size(w * 0.20f, h * 0.10f),
        )

        rotate(degrees = tiltDeg, pivot = pivot) {
            // NEEDLE — metallic spike from the bulb down to the tip.
            val needle = Path().apply {
                moveTo(cx - w * 0.05f, h * 0.62f)
                lineTo(cx + w * 0.05f, h * 0.62f)
                lineTo(cx, h * 0.98f)
                close()
            }
            drawPath(needle, needleBase)
            drawLine(
                color = needleLight,
                start = Offset(cx - w * 0.015f, h * 0.64f),
                end = Offset(cx - w * 0.004f, h * 0.94f),
                strokeWidth = w * 0.022f,
            )

            // BULB — the large rounded body, widest of the pin. Shade fills the lower half for
            // volume; the lit tone sits on top; glossy speckles catch the light.
            drawOval(bodyShade, Offset(cx - w * 0.30f, h * 0.34f), Size(w * 0.60f, h * 0.34f))
            drawOval(color, Offset(cx - w * 0.30f, h * 0.30f), Size(w * 0.60f, h * 0.30f))
            drawOval(headLight, Offset(cx - w * 0.22f, h * 0.34f), Size(w * 0.22f, h * 0.13f))
            drawCircle(headLight.copy(alpha = 0.75f), w * 0.03f, Offset(cx + w * 0.11f, h * 0.52f))

            // NECK — narrow waist joining grip to bulb.
            drawRoundRect(
                color = bodyShade,
                topLeft = Offset(cx - w * 0.07f, h * 0.20f),
                size = Size(w * 0.14f, h * 0.14f),
                cornerRadius = CornerRadius(w * 0.05f),
            )

            // GRIP CAP — the small tilted disc on top, with its own rim shade and highlight.
            drawOval(headShade, Offset(cx - w * 0.20f, h * 0.10f), Size(w * 0.40f, h * 0.15f))
            drawOval(color, Offset(cx - w * 0.20f, h * 0.07f), Size(w * 0.40f, h * 0.13f))
            drawOval(headLight, Offset(cx - w * 0.14f, h * 0.09f), Size(w * 0.16f, h * 0.07f))
        }
    }
}

private const val MinExpandThreshold = 140

/** Resting lift for a pinned Moment card — enough shadow to read as a card off the paper. */
private val MomentCardElevation: Dp = 10.dp

// The card is always white, so in dark mode its content still needs dark-on-white tones and a light
// chip fill rather than the dark-theme foreground tokens.
private val OnPrintPrimary = Color(0xFF221D2B)
private val OnPrintSecondary = Color(0xFF5A5563)
private val OnPrintChipFill = Color(0xFFEFEBF3)

@Composable
private fun ExpandableContent(
    text: String,
    contentColor: Color,
    accentColor: Color,
) {
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    var expanded by remember(text) { mutableStateOf(false) }
    val collapsedLines = 3

    Column(modifier = Modifier.animateContentSize()) {
        Text(
            text = text,
            style = type.body,
            color = contentColor,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
        )
        if (text.length > MinExpandThreshold) {
            Spacer(Modifier.height(dims.spacing.xs))
            Text(
                text = if (expanded) "less" else "... more",
                style = type.action,
                color = accentColor,
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

@Composable
private fun FavoriteHeart(
    isFavorite: Boolean,
    momentColors: TimelineMomentForegroundColors,
    onToggle: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val tint = if (isFavorite) momentColors.accent else momentColors.textMuted
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
