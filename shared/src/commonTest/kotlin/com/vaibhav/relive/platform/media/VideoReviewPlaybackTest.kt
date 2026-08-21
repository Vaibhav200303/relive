package com.vaibhav.relive.platform.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoReviewPlaybackTest {

    @Test fun initialStateIsPausedWithPlayOverlay() {
        val s = VideoReviewPlayback.Initial
        assertFalse(s.isPlaying)
        assertEquals(VideoReviewOverlay.Play, s.overlay)
        assertFalse(s.reachedEnd)
    }

    @Test fun tapPlayStartsPlaybackAndHidesOverlay() {
        val s = VideoReviewPlayback.Initial.tapPlay()
        assertTrue(s.isPlaying)
        assertEquals(VideoReviewOverlay.Hidden, s.overlay)
        assertFalse(s.reachedEnd)
    }

    @Test fun singleTapWhilePlayingRevealsPauseOverlay() {
        val playing = VideoReviewPlayback.Initial.tapPlay()
        val tapped = playing.singleTap()
        assertTrue(tapped.isPlaying)
        assertEquals(VideoReviewOverlay.Pause, tapped.overlay)
    }

    @Test fun singleTapWhilePausedIsNoop() {
        val s = VideoReviewPlayback.Initial // paused, Play overlay
        val tapped = s.singleTap()
        assertEquals(s, tapped)
    }

    @Test fun tapPausePausesAndShowsPlayOverlay() {
        val playing = VideoReviewPlayback.Initial.tapPlay().singleTap() // playing, Pause overlay
        val paused = playing.tapPause()
        assertFalse(paused.isPlaying)
        assertEquals(VideoReviewOverlay.Play, paused.overlay)
    }

    @Test fun tapPlayResumesFromPausedWithoutRestartFlag() {
        val paused = VideoReviewPlayback.Initial.tapPlay().singleTap().tapPause()
        val resumed = paused.tapPlay()
        assertTrue(resumed.isPlaying)
        assertEquals(VideoReviewOverlay.Hidden, resumed.overlay)
        assertFalse(resumed.reachedEnd)
    }

    @Test fun playbackEndedReturnsToPlayableStateAndFlagsEnd() {
        val playing = VideoReviewPlayback.Initial.tapPlay()
        val ended = playing.playbackEnded()
        assertFalse(ended.isPlaying)
        assertEquals(VideoReviewOverlay.Play, ended.overlay)
        assertTrue(ended.reachedEnd)
    }

    @Test fun tapPlayAfterEndClearsReachedEndFlag() {
        val ended = VideoReviewPlayback.Initial.tapPlay().playbackEnded()
        val restarted = ended.tapPlay()
        assertTrue(restarted.isPlaying)
        assertFalse(restarted.reachedEnd)
    }

    @Test fun pauseOverlayTimeoutHidesWhileStillPlaying() {
        val revealed = VideoReviewPlayback.Initial.tapPlay().singleTap()
        val hidden = revealed.pauseOverlayTimeout()
        assertTrue(hidden.isPlaying)
        assertEquals(VideoReviewOverlay.Hidden, hidden.overlay)
    }

    @Test fun pauseOverlayTimeoutIsNoopWhenPaused() {
        val paused = VideoReviewPlayback.Initial // paused, Play overlay
        assertEquals(paused, paused.pauseOverlayTimeout())
    }

    @Test fun pauseOverlayTimeoutIsNoopWhenOverlayAlreadyHidden() {
        val playing = VideoReviewPlayback.Initial.tapPlay() // playing, hidden
        assertEquals(playing, playing.pauseOverlayTimeout())
    }
}
