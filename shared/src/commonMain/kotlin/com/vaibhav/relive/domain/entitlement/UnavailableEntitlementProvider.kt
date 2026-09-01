package com.vaibhav.relive.domain.entitlement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Safe default used when a platform has no configured RevenueCat public key. */
class UnavailableEntitlementProvider(
    message: String = "Purchasing is not available in this build.",
) : EntitlementProvider {
    override val state: StateFlow<EntitlementState> = MutableStateFlow(
        EntitlementState(purchasingAvailable = false, message = message),
    )

    override suspend fun purchase(option: RelivePurchaseOption): PurchaseOutcome =
        PurchaseOutcome.Unavailable(state.value.message ?: "Purchasing is not available in this build.")

    override suspend fun restorePurchases(): PurchaseOutcome =
        PurchaseOutcome.Unavailable(state.value.message ?: "Purchasing is not available in this build.")
}
