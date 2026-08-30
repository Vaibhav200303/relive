package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.NaturalSizePx
import com.vaibhav.relive.platform.media.RelivedAudioTile
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedTimelineInlineVideo
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.platform.media.rememberImageNaturalSizeFor
import com.vaibhav.relive.platform.media.rememberVideoNaturalSizeFor
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.ui.media.computeAdaptiveMediaPreview
import com.vaibhav.relive.ui.media.fallbackAdaptivePreviewSize
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Adaptive timeline collage for a moment's attachments (ADR-0019 §1). All
 * media types — image, video, audio — participate in the same grid. Order
 * follows the attachment list, which the presentation mapper produces by
 * ADR-0013 `sort_index`. For 5+ attachments only the first four render
 * inline; the fourth tile receives a translucent `+N` overlay.
 *
 * Single-media Moments use adaptive natural sizing: the container
 * shrink-wraps around the media's aspect ratio, subject only to timeline
 * width/height maximums. Multi-media Moments retain the existing 2/3/4/5+
 * collage geometry unchanged.
 *
 * Tapping a tile invokes [onOpen] with the full attachment list and the
 * tapped attachment's index into that list (ADR-0019 §5). The `+N` tile
 * opens the viewer at the fourth attachment (index 3) so the swipe pager
 * exposes the hidden overflow.
 */
@Composable
fun TimelineMediaSection(
    attachments: List<MomentAttachmentPresentation>,
    mediaStore: MediaStore,
    onOpen: (List<MomentAttachmentPresentation>, Int) -> Unit,
    sharedTransition: TimelineMediaSharedTransition? = null,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    val selection = remember(attachments) { selectCollage(attachments) }
    val openAt: (Int) -> Unit = { idx -> onOpen(attachments, idx) }
    Box(modifier = modifier.fillMaxWidth()) {
        when (selection.shape) {
            CollageShape.Single -> SingleTile(selection.visible[0], mediaStore, sharedTransition, onClick = { openAt(0) })
            CollageShape.Two -> TwoTiles(selection.visible, mediaStore, sharedTransition, openAt)
            CollageShape.Three -> ThreeTiles(selection.visible, mediaStore, sharedTransition, openAt)
            CollageShape.Four -> FourTiles(selection.visible, mediaStore, sharedTransition, overflow = 0, openAt = openAt)
            CollageShape.FourPlus -> FourTiles(selection.visible, mediaStore, sharedTransition, overflow = selection.overflow, openAt = openAt)
        }
    }
}

@Composable
private fun SingleTile(
    att: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    sharedTransition: TimelineMediaSharedTransition?,
    onClick: () -> Unit,
) {
    if (att.type == MediaType.Audio) {
        SingleAudioTile(att, mediaStore, sharedTransition, onClick)
        return
    }
    SingleVisualTile(att, mediaStore, sharedTransition, onClick)
}

/**
 * Adaptive photo/video single tile. Container matches the media's natural
 * aspect ratio (rotation-corrected for videos), bounded by the timeline
 * single-media max width/height. A hairline hugs the resulting box.
 */
