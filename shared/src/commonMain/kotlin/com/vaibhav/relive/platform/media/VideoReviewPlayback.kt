package com.vaibhav.relive.platform.media

/**
 * Which center overlay control is currently visible over the video review
 * surface. Hidden means the video plays cleanly with no icon on top.
 */
enum class VideoReviewOverlay { Play, Pause, Hidden }

/**
 * Deterministic playback UI state for the video review overlay. The actual
 * player (e.g. Android VideoView) lives in the platform surface; this holds
 * only what the UI needs to render and can be exercised in host-JVM tests
 * without a real player.
 *
 * Rules encoded in the transitions below:
 *  - Initial state is paused with a centered Play icon.
 *  - Tapping Play starts playback and hides the overlay. If playback had
 *    reached the end, the caller seeks back to 0 and clears [reachedEnd].
 *  - Tapping Pause pauses and re-shows the Play icon.
 *  - A single tap on the surface while playing reveals the Pause icon; it
 *    hides again after a short timeout unless the user taps it.
 *  - Reaching the end pauses and returns to the Play state (the caller seeks
 *    to 0 on the next Play so it does not restart from the current end frame).
 */
data class VideoReviewPlayback(
    val isPlaying: Boolean = false,
    val overlay: VideoReviewOverlay = VideoReviewOverlay.Play,
    val reachedEnd: Boolean = false,
) {
    companion object {
        val Initial: VideoReviewPlayback = VideoReviewPlayback()
    }
}

/** User tapped the centered Play icon. */
fun VideoReviewPlayback.tapPlay(): VideoReviewPlayback =
    copy(isPlaying = true, overlay = VideoReviewOverlay.Hidden, reachedEnd = false)

/** User tapped the centered Pause icon. */
fun VideoReviewPlayback.tapPause(): VideoReviewPlayback =
    copy(isPlaying = false, overlay = VideoReviewOverlay.Play)

/**
 * User single-tapped anywhere on the surface. While playing this reveals the
 * Pause overlay; while paused the Play overlay is already the resting state
 * so nothing changes.
 */
fun VideoReviewPlayback.singleTap(): VideoReviewPlayback =
    if (isPlaying) copy(overlay = VideoReviewOverlay.Pause) else this

/**
 * Auto-hide timer for the Pause overlay expired. Only hides if we are still
 * playing and the Pause overlay is still on screen.
 */
fun VideoReviewPlayback.pauseOverlayTimeout(): VideoReviewPlayback =
    if (isPlaying && overlay == VideoReviewOverlay.Pause) copy(overlay = VideoReviewOverlay.Hidden) else this

/** Player reported playback reached the end. */
fun VideoReviewPlayback.playbackEnded(): VideoReviewPlayback =
    copy(isPlaying = false, overlay = VideoReviewOverlay.Play, reachedEnd = true)
