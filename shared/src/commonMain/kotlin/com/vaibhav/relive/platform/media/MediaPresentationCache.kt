package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaStorageRef
import kotlin.concurrent.Volatile

/**
 * Bounded in-memory cache of lightweight media presentation metadata
 * (aspect ratio, duration) keyed by [MediaStorageRef.value]. Populated off the
 * main thread by the platform tile Composables; read cheaply during
 * composition. Copy-on-write map — races between writers only lose entries,
 * which is harmless (a miss simply re-probes). Bounded so a long timeline
 * cannot grow the map without limit.
 */
data class NaturalSizePx(val widthPx: Int, val heightPx: Int)

object MediaPresentationCache {

    internal const val CAPACITY: Int = 256

    @Volatile private var aspectRatios: Map<String, Float> = emptyMap()
    @Volatile private var durationsMs: Map<String, Long> = emptyMap()
    @Volatile private var naturalSizesPx: Map<String, NaturalSizePx> = emptyMap()
    @Volatile private var sourcePathNaturalSizesPx: Map<String, NaturalSizePx> = emptyMap()

    fun aspectRatio(ref: MediaStorageRef): Float? = aspectRatios[ref.value]

    fun putAspectRatio(ref: MediaStorageRef, value: Float) {
        aspectRatios = trim(aspectRatios) + (ref.value to value)
    }

    fun durationMs(ref: MediaStorageRef): Long? = durationsMs[ref.value]

    fun putDurationMs(ref: MediaStorageRef, value: Long) {
        durationsMs = trim(durationsMs) + (ref.value to value)
    }

    fun naturalSizePx(ref: MediaStorageRef): NaturalSizePx? = naturalSizesPx[ref.value]

    fun putNaturalSizePx(ref: MediaStorageRef, value: NaturalSizePx) {
        naturalSizesPx = trim(naturalSizesPx) + (ref.value to value)
    }

    fun naturalSizePxForPath(path: String): NaturalSizePx? = sourcePathNaturalSizesPx[path]

    fun putNaturalSizePxForPath(path: String, value: NaturalSizePx) {
        sourcePathNaturalSizesPx = trim(sourcePathNaturalSizesPx) + (path to value)
    }

    fun clear() {
        aspectRatios = emptyMap()
        durationsMs = emptyMap()
        naturalSizesPx = emptyMap()
        sourcePathNaturalSizesPx = emptyMap()
    }

    internal fun aspectRatioSize(): Int = aspectRatios.size
    internal fun durationSize(): Int = durationsMs.size
    internal fun naturalSizeCount(): Int = naturalSizesPx.size

    private fun <V> trim(cur: Map<String, V>): Map<String, V> =
        if (cur.size < CAPACITY) cur
        else cur.entries.drop(cur.size - CAPACITY + 1).associate { it.key to it.value }
}