@Composable
private fun SingleVisualTile(
    att: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    sharedTransition: TimelineMediaSharedTransition?,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val density = LocalDensity.current

    val natural: NaturalSizePx? = when (att.type) {
        MediaType.Image -> rememberImageNaturalSizeFor(att.storageRef, mediaStore)
        MediaType.Video -> rememberVideoNaturalSizeFor(att.storageRef, mediaStore)
        MediaType.Audio -> null
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        // A single tile shrink-wraps its media; centre it in the card rather than left-align.
        contentAlignment = Alignment.TopCenter,
    ) {
        val maxW = maxWidth
        val maxH = dims.media.timelineSinglePreviewMaxHeight
        val displaySize: DpSize
        val inlineVideoAllowed: Boolean
        if (natural != null) {
            val preview = computeAdaptiveMediaPreview(natural, maxW, maxH, density)
            displaySize = preview.size
            inlineVideoAllowed = att.type == MediaType.Video && !preview.wasConstrained
        } else {
            displaySize = fallbackAdaptivePreviewSize(
                maxWidth = maxW,
                maxHeight = maxH,
                fallbackAspect = dims.media.timelineSingleFallbackAspect,
                fallbackHeight = dims.media.timelineSingleFallbackHeight,
            )
            inlineVideoAllowed = false
        }
        val outerModifier = (sharedTransition?.sourceModifier(att) ?: Modifier)
            .size(displaySize.width, displaySize.height)
            .clip(RoundedCornerShape(dims.radii.md))
            .border(
                width = dims.media.collageBorder,
                color = colors.accent,
                shape = RoundedCornerShape(dims.radii.md),
            )
        if (inlineVideoAllowed) {
            // Inline video owns its own hit targets: Play/Pause button
            // toggles playback in place, body tap opens the full-screen
            // viewer. No outer clickable so the button never routes to nav.
            Box(modifier = outerModifier) {
                RelivedTimelineInlineVideo(
                    ref = att.storageRef,
                    mediaStore = mediaStore,
                    onOpenFullScreen = onClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Box(
                modifier = outerModifier
                    .clickable(onClick = onClick)
                    .semantics { contentDescription = openLabelFor(att.type) },
            ) {
                Tile(att, mediaStore, Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * Adaptive audio single tile. Preserves the Stage 2 waveform visual
 * identity: bounded black canvas at column width, waveform + controls
 * rendered by [RelivedAudioTile]. A hairline hugs the black canvas so it
 * reads as a deliberate media surface rather than a raw block.
 */
@Composable
private fun SingleAudioTile(
    att: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    sharedTransition: TimelineMediaSharedTransition?,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Box(
        modifier = (sharedTransition?.sourceModifier(att) ?: Modifier)
            .fillMaxWidth()
            .height(dims.media.timelineSingleAudioHeight)
            .clip(RoundedCornerShape(dims.radii.md))
            .border(
                width = dims.media.collageBorder,
                color = colors.accent,
                shape = RoundedCornerShape(dims.radii.md),
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = openLabelFor(att.type) },
    ) {
        RelivedAudioTile(att.storageRef, mediaStore, Modifier.fillMaxSize())
    }
}

@Composable
private fun TwoTiles(
    items: List<MomentAttachmentPresentation>,
    mediaStore: MediaStore,
    sharedTransition: TimelineMediaSharedTransition?,
    openAt: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.radii.md))
            .background(colors.accent, RoundedCornerShape(dims.radii.md))
            .border(
                width = dims.media.collageBorder,
                color = colors.accent,
                shape = RoundedCornerShape(dims.radii.md),
            ),
        horizontalArrangement = Arrangement.spacedBy(dims.media.collageBorder),
    ) {
        GridCellImpl(items[0], mediaStore, sharedTransition, Modifier.weight(1f).aspectRatio(dims.media.collageTileAspectSquare), overflow = 0, onClick = { openAt(0) })
        GridCellImpl(items[1], mediaStore, sharedTransition, Modifier.weight(1f).aspectRatio(dims.media.collageTileAspectSquare), overflow = 0, onClick = { openAt(1) })
    }
}

@Composable
private fun ThreeTiles(
    items: List<MomentAttachmentPresentation>,
    mediaStore: MediaStore,
    sharedTransition: TimelineMediaSharedTransition?,
    openAt: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(dims.media.collageDominantAspect)
            .clip(RoundedCornerShape(dims.radii.md))
            .background(colors.accent, RoundedCornerShape(dims.radii.md))
            .border(
                width = dims.media.collageBorder,
                color = colors.accent,
                shape = RoundedCornerShape(dims.radii.md),
            ),
        horizontalArrangement = Arrangement.spacedBy(dims.media.collageBorder),
    ) {
        GridCellImpl(items[0], mediaStore, sharedTransition, Modifier.weight(2f).fillMaxSize(), overflow = 0, onClick = { openAt(0) })
        Column(
            modifier = Modifier.weight(1f).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(dims.media.collageBorder),
        ) {
            GridCellImpl(items[1], mediaStore, sharedTransition, Modifier.weight(1f).fillMaxSize(), overflow = 0, onClick = { openAt(1) })
            GridCellImpl(items[2], mediaStore, sharedTransition, Modifier.weight(1f).fillMaxSize(), overflow = 0, onClick = { openAt(2) })
        }
    }
}

@Composable
private fun FourTiles(
    items: List<MomentAttachmentPresentation>,
    mediaStore: MediaStore,
    sharedTransition: TimelineMediaSharedTransition?,
    overflow: Int,
    openAt: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.radii.md))
            .background(colors.accent, RoundedCornerShape(dims.radii.md))
            .border(
                width = dims.media.collageBorder,
                color = colors.accent,
                shape = RoundedCornerShape(dims.radii.md),
            ),
        verticalArrangement = Arrangement.spacedBy(dims.media.collageBorder),
    ) {
        FourRow(items[0], items[1], mediaStore, sharedTransition, overlayIndex = -1, overflow = 0, baseIndex = 0, openAt = openAt)
        FourRow(items[2], items[3], mediaStore, sharedTransition, overlayIndex = 1, overflow = overflow, baseIndex = 2, openAt = openAt)
    }
}

