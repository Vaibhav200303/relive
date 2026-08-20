package com.vaibhav.relive.domain.model

import com.vaibhav.relive.domain.time.Duration
import com.vaibhav.relive.domain.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun instant(ms: Long) = Instant(ms)

private fun baseMoment(
    id: String = "m-1",
    createdAt: Instant = instant(1_000_000),
    title: String = "Sunrise",
    content: String = "Watched the sky",
): Moment = Moment(
    id = MomentId(id),
    createdAt = createdAt,
    title = title,
    content = content,
)

class MomentConstructionTest {

    @Test
    fun text_only_moment_is_valid() {
        val m = baseMoment()
        assertEquals("Sunrise", m.title)
        assertEquals("Watched the sky", m.content)
        assertTrue(m.attachments.isEmpty())
        assertNull(m.location)
        assertNull(m.updatedAt)
        assertFalse(m.isFavorite)
        assertEquals(MomentValidation.Result.Ok, MomentValidation.validate(m))
    }

    @Test
    fun media_only_moment_is_valid() {
        val att = MediaAttachment(
            id = MediaAttachmentId("a-1"),
            type = MediaType.Image,
            storageRef = MediaStorageRef("file://img.jpg"),
            sortIndex = 0,
        )
        val m = baseMoment(title = "", content = "").copy(attachments = listOf(att))
        assertEquals(MomentValidation.Result.Ok, MomentValidation.validate(m))
    }

    @Test
    fun completely_empty_moment_is_invalid() {
        val m = baseMoment(title = "", content = "")
        val r = MomentValidation.validate(m)
        assertTrue(r is MomentValidation.Result.Invalid)
        assertEquals(listOf(MomentValidation.Reason.Empty), r.reasons)
    }

    @Test
    fun content_preserved_verbatim_including_whitespace() {
        val raw = "  line one\n\n  line two  "
        val m = baseMoment(content = raw)
        assertEquals(raw, m.content)
    }

    @Test
    fun createdAt_is_stable_across_copy_of_updatedAt() {
        val m1 = baseMoment()
        val m2 = m1.copy(updatedAt = m1.createdAt + Duration.ofHours(1))
        assertEquals(m1.createdAt, m2.createdAt)
        assertNotEquals(m1.updatedAt, m2.updatedAt)
    }

    @Test
    fun updatedAt_before_createdAt_rejected() {
        assertFails {
            baseMoment().copy(updatedAt = instant(0))
        }
    }

    @Test
    fun attachment_sortIndex_collision_rejected() {
        val a = MediaAttachment(MediaAttachmentId("a"), MediaType.Image, MediaStorageRef("r1"), 0)
        val b = MediaAttachment(MediaAttachmentId("b"), MediaType.Image, MediaStorageRef("r2"), 0)
        assertFails { baseMoment().copy(attachments = listOf(a, b)) }
    }

    @Test
    fun attachment_id_collision_rejected() {
        val a = MediaAttachment(MediaAttachmentId("dup"), MediaType.Image, MediaStorageRef("r1"), 0)
        val b = MediaAttachment(MediaAttachmentId("dup"), MediaType.Video, MediaStorageRef("r2"), 1)
        assertFails { baseMoment().copy(attachments = listOf(a, b)) }
    }

    @Test
    fun attachment_ordering_by_sortIndex_is_preserved_by_caller() {
        val a = MediaAttachment(MediaAttachmentId("a"), MediaType.Image, MediaStorageRef("r1"), 2)
        val b = MediaAttachment(MediaAttachmentId("b"), MediaType.Video, MediaStorageRef("r2"), 0)
        val c = MediaAttachment(MediaAttachmentId("c"), MediaType.Audio, MediaStorageRef("r3"), 1)
        val m = baseMoment().copy(attachments = listOf(a, b, c))
        val ordered = m.attachments.sortedBy { it.sortIndex }.map { it.id.value }
        assertEquals(listOf("b", "c", "a"), ordered)
    }

    @Test
    fun negative_sortIndex_rejected() {
        assertFails {
            MediaAttachment(MediaAttachmentId("a"), MediaType.Image, MediaStorageRef("r"), -1)
        }
    }

    @Test
    fun duplicate_tags_rejected() {
        val t1 = Tag.of("Travel")
        val t2 = Tag.of("travel")
        assertFails { baseMoment().copy(tags = listOf(t1, t2)) }
    }

    @Test
    fun favorite_default_is_false_and_toggles_via_copy() {
        val m = baseMoment()
        assertFalse(m.isFavorite)
        assertTrue(m.copy(isFavorite = true).isFavorite)
    }

}
