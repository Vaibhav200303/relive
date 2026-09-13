package com.vaibhav.relive.data.exporting

import com.vaibhav.relive.domain.exporting.PortableArchiveEntry
import com.vaibhav.relive.domain.exporting.PortableArchiveManifest
import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot
import com.vaibhav.relive.domain.exporting.PortableTimelineIdentity
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortableArchiveValidationTest {
    private val attachment = MediaAttachment(MediaAttachmentId("a1"), MediaType.Image, MediaStorageRef("images/photo.jpg"), 0)
    private val moment = Moment(MomentId("m1"), Instant(100), attachments = listOf(attachment))
    private val timeline = Timeline.Custom(TimelineId("t1"), "Trips")
    private val snapshot = PortableArchiveSnapshot(200, listOf(moment), listOf(timeline), mapOf(moment.id to setOf(timeline.id)), TimelineAppearance())
    private val entry = PortableArchiveEntry(attachment.storageRef.value, "media/${"a".repeat(64)}.jpg", 12, "a".repeat(64), MediaType.Image.name)
    private val manifest = PortableArchiveManifest(appVersion = "app", exportedAtEpochMilliseconds = 200, momentCount = 1, timelineCount = 1, earliestMomentEpochMilliseconds = 100, latestMomentEpochMilliseconds = 100, archiveSha256 = "b".repeat(64), media = listOf(entry))

    @Test fun valid_relationships_and_inventory_are_accepted() {
        validatePortableArchiveMetadata(snapshot, manifest, 100)
    }

    @Test fun unsafe_paths_are_rejected() {
        assertFalse(isSafePortableArchivePath("../archive.json"))
        assertFalse(isSafePortableArchivePath("media\\photo.jpg"))
        assertTrue(isSafePortableArchivePath("media/photo.jpg"))
        assertFails { validatePortableArchiveMetadata(snapshot, manifest.copy(media = listOf(entry.copy(archivePath = "../photo.jpg"))), 100) }
    }

    @Test fun missing_relationships_and_wrong_media_types_are_rejected() {
        val unknown = snapshot.copy(memberships = mapOf(MomentId("missing") to setOf(timeline.id)))
        assertFails { validatePortableArchiveMetadata(unknown, manifest, 100) }
        assertFails { validatePortableArchiveMetadata(snapshot, manifest.copy(media = listOf(entry.copy(mediaType = MediaType.Video.name))), 100) }
    }

    @Test fun duplicate_ids_are_rejected() {
        assertFails { validatePortableArchiveMetadata(snapshot.copy(moments = listOf(moment, moment)), manifest.copy(momentCount = 2), 100) }
    }

    @Test fun exported_custom_identity_must_match_the_selected_timeline() {
        val scoped = snapshot.copy(
            exportedTimeline = PortableTimelineIdentity(timeline.name, timeline.id, timeline.appearance, timeline.coverPhotoRef),
        )
        validatePortableArchiveMetadata(scoped, manifest, 100)
        assertFails {
            validatePortableArchiveMetadata(
                scoped.copy(exportedTimeline = scoped.exportedTimeline.copy(name = "Renamed")),
                manifest,
                100,
            )
        }
    }
}
