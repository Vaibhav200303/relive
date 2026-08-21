package com.vaibhav.relive.ui.components.timeline

import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation

/**
 * Pure layout selection for the timeline adaptive media collage (ADR-0019 §1).
 * Kept free of Compose so it can be exercised by fast host tests.
 */
enum class CollageShape { Single, Two, Three, Four, FourPlus }

data class CollageSelection(
    val shape: CollageShape,
    val visible: List<MomentAttachmentPresentation>,
    val overflow: Int,
)

fun selectCollage(attachments: List<MomentAttachmentPresentation>): CollageSelection {
    require(attachments.isNotEmpty()) { "collage requires at least one attachment" }
    return when (attachments.size) {
        1 -> CollageSelection(CollageShape.Single, attachments, 0)
        2 -> CollageSelection(CollageShape.Two, attachments, 0)
        3 -> CollageSelection(CollageShape.Three, attachments, 0)
        4 -> CollageSelection(CollageShape.Four, attachments, 0)
        else -> CollageSelection(
            shape = CollageShape.FourPlus,
            visible = attachments.take(4),
            overflow = attachments.size - 4,
        )
    }
}
