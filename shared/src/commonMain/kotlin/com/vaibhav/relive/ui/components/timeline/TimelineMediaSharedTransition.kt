package com.vaibhav.relive.ui.components.timeline

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.spec

/** Coordinates the Phase 3 shared media containers for timeline media destinations. */
@OptIn(ExperimentalSharedTransitionApi::class)
class TimelineMediaSharedTransition(
    private val scope: SharedTransitionScope,
    private val activeAttachmentId: String?,
    private val viewerVisible: Boolean,
    private val activeGalleryAttachmentId: String?,
    private val galleryVisible: Boolean,
    private val reduceMotion: Boolean,
    private val boundsTransform: BoundsTransform,
) {
    @Composable
    fun sourceModifier(attachment: MomentAttachmentPresentation): Modifier {
        if (reduceMotion) return Modifier
        val isViewerHero = attachment.sharedTransitionKey() == activeAttachmentId
        val isGalleryHero = attachment.sharedTransitionKey() == activeGalleryAttachmentId
        val motion = ReliveTheme.motion
        val radius = androidx.compose.animation.core.animateDpAsState(
            targetValue = if (isViewerHero && viewerVisible) ReliveTheme.dimensions.radii.none else ReliveTheme.dimensions.radii.medium,
            animationSpec = motion.spec(
                reduceMotion = reduceMotion,
                full = tween(motion.durations.long2, easing = motion.easings.emphasized),
            ),
            label = "media hero thumbnail corner",
        ).value
        return with(scope) {
            Modifier
                .sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(attachment.sharedTransitionKey()),
                    visible = !((isViewerHero && viewerVisible) || (isGalleryHero && galleryVisible)),
                    boundsTransform = boundsTransform,
                )
                .clip(RoundedCornerShape(radius))
        }
    }

    @Composable
    fun viewerModifier(attachment: MomentAttachmentPresentation): Modifier {
        if (attachment.sharedTransitionKey() != activeAttachmentId || reduceMotion) return Modifier
        return with(scope) {
            Modifier
                .sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(attachment.sharedTransitionKey()),
                    visible = viewerVisible,
                    boundsTransform = boundsTransform,
                )
                .clip(RoundedCornerShape(ReliveTheme.dimensions.radii.none))
        }
    }

    @Composable
    fun galleryModifier(attachment: MomentAttachmentPresentation): Modifier {
        if (attachment.sharedTransitionKey() != activeGalleryAttachmentId || reduceMotion) return Modifier
        return with(scope) {
            Modifier.sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(attachment.sharedTransitionKey()),
                visible = galleryVisible,
                boundsTransform = boundsTransform,
            )
        }
    }
}

fun MomentAttachmentPresentation.sharedTransitionKey(): String =
    "timeline-media:${id.ifBlank { storageRef.value }}"
