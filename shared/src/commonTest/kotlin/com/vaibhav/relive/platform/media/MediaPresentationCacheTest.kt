package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaStorageRef
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaPresentationCacheTest {

    @BeforeTest
    fun setUp() {
        MediaPresentationCache.clear()
    }

    @AfterTest
    fun tearDown() {
        MediaPresentationCache.clear()
    }

    @Test
    fun aspectRatioMissReturnsNull() {
        assertNull(MediaPresentationCache.aspectRatio(MediaStorageRef("missing")))
    }

    @Test
    fun aspectRatioPutThenGet() {
        val ref = MediaStorageRef("a")
        MediaPresentationCache.putAspectRatio(ref, 1.5f)
        assertEquals(1.5f, MediaPresentationCache.aspectRatio(ref))
    }

    @Test
    fun aspectRatioPutReplacesExistingValue() {
        val ref = MediaStorageRef("a")
        MediaPresentationCache.putAspectRatio(ref, 1.0f)
        MediaPresentationCache.putAspectRatio(ref, 2.0f)
        assertEquals(2.0f, MediaPresentationCache.aspectRatio(ref))
    }

    @Test
    fun durationRoundTrip() {
        val ref = MediaStorageRef("d")
        MediaPresentationCache.putDurationMs(ref, 12_345L)
        assertEquals(12_345L, MediaPresentationCache.durationMs(ref))
    }

    @Test
    fun cachedHitAvoidsReprobeSignal() {
        // The produceState contract in rememberImageAspectRatioFor short-circuits
        // when the cached value is non-null. Simulate that with a probe counter:
        // once the cache is populated, further "would probe" checks stay at zero.
        val ref = MediaStorageRef("a")
        var probes = 0
        fun ensureAspect(): Float {
            val cached = MediaPresentationCache.aspectRatio(ref)
            if (cached != null) return cached
            probes += 1
            val v = 1.777f
            MediaPresentationCache.putAspectRatio(ref, v)
            return v
        }
        repeat(10) { ensureAspect() }
        assertEquals(1, probes, "probe must run once for a given key, then be served from cache")
    }

    @Test
    fun exceedingCapacityBoundsMapSize() {
        val cap = MediaPresentationCache.CAPACITY
        // Insert well past capacity.
        repeat(cap + 50) { i ->
            MediaPresentationCache.putAspectRatio(MediaStorageRef("k$i"), i.toFloat())
        }
        assertTrue(
            MediaPresentationCache.aspectRatioSize() <= cap,
            "cache size ${MediaPresentationCache.aspectRatioSize()} must stay within CAPACITY=$cap",
        )
        // Most-recently-inserted key must still be present.
        val last = MediaStorageRef("k${cap + 49}")
        assertNotNull(MediaPresentationCache.aspectRatio(last))
    }

    @Test
    fun clearWipesBothMaps() {
        MediaPresentationCache.putAspectRatio(MediaStorageRef("a"), 1.0f)
        MediaPresentationCache.putDurationMs(MediaStorageRef("a"), 1L)
        MediaPresentationCache.clear()
        assertEquals(0, MediaPresentationCache.aspectRatioSize())
        assertEquals(0, MediaPresentationCache.durationSize())
    }
}
