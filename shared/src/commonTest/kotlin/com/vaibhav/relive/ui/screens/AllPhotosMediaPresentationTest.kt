package com.vaibhav.relive.ui.screens

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import kotlin.test.Test
import kotlin.test.assertEquals

class AllPhotosMediaPresentationTest {
    @Test
    fun flattensEveryMediaTypeInMomentAndAttachmentOrder() {
        val moments = listOf(
            moment(
                "newer",
                attachment("photo-1", MediaType.Image),
                attachment("voice", MediaType.Audio),
                attachment("video-1", MediaType.Video),
            ),
            moment("older", attachment("photo-2", MediaType.Image)),
        )

        val media = allPhotosVisualAttachments(moments)

        assertEquals(listOf("photo-1", "voice", "video-1", "photo-2"), media.map { it.storageRef.value })
    }

    private fun moment(id: String, vararg attachments: MomentAttachmentPresentation) = MomentPresentation(
        id = MomentId(id),
        createdAt = Instant(0),
        updatedAt = null,
        formattedDate = "",
        formattedTime = "",
        title = "",
        content = "",
        locationLabel = null,
        location = null,
        isFavorite = false,
        tags = emptyList(),
        attachments = attachments.toList(),
    )

    private fun attachment(id: String, type: MediaType) =
        MomentAttachmentPresentation(storageRef = MediaStorageRef(id), type = type, id = id)
}
