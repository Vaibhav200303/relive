package com.vaibhav.relive.platform.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaveformProcessorTest {

    @Test
    fun emptyInputReturnsEmpty() {
        assertEquals(0, WaveformProcessor.envelope(ShortArray(0), 100).size)
        assertEquals(0, WaveformProcessor.envelopeFromFloats(FloatArray(0), 100).size)
    }

    @Test
    fun zeroBucketsReturnsEmpty() {
        assertEquals(0, WaveformProcessor.envelope(ShortArray(100), 0).size)
    }

    @Test
    fun negativeBucketsReturnsEmpty() {
        assertEquals(0, WaveformProcessor.envelope(ShortArray(100), -5).size)
    }

    @Test
    fun allZeroPcmProducesFlatEnvelope() {
        val env = WaveformProcessor.envelope(ShortArray(1000), 100)
        assertEquals(100, env.size)
        for (v in env) assertEquals(0f, v)
    }

    @Test
    fun quietPcmProducesLowAmplitude() {
        val samples = ShortArray(1000) { 1000 }
        val env = WaveformProcessor.envelope(samples, 100)
        for (v in env) assertTrue(v < 0.05f, "expected <0.05, got $v")
    }

    @Test
    fun loudPcmProducesLargerAmplitude() {
        val samples = ShortArray(1000) { 30000 }
        val env = WaveformProcessor.envelope(samples, 100)
        for (v in env) assertTrue(v > 0.8f, "expected >0.8, got $v")
    }

    @Test
    fun isolatedPeakSurvivesDownsampling() {
        val samples = ShortArray(1000)
        samples[500] = 30000
        val env = WaveformProcessor.envelope(samples, 100)
        // sample index 500 lands in bucket 50 (500 * 100 / 1000 = 50)
        assertTrue(env[50] > 0.8f, "peak lost in bucket 50: ${env[50]}")
        // other buckets remain near zero
        for (i in env.indices) if (i != 50) assertEquals(0f, env[i])
    }

    @Test
    fun envelopeStaysBounded01() {
        val samples = ShortArray(5000) { i -> ((i * 37) % 32767).toShort() }
        val env = WaveformProcessor.envelope(samples, 50)
        for (v in env) assertTrue(v in 0f..1f, "out of range: $v")
    }

    @Test
    fun floatEnvelopeSaturatesAtOne() {
        val samples = FloatArray(200) { if (it % 2 == 0) 2.5f else -3f }
        val env = WaveformProcessor.envelopeFromFloats(samples, 20)
        for (v in env) assertEquals(1f, v)
    }

    @Test
    fun floatSilenceStaysFlat() {
        val samples = FloatArray(400)
        val env = WaveformProcessor.envelopeFromFloats(samples, 40)
        for (v in env) assertEquals(0f, v)
    }

    @Test
    fun silenceSegmentDoesNotInflate() {
        // First half loud, second half silence — silence half must be flat.
        val samples = ShortArray(1000) { if (it < 500) 25000 else 0 }
        val env = WaveformProcessor.envelope(samples, 100)
        for (i in 0 until 50) assertTrue(env[i] > 0.5f, "loud bucket $i too small: ${env[i]}")
        for (i in 50 until 100) assertEquals(0f, env[i], "silence bucket $i inflated: ${env[i]}")
    }

    @Test
    fun bucketsAtOrBelowSamplesRespected() {
        // more buckets than samples clamps to sample count
        val env = WaveformProcessor.envelope(ShortArray(4) { 10000 }, targetBuckets = 100)
        assertEquals(4, env.size)
    }

    @Test
    fun progressZeroWhenDurationZero() {
        assertEquals(0f, WaveformProcessor.progressFraction(500L, 0L))
        assertEquals(0f, WaveformProcessor.progressFraction(-1L, 1000L))
    }

    @Test
    fun progressAtStartMidEnd() {
        assertEquals(0f, WaveformProcessor.progressFraction(0L, 1000L))
        assertEquals(0.5f, WaveformProcessor.progressFraction(500L, 1000L))
        assertEquals(1f, WaveformProcessor.progressFraction(1000L, 1000L))
    }

    @Test
    fun progressClampsBeyondEnd() {
        assertEquals(1f, WaveformProcessor.progressFraction(5000L, 1000L))
    }

    @Test
    fun bucketsForClampsRange() {
        assertEquals(WaveformProcessor.DEFAULT_BUCKETS, WaveformProcessor.bucketsFor(0L))
        assertTrue(WaveformProcessor.bucketsFor(1_000L) in 100..WaveformProcessor.DEFAULT_BUCKETS)
        assertEquals(WaveformProcessor.DEFAULT_BUCKETS, WaveformProcessor.bucketsFor(600_000L))
    }

    @Test
    fun windowEmptyEnvelopeReturnsFlatWindow() {
        val out = WaveformProcessor.window(null, 15, 0.5f)
        assertEquals(15, out.size)
        for (v in out) assertEquals(0f, v)
        val out2 = WaveformProcessor.window(FloatArray(0), 15, 0.5f)
        assertEquals(15, out2.size)
        for (v in out2) assertEquals(0f, v)
    }

    @Test
    fun windowZeroSizeReturnsEmpty() {
        assertEquals(0, WaveformProcessor.window(FloatArray(20) { 0.5f }, 0, 0.5f).size)
    }

    @Test
    fun windowAtStartAlignsToHead() {
        val env = FloatArray(100) { i -> i / 100f }
        val out = WaveformProcessor.window(env, 15, 0f)
        assertEquals(15, out.size)
        for (i in 0 until 15) assertEquals(env[i], out[i])
    }

    @Test
    fun windowAtEndAlignsToTail() {
        val env = FloatArray(100) { i -> i / 100f }
        val out = WaveformProcessor.window(env, 15, 1f)
        for (i in 0 until 15) assertEquals(env[100 - 15 + i], out[i])
    }

    @Test
    fun windowMidCentersOnPlayhead() {
        val env = FloatArray(100) { i -> i / 100f }
        val out = WaveformProcessor.window(env, 15, 0.5f)
        // centerBucket = 49, start = 49 - 7 = 42
        assertEquals(env[42], out[0])
        assertEquals(env[49], out[7])
        assertEquals(env[56], out[14])
    }

    @Test
    fun windowAdvancesAsProgressAdvances() {
        val env = FloatArray(200) { i -> i / 200f }
        val a = WaveformProcessor.window(env, 15, 0.30f)
        val b = WaveformProcessor.window(env, 15, 0.60f)
        // Later progress must not start earlier in the envelope.
        assertTrue(b[7] > a[7], "expected later window center to advance, got ${a[7]} vs ${b[7]}")
    }

    @Test
    fun windowFrozenForSameProgress() {
        val env = FloatArray(200) { i -> i / 200f }
        val a = WaveformProcessor.window(env, 15, 0.42f)
        val b = WaveformProcessor.window(env, 15, 0.42f)
        for (i in a.indices) assertEquals(a[i], b[i])
    }

    @Test
    fun windowShorterEnvelopeCentersAndPadsWithSilence() {
        val env = FloatArray(5) { 0.7f }
        val out = WaveformProcessor.window(env, 15, 0f)
        // 5 real samples centered → offset (15-5)/2 = 5
        for (i in 0 until 5) assertEquals(0f, out[i])
        for (i in 5 until 10) assertEquals(0.7f, out[i])
        for (i in 10 until 15) assertEquals(0f, out[i])
    }

    @Test
    fun windowSilenceEnvelopeStaysFlat() {
        val env = FloatArray(100)
        val out = WaveformProcessor.window(env, 15, 0.5f)
        for (v in out) assertEquals(0f, v)
    }

    @Test
    fun windowValuesStayInEnvelopeRange() {
        val env = FloatArray(200) { i -> (i % 32) / 32f }
        val out = WaveformProcessor.window(env, 15, 0.5f)
        for (v in out) assertTrue(v in 0f..1f)
    }

    @Test
    fun fitSegmentsRespectsWidth() {
        // 15 preferred, each 4px + 5px gap = stride 9. 100px fits (100+5)/9 = 11.
        assertEquals(11, WaveformProcessor.fitSegments(100f, 4f, 5f, 15))
        // Large width caps at preferred.
        assertEquals(15, WaveformProcessor.fitSegments(1000f, 4f, 5f, 15))
        // Zero width returns 0.
        assertEquals(0, WaveformProcessor.fitSegments(0f, 4f, 5f, 15))
        // Below one segment still returns at least 1.
        assertEquals(1, WaveformProcessor.fitSegments(3f, 4f, 5f, 15))
    }
}
