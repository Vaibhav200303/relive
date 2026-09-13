package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.exporting.DiaryPaper
import com.vaibhav.relive.domain.exporting.MagazineOptions
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.exporting.MagazineDocument
import com.vaibhav.relive.platform.exporting.MagazineMediaAsset
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertTrue("--paper:#F3EBDD" in html)
        assertTrue("--ink:#2D2722" in html)
        assertTrue("--doodle:rgba(139,94,60,.22)" in html)
        assertTrue("background:transparent" in html)
        assertTrue("size:A4 portrait" in html)
        assertTrue(".content{font-family:Kalam,Inter,sans-serif;font-size:18pt" in html)
        assertTrue(".metadata{display:flex;flex-wrap:wrap;gap:3mm;color:var(--ink-soft);font-family:Kalam,Inter,sans-serif;font-size:12pt" in html)
        assertTrue(".feeling{display:block" in html && "font-size:30pt" in html)
    }

    @Test
    fun renders_feeling_as_a_visible_emoji_without_printing_the_enum_as_copy() {
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(Moment(MomentId("m"), Instant(0), feeling = MomentFeeling.Great)),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
            ),
        )

        assertTrue(">😊</span>" in html)
        assertTrue("aria-label=\"Feeling: Great\"" in html)
        assertFalse(">Great</span>" in html)
    }

    @Test
    fun selected_paper_changes_page_ink_rules_and_doodles_together() {
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(Moment(MomentId("m"), Instant(0), content = "A blue-page memory")),
                options = MagazineOptions("Title", "Subtitle", paper = DiaryPaper.PowderBlue),
                scopeTitle = "All moments",
            ),
        )

        assertTrue("--paper:#E6F0F7" in html)
        assertTrue("--ink:#22323E" in html)
        assertTrue("--accent:#41657C" in html)
        assertTrue("--doodle:rgba(111,147,170,.23)" in html)
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

    @Test
    fun keeps_a_short_image_moment_visible_beside_its_feeling_on_a_multi_moment_day() {
        val image = MediaAttachment(MediaAttachmentId("image"), MediaType.Image, MediaStorageRef("images/a.jpg"), 0)
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(
                    Moment(MomentId("image-moment"), Instant(0), feeling = MomentFeeling.Great, attachments = listOf(image)),
                    Moment(MomentId("written-moment"), Instant(1), title = "A long entry", content = "A".repeat(500)),
                ),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
                mediaAssets = mapOf(image.storageRef.value to MagazineMediaAsset("/tmp/photo.jpg")),
            ),
        )

        val firstMomentStart = html.indexOf("moment-0")
        val secondMomentStart = html.indexOf("moment-1")
        val photoStart = html.indexOf("class=\"photos photos-1\"")
        assertTrue(firstMomentStart in 0..<photoStart)
        assertTrue(photoStart in 0..<secondMomentStart)
        assertFalse("class=\"diary-photo-page\"" in html)
    }

    @Test
    fun keeps_every_photo_sheet_with_its_moment_before_rendering_the_next_moment() {
        val firstMomentImages = (0..12).map { index ->
            MediaAttachment(
                id = MediaAttachmentId("first-$index"),
                type = MediaType.Image,
                storageRef = MediaStorageRef("images/first-$index.jpg"),
                sortIndex = index,
            )
        }
        val secondMomentImages = (0..1).map { index ->
            MediaAttachment(
                id = MediaAttachmentId("second-$index"),
                type = MediaType.Image,
                storageRef = MediaStorageRef("images/second-$index.jpg"),
                sortIndex = index,
            )
        }
        val allImages = firstMomentImages + secondMomentImages
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(
                    Moment(MomentId("first"), Instant(0), title = "First", attachments = firstMomentImages),
                    Moment(MomentId("second"), Instant(1), title = "Second", attachments = secondMomentImages),
                ),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
                mediaAssets = allImages.associate { attachment ->
                    attachment.storageRef.value to MagazineMediaAsset("/tmp/${attachment.id.value}.jpg")
                },
            ),
        )

        val secondMomentStart = html.indexOf("moment-1")
        assertTrue(html.indexOf("first-12.jpg") in 0..<secondMomentStart)
        assertTrue(html.indexOf("second-0.jpg") > secondMomentStart)
        assertTrue(html.windowed("class=\"diary-photo-page\"".length).count { it == "class=\"diary-photo-page\"" } == 3)
    }

    @Test
    fun long_writing_is_split_into_complete_page_sections_with_one_heading_and_one_feeling() {
        val writing = List(250) { "memory" }.joinToString(" ")
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(
                    Moment(
                        MomentId("long-moment"),
                        Instant(0),
                        title = "A long day",
                        content = writing,
                        feeling = MomentFeeling.Low,
                    ),
                ),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
            ),
        )

        assertTrue("class=\"diary-continuation\"" in html)
        assertTrue(html.windowed("A long day".length).count { it == "A long day" } == 1)
        assertTrue(html.windowed("😔".length).count { it == "😔" } == 1)
        assertTrue(html.windowed("memory".length).count { it == "memory" } == 250)
    }

    @Test
    fun long_writing_balances_continuation_pages_instead_of_leaving_a_tiny_final_fragment() {
        val writing = List(850) { "memory" }.joinToString(" ")
        val html = MagazineDocumentBuilder.html(
            MagazineDocument(
                moments = listOf(Moment(MomentId("long-moment"), Instant(0), content = writing)),
                options = MagazineOptions("Title", "Subtitle"),
                scopeTitle = "All moments",
            ),
        )

        val chunkLengths = Regex("<p class=\"content\">(.*?)</p>")
            .findAll(html)
            .map { it.groupValues[1].length }
            .toList()
        assertEquals(4, chunkLengths.size)
        assertTrue(chunkLengths.min() >= chunkLengths.max() * 7 / 10)
        assertTrue(chunkLengths.first() <= 1_200)
        assertTrue(chunkLengths.drop(1).all { it <= 1_650 })
    }
}
