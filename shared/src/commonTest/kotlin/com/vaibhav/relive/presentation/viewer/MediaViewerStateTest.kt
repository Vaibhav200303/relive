package com.vaibhav.relive.presentation.viewer

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MediaViewerStateTest {

    private fun att(id: String, type: MediaType = MediaType.Image) =
        MomentAttachmentPresentation(MediaStorageRef(id), type)

    @Test
    fun openAtTappedIndexPreservesIndex() {
        val list = listOf(att("a"), att("b"), att("c"))
        val s = openAt(list, 2)
        assertEquals(2, s.initialIndex)
        assertEquals(2, s.currentIndex)
        assertSame(list[2], s.current)
    }

    @Test
    fun openAtFirstAttachment() {
        val list = listOf(att("a"), att("b"))
        val s = openAt(list, 0)
        assertEquals(0, s.currentIndex)
        assertEquals("a", s.current.storageRef.value)
    }

    @Test
    fun openAtLastAttachment() {
        val list = List(5) { att("a$it") }
        val s = openAt(list, 4)
        assertEquals(4, s.currentIndex)
        assertEquals("a4", s.current.storageRef.value)
    }

    @Test
    fun currentIndexChangesViaWithCurrent() {
        val list = List(4) { att("a$it") }
        val s = openAt(list, 0).withCurrent(3)
        assertEquals(0, s.initialIndex)
        assertEquals(3, s.currentIndex)
    }

    @Test
    fun withCurrentClampsToBounds() {
        val list = listOf(att("a"), att("b"), att("c"))
        assertEquals(0, openAt(list, 1).withCurrent(-5).currentIndex)
        assertEquals(2, openAt(list, 1).withCurrent(99).currentIndex)
    }

    @Test
    fun mixedMediaOrderingPreserved() {
        val a = att("a", MediaType.Image)
        val b = att("b", MediaType.Video)
        val c = att("c", MediaType.Audio)
        val d = att("d", MediaType.Image)
        val list = listOf(a, b, c, d)
        val s = openAt(list, 2)
        assertSame(a, s.attachments[0])
        assertSame(b, s.attachments[1])
        assertSame(c, s.attachments[2])
        assertSame(d, s.attachments[3])
        assertEquals(MediaType.Audio, s.current.type)
    }

    @Test
    fun plusNTileOpensAtIndexThreeWithFullList() {
        // Simulates timeline tap on the +N tile of a 7-attachment moment.
        val full = List(7) { att("a$it") }
        val s = openAt(full, 3)
        assertEquals(3, s.currentIndex)
        assertEquals(7, s.size)
        // Hidden overflow (indices 4..6) reachable by paging forward.
        val advanced = s.withCurrent(6)
        assertEquals("a6", advanced.current.storageRef.value)
    }

    @Test
    fun tappedIndexOutOfBoundsIsClamped() {
        val list = listOf(att("a"), att("b"))
        assertEquals(0, openAt(list, -3).currentIndex)
        assertEquals(1, openAt(list, 42).currentIndex)
    }

    @Test
    fun emptyAttachmentsRejected() {
        assertFailsWith<IllegalArgumentException> { openAt(emptyList(), 0) }
    }

    @Test
    fun stateIsValueEqualityForRecomposition() {
        val list = List(3) { att("a$it") }
        val a = openAt(list, 1)
        val b = openAt(list, 1)
        // Data-class equality means Compose can skip recomposition when
        // state has not changed even if the enclosing lambda re-runs.
        assertEquals(a, b)
    }
}
