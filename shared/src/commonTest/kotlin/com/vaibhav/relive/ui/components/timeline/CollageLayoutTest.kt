package com.vaibhav.relive.ui.components.timeline

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CollageLayoutTest {

    private fun att(id: String, type: MediaType = MediaType.Image): MomentAttachmentPresentation =
        MomentAttachmentPresentation(storageRef = MediaStorageRef(id), type = type)

    @Test
    fun oneAttachmentSelectsSingle() {
        val input = listOf(att("a"))
        val r = selectCollage(input)
        assertEquals(CollageShape.Single, r.shape)
        assertEquals(1, r.visible.size)
        assertEquals(0, r.overflow)
    }

    @Test
    fun twoAttachmentsSelectsTwo() {
        val r = selectCollage(listOf(att("a"), att("b")))
        assertEquals(CollageShape.Two, r.shape)
        assertEquals(2, r.visible.size)
        assertEquals(0, r.overflow)
    }

    @Test
    fun threeAttachmentsSelectsThree() {
        val r = selectCollage(listOf(att("a"), att("b"), att("c")))
        assertEquals(CollageShape.Three, r.shape)
        assertEquals(3, r.visible.size)
        assertEquals(0, r.overflow)
    }

    @Test
    fun fourAttachmentsSelectsFour() {
        val r = selectCollage(listOf(att("a"), att("b"), att("c"), att("d")))
        assertEquals(CollageShape.Four, r.shape)
        assertEquals(4, r.visible.size)
        assertEquals(0, r.overflow)
    }

    @Test
    fun fiveAttachmentsSelectsFourPlusOne() {
        val input = List(5) { att("a$it") }
        val r = selectCollage(input)
        assertEquals(CollageShape.FourPlus, r.shape)
        assertEquals(4, r.visible.size)
        assertEquals(1, r.overflow)
    }

    @Test
    fun eightAttachmentsSelectsFourPlusFour() {
        val input = List(8) { att("a$it") }
        val r = selectCollage(input)
        assertEquals(CollageShape.FourPlus, r.shape)
        assertEquals(4, r.visible.size)
        assertEquals(4, r.overflow)
    }

    @Test
    fun tenAttachmentsSelectsFourPlusSix() {
        val r = selectCollage(List(10) { att("a$it") })
        assertEquals(6, r.overflow)
    }

    @Test
    fun attachmentOrderIsPreserved() {
        val input = listOf(att("a"), att("b"), att("c"), att("d"))
        val r = selectCollage(input)
        assertEquals(listOf("a", "b", "c", "d"), r.visible.map { it.storageRef.value })
    }

    @Test
    fun fourPlusPreservesFirstFourOrder() {
        val input = listOf(
            att("a"), att("b"), att("c"), att("d"), att("e"), att("f"), att("g"),
        )
        val r = selectCollage(input)
        assertEquals(listOf("a", "b", "c", "d"), r.visible.map { it.storageRef.value })
        assertEquals(3, r.overflow)
    }

    @Test
    fun mixedMediaOrderingIsPreserved() {
        // Photo | Audio then Video | Photo.
        val a = att("a", MediaType.Image)
        val b = att("b", MediaType.Audio)
        val c = att("c", MediaType.Video)
        val d = att("d", MediaType.Image)
        val r = selectCollage(listOf(a, b, c, d))
        assertSame(a, r.visible[0])
        assertSame(b, r.visible[1])
        assertSame(c, r.visible[2])
        assertSame(d, r.visible[3])
    }

    @Test
    fun audioParticipatesLikeImageAndVideoInLayoutSelection() {
        val photoAudio = selectCollage(listOf(att("a", MediaType.Image), att("b", MediaType.Audio)))
        assertEquals(CollageShape.Two, photoAudio.shape)
        assertEquals(2, photoAudio.visible.size)

        val dominantMixed = selectCollage(
            listOf(att("a", MediaType.Image), att("b", MediaType.Video), att("c", MediaType.Audio)),
        )
        assertEquals(CollageShape.Three, dominantMixed.shape)
        assertEquals(MediaType.Audio, dominantMixed.visible[2].type)

        val allAudio = selectCollage(List(4) { att("a$it", MediaType.Audio) })
        assertEquals(CollageShape.Four, allAudio.shape)
    }

    @Test
    fun emptyAttachmentsThrows() {
        assertFailsWith<IllegalArgumentException> { selectCollage(emptyList()) }
    }
}
