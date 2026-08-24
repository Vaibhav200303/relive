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
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedAudioTile
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.presentation.viewer.MomentMediaGalleryState

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
) {
    val gridState = rememberLazyGridState()

    ReliveBackHandler(enabled = backEnabled, onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                    onClick = { onOpenItem(index) },
                )
            }
        }
    }
}

@Composable
private fun GalleryTile(
    att: MomentAttachmentPresentation,
    mediaStore: MediaStore,
    onClick: () -> Unit,
) {
    val desc = when (att.type) {
        MediaType.Image -> "Open image"
        MediaType.Video -> "Open video"
        MediaType.Audio -> "Open audio"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
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
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .semantics { contentDescription = "Close media gallery" },
        ) { Text("✕", color = Color.White) }
        Text(
            text = "$count items",
            color = Color(0xFFEFECE5),
            modifier = Modifier.align(Alignment.Center),
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd).size(48.dp))
    }
}

private val GalleryGutter = 8.dp
private val GalleryGap = 4.dp
