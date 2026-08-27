package com.vaibhav.relive.ui.components.rediscover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.RediscoveredMoment
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.platform.media.rememberMediaDurationMs
import com.vaibhav.relive.platform.media.rememberWaveformFor
import com.vaibhav.relive.ui.components.timeline.WaveformView
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun RediscoverMomentVisual(
    moment: RediscoveredMoment,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    hero: Boolean = false,
    metadata: String? = null,
    onOpenMedia: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val attachment = moment.attachments.minByOrNull { it.sortIndex }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.radii.md))
            .background(colors.surfaceCard),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        if (attachment != null) {
            RediscoverMediaLead(
                attachment = attachment,
                attachmentCount = moment.attachments.size,
                mediaStore = mediaStore,
                compact = compact,
                onClick = onOpenMedia,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = if (hero) dims.rediscover.heroInfoAreaMinHeight else dims.rediscover.compactInfoAreaHeight,
                )
                .padding(dims.spacing.lg),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                when {
                    moment.title.isNotBlank() -> Text(
                        text = moment.title,
                        style = if (compact) ReliveTheme.typography.action else ReliveTheme.typography.title,
                        color = colors.textPrimary,
                        maxLines = if (compact) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    moment.content.isBlank() && attachment != null -> Text(
                        "A saved memory",
                        style = ReliveTheme.typography.subtitle,
                        color = colors.textSecondary,
                    )
                }
            }
            if (moment.content.isNotBlank()) {
                Text(
                    text = moment.content,
                    style = if (compact) ReliveTheme.typography.subtitle else ReliveTheme.typography.body,
                    color = colors.textSecondary,
                    maxLines = if (compact) 3 else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            metadata?.let {
                Text(it, style = ReliveTheme.typography.eyebrow, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun RediscoverMediaLead(
    attachment: MediaAttachment,
    attachmentCount: Int,
    mediaStore: MediaStore,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val height = if (compact) dims.rediscover.compactMediaHeight else dims.rediscover.heroMediaHeight
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(dims.radii.md))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Open memory media" },
    ) {
        when (attachment.type) {
            MediaType.Image -> RelivedImageTile(attachment.storageRef, mediaStore, Modifier.matchParentSize())
            MediaType.Video -> RelivedVideoTile(attachment.storageRef, mediaStore, Modifier.matchParentSize())
            MediaType.Audio -> RediscoverAudioVisual(attachment, mediaStore)
        }
        if (attachmentCount > 1) {
            Text(
                text = "1 of $attachmentCount",
                style = ReliveTheme.typography.eyebrow,
                color = colors.textOnAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(dims.spacing.sm),
            )
        }
    }
}

@Composable
private fun RediscoverAudioVisual(attachment: MediaAttachment, mediaStore: MediaStore) {
    val dims = ReliveTheme.dimensions
    val waveform = rememberWaveformFor(attachment.storageRef, mediaStore)
    val duration = rememberMediaDurationMs(attachment.storageRef, mediaStore)
    Box(
        modifier = Modifier.fillMaxSize().background(ReliveTheme.colors.surfaceAudio),
        contentAlignment = Alignment.Center,
    ) {
        WaveformView(
            envelope = waveform,
            progress = 0f,
            modifier = Modifier.fillMaxWidth(0.55f).height(dims.rediscover.waveformHeight),
        )
        duration?.let {
            Text(
                text = "${it / 1000}s",
                style = ReliveTheme.typography.eyebrow,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(dims.spacing.sm),
            )
        }
    }
}
