package com.vaibhav.relive.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.RelivedVideoTile
import com.vaibhav.relive.presentation.cardcover.AllTimelineCollageLayout
import com.vaibhav.relive.ui.theme.ReliveTheme

/** Presentation-only collage: cached media tiles are measured directly into the curated grid. */
@Composable
fun AllTimelineCollage(
    attachments: List<MediaAttachment>,
    layout: AllTimelineCollageLayout,
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
) {
    require(attachments.size == layout.cells.size)
    val gap = ReliveTheme.dimensions.media.collageGap
    Layout(
        modifier = modifier.background(ReliveTheme.colors.accent),
        content = {
            attachments.forEach { attachment ->
                key(attachment.id.value) {
                    when (attachment.type) {
                        MediaType.Image -> RelivedImageTile(attachment.storageRef, mediaStore, Modifier)
                        MediaType.Video -> RelivedVideoTile(attachment.storageRef, mediaStore, Modifier)
                        MediaType.Audio -> Unit
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth.coerceAtLeast(constraints.minWidth)
        val height = constraints.maxHeight.coerceAtLeast(constraints.minHeight)
        val gapPx = gap.roundToPx()

        fun start(index: Int, count: Int, total: Int): Int {
            val content = total - gapPx * (count - 1)
            return content * index / count + gapPx * index
        }

        val placeables = measurables.mapIndexed { index, measurable ->
            val cell = layout.cells[index]
            val left = start(cell.column, layout.columns, width)
            val right = if (cell.column + cell.columnSpan == layout.columns) {
                width
            } else {
                start(cell.column + cell.columnSpan, layout.columns, width) - gapPx
            }
            val top = start(cell.row, layout.rows, height)
            val bottom = if (cell.row + cell.rowSpan == layout.rows) {
                height
            } else {
                start(cell.row + cell.rowSpan, layout.rows, height) - gapPx
            }
            measurable.measure(Constraints.fixed(right - left, bottom - top))
        }

        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val cell = layout.cells[index]
                placeable.placeRelative(
                    x = start(cell.column, layout.columns, width),
                    y = start(cell.row, layout.rows, height),
                )
            }
        }
    }
}
