package com.vaibhav.relive.platform.media

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ActivePlaybackTest {

    @BeforeTest fun setUp() { ActivePlayback.clear() }
    @AfterTest fun tearDown() { ActivePlayback.clear() }

    @Test
    fun secondClaimStopsFirst() {
        var stoppedA = 0
        var stoppedB = 0
        val a: () -> Unit = { stoppedA++ }
        val b: () -> Unit = { stoppedB++ }
        ActivePlayback.claim(a)
        assertEquals(0, stoppedA)
        ActivePlayback.claim(b)
        assertEquals(1, stoppedA, "first claim not stopped by second")
        assertEquals(0, stoppedB)
    }

    @Test
    fun releaseCurrentClearsSlot() {
        var stoppedA = 0
        val a: () -> Unit = { stoppedA++ }
        ActivePlayback.claim(a)
        ActivePlayback.release(a)
        // A subsequent claim must not invoke the released callback.
        ActivePlayback.claim { }
        assertEquals(0, stoppedA)
    }

    @Test
    fun releaseNonCurrentIsNoop() {
        var stoppedA = 0
        val a: () -> Unit = { stoppedA++ }
        val b: () -> Unit = { }
        ActivePlayback.claim(a)
        ActivePlayback.release(b) // b was never current
        ActivePlayback.claim { }
        assertEquals(1, stoppedA, "current a should still be stopped by new claim")
    }

    @Test
    fun sameClaimTwiceDoesNotStopItself() {
        var stopped = 0
        val a: () -> Unit = { stopped++ }
        ActivePlayback.claim(a)
        ActivePlayback.claim(a)
        assertEquals(0, stopped)
    }

    @Test
    fun stopActiveInvokesAndClearsCurrent() {
        var stoppedA = 0
        val a: () -> Unit = { stoppedA++ }
        ActivePlayback.claim(a)
        ActivePlayback.stopActive()
        assertEquals(1, stoppedA)
        // subsequent claim must not re-stop a
        ActivePlayback.claim { }
        assertEquals(1, stoppedA)
    }

    @Test
    fun stopActiveWithNoOwnerIsNoop() {
        // Must not throw and must leave slot empty.
        ActivePlayback.stopActive()
        var stopped = 0
        ActivePlayback.claim { stopped++ }
        assertEquals(0, stopped)
    }

    @Test
    fun stopActiveIsIdempotent() {
        var stoppedA = 0
        val a: () -> Unit = { stoppedA++ }
        ActivePlayback.claim(a)
        ActivePlayback.stopActive()
        ActivePlayback.stopActive()
        assertEquals(1, stoppedA, "stopActive twice must not re-invoke old stop")
    }

    @Test
    fun atMostOneActiveOwnerAcrossManyClaims() {
        val stops = IntArray(5)
        val callbacks: List<() -> Unit> = List(5) { idx -> { stops[idx]++ } }
        callbacks.forEach { ActivePlayback.claim(it) }
        // Only the last claim owns the slot; all previous were stopped exactly once.
        for (i in 0..3) assertEquals(1, stops[i], "owner $i not stopped exactly once")
        assertEquals(0, stops[4], "current owner must not have been stopped")
    }
}
