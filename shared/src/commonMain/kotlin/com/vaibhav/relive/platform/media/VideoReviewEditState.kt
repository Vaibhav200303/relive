package com.vaibhav.relive.platform.media

/**
 * Deterministic trim / mute / playback-cursor state for the video review
 * editor strip. Kept as a plain data value so trim math is testable without a
 * real player. UI reads it, drives handle drags and mute taps against the
 * reducers below, then reflects the resulting values back into the framework
 * VideoView.
 *
 * Trim invariants:
 *  - 0 <= trimStartMs
 *  - trimStartMs + MIN_TRIM_DURATION_MS <= trimEndMs <= durationMs
 *  - a zero [durationMs] is legal (unknown / not yet probed) and simply pins
 *    both handles to 0 until a real duration arrives.
 *
 * Playback:
 *  - [playbackPositionMs] is always clamped into [trimStartMs, trimEndMs].
 *  - Pressing Play when the cursor is outside the trim range (either before
 *    trimStart or at/past trimEnd) rewinds to trimStart.
 *  - When playback reaches trimEnd we stop and rewind to trimStart so the
 *    next Play starts cleanly at the head.
 */
data class VideoReviewEditState(
    val durationMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val isMuted: Boolean,
    val playbackPositionMs: Long,
    val isPlaying: Boolean,
) {
    /** True when the trim range is anything shorter than the full clip. */
    val isTrimmed: Boolean get() = trimStartMs > 0L || trimEndMs < durationMs

    /** Selected duration in ms, always >= 0. */
    val selectedDurationMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)

    companion object {
        /** Minimum allowed trimmed clip length (prevents zero-length exports). */
        const val MIN_TRIM_DURATION_MS: Long = 500L

        fun initial(durationMs: Long): VideoReviewEditState {
            val d = durationMs.coerceAtLeast(0L)
            return VideoReviewEditState(
                durationMs = d,
                trimStartMs = 0L,
                trimEndMs = d,
                isMuted = false,
                playbackPositionMs = 0L,
                isPlaying = false,
            )
        }
    }
}

/**
 * Rebase the state on a freshly probed [durationMs]. Preserves existing trim
 * fractions where they still fit; when no meaningful trim exists yet (initial
 * placeholder with duration 0) the whole clip becomes the default selection.
 */
fun VideoReviewEditState.withDuration(durationMs: Long): VideoReviewEditState {
    val d = durationMs.coerceAtLeast(0L)
    if (d == this.durationMs) return this
    // First real duration -> full selection.
    if (this.durationMs == 0L) return VideoReviewEditState.initial(d).copy(isMuted = isMuted)
    val newEnd = trimEndMs.coerceAtMost(d)
    val newStart = trimStartMs.coerceAtMost(
        (newEnd - VideoReviewEditState.MIN_TRIM_DURATION_MS).coerceAtLeast(0L),
    )
    return copy(
        durationMs = d,
        trimStartMs = newStart,
        trimEndMs = newEnd,
        playbackPositionMs = playbackPositionMs.coerceIn(newStart, newEnd),
    )
}

/** Drag the left trim handle to [ms]. Clamped to [0, trimEndMs - min]. */
fun VideoReviewEditState.setTrimStart(ms: Long): VideoReviewEditState {
    if (durationMs <= 0L) return this
    val maxAllowed = (trimEndMs - VideoReviewEditState.MIN_TRIM_DURATION_MS).coerceAtLeast(0L)
    val newStart = ms.coerceIn(0L, maxAllowed)
    val newPos = playbackPositionMs.coerceIn(newStart, trimEndMs)
    return copy(trimStartMs = newStart, playbackPositionMs = newPos)
}

/** Drag the right trim handle to [ms]. Clamped to [trimStartMs + min, durationMs]. */
fun VideoReviewEditState.setTrimEnd(ms: Long): VideoReviewEditState {
    if (durationMs <= 0L) return this
    val minAllowed = (trimStartMs + VideoReviewEditState.MIN_TRIM_DURATION_MS).coerceAtMost(durationMs)
    val newEnd = ms.coerceIn(minAllowed, durationMs)
    val newPos = playbackPositionMs.coerceIn(trimStartMs, newEnd)
    return copy(trimEndMs = newEnd, playbackPositionMs = newPos)
}

/** Toggle the mute flag. Does not touch the underlying source file. */
fun VideoReviewEditState.toggleMute(): VideoReviewEditState = copy(isMuted = !isMuted)

/**
 * Press Play. When the cursor sits outside the selected range (before start
 * or at/past end) it snaps to trimStart before playing.
 */
fun VideoReviewEditState.pressPlay(): VideoReviewEditState {
    val pos = if (playbackPositionMs < trimStartMs || playbackPositionMs >= trimEndMs) {
        trimStartMs
    } else {
        playbackPositionMs
    }
    return copy(isPlaying = true, playbackPositionMs = pos)
}

/** Press Pause. Keeps the current playback cursor. */
fun VideoReviewEditState.pressPause(): VideoReviewEditState = copy(isPlaying = false)

/**
 * Playback reached (or crossed) trimEndMs during playing. Stops and rewinds
 * to trimStart so the Play button re-shows and the next tap starts fresh.
 */
fun VideoReviewEditState.reachedTrimEnd(): VideoReviewEditState =
    copy(isPlaying = false, playbackPositionMs = trimStartMs)

/** Player-reported position tick while playing. Clamped into trim range. */
fun VideoReviewEditState.updatePlaybackPosition(ms: Long): VideoReviewEditState {
    val clamped = ms.coerceIn(trimStartMs, trimEndMs)
    if (clamped == playbackPositionMs) return this
    return copy(playbackPositionMs = clamped)
}

/**
 * Human-readable clock: `m:ss` for < 1h, `h:mm:ss` beyond. Rounds down to the
 * nearest whole second so a trim of 8010ms reads "0:08" (matches player HUDs).
 */
fun formatDurationClock(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        "${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "${m}:${s.toString().padStart(2, '0')}"
    }
}

/**
 * Human-readable byte size: B / KB / MB / GB with one decimal above KB.
 * Matches the compact "1.9 MB" style used in messaging apps.
 */
fun formatFileSize(bytes: Long): String {
    val b = bytes.coerceAtLeast(0L)
    if (b < 1024L) return "${b} B"
    val kb = b.toDouble() / 1024.0
    if (kb < 1024.0) return "${kb.toInt()} KB"
    val mb = kb / 1024.0
    if (mb < 1024.0) return oneDecimal(mb) + " MB"
    val gb = mb / 1024.0
    return oneDecimal(gb) + " GB"
}

private fun oneDecimal(v: Double): String {
    val tenths = kotlin.math.round(v * 10.0).toLong()
    val whole = tenths / 10L
    val frac = kotlin.math.abs(tenths % 10L)
    return "${whole}.${frac}"
}
