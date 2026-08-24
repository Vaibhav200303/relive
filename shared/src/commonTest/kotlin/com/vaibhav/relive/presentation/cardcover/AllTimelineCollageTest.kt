package com.vaibhav.relive.presentation.cardcover

import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AllTimelineCollageTest {

    @Test
    fun threeHourBucketIsStableInsideIntervalAndChangesAtBoundary() {
        assertEquals(0L, allTimelineCollageBucket(Instant(0L)))
        assertEquals(0L, allTimelineCollageBucket(Instant(ALL_TIMELINE_COLLAGE_BUCKET_MILLIS - 1L)))
        assertEquals(1L, allTimelineCollageBucket(Instant(ALL_TIMELINE_COLLAGE_BUCKET_MILLIS)))
    }

    @Test
    fun zeroVisualMediaUsesGeneratedCoverAndAudioIsExcluded() {
        val empty = resolveAllTimelineCollage(emptyList(), bucket = 4L)
        assertTrue(empty.attachments.isEmpty())
        assertNull(empty.layout)

        val audioOnly = resolveAllTimelineCollage(listOf(attachment("voice", MediaType.Audio)), bucket = 4L)
        assertTrue(audioOnly.attachments.isEmpty())
        assertNull(audioOnly.layout)
    }

    @Test
    fun selectionNeverExceedsAvailableOrNineAndHasNoDuplicates() {
        (1..20).forEach { availableCount ->
            val selection = resolveAllTimelineCollage(
                available = List(availableCount) { attachment("item-$it") },
                bucket = 11L,
            )
            assertTrue(selection.attachments.size in 1..minOf(availableCount, 9))
            assertEquals(selection.attachments.size, selection.attachments.map { it.id }.toSet().size)
        }
    }

    @Test
    fun sameBucketIsStableAcrossRecomputation() {
        val available = List(20) { attachment("item-$it") }
        val first = resolveAllTimelineCollage(available, bucket = 99L)
        val second = resolveAllTimelineCollage(available.toList(), bucket = 99L)
        assertEquals(first, second)
    }

    @Test
    fun laterBucketsCanChangeCountSelectionAndArrangement() {
        val available = List(20) { attachment("item-$it") }
        val first = resolveAllTimelineCollage(available, bucket = 0L)
        val changed = (1L..64L)
            .map { resolveAllTimelineCollage(available, bucket = it) }
            .first { it != first }
        assertNotEquals(first, changed)
    }

    @Test
    fun selectedCountVariesAcrossBuckets() {
        val available = List(20) { attachment("item-$it") }
        val counts = (0L..64L).map { resolveAllTimelineCollage(available, it).attachments.size }.toSet()
        assertTrue(counts.size > 1)
        assertTrue(counts.all { it in 1..9 })
    }

    @Test
    fun firstVisualAdditionReplacesGeneratedFallback() {
        assertTrue(resolveAllTimelineCollage(emptyList(), 2L).attachments.isEmpty())
        val photo = attachment("first-photo")
        assertEquals(listOf(photo), resolveAllTimelineCollage(listOf(photo), 2L).attachments)
    }

    @Test
    fun layoutResolverCoversEveryItemCountWithoutOverlapOrSlivers() {
        (1..9).forEach { count ->
            listOf(0L, 1L).forEach { seed ->
                val layout = resolveAllTimelineCollageLayout(count, seed)
                assertEquals(count, layout.cells.size)
                val occupied = mutableSetOf<Pair<Int, Int>>()
                layout.cells.forEach { cell ->
                    for (column in cell.column until cell.column + cell.columnSpan) {
                        for (row in cell.row until cell.row + cell.rowSpan) {
                            assertTrue(occupied.add(column to row), "cells must not overlap for $count items")
                        }
                    }
                }
                assertEquals(layout.columns * layout.rows, occupied.size)
            }
        }
    }

    private fun attachment(id: String, type: MediaType = MediaType.Image) = MediaAttachment(
        id = MediaAttachmentId(id),
        type = type,
        storageRef = MediaStorageRef("preview/$id"),
        sortIndex = 0,
    )
}
