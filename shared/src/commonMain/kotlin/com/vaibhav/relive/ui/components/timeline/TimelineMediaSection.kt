package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedAudioTile
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.platform.media.rememberImageAspectRatioFor
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Adaptive timeline collage for a moment's attachments (ADR-0019 §1). All
 * media types — image, video, audio — participate in the same grid. Order
 * follows the attachment list, which the presentation mapper produces by
 * ADR-0013 `sort_index`. For 5+ attachments only the first four render
 * inline; the fourth tile receives a translucent `+N` overlay.
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
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    val selection = remember(attachments) { selectCollage(attachments) }
    val openAt: (Int) -> Unit = { idx -> onOpen(attachments, idx) }
    Box(modifier = modifier.fillMaxWidth()) {
        when (selection.shape) {
            CollageShape.Single -> SingleTile(selection.visible[0], mediaStore, onClick = { openAt(0) })
            CollageShape.Two -> TwoTiles(selection.visible, mediaStore, openAt)
            CollageShape.Three -> ThreeTiles(selection.visible, mediaStore, openAt)
            CollageShape.Four -> FourTiles(selection.visible, mediaStore, overflow = 0, openAt = openAt)
            CollageShape.FourPlus -> FourTiles(selection.visible, mediaStore, overflow = selection.overflow, openAt = openAt)
        }
    }
}

@Composable
private fun SingleTile(
    att: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val naturalAspect = when (att.type) {
        MediaType.Image -> rememberImageAspectRatioFor(att.storageRef, mediaStore)
            ?: dims.media.collageDominantAspect
        MediaType.Video -> dims.media.collageVideoAspect
        MediaType.Audio -> dims.media.collageAudioAspect
    }
    // ADR-0019 §3: single-tile height MUST NOT exceed collageSingleMaxHeight.
    // A `heightIn(max)` before `aspectRatio` is silently ignored when
    // `fillMaxWidth` pins the width — Compose's aspectRatio then returns
    // height = width / aspect, which for a portrait image dominates the screen.
    // Bound the aspect to the ratio that yields exactly the height cap at the
    // available width; RelivedImageTile's ContentScale.Crop absorbs the rest.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val minAllowedAspect = maxWidth.value / dims.media.collageSingleMaxHeight.value
        val effectiveAspect = naturalAspect.coerceAtLeast(minAllowedAspect)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(effectiveAspect)
                .clip(RoundedCornerShape(dims.radii.md))
                .clickable(onClick = onClick)
                .semantics { contentDescription = openLabelFor(att.type) },
        ) {
            Tile(att, mediaStore, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TwoTiles(
    items: List<MomentAttachmentPresentation>,
    mediaStore: MediaStore,
    openAt: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.media.collageGap),
    ) {
        GridCellImpl(items[0], mediaStore, Modifier.weight(1f).aspectRatio(dims.media.collageTileAspectSquare), overflow = 0, onClick = { openAt(0) })
        GridCellImpl(items[1], mediaStore, Modifier.weight(1f).aspectRatio(dims.media.collageTileAspectSquare), overflow = 0, onClick = { openAt(1) })
    }
}

@Composable
private fun ThreeTiles(
    items: List<MomentAttachmentPresentation>,
    mediaStore: MediaStore,
    openAt: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier.fillMaxWidth().aspectRatio(dims.media.collageDominantAspect),
        horizontalArrangement = Arrangement.spacedBy(dims.media.collageGap),
    ) {
        GridCellImpl(items[0], mediaStore, Modifier.weight(2f).fillMaxSize(), overflow = 0, onClick = { openAt(0) })
        Column(
            modifier = Modifier.weight(1f).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(dims.media.collageGap),
        ) {
            GridCellImpl(items[1], mediaStore, Modifier.weight(1f).fillMaxSize(), overflow = 0, onClick = { openAt(1) })
            GridCellImpl(items[2], mediaStore, Modifier.weight(1f).fillMaxSize(), overflow = 0, onClick = { openAt(2) })
        }
    }
}

@Composable
private fun FourTiles(
    items: List<MomentAttachmentPresentation>,
    mediaStore: MediaStore,
    overflow: Int,
    openAt: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dims.media.collageGap),
    ) {
        FourRow(items[0], items[1], mediaStore, overlayIndex = -1, overflow = 0, baseIndex = 0, openAt = openAt)
        FourRow(items[2], items[3], mediaStore, overlayIndex = 1, overflow = overflow, baseIndex = 2, openAt = openAt)
    }
}

@Composable
private fun FourRow(
    left: MomentAttachmentPresentation,
    right: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    overlayIndex: Int,
    overflow: Int,
    baseIndex: Int,
    openAt: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.media.collageGap),
    ) {
        GridCellImpl(
            att = left,
            mediaStore = mediaStore,
            modifier = Modifier.weight(1f).aspectRatio(dims.media.collageTileAspectSquare),
            overflow = if (overlayIndex == 0) overflow else 0,
            onClick = { openAt(baseIndex) },
        )
        GridCellImpl(
            att = right,
            mediaStore = mediaStore,
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
    modifier: Modifier,
    overflow: Int,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val desc = if (overflow > 0) "Open $overflow more media" else openLabelFor(att.type)
    Box(
        modifier = modifier
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
