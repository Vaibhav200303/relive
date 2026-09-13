package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.exporting.MagazineOptions
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.exporting.MagazineDocument
import com.vaibhav.relive.platform.exporting.MagazineMediaAsset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MagazineDocumentBuilderTest {
    @Test
    fun escapes_user_content_and_keeps_complete_text() {
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                listOf(Moment(MomentId("m"), Instant(0), title = "<script>", content = "one & two\nthree")),
                MagazineOptions("My <Relive>", "A & B"),
                "All moments",
            ),
        )
        assertTrue("&lt;script&gt;" in html)
        assertTrue("one &amp; two<br>three" in html)
        assertFalse("<script>" in html)
        assertTrue("class=\"diary-day\"" in html)
        assertTrue("class=\"day-heading\"" in html)
        assertTrue("day-body single" in html)
        assertTrue("cover-doodle cover-journal" in html)
        assertTrue("class=\"page-doodles\"" in html)
        assertTrue("font-family:Kalam" in html)
        assertTrue("color:#2D2722" in html)
        assertTrue("color:rgba(139,94,60,.17)" in html)
        assertTrue("background:transparent" in html)
        assertTrue("size:A4 portrait" in html)
        assertTrue(".content{font-family:Kalam,Inter,sans-serif;font-size:15pt" in html)
        assertTrue(".metadata{display:flex;flex-wrap:wrap;gap:3mm;color:#554A42;font-family:Kalam,Inter,sans-serif;font-size:10.5pt" in html)
        assertTrue(".tags span,.feeling{" in html && "font-size:8.5pt" in html)
    }

    @Test
    fun percent_encodes_local_asset_paths() {
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = emptyList(),
                options = MagazineOptions("Title", "Subtitle", "/tmp/a photo's #1.jpg"),
                scopeTitle = "All moments",
            ),
        )

        assertTrue("file:///tmp/a%20photo%27s%20%231.jpg" in html)
        assertFalse("a photo's #1.jpg" in html)
    }

    @Test
    fun groups_each_date_and_turns_missing_dates_into_one_quiet_page() {
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(
                    Moment(MomentId("first"), Instant(0), content = "First"),
                    Moment(MomentId("second"), Instant(3 * 86_400_000L), content = "Second"),
                ),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
            ),
        )

        assertTrue(html.windowed("class=\"diary-day\"".length).count { it == "class=\"diary-day\"" } == 2)
        assertTrue("class=\"quiet-gap\"" in html)
        assertTrue("Life was still being lived." in html)
    }

    @Test
    fun prints_images_as_pasted_photos_and_omits_audio_and_video() {
        val image = MediaAttachment(MediaAttachmentId("image"), MediaType.Image, MediaStorageRef("images/a.jpg"), 0)
        val video = MediaAttachment(MediaAttachmentId("video"), MediaType.Video, MediaStorageRef("videos/a.mp4"), 1)
        val audio = MediaAttachment(MediaAttachmentId("audio"), MediaType.Audio, MediaStorageRef("audio/a.m4a"), 2)
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(Moment(MomentId("m"), Instant(0), attachments = listOf(image, video, audio))),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
                mediaAssets = mapOf(
                    image.storageRef.value to MagazineMediaAsset("/tmp/photo.jpg"),
                    video.storageRef.value to MagazineMediaAsset("/tmp/poster.jpg"),
                    audio.storageRef.value to MagazineMediaAsset(),
                ),
            ),
        )

        assertTrue("class=\"photo\"" in html)
        assertTrue("file:///tmp/photo.jpg" in html)
        assertFalse("poster.jpg" in html)
        assertFalse("AUDIO" in html)
        assertFalse("VIDEO" in html)
    }

    @Test
    fun gives_three_images_a_scrapbook_layout_without_wrapping_text_in_a_card() {
        val images = (0..2).map { index ->
            MediaAttachment(
                id = MediaAttachmentId("image-$index"),
                type = MediaType.Image,
                storageRef = MediaStorageRef("images/$index.jpg"),
                sortIndex = index,
            )
        }
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(Moment(MomentId("m"), Instant(0), content = "Written on the page", attachments = images)),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
                mediaAssets = images.associate { it.storageRef.value to MagazineMediaAsset("/tmp/${it.sortIndex}.jpg") },
            ),
        )

        assertTrue("class=\"moment moment-0 photo-count-3\"" in html)
        assertTrue("class=\"photos photos-3\"" in html)
        assertTrue(html.windowed("class=\"photo\"".length).count { it == "class=\"photo\"" } == 3)
        assertTrue("<div class=\"moment-copy\">" in html)
        assertFalse("class=\"moment-card\"" in html)
    }

    @Test
    fun puts_large_photo_sets_on_complete_unlabelled_pages() {
        val images = (0..8).map { index ->
            MediaAttachment(
                id = MediaAttachmentId("image-$index"),
                type = MediaType.Image,
                storageRef = MediaStorageRef("images/$index.jpg"),
                sortIndex = index,
            )
        }
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(Moment(MomentId("m"), Instant(0), attachments = images)),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
                mediaAssets = images.associate { it.storageRef.value to MagazineMediaAsset("/tmp/${it.sortIndex}.jpg") },
            ),
        )

        assertTrue("class=\"photos photos-4\"" in html)
        assertTrue("class=\"photos photos-1\"" in html)
        assertTrue(html.windowed("class=\"diary-photo-page\"".length).count { it == "class=\"diary-photo-page\"" } == 2)
        assertTrue(html.windowed("1st January 1970".length).count { it == "1st January 1970" } == 1)
        assertTrue("object-fit:contain" in html)
        assertFalse("continued" in html)
        assertFalse("photos-many" in html)
    }

    @Test
    fun moves_photos_after_long_copy_to_an_isolated_page() {
        val image = MediaAttachment(MediaAttachmentId("image"), MediaType.Image, MediaStorageRef("images/a.jpg"), 0)
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(Moment(MomentId("m"), Instant(0), content = "A".repeat(181), attachments = listOf(image))),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
                mediaAssets = mapOf(image.storageRef.value to MagazineMediaAsset("/tmp/photo.jpg")),
            ),
        )

        assertTrue("class=\"diary-photo-page\"" in html)
        assertTrue("class=\"photos photos-1\"" in html)
        assertFalse("continued" in html)
    }
}
