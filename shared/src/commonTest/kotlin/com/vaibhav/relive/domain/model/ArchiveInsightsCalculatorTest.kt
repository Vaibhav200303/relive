package com.vaibhav.relive.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveInsightsCalculatorTest {
    @Test fun aggregates_media_and_deduplicates_bytes_by_storage_reference() {
        val result = calculateArchiveInsights(
            momentCount = 2,
            attachments = listOf(
                ArchiveAttachmentReference("Image", "images/a.jpg"),
                ArchiveAttachmentReference("Image", "images/a.jpg"),
                ArchiveAttachmentReference("Video", "videos/b.mp4"),
                ArchiveAttachmentReference("Audio", "audio/c.m4a"),
                ArchiveAttachmentReference("Document", "other/d.bin"),
            ),
            inspect = {
                when (it) {
                    "images/a.jpg" -> ArchiveFileInspection.Available(100)
                    "videos/b.mp4" -> ArchiveFileInspection.Available(200)
                    "audio/c.m4a" -> ArchiveFileInspection.Available(50)
                    else -> ArchiveFileInspection.Available(25)
                }
            },
        )

        assertEquals(2, result.momentCount)
        assertEquals(5, result.attachmentCount)
        assertEquals(2, result.photo.attachmentCount)
        assertEquals(100, result.photo.bytes)
        assertEquals(200, result.video.bytes)
        assertEquals(50, result.audio.bytes)
        assertEquals(1, result.other.attachmentCount)
        assertEquals(25, result.other.bytes)
        assertEquals(375, result.totalBytes)
    }

    @Test fun missing_and_inaccessible_files_contribute_no_bytes_without_failing() {
        val result = calculateArchiveInsights(
            momentCount = 1,
            attachments = listOf(
                ArchiveAttachmentReference("Image", "missing"),
                ArchiveAttachmentReference("Video", "denied"),
            ),
            inspect = { if (it == "missing") ArchiveFileInspection.Missing else ArchiveFileInspection.Inaccessible },
        )

        assertEquals(2, result.attachmentCount)
        assertEquals(0, result.totalBytes)
        assertEquals(2, result.unavailableFileCount)
    }

    @Test fun empty_and_text_only_archives_have_no_media() {
        val result = calculateArchiveInsights(14, emptyList()) { error("No files should be inspected") }

        assertEquals(14, result.momentCount)
        assertEquals(0, result.attachmentCount)
        assertEquals(0, result.totalBytes)
    }

    @Test fun total_bytes_saturates_at_long_max_value() {
        val result = calculateArchiveInsights(
            momentCount = 1,
            attachments = listOf(
                ArchiveAttachmentReference("Image", "a"),
                ArchiveAttachmentReference("Video", "b"),
            ),
            inspect = { ArchiveFileInspection.Available(Long.MAX_VALUE) },
        )

        assertEquals(Long.MAX_VALUE, result.totalBytes)
    }
}
