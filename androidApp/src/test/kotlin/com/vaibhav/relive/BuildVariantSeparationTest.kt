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
}
