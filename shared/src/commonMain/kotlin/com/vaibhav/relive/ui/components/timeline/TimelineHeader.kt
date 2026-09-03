package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.icons.TimelineActionIcons
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.presentation.cardcover.resolveAllTimelineCollage
import com.vaibhav.relive.ui.components.AllTimelineCollage
import com.vaibhav.relive.ui.screens.NoCoverPhotoPlaceholder
import com.vaibhav.relive.ui.theme.ReliveOpacity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

/** Timeline detail header with optional back navigation and a centered wordmark. */
@Composable
fun TimelineHeader(
    onBack: (() -> Unit)? = null,
    onJumpToDate: (() -> Unit)? = null,
    onChangeTheme: (() -> Unit)? = null,
    /** Leading slot for surfaces that are roots rather than details, so carry no back action. */
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .align(Alignment.CenterStart)
                    .semantics { contentDescription = "Back to Timeline Home" },
            ) {
                BackGlyph(size = dims.icon.lg, color = colors.textSecondary, strokeWidth = dims.stroke.icon)
            }
        } else if (leading != null) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) { leading() }
        }
        Text(
            text = "Relive",
            style = type.wordmark,
            // Ink, like every other title in the app: the palette's one-ink rule makes ink the
            // single font colour for a mode and leaves hierarchy to the serif wordmark style, so
            // accent stays on buttons, pills and selection. This is the same wordmark the Timelines
            // root draws one tab away, and it matches how a custom timeline titles its own hero.
            color = colors.textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
        if (onJumpToDate != null || onChangeTheme != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onChangeTheme != null) {
                    IconButton(
                        onClick = onChangeTheme,
                        modifier = Modifier
                            .size(dims.minTouchTarget)
                            .semantics { contentDescription = "Change timeline theme" },
                    ) {
                        PaletteGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
                    }
                }
                if (onJumpToDate != null) {
                    IconButton(
                        onClick = onJumpToDate,
                        modifier = Modifier
                            .size(dims.minTouchTarget)
                            .semantics { contentDescription = "Jump to date" },
                    ) {
                        CalendarGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
                    }
                }
            }
        }
    }
}

/** Custom-timeline identity hero. The cover is independent from Moment media. */
@Composable
fun TimelineCoverHero(
    name: String,
    coverPhotoRef: MediaStorageRef?,
    mediaStore: MediaStore,
    onBack: (() -> Unit)?,
    onJumpToDate: () -> Unit,
    onChangeTheme: () -> Unit,
    onUpdateCover: (() -> Unit)? = null,
    stretchPx: Float = 0f,
    coverContent: (@Composable (Modifier) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val density = LocalDensity.current
    val stretchHeight = with(density) { stretchPx.toDp() }
    val zoom = 1f + (stretchPx / with(density) { dims.timeline.coverHeroHeight.toPx() }) * .65f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dims.timeline.coverHeroHeight + stretchHeight)
            .clipToBounds()
            .background(colors.bgCanvas)
            .then(
                if (onUpdateCover != null) {
                    Modifier.clickable(
                        onClick = onUpdateCover,
                        role = Role.Button,
                    ).semantics { contentDescription = "Update cover photo" }
                } else {
                    Modifier
                },
            ),
    ) {
        if (coverContent != null) {
            coverContent(Modifier.fillMaxSize())
        } else if (coverPhotoRef != null && mediaStore.exists(coverPhotoRef)) {
            RelivedImageTile(
                coverPhotoRef,
                mediaStore,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                },
            )
        } else NoCoverPhotoPlaceholder(Modifier.fillMaxSize())
        // The lower canvas-color wash lets the cover resolve into the timeline instead
        // of ending at a hard visual boundary. It also keeps the title legible without
        // changing the centered crop of the selected image.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.42f to colors.bgCanvas.copy(alpha = 0f),
                            0.76f to colors.bgCanvas.copy(alpha = 0.74f),
                            1f to colors.bgCanvas,
                        ),
                    ),
                ),
        )
        if (onBack != null) IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(dims.spacing.md).size(dims.minTouchTarget).background(colors.surfaceFloating.copy(alpha = ReliveOpacity.VeryHigh), CircleShape).semantics { contentDescription = "Back to Timeline Home" },
        ) { BackGlyph(dims.icon.lg, colors.textPrimary, dims.stroke.icon) }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(dims.spacing.md)
                .background(
                    colors.surfaceFloating.copy(alpha = ReliveOpacity.VeryHigh),
                    RoundedCornerShape(dims.radii.pill),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onChangeTheme, modifier = Modifier.size(dims.minTouchTarget).semantics { contentDescription = "Change timeline theme" }) { PaletteGlyph(dims.icon.md, colors.textPrimary, dims.stroke.icon) }
            IconButton(onClick = onJumpToDate, modifier = Modifier.size(dims.minTouchTarget).semantics { contentDescription = "Jump to date" }) { CalendarGlyph(dims.icon.md, colors.textPrimary, dims.stroke.icon) }
        }
        Text(name, style = ReliveTheme.typography.coverTitle, color = colors.textPrimary, modifier = Modifier.align(Alignment.BottomStart).padding(dims.spacing.xl))
    }
}

