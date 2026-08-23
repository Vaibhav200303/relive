package com.vaibhav.relive.ui.components.rediscover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.vaibhav.relive.ui.components.MediaToCardSurfaceFade
import com.vaibhav.relive.ui.components.reliveCardOuterBorder
import com.vaibhav.relive.ui.theme.ReliveTheme
import androidx.compose.ui.unit.dp

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
    val lead = moment.attachments.minByOrNull { it.sortIndex }
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
                .padding(horizontal = dims.spacing.md)
                .height(dims.rediscover.heroMediaHeight)
                .clip(RoundedCornerShape(0.dp)),
        ) {
            when (lead?.type) {
                MediaType.Image -> RelivedImageTile(lead.storageRef, mediaStore, Modifier.fillMaxSize())
                MediaType.Video -> RelivedVideoTile(lead.storageRef, mediaStore, Modifier.fillMaxSize())
                else -> Box(Modifier.fillMaxSize().background(colors.surfaceCardTranslucent))
            }
            MediaToCardSurfaceFade(modifier = Modifier.align(Alignment.BottomCenter))
        }
        Column(
            modifier = Modifier.padding(dims.spacing.lg),
        ) {
            Text(
                text = moment.title.ifBlank { moment.content.ifBlank { "A saved memory" } },
                style = ReliveTheme.typography.title,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = anniversaryLabel,
                style = ReliveTheme.typography.eyebrow,
                color = colors.textMuted,
                modifier = Modifier.padding(top = dims.spacing.xs),
            )
        }
    }
}
