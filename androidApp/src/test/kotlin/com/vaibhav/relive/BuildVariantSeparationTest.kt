package com.vaibhav.relive

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildVariantSeparationTest {
    @Test
    fun testStoreKeysAreAcceptedOnlyByDemoBuilds() {
        val configuredKey = BuildConfig.REVENUECAT_PUBLIC_API_KEY
        if (configuredKey.startsWith("test_")) {
            assertTrue(BuildConfig.IS_DEMO || BuildConfig.IS_FRIENDS)
        } else if (!BuildConfig.IS_DEMO && !BuildConfig.IS_FRIENDS) {
            assertFalse(configuredKey.startsWith("test_"))
        }
    }

    @Test
    fun onlyDemoBuildHasAnExpirationCutoff() {
        if (BuildConfig.IS_DEMO) {
            assertTrue(BuildConfig.DEMO_EXPIRES_AT_EPOCH_MILLIS == 1_793_471_400_000L)
        } else {
            assertTrue(BuildConfig.DEMO_EXPIRES_AT_EPOCH_MILLIS == 0L)
        }
    }
}
