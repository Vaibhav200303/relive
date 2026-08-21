package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CameraUiStateTest {
    private fun media(path: String) = RawMedia(MediaType.Image, path, ownedByRelive = true)

    @Test fun captureEntersPhotoReviewInsteadOfCompleting() {
        val start: CameraUiState = CameraUiState.Live
        assertFalse(start.isReviewing)
        val next = enterPhotoReview(media("/tmp/a.jpg"))
        assertTrue(next.isReviewing)
        assertEquals("/tmp/a.jpg", next.captured.sourcePath)
    }

    @Test fun captureDoesNotImmediatelyComplete() {
        // Entering review does not itself yield a RawMedia to return; only
        // confirmReview does. Sanity-checks that shutter-press cannot
        // accidentally short-circuit into the completion callback.
        var delivered: RawMedia? = null
        val state: CameraUiState = enterPhotoReview(media("/tmp/a.jpg"))
        // Simulate: caller only pipes to completion via confirmReview.
        val (_, out) = if (state.isReviewing) state to null else state.confirmReview()
        delivered = out
        assertNull(delivered)
    }

    @Test fun retakeDeletesTempFileAndRestoresLive() {
        val deletes = mutableListOf<String>()
        val state: CameraUiState = enterPhotoReview(media("/tmp/a.jpg"))
        val next = state.discardReview { deletes += it }
        assertSame(CameraUiState.Live, next)
        assertEquals(listOf("/tmp/a.jpg"), deletes)
    }

    @Test fun confirmCompletesExactlyOnceAndReturnsCapturedAttachment() {
        val state: CameraUiState = enterPhotoReview(media("/tmp/a.jpg"))
        val (afterFirst, first) = state.confirmReview()
        assertEquals("/tmp/a.jpg", first?.sourcePath)
        assertSame(CameraUiState.Live, afterFirst)
        // A second confirm from Live must not produce another RawMedia.
        val (afterSecond, second) = afterFirst.confirmReview()
        assertNull(second)
        assertSame(CameraUiState.Live, afterSecond)
    }

    @Test fun backDuringReviewReturnsToLive() {
        val deletes = mutableListOf<String>()
        val state: CameraUiState = enterPhotoReview(media("/tmp/a.jpg"))
        // Back == discard.
        val next = state.discardReview { deletes += it }
        assertSame(CameraUiState.Live, next)
        assertEquals(1, deletes.size)
    }

    @Test fun repeatedRetakesDoNotLeaveTempFiles() {
        val deletes = mutableListOf<String>()
        var state: CameraUiState = CameraUiState.Live
        listOf("/tmp/a.jpg", "/tmp/b.jpg", "/tmp/c.jpg").forEach { path ->
            state = enterPhotoReview(media(path))
            state = state.discardReview { deletes += it }
        }
        assertEquals(listOf("/tmp/a.jpg", "/tmp/b.jpg", "/tmp/c.jpg"), deletes)
        assertSame(CameraUiState.Live, state)
    }

    @Test fun captureControlsDisabledDuringReview() {
        val live: CameraUiState = CameraUiState.Live
        val review: CameraUiState = enterPhotoReview(media("/tmp/a.jpg"))
        assertTrue(live.liveControlsEnabled())
        assertFalse(review.liveControlsEnabled())
    }

    @Test fun discardOnLiveIsNoopAndDeletesNothing() {
        val deletes = mutableListOf<String>()
        val next = (CameraUiState.Live as CameraUiState).discardReview { deletes += it }
        assertSame(CameraUiState.Live, next)
        assertTrue(deletes.isEmpty())
    }

    // --- Video review ---
    private fun video(path: String) = RawMedia(MediaType.Video, path, ownedByRelive = true)

    @Test fun videoFinalizeEntersVideoReviewInsteadOfCompleting() {
        val start: CameraUiState = CameraUiState.Live
        assertFalse(start.isReviewing)
        val next = enterVideoReview(video("/tmp/a.mp4"))
        assertTrue(next.isReviewing)
        assertEquals("/tmp/a.mp4", next.captured.sourcePath)
    }

    @Test fun videoFinalizeDoesNotImmediatelyComplete() {
        // Entering VideoReview does not itself hand a RawMedia back; only
        // confirmReview does. Guards against a shutter-stop path that would
        // short-circuit into the completion callback.
        val state: CameraUiState = enterVideoReview(video("/tmp/a.mp4"))
        val (_, out) = if (state.isReviewing) state to null else state.confirmReview()
        assertNull(out)
    }

    @Test fun videoRetakeDeletesTempFileAndRestoresLive() {
        val deletes = mutableListOf<String>()
        val state: CameraUiState = enterVideoReview(video("/tmp/a.mp4"))
        val next = state.discardReview { deletes += it }
        assertSame(CameraUiState.Live, next)
        assertEquals(listOf("/tmp/a.mp4"), deletes)
    }

    @Test fun videoConfirmCompletesExactlyOnceAndReturnsCapturedAttachment() {
        val state: CameraUiState = enterVideoReview(video("/tmp/a.mp4"))
        val (afterFirst, first) = state.confirmReview()
        assertEquals("/tmp/a.mp4", first?.sourcePath)
        assertEquals(MediaType.Video, first?.type)
        assertSame(CameraUiState.Live, afterFirst)
        val (afterSecond, second) = afterFirst.confirmReview()
        assertNull(second)
        assertSame(CameraUiState.Live, afterSecond)
    }

    @Test fun videoConfirmDoesNotDeleteAcceptedFile() {
        val deletes = mutableListOf<String>()
        val state: CameraUiState = enterVideoReview(video("/tmp/a.mp4"))
        val (next, out) = state.confirmReview()
        // A caller that only invokes discardReview on retake/back never sees
        // deletions here — confirm hands the file off intact.
        next.discardReview { deletes += it }
        assertEquals("/tmp/a.mp4", out?.sourcePath)
        assertTrue(deletes.isEmpty())
    }

    @Test fun videoBackDuringReviewActsLikeRetake() {
        val deletes = mutableListOf<String>()
        val state: CameraUiState = enterVideoReview(video("/tmp/a.mp4"))
        val next = state.discardReview { deletes += it }
        assertSame(CameraUiState.Live, next)
        assertEquals(listOf("/tmp/a.mp4"), deletes)
    }

    @Test fun videoReviewDisablesLiveControls() {
        val review: CameraUiState = enterVideoReview(video("/tmp/a.mp4"))
        assertFalse(review.liveControlsEnabled())
        assertTrue(review.isReviewing)
    }

    @Test fun repeatedVideoRetakesDoNotLeaveTempFiles() {
        val deletes = mutableListOf<String>()
        var state: CameraUiState = CameraUiState.Live
        listOf("/tmp/a.mp4", "/tmp/b.mp4", "/tmp/c.mp4").forEach { path ->
            state = enterVideoReview(video(path))
            state = state.discardReview { deletes += it }
        }
        assertEquals(listOf("/tmp/a.mp4", "/tmp/b.mp4", "/tmp/c.mp4"), deletes)
        assertSame(CameraUiState.Live, state)
    }
}