@Composable
private fun FourRow(
    left: MomentAttachmentPresentation,
    right: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    sharedTransition: TimelineMediaSharedTransition?,
    overlayIndex: Int,
    overflow: Int,
    baseIndex: Int,
    openAt: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.media.collageBorder),
    ) {
        GridCellImpl(
            att = left,
            mediaStore = mediaStore,
            sharedTransition = sharedTransition,
            modifier = Modifier.weight(1f).aspectRatio(dims.media.collageTileAspectSquare),
            overflow = if (overlayIndex == 0) overflow else 0,
            onClick = { openAt(baseIndex) },
        )
        GridCellImpl(
            att = right,
            mediaStore = mediaStore,
            sharedTransition = sharedTransition,
            modifier = Modifier.weight(1f).aspectRatio(dims.media.collageTileAspectSquare),
            overflow = if (overlayIndex == 1) overflow else 0,
            onClick = { openAt(baseIndex + 1) },
        )
    }
}

@Composable
private fun GridCellImpl(
    att: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    sharedTransition: TimelineMediaSharedTransition?,
    modifier: Modifier,
    overflow: Int,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val desc = if (overflow > 0) "Open $overflow more media" else openLabelFor(att.type)
    Box(
        modifier = (sharedTransition?.sourceModifier(att) ?: Modifier)
            .then(modifier)
            .clip(RoundedCornerShape(dims.radii.md))
            .clickable(onClick = onClick)
            .semantics { contentDescription = desc },
    ) {
        Tile(att, mediaStore, Modifier.fillMaxSize())
        if (overflow > 0) OverflowBadge(overflow)
    }
}

@Composable
private fun Tile(
    att: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    modifier: Modifier,
) {
    when (att.type) {
        MediaType.Image -> RelivedImageTile(att.storageRef, mediaStore, modifier)
        MediaType.Video -> RelivedVideoTile(att.storageRef, mediaStore, modifier)
        MediaType.Audio -> RelivedAudioTile(att.storageRef, mediaStore, modifier)
    }
}

@Composable
private fun BoxScope.OverflowBadge(overflow: Int) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "+$overflow", color = Color.White)
    }
}

private fun openLabelFor(type: MediaType): String = when (type) {
    MediaType.Image -> "Open image"
    MediaType.Video -> "Open video"
    MediaType.Audio -> "Open audio"
}
