package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaStorageRef
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WaveformCacheTest {

    @BeforeTest fun setUp() { WaveformCache.clear() }
    @AfterTest fun tearDown() { WaveformCache.clear() }

    @Test
    fun missReturnsNull() {
        assertNull(WaveformCache.get(MediaStorageRef("x")))
    }

    @Test
    fun putThenGetReturnsSameArray() {
        val ref = MediaStorageRef("a")
        val arr = FloatArray(4) { it.toFloat() }
        WaveformCache.put(ref, arr)
        assertSame(arr, WaveformCache.get(ref))
    }

    @Test
    fun cachedWaveformReusedForRepeatedReads() {
        val ref = MediaStorageRef("a")
        var extractions = 0
        fun ensure(): FloatArray {
            val cached = WaveformCache.get(ref)
            if (cached != null) return cached
            extractions += 1
            val v = FloatArray(8) { it.toFloat() }
            WaveformCache.put(ref, v)
            return v
        }
        repeat(10) { ensure() }
        assertEquals(1, extractions, "extraction must run once per key, then be served from cache")
    }

    @Test
    fun boundedByCapacity() {
        val cap = WaveformCache.CAPACITY
        repeat(cap + 25) { i ->
            WaveformCache.put(MediaStorageRef("k$i"), FloatArray(2) { i.toFloat() })
        }
        assertTrue(WaveformCache.size() <= cap, "cache ${WaveformCache.size()} > CAPACITY=$cap")
        assertNotNull(WaveformCache.get(MediaStorageRef("k${cap + 24}")))
    }

    @Test
    fun clearWipesEntries() {
        WaveformCache.put(MediaStorageRef("a"), FloatArray(1))
        WaveformCache.clear()
        assertEquals(0, WaveformCache.size())
    }
}
