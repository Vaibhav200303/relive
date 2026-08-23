package com.vaibhav.relive.presentation.cardcover

import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import kotlin.test.Test
import kotlin.test.assertEquals

class CardCoverStateTest {

    @Test
    fun noMediaUsesGeneratedCover() {
        assertEquals(CardCoverState.GeneratedCover, cardCoverState(emptyList()))
    }

    @Test
    fun imageUsesMedia() {
        assertEquals(CardCoverState.Media, cardCoverState(listOf(attachment(MediaType.Image))))
    }

    @Test
    fun videoUsesMedia() {
        assertEquals(CardCoverState.Media, cardCoverState(listOf(attachment(MediaType.Video))))
    }

    @Test
    fun audioOnlyUsesGeneratedCover() {
        assertEquals(CardCoverState.GeneratedCover, cardCoverState(listOf(attachment(MediaType.Audio))))
    }

    @Test
    fun textOnlyUsesGeneratedCover() {
        assertEquals(CardCoverState.GeneratedCover, cardCoverState(emptyList()))
    }

    @Test
    fun addingVisualMediaReplacesGeneratedCover() {
        val audioOnly = listOf(attachment(MediaType.Audio))
        assertEquals(CardCoverState.GeneratedCover, cardCoverState(audioOnly))
        assertEquals(CardCoverState.Media, cardCoverState(audioOnly + attachment(MediaType.Image, 1)))
    }

    @Test
    fun firstVisualPreviewSkipsAudioAndUsesAttachmentOrder() {
        val firstImage = attachment(MediaType.Image, sortIndex = 2)
        val earlierVideo = attachment(MediaType.Video, sortIndex = 1)
        assertEquals(earlierVideo, listOf(attachment(MediaType.Audio), firstImage, earlierVideo).firstVisualPreviewAttachment())
    }

    @Test
    fun allUsesAStableLogicalKey() {
        assertEquals("timeline-all", Timeline.All.cardCoverStableKey())
        assertEquals("timeline-timeline-1", Timeline.Custom(TimelineId("timeline-1"), "Trips").cardCoverStableKey())
    }

    private fun attachment(type: MediaType, sortIndex: Int = 0) = MediaAttachment(
        id = MediaAttachmentId("attachment-$type-$sortIndex"),
        type = type,
        storageRef = MediaStorageRef("$type-$sortIndex"),
        sortIndex = sortIndex,
    )
}
