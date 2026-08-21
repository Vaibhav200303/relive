package com.vaibhav.relive.platform.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoReviewEditStateTest {

    // ---- defaults / duration --------------------------------------------

    @Test fun defaultTrimIsEntireVideo() {
        val s = VideoReviewEditState.initial(10_000L)
        assertEquals(0L, s.trimStartMs)
        assertEquals(10_000L, s.trimEndMs)
        assertEquals(10_000L, s.selectedDurationMs)
        assertFalse(s.isTrimmed)
        assertFalse(s.isMuted)
        assertFalse(s.isPlaying)
    }

    @Test fun withDurationInitializesTrimOnFirstProbe() {
        val zero = VideoReviewEditState.initial(0L)
        val probed = zero.withDuration(8_000L)
        assertEquals(0L, probed.trimStartMs)
        assertEquals(8_000L, probed.trimEndMs)
    }

    @Test fun withDurationPreservesMuteAcrossFirstProbe() {
        val zero = VideoReviewEditState.initial(0L).toggleMute()
        val probed = zero.withDuration(4_000L)
        assertTrue(probed.isMuted)
    }

    @Test fun withDurationClampsTrimWhenSourceShrinks() {
        val s = VideoReviewEditState.initial(10_000L)
            .setTrimStart(3_000L)
            .setTrimEnd(9_000L)
        val shrunk = s.withDuration(6_000L)
        assertEquals(6_000L, shrunk.durationMs)
        assertEquals(6_000L, shrunk.trimEndMs)
        assertTrue(shrunk.trimStartMs <= shrunk.trimEndMs - VideoReviewEditState.MIN_TRIM_DURATION_MS)
    }

    // ---- trim handles ----------------------------------------------------

    @Test fun leftHandleUpdatesTrimStart() {
        val s = VideoReviewEditState.initial(10_000L).setTrimStart(2_500L)
        assertEquals(2_500L, s.trimStartMs)
        assertEquals(10_000L, s.trimEndMs)
    }

    @Test fun rightHandleUpdatesTrimEnd() {
        val s = VideoReviewEditState.initial(10_000L).setTrimEnd(7_500L)
        assertEquals(0L, s.trimStartMs)
        assertEquals(7_500L, s.trimEndMs)
    }

    @Test fun leftHandleCannotCrossRight() {
        val s = VideoReviewEditState.initial(10_000L).setTrimEnd(4_000L).setTrimStart(9_000L)
        assertTrue(s.trimStartMs <= s.trimEndMs - VideoReviewEditState.MIN_TRIM_DURATION_MS)
        assertEquals(4_000L - VideoReviewEditState.MIN_TRIM_DURATION_MS, s.trimStartMs)
    }

    @Test fun rightHandleCannotCrossLeft() {
        val s = VideoReviewEditState.initial(10_000L).setTrimStart(6_000L).setTrimEnd(3_000L)
        assertEquals(6_000L + VideoReviewEditState.MIN_TRIM_DURATION_MS, s.trimEndMs)
    }

    @Test fun trimStaysWithinDuration() {
        val s = VideoReviewEditState.initial(5_000L)
            .setTrimStart(-1_000L)
            .setTrimEnd(999_999L)
        assertEquals(0L, s.trimStartMs)
        assertEquals(5_000L, s.trimEndMs)
    }

    @Test fun minimumTrimDurationEnforced() {
        val s = VideoReviewEditState.initial(10_000L)
            .setTrimStart(5_000L)
            .setTrimEnd(5_100L)
        // right handle was clamped to left + MIN
        assertEquals(5_000L + VideoReviewEditState.MIN_TRIM_DURATION_MS, s.trimEndMs)
    }

    @Test fun displayedDurationReflectsSelectedTrim() {
        val s = VideoReviewEditState.initial(10_000L).setTrimStart(2_000L).setTrimEnd(6_000L)
        assertEquals(4_000L, s.selectedDurationMs)
    }

    @Test fun isTrimmedFlipsOnAnyHandleMove() {
        val full = VideoReviewEditState.initial(8_000L)
        assertFalse(full.isTrimmed)
        assertTrue(full.setTrimStart(100L).isTrimmed)
        assertTrue(full.setTrimEnd(7_900L).isTrimmed)
    }

    // ---- playback --------------------------------------------------------

    @Test fun playbackStartsAtTrimStartWhenBefore() {
        val s = VideoReviewEditState.initial(10_000L).setTrimStart(2_000L)
        val playing = s.pressPlay()
        assertEquals(2_000L, playing.playbackPositionMs)
        assertTrue(playing.isPlaying)
    }

    @Test fun playbackStartsAtTrimStartWhenAtOrPastEnd() {
        val s = VideoReviewEditState.initial(10_000L).setTrimEnd(6_000L)
            .updatePlaybackPosition(6_000L)
        val playing = s.pressPlay()
        assertEquals(0L, playing.playbackPositionMs)
    }

    @Test fun playbackResumesFromMiddleWhenInsideRange() {
        val s = VideoReviewEditState.initial(10_000L)
            .setTrimStart(2_000L).setTrimEnd(8_000L)
            .updatePlaybackPosition(4_000L)
            .pressPause()
        val resumed = s.pressPlay()
        assertEquals(4_000L, resumed.playbackPositionMs)
    }

    @Test fun playbackStopsAndResetsAtTrimEnd() {
        val ended = VideoReviewEditState.initial(10_000L)
            .setTrimStart(1_000L).setTrimEnd(5_000L)
            .pressPlay()
            .reachedTrimEnd()
        assertFalse(ended.isPlaying)
        assertEquals(1_000L, ended.playbackPositionMs)
    }

    @Test fun tickClampsIntoTrimRange() {
        val s = VideoReviewEditState.initial(10_000L)
            .setTrimStart(2_000L).setTrimEnd(4_000L)
            .updatePlaybackPosition(9_000L)
        assertEquals(4_000L, s.playbackPositionMs)
        val below = s.updatePlaybackPosition(500L)
        assertEquals(2_000L, below.playbackPositionMs)
    }

    @Test fun handleMoveClampsCursorIntoNewRange() {
        val s = VideoReviewEditState.initial(10_000L)
            .updatePlaybackPosition(9_000L)
            .setTrimEnd(5_000L)
        assertEquals(5_000L, s.playbackPositionMs)
    }

    // ---- mute ------------------------------------------------------------

    @Test fun muteToggles() {
        val s = VideoReviewEditState.initial(10_000L)
        assertFalse(s.isMuted)
        assertTrue(s.toggleMute().isMuted)
        assertFalse(s.toggleMute().toggleMute().isMuted)
    }

    // ---- formatters ------------------------------------------------------

    @Test fun formatDurationClockHandlesRanges() {
        assertEquals("0:08", formatDurationClock(8_010L))
        assertEquals("1:24", formatDurationClock(84_000L))
        assertEquals("12:05", formatDurationClock(12L * 60_000L + 5_000L))
        assertEquals("1:02:03", formatDurationClock(3_723_000L))
    }

    @Test fun formatFileSizeHandlesRanges() {
        assertEquals("842 KB", formatFileSize(842L * 1024L))
        assertEquals("1.9 MB", formatFileSize((1.9 * 1024 * 1024).toLong()))
        assertEquals("24.3 MB", formatFileSize((24.3 * 1024 * 1024).toLong()))
        assertEquals("0 B", formatFileSize(0L))
    }
}
