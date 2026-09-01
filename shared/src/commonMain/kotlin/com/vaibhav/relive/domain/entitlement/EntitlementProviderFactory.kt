package com.vaibhav.relive.domain.entitlement

/** Keeps unset build placeholders from reaching the RevenueCat SDK. */
fun entitlementProviderFor(publicApiKey: String, enableDebugLogging: Boolean = false): EntitlementProvider {
    val key = publicApiKey.trim()
    return if (key.isBlank() || key.startsWith("RELIVE_REVENUECAT_")) {
        println("RevenueCat is not configured: set the platform public API key for development purchases.")
        UnavailableEntitlementProvider()
    } else {
        RevenueCatEntitlementProvider(key, enableDebugLogging)
    }
}
