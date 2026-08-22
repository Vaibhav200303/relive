package com.vaibhav.relive.ui.screens

import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.media.MediaStore
import kotlin.test.Test
import kotlin.test.assertEquals

class ForgottenAttachmentCleanupTest {
    @Test
    fun cleanupIsBestEffortWhenOneFileDeleteFails() {
        val store = FailingStore()
        val moment = Moment(
            MomentId("m"), Instant(0L), attachments = listOf(
                MediaAttachment(MediaAttachmentId("a"), MediaType.Image, MediaStorageRef("bad.jpg"), 0),
                MediaAttachment(MediaAttachmentId("b"), MediaType.Image, MediaStorageRef("good.jpg"), 1),
            ),
        )

        cleanupForgottenAttachments(moment, store)

        assertEquals(listOf(MediaStorageRef("bad.jpg"), MediaStorageRef("good.jpg")), store.attempted)
    }

    private class FailingStore : MediaStore {
        val attempted = mutableListOf<MediaStorageRef>()
        override fun extensionFor(type: MediaType) = "jpg"
        override fun allocateKey(type: MediaType) = MediaStorageRef("new.jpg")
        override fun resolveAbsolutePath(ref: MediaStorageRef) = ref.value
        override fun exists(ref: MediaStorageRef) = true
        override fun delete(ref: MediaStorageRef) { attempted += ref; if (ref.value == "bad.jpg") error("disk") }
        override fun sizeBytes(ref: MediaStorageRef) = 0L
    }
}
