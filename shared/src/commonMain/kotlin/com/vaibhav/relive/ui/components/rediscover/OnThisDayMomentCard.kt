package com.vaibhav.relive.ui.components.rediscover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.OnThisDayMomentPreview
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.presentation.cardcover.firstVisualPreviewAttachment
import com.vaibhav.relive.ui.components.reliveCardOuterBorder
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
fun OnThisDayMomentCard(
    moment: OnThisDayMomentPreview,
    anniversaryLabel: String,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val shape = RoundedCornerShape(dims.rediscover.cardOuterRadius)
    val lead = moment.attachments.firstVisualPreviewAttachment()
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceCard)
            .reliveCardOuterBorder(shape)
            .clickable(onClick = onOpen)
            .semantics { contentDescription = "Open On This Day memory" },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.rediscover.heroMediaHeight)
        ) {
            when (lead?.type) {
                MediaType.Image -> RelivedImageTile(lead.storageRef, mediaStore, Modifier.fillMaxSize())
                MediaType.Video -> RelivedVideoTile(lead.storageRef, mediaStore, Modifier.fillMaxSize())
                else -> com.vaibhav.relive.ui.theme.ReliveGeneratedCover(
                    stableKey = moment.id.value,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dims.rediscover.heroInfoAreaMinHeight)
                .padding(dims.spacing.lg),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = moment.title.ifBlank { moment.content.ifBlank { "A saved memory" } },
                    style = ReliveTheme.typography.title,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = anniversaryLabel,
                style = ReliveTheme.typography.eyebrow,
                color = colors.textMuted,
                modifier = Modifier.padding(top = dims.spacing.xs),
            )
        }
    }
}
