package com.vaibhav.relive.domain.entitlement

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.freePhase
import com.revenuecat.purchases.kmp.models.introPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** RevenueCat implementation; all feature gates continue to depend only on [EntitlementProvider]. */
class RevenueCatEntitlementProvider(private val apiKey: String, enableDebugLogging: Boolean = false) : EntitlementProvider {
    init {
        if (enableDebugLogging) Purchases.logLevel = LogLevel.DEBUG
    }

    private val purchases = Purchases.configure(apiKey)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(EntitlementState(purchasingAvailable = true, isLoading = true))
    override val state: StateFlow<EntitlementState> = _state.asStateFlow()

    init {
        refresh()
    }

    override suspend fun purchase(option: RelivePurchaseOption): PurchaseOutcome = runCatching {
        _state.value = _state.value.copy(isLoading = true, message = null)
        val offering = purchases.awaitOfferings().current
            ?: return unavailable("Relive Pro is not configured for this store yet.")
        val packageToPurchase = offering.availablePackages.firstOrNull { it.storeProduct.id == option.productId }
            ?: return unavailable("This Relive Pro option is not available in your store.")
        purchases.awaitPurchase(packageToPurchase)
        refresh()
        PurchaseOutcome.Succeeded
    }.getOrElse { error ->
        if (error is PurchasesTransactionException && error.userCancelled) cancelled()
        else failed(error.safeMessage(apiKey, "Purchase could not be completed."))
    }

    override suspend fun restorePurchases(): PurchaseOutcome = runCatching {
        _state.value = _state.value.copy(isLoading = true, message = null)
        purchases.awaitRestore()
        refresh()
        PurchaseOutcome.Succeeded
    }.getOrElse { error ->
        failed(error.safeMessage(apiKey, "Purchases could not be restored."))
    }

    private fun refresh() {
        scope.launchRefresh()
    }

    private fun CoroutineScope.launchRefresh() = launch {
        runCatching {
            val offering = purchases.awaitOfferings().current
            val info = purchases.awaitCustomerInfo()
            info to offering?.availablePackages.orEmpty().mapNotNull { packageInfo ->
                relivePurchaseOptionForProductId(packageInfo.storeProduct.id)
                    ?.let { option -> option to packageInfo.storeProduct.toPurchaseProduct() }
            }.toMap()
        }
            .onSuccess { (info, products) ->
                _state.value = EntitlementState(
                    isPro = info.entitlements[ReliveMonetization.entitlementId]?.isActive == true,
                    purchasingAvailable = true,
                    localizedPrices = products.mapValues { it.value.price },
                    products = products,
                )
            }
            .onFailure { error ->
                _state.value = offeringsFailureState(error.safeMessage(apiKey, "Could not check Relive Pro right now."))
            }
    }

    private fun unavailable(message: String): PurchaseOutcome.Unavailable {
        _state.value = _state.value.copy(isLoading = false, message = message)
        return PurchaseOutcome.Unavailable(message)
    }

    private fun failed(message: String): PurchaseOutcome.Failed {
        _state.value = _state.value.copy(isLoading = false, message = message)
        return PurchaseOutcome.Failed(message)
    }

    private fun cancelled(): PurchaseOutcome.Cancelled {
        _state.value = _state.value.copy(isLoading = false)
        return PurchaseOutcome.Cancelled
    }
}

internal fun offeringsFailureState(message: String): EntitlementState = EntitlementState(
    purchasingAvailable = true,
    message = message,
)

private fun Throwable.safeMessage(apiKey: String, fallback: String): String =
    message?.takeIf { it.isNotBlank() }?.replace(apiKey, "[redacted]") ?: fallback

private fun com.revenuecat.purchases.kmp.models.StoreProduct.toPurchaseProduct(): RelivePurchaseProduct {
    val androidOption = defaultOption
    val trial = androidOption?.freePhase?.let { "${it.billingPeriod.readable()} free trial" }
        ?: androidOption?.introPhase?.let { "${it.price.formatted} for ${it.billingPeriod.readable()}" }
        ?: introductoryDiscount?.let { discount ->
            if (discount.price.amountMicros == 0L) "${discount.subscriptionPeriod.readable()} free trial"
            else "${discount.price.formatted} for ${discount.subscriptionPeriod.readable()}"
        }
    return RelivePurchaseProduct(price = price.formatted, period = period?.readable(), introductoryOffer = trial)
}

private fun com.revenuecat.purchases.kmp.models.Period.readable(): String {
    val unit = when (unit.name) {
        "DAY" -> "day"
        "WEEK" -> "week"
        "MONTH" -> "month"
        "YEAR" -> "year"
        else -> return "$value ${unit.name.lowercase()}"
    }
    return "$value $unit${if (value == 1) "" else "s"}"
}
