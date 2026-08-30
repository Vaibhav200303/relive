package com.vaibhav.relive.ui.components.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.ui.components.timeline.TimelineWallpaperSurface
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedAudioTile
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.presentation.viewer.MomentMediaGalleryState
import com.vaibhav.relive.ui.components.timeline.TimelineMediaSharedTransition

/**
 * Dedicated per-Moment media gallery (multi-attachment flow only). Black
 * media-focused surface. Vertically scrollable adaptive grid across all
 * attachments in original order. Reuses collage tile composables so audio
 * keeps its approved black-tile + waveform identity and video shows a
 * still frame with a Play affordance (no autoplay). This surface is not a
 * timeline: no rail, dot, metadata, tags, or favorite UI.
 */
@Composable
fun MomentMediaGallery(
    state: MomentMediaGalleryState,
    mediaStore: MediaStore,
    onOpenItem: (Int) -> Unit,
    onClose: () -> Unit,
    backEnabled: Boolean = true,
    wallpaper: TimelineWallpaper = TimelineWallpaper.WarmCream,
    sharedTransition: TimelineMediaSharedTransition? = null,
) {
    val gridState = rememberLazyGridState()

    ReliveBackHandler(enabled = backEnabled, onBack = onClose)

    // The grid lays its tiles over the timeline's own doodle wallpaper; the gaps between tiles
    // let the wallpaper breathe through so the gallery stays part of the memory's world.
    TimelineWallpaperSurface(
        wallpaper = wallpaper,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            GalleryTopBar(count = state.size, onClose = onClose)
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = GalleryGutter, vertical = GalleryGutter),
                horizontalArrangement = Arrangement.spacedBy(GalleryGap),
                verticalArrangement = Arrangement.spacedBy(GalleryGap),
            ) {
                itemsIndexed(
                    items = state.attachments,
                    key = { i, a -> a.storageRef.value + ":" + i },
                ) { index, att ->
                    GalleryTile(
                        att = att,
                        mediaStore = mediaStore,
                        sharedTransition = sharedTransition,
                        onClick = { onOpenItem(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryTile(
    att: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    sharedTransition: TimelineMediaSharedTransition?,
    onClick: () -> Unit,
) {
    val desc = when (att.type) {
        MediaType.Image -> "Open image"
        MediaType.Video -> "Open video"
        MediaType.Audio -> "Open audio"
    }
    Box(
        modifier = Modifier
            .then(sharedTransition?.galleryModifier(att) ?: Modifier)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(GalleryTileBase)
            .clickable(onClick = onClick)
            .semantics { contentDescription = desc },
    ) {
        when (att.type) {
            MediaType.Image -> RelivedImageTile(att.storageRef, mediaStore, Modifier.fillMaxSize())
            MediaType.Video -> RelivedVideoTile(att.storageRef, mediaStore, Modifier.fillMaxSize())
            MediaType.Audio -> RelivedAudioTile(att.storageRef, mediaStore, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun GalleryTopBar(count: Int, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clip(RoundedCornerShape(GalleryChromeRadius))
                .background(GalleryChromeScrim),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = "Close media gallery" },
            ) { Text("✕", color = GalleryChromeInk) }
        }
        Text(
            text = "$count items",
            color = GalleryChromeInk,
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(GalleryChromeRadius))
                .background(GalleryChromeScrim)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd).size(44.dp))
    }
}

private val GalleryGutter = 12.dp
private val GalleryGap = 8.dp
// Chrome and tile bases sit over the fixed-light wallpaper, so they use mode-independent tones.
private val GalleryChromeScrim = Color(0xE6FFFFFF)
private val GalleryChromeInk = Color(0xFF23202B)
private val GalleryChromeRadius = 999.dp
private val GalleryTileBase = Color(0x1F000000)
