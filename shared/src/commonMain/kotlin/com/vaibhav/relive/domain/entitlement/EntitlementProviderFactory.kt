package com.vaibhav.relive.domain.entitlement

/** Keeps unset build placeholders from reaching the RevenueCat SDK. */
fun entitlementProviderFor(
    publicApiKey: String,
    enableDebugLogging: Boolean = false,
    allowTestStore: Boolean = true,
): EntitlementProvider {
    val key = publicApiKey.trim()
    return if (!isRevenueCatKeyUsable(key, allowTestStore)) {
        println("RevenueCat is not configured: set the platform public API key for development purchases.")
        UnavailableEntitlementProvider()
    } else {
        RevenueCatEntitlementProvider(key, enableDebugLogging)
    }
}

internal fun isRevenueCatKeyUsable(key: String, allowTestStore: Boolean): Boolean =
    key.isNotBlank() &&
        !key.startsWith("RELIVE_REVENUECAT_") &&
        !key.startsWith("sk_") &&
        key.substringBefore('_') in setOf("test", "goog", "appl", "amzn", "rcb") &&
        (allowTestStore || !key.startsWith("test_"))
