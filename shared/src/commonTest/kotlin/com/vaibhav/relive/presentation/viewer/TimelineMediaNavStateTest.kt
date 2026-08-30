package com.vaibhav.relive.presentation.viewer

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class TimelineMediaNavStateTest {

    private fun att(id: String, type: MediaType = MediaType.Image) =
        MomentAttachmentPresentation(MediaStorageRef(id), type)

    // ── 1 attachment tap → full-screen viewer directly ─────────────

    @Test
    fun singleAttachmentTapOpensViewerDirectly() {
        val list = listOf(att("only"))
        val s = TimelineMediaNavState.Idle.openFromCollage(list, 0)
        assertNull(s.gallery, "single-attachment moment must skip gallery")
        val v = assertNotNull(s.viewer)
        assertEquals(0, v.currentIndex)
        assertEquals("only", v.current.storageRef.value)
    }

    @Test
    fun singleImageOpensImageViewerDirectly() {
        val s = TimelineMediaNavState.Idle.openFromCollage(listOf(att("i", MediaType.Image)), 0)
        assertEquals(MediaType.Image, s.viewer?.current?.type)
        assertNull(s.gallery)
    }

    @Test
    fun singleVideoOpensVideoViewerDirectly() {
        val s = TimelineMediaNavState.Idle.openFromCollage(listOf(att("v", MediaType.Video)), 0)
        assertEquals(MediaType.Video, s.viewer?.current?.type)
        assertNull(s.gallery)
    }

    @Test
    fun singleAudioOpensAudioViewerDirectly() {
        val s = TimelineMediaNavState.Idle.openFromCollage(listOf(att("a", MediaType.Audio)), 0)
        assertEquals(MediaType.Audio, s.viewer?.current?.type)
        assertNull(s.gallery)
    }

    // ── 2+ attachment tap → gallery, not viewer ────────────────────

    @Test
    fun twoAttachmentTapOpensGalleryNotViewer() {
        val list = listOf(att("a"), att("b"))
        val s = TimelineMediaNavState.Idle.openFromCollage(list, 0)
        assertNull(s.viewer, "multi-attachment moment must not jump to viewer")
        val g = assertNotNull(s.gallery)
        assertEquals(2, g.size)
    }

    @Test
    fun anyTappedIndexInMultiOpensGallery() {
        val list = listOf(att("a"), att("b"), att("c"))
        listOf(0, 1, 2).forEach { i ->
            val s = TimelineMediaNavState.Idle.openFromCollage(list, i)
            assertNull(s.viewer, "tap at index $i must not open viewer")
            assertSame(list[i], assertNotNull(s.gallery).heroAttachment)
        }
    }

    // ── gallery contains ALL attachments, ordering preserved ───────

    @Test
    fun galleryContainsAllAttachmentsInOrder() {
        val list = listOf(
            att("a", MediaType.Image),
            att("b", MediaType.Video),
            att("c", MediaType.Audio),
            att("d", MediaType.Image),
            att("e", MediaType.Audio),
            att("f", MediaType.Video),
        )
        val s = TimelineMediaNavState.Idle.openFromCollage(list, 0)
        val g = assertNotNull(s.gallery)
        assertEquals(6, g.size)
        list.forEachIndexed { i, a -> assertSame(a, g.attachments[i]) }
    }

    // ── +N tile → gallery (not direct viewer at index 3) ───────────

    @Test
    fun plusNTileOpensGalleryWithAllAttachments() {
        // 7-attachment moment: timeline shows first four with +3 overlay.
        // Tapping the +N tile passes tappedIndex=3 (the fourth item's index).
        val list = List(7) { att("a$it") }
        val s = TimelineMediaNavState.Idle.openFromCollage(list, 3)
        assertNull(s.viewer, "+N must not open viewer directly")
        val g = assertNotNull(s.gallery)
        assertEquals(7, g.size, "gallery must expose ALL attachments including hidden overflow")
        assertSame(list[3], g.heroAttachment, "+N must expand from the visible fourth tile")
    }

    @Test
    fun galleryStateRejectsSingleAttachment() {
        val solo = att("solo")
        assertFailsWith<IllegalArgumentException> {
            MomentMediaGalleryState(listOf(solo), solo)
        }
    }

    // ── gallery item tap → viewer at correct attachment ────────────

    @Test
    fun galleryItemTapOpensViewerAtThatIndex() {
        val list = List(5) { att("a$it") }
        val opened = TimelineMediaNavState.Idle.openFromCollage(list, 0)
        val fromTile = opened.openFromGallery(3)
        val v = assertNotNull(fromTile.viewer)
        assertEquals(3, v.currentIndex)
        assertEquals("a3", v.current.storageRef.value)
        assertNotNull(fromTile.gallery, "gallery remains mounted under viewer")
    }

    @Test
    fun galleryItemTapPreservesFullSwipeableList() {
        val list = List(6) { att("a$it") }
        val fromTile = TimelineMediaNavState.Idle
            .openFromCollage(list, 0)
            .openFromGallery(1)
        val v = assertNotNull(fromTile.viewer)
        assertEquals(6, v.size)
    }

    // ── back hierarchy ─────────────────────────────────────────────

    @Test
    fun viewerBackReturnsToGalleryForMultiMedia() {
        val list = List(3) { att("a$it") }
        val afterOpen = TimelineMediaNavState.Idle
            .openFromCollage(list, 0)
            .openFromGallery(2)
        val afterViewerBack = afterOpen.closeViewer()
        assertNull(afterViewerBack.viewer)
        assertNotNull(afterViewerBack.gallery, "gallery must remain so scroll position survives")
    }

    @Test
    fun galleryBackReturnsToTimeline() {
        val list = List(3) { att("a$it") }
        val afterOpen = TimelineMediaNavState.Idle.openFromCollage(list, 0)
        val afterGalleryBack = afterOpen.closeGallery()
        assertEquals(TimelineMediaNavState.Idle, afterGalleryBack)
    }

    @Test
    fun singleMediaViewerBackReturnsToTimeline() {
        val list = listOf(att("only"))
        val afterOpen = TimelineMediaNavState.Idle.openFromCollage(list, 0)
        val afterBack = afterOpen.closeViewer()
        assertEquals(TimelineMediaNavState.Idle, afterBack, "single-media viewer must NOT strand a gallery")
    }

    // ── gallery identity preserved across viewer open/close ────────

    @Test
    fun galleryReferenceUnchangedWhenOpeningAndClosingViewer() {
        val list = List(4) { att("a$it") }
        val opened = TimelineMediaNavState.Idle.openFromCollage(list, 0)
        val galleryRef = opened.gallery
        val afterOpenViewer = opened.openFromGallery(2)
        val afterCloseViewer = afterOpenViewer.closeViewer()
        assertSame(galleryRef, afterOpenViewer.gallery)
        assertSame(galleryRef, afterCloseViewer.gallery)
    }

    @Test
    fun collageTapRejectsEmptyList() {
        assertFailsWith<IllegalArgumentException> {
            TimelineMediaNavState.Idle.openFromCollage(emptyList(), 0)
        }
    }

    @Test
    fun openFromGalleryWithNoGalleryIsNoop() {
        val s = TimelineMediaNavState.Idle.openFromGallery(0)
        assertEquals(TimelineMediaNavState.Idle, s)
    }

    // ── audio is a first-class viewer attachment ───────────────────

    @Test
    fun galleryAudioTapOpensViewerAtThatAudioIndex() {
        val list = listOf(
            att("p1", MediaType.Image),
            att("v1", MediaType.Video),
            att("a1", MediaType.Audio),
            att("p2", MediaType.Image),
        )
        val opened = TimelineMediaNavState.Idle.openFromCollage(list, 0)
        val atAudio = opened.openFromGallery(2)
        val v = assertNotNull(atAudio.viewer)
        assertEquals(2, v.currentIndex, "viewer must open at the exact audio index, not skip it")
        assertEquals(MediaType.Audio, v.current.type)
        assertEquals("a1", v.current.storageRef.value)
    }

    @Test
    fun mixedMediaViewerSwipesAcrossAudio() {
        val list = listOf(
            att("i", MediaType.Image),
            att("a", MediaType.Audio),
            att("v", MediaType.Video),
        )
        val v = openAt(list, 0)
        assertEquals(MediaType.Image, v.current.type)
        val atAudio = v.withCurrent(1)
        assertEquals(MediaType.Audio, atAudio.current.type)
        val atVideo = atAudio.withCurrent(2)
        assertEquals(MediaType.Video, atVideo.current.type)
    }
}