/** Automatic All-timeline cover: an existing deterministic photo collage or no-media placeholder. */
@Composable
fun AllTimelineCoverHero(
    attachments: List<MediaAttachment>,
    collageBucket: Long,
    mediaStore: MediaStore,
    onBack: (() -> Unit)?,
    onJumpToDate: () -> Unit,
    onChangeTheme: () -> Unit,
    stretchPx: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val selection = resolveAllTimelineCollage(available = attachments, bucket = collageBucket)
    TimelineCoverHero(
        name = "All",
        coverPhotoRef = null,
        mediaStore = mediaStore,
        onBack = onBack,
        onJumpToDate = onJumpToDate,
        onChangeTheme = onChangeTheme,
        onUpdateCover = null,
        stretchPx = stretchPx,
        coverContent = { modifier ->
            if (selection.attachments.isEmpty()) {
                NoCoverPhotoPlaceholder(modifier)
            } else {
                AllTimelineCollage(
                    attachments = selection.attachments,
                    layout = requireNotNull(selection.layout),
                    mediaStore = mediaStore,
                    modifier = modifier,
                )
            }
        },
        modifier = modifier,
    )
}

/** A temporary All-timeline action bar for one long-pressed Moment. */
@Composable
fun TimelineMomentActionHeader(
    showEdit: Boolean,
    showAddToTimeline: Boolean,
    addToTimelineEnabled: Boolean,
    showForget: Boolean,
    onExit: () -> Unit,
    onEdit: () -> Unit,
    onAddToTimeline: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .size(dims.minTouchTarget)
                .align(Alignment.CenterStart)
                .semantics { contentDescription = "Exit moment selection" },
        ) {
            BackGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showEdit) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Edit moment" },
                ) {
                    Icon(TimelineActionIcons.Rename, contentDescription = null, tint = colors.textSecondary)
                }
            }
            if (showAddToTimeline) {
                IconButton(
                    onClick = onAddToTimeline,
                    enabled = addToTimelineEnabled,
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Add to timeline" },
                ) {
                    Icon(TimelineActionIcons.AddToTimeline, contentDescription = null, tint = colors.textSecondary)
                }
            }
            if (showForget) {
                IconButton(
                    onClick = onForget,
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Forget moment" },
                ) {
                    Icon(TimelineActionIcons.Delete, contentDescription = null, tint = colors.actionDestructive)
                }
            }
        }
    }
}

@Composable
private fun PaletteGlyph(
    size: androidx.compose.ui.unit.Dp,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
) {
    Canvas(Modifier.size(size)) {
        val stroke = Stroke(strokeWidth.toPx())
        drawCircle(color, radius = this.size.minDimension * 0.34f, style = stroke)
        listOf(
            Offset(this.size.width * 0.50f, this.size.height * 0.26f),
            Offset(this.size.width * 0.70f, this.size.height * 0.48f),
            Offset(this.size.width * 0.38f, this.size.height * 0.68f),
        ).forEach { center ->
            drawCircle(color, radius = this.size.minDimension * 0.035f, center = center)
        }
    }
}
