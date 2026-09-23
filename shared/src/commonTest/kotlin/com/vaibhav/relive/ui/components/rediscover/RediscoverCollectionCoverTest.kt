package com.vaibhav.relive.ui.components.rediscover

import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.presentation.timeline.SystemCollectionCover
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class RediscoverCollectionCoverTest {
    @Test
    fun coverIsStableWithinAnHourAndChangesAtTheNextHour() {
        val photos = listOf(photo("one"), photo("two"), photo("three"))

        val first = resolvedRediscoverCollectionCover("favourites", photos, hourBucket = 42)
        val sameHour = resolvedRediscoverCollectionCover("favourites", photos, hourBucket = 42)
        val nextHour = resolvedRediscoverCollectionCover("favourites", photos, hourBucket = 43)

        assertEquals(first, sameHour)
        assertNotEquals(first, nextHour)
    }

    @Test
    fun onlyPhotosAreEligibleAndEmptyPhotoSetsUseGeneratedFallback() {
        val video = attachment("video", MediaType.Video)
        val audio = attachment("audio", MediaType.Audio)

        val cover = resolvedRediscoverCollectionCover("all-photos", listOf(video, audio), hourBucket = 7)

        assertIs<SystemCollectionCover.Generated>(cover)
    }

    @Test
    fun duplicatePhotoAttachmentsAreOnlyOneCandidate() {
        val photo = photo("same")

        val cover = resolvedRediscoverCollectionCover("on-this-day", listOf(photo, photo), hourBucket = 9)

        assertEquals(SystemCollectionCover.Media(photo.storageRef, MediaType.Image), cover)
    }

    private fun photo(id: String) = attachment(id, MediaType.Image)

    private fun attachment(id: String, type: MediaType) = MediaAttachment(
        id = MediaAttachmentId(id),
        type = type,
        storageRef = MediaStorageRef("media/$id"),
        sortIndex = 0,
    )
}
