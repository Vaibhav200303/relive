package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedAudio
import com.vaibhav.relive.platform.media.RelivedImage
import com.vaibhav.relive.platform.media.RelivedVideo
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Renders a moment's stored attachments in the timeline.
 *
 * Photo/video attachments render inside an Instagram-style horizontal
 * carousel with snap behaviour and subtle page indicators. Audio
 * attachments are always drawn as a compact strip below the carousel.
 * A single-image moment collapses the carousel chrome and shows the image
 * on its own.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineMediaSection(
    attachments: List<MomentAttachmentPresentation>,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
) {
    val visual = attachments.filter { it.type == MediaType.Image || it.type == MediaType.Video }
    val audio = attachments.filter { it.type == MediaType.Audio }

    Column(modifier = modifier.fillMaxWidth()) {
        if (visual.isNotEmpty()) {
            VisualCarousel(visual, mediaStore)
        }
        if (audio.isNotEmpty()) {
            val dims = ReliveTheme.dimensions
            if (visual.isNotEmpty()) Spacer(Modifier.height(dims.spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.sm)) {
                audio.forEach { att ->
                    RelivedAudio(
                        ref = att.storageRef,
                        mediaStore = mediaStore,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VisualCarousel(
    items: List<MomentAttachmentPresentation>,
    mediaStore: MediaStore,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(dims.radii.md))
                .background(colors.surfaceCard)
                .border(
                    width = dims.stroke.hairline,
                    color = colors.borderMuted,
                    shape = RoundedCornerShape(dims.radii.md),
                ),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val att = items[page]
                when (att.type) {
                    MediaType.Image -> RelivedImage(att.storageRef, mediaStore, Modifier.fillMaxSize())
                    MediaType.Video -> RelivedVideo(att.storageRef, mediaStore, Modifier.fillMaxSize())
                    MediaType.Audio -> Unit
                }
            }
        }
        if (items.size > 1) {
            Spacer(Modifier.height(dims.spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                items.indices.forEach { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 6.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (active) colors.accent else colors.textMuted),
                    )
                }
            }
        }
    }
}
