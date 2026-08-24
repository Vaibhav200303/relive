package com.vaibhav.relive.data.local

import com.vaibhav.relive.data.local.repository.SqlDelightArchiveInsightsRepository
import com.vaibhav.relive.domain.model.ArchiveFileInspection
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveInsightsPersistenceTest {
    private lateinit var fixture: TestFixture

    @BeforeTest fun setup() { fixture = TestFixture() }
    @AfterTest fun tearDown() { fixture.close() }

    @Test fun reads_bounded_attachment_references_and_measures_each_file_once() = runTest {
        fixture.moments.insert(
            sampleMoment(
                id = "archive",
                attachments = listOf(
                    sampleAttachment("photo", MediaType.Image, "images/photo.jpg", 0),
                    sampleAttachment("video", MediaType.Video, "videos/video.mp4", 1),
                ),
            ),
        )
        val repository = SqlDelightArchiveInsightsRepository(
            fixture.database,
            SizeStore(mapOf("images/photo.jpg" to 10L, "videos/video.mp4" to 20L)),
            Dispatchers.Unconfined,
        )

        val insights = repository.load()

        assertEquals(1, insights.momentCount)
        assertEquals(2, insights.attachmentCount)
        assertEquals(10, insights.photo.bytes)
        assertEquals(20, insights.video.bytes)
    }

    private class SizeStore(private val sizes: Map<String, Long>) : MediaStore {
        override fun extensionFor(type: MediaType): String = "bin"
        override fun allocateKey(type: MediaType): MediaStorageRef = MediaStorageRef("unused")
        override fun resolveAbsolutePath(ref: MediaStorageRef): String = ref.value
        override fun exists(ref: MediaStorageRef): Boolean = ref.value in sizes
        override fun delete(ref: MediaStorageRef) = Unit
        override fun sizeBytes(ref: MediaStorageRef): Long = sizes[ref.value] ?: 0L
        override fun inspectManagedFile(ref: MediaStorageRef): ArchiveFileInspection =
            sizes[ref.value]?.let(ArchiveFileInspection::Available) ?: ArchiveFileInspection.Missing
    }
}
