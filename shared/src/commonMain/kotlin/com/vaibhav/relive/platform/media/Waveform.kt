package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaStorageRef
import kotlin.concurrent.Volatile
import kotlin.math.abs

/**
 * Pure envelope processing used by every platform waveform extractor
 * (ADR-0019 Stage 2). Deterministic peak-per-bucket downsampling of PCM
 * samples so silence maps to ~0, quiet to small values, and loud sections
 * preserve their real peaks. No random data, no per-file normalization —
 * absolute amplitude is preserved so callers can compare loudness across
 * segments of the same file.
 */
object WaveformProcessor {

    const val DEFAULT_BUCKETS: Int = 200
    private const val SHORT_PEAK: Float = 32767f

    /**
     * Downsample [samples] (signed 16-bit PCM) into [targetBuckets] normalized
     * peaks in `0f..1f`. Empty input or non-positive buckets return an empty
     * array. Bucket boundaries are deterministic (proportional index split),
     * so tests can reason about which sample lands in which bucket.
     */
    fun envelope(samples: ShortArray, targetBuckets: Int = DEFAULT_BUCKETS): FloatArray {
        if (samples.isEmpty() || targetBuckets <= 0) return FloatArray(0)
        val buckets = targetBuckets.coerceAtMost(samples.size)
        val out = FloatArray(buckets)
        for (b in 0 until buckets) {
            val start = (b.toLong() * samples.size / buckets).toInt()
            val endRaw = ((b + 1).toLong() * samples.size / buckets).toInt()
            val end = if (endRaw > start) endRaw.coerceAtMost(samples.size) else (start + 1).coerceAtMost(samples.size)
            var peak = 0
            var i = start
            while (i < end) {
                val v = samples[i].toInt()
                val a = if (v < 0) -v else v
                if (a > peak) peak = a
                i++
            }
            out[b] = (peak / SHORT_PEAK).coerceIn(0f, 1f)
        }
        return out
    }

    /**
     * Same as [envelope] but accepts pre-normalized float PCM in `-1f..1f`
     * (Core Audio / AVAudioFile format). Silence remains 0, saturation caps
     * at 1.
     */
    fun envelopeFromFloats(samples: FloatArray, targetBuckets: Int = DEFAULT_BUCKETS): FloatArray {
        if (samples.isEmpty() || targetBuckets <= 0) return FloatArray(0)
        val buckets = targetBuckets.coerceAtMost(samples.size)
        val out = FloatArray(buckets)
        for (b in 0 until buckets) {
            val start = (b.toLong() * samples.size / buckets).toInt()
            val endRaw = ((b + 1).toLong() * samples.size / buckets).toInt()
            val end = if (endRaw > start) endRaw.coerceAtMost(samples.size) else (start + 1).coerceAtMost(samples.size)
            var peak = 0f
            var i = start
            while (i < end) {
                val a = abs(samples[i])
                if (a > peak) peak = a
                i++
            }
            out[b] = peak.coerceIn(0f, 1f)
        }
        return out
    }

    /**
     * Extract [windowSize] envelope samples centered on [progress] (0..1).
     * Clamped so the window always sits on real data: at 0 the window sits
     * at the head, at 1 at the tail. When [envelope] is shorter than
     * [windowSize] the real samples are centered inside the window and the
     * remaining slots stay 0 (silence pad). Deterministic — never invents
     * amplitude.
     */
    fun window(envelope: FloatArray?, windowSize: Int, progress: Float): FloatArray {
        if (windowSize <= 0) return FloatArray(0)
        val out = FloatArray(windowSize)
        if (envelope == null || envelope.isEmpty()) return out
        val m = envelope.size
        val p = progress.coerceIn(0f, 1f)
        if (m <= windowSize) {
            val offset = (windowSize - m) / 2
            for (i in 0 until m) out[offset + i] = envelope[i]
            return out
        }
        val maxStart = m - windowSize
        val centerBucket = (p * (m - 1)).toInt().coerceIn(0, m - 1)
        val rawStart = centerBucket - windowSize / 2
        val start = rawStart.coerceIn(0, maxStart)
        for (i in 0 until windowSize) out[i] = envelope[start + i]
        return out
    }

    /**
     * Number of segments that fit in [widthPx] at [segmentWidthPx] and
     * [gapPx], never exceeding [preferred]. Returns 0 for a zero-width
     * viewport.
     */
    fun fitSegments(widthPx: Float, segmentWidthPx: Float, gapPx: Float, preferred: Int): Int {
        if (widthPx <= 0f || segmentWidthPx <= 0f || preferred <= 0) return 0
        val stride = segmentWidthPx + gapPx
        if (stride <= 0f) return preferred
        val maxN = ((widthPx + gapPx) / stride).toInt()
        return maxN.coerceIn(1, preferred)
    }

    /**
     * Playback position → normalized progress in `0f..1f`. Returns 0 for
     * unknown/zero durations so callers can render a stationary waveform
     * before duration is known.
     */
    fun progressFraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L || positionMs <= 0L) return 0f
        val f = positionMs.toDouble() / durationMs.toDouble()
        return f.coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Adaptive bucket target: shorter clips get fewer buckets so each bucket
     * still spans enough samples to be meaningful; longer clips cap at
     * [DEFAULT_BUCKETS] so rendering cost stays bounded.
     */
    fun bucketsFor(durationMs: Long): Int {
        if (durationMs <= 0L) return DEFAULT_BUCKETS
        // ~1 bucket per 50 ms, clamped 100..DEFAULT_BUCKETS.
        val target = (durationMs / 50L).toInt()
        return target.coerceIn(100, DEFAULT_BUCKETS)
    }
}

/**
 * Bounded per-process cache of extracted waveform envelopes keyed by
 * [MediaStorageRef.value]. Copy-on-write so the composition read path stays
 * lock-free during scrolling. One extraction per media item per cache
 * lifetime — scroll-away/back reuses the envelope.
 */
object WaveformCache {

    internal const val CAPACITY: Int = 64

    @Volatile private var map: Map<String, FloatArray> = emptyMap()

    fun get(ref: MediaStorageRef): FloatArray? = map[ref.value]

    fun put(ref: MediaStorageRef, value: FloatArray) {
        val cur = map
        map = if (cur.size < CAPACITY) cur + (ref.value to value)
        else cur.entries.drop(cur.size - CAPACITY + 1).associate { it.key to it.value } + (ref.value to value)
    }

    fun clear() { map = emptyMap() }

    internal fun size(): Int = map.size
}

/**
 * At-most-one active timeline audio session (ADR-0019 §9). When a tile
 * starts playback it claims the slot with a stop callback; a subsequent
 * claim invokes the previous callback so only one tile advances at a time.
 * Kept intentionally tiny — no state graph, no lifecycle, no threads.
 */
object ActivePlayback {

    @Volatile private var stopCurrent: (() -> Unit)? = null

    fun claim(stop: () -> Unit) {
        val prev = stopCurrent
        stopCurrent = stop
        if (prev != null && prev !== stop) prev.invoke()
    }

    fun release(stop: () -> Unit) {
        if (stopCurrent === stop) stopCurrent = null
    }

    /**
     * Navigation-driven stop. Invokes the current owner's stop callback (if
     * any) and clears the slot. Safe to call when nothing is active — no-op.
     * Called from timeline/gallery/viewer transitions so playback owned by
     * the departing surface stops before the new surface allocates its own.
     */
    fun stopActive() {
        val prev = stopCurrent
        stopCurrent = null
        prev?.invoke()
    }

    internal fun clear() { stopCurrent = null }
}
