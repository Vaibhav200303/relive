package com.vaibhav.relive.domain.entitlement

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.freePhase
import com.revenuecat.purchases.kmp.models.introPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** RevenueCat implementation; all feature gates continue to depend only on [EntitlementProvider]. */
class RevenueCatEntitlementProvider(private val apiKey: String, enableDebugLogging: Boolean = false) : EntitlementProvider {
    init {
        if (enableDebugLogging) Purchases.logLevel = LogLevel.DEBUG
    }

    private val purchases = Purchases.configure(apiKey)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val refreshMutex = Mutex()
    private val _state = MutableStateFlow(EntitlementState(purchasingAvailable = true, isLoading = true))
    override val state: StateFlow<EntitlementState> = _state.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    override suspend fun purchase(option: RelivePurchaseOption): PurchaseOutcome = runCatching {
        _state.value = _state.value.copy(isLoading = true, message = null)
        val offering = purchases.awaitOfferings().current
            ?: return unavailable("Relive Pro is not configured for this store yet.")
        val packageToPurchase = offering.availablePackages.firstOrNull {
            relivePurchaseOptionForPackage(it.identifier, it.storeProduct.id) == option
        }
            ?: return unavailable("This Relive Pro option is not available in your store.")
        val customerInfo = purchases.awaitPurchase(packageToPurchase).customerInfo
        updateCustomerInfo(customerInfo)
        if (_state.value.isPro) {
            PurchaseOutcome.Succeeded
        } else {
            failed("Your purchase is still pending or could not be verified yet. Relive Pro will unlock after the store confirms it.")
        }
    }.getOrElse { error ->
        if (error is PurchasesTransactionException && error.userCancelled) cancelled()
        else failed("Purchase could not be completed. Check your connection and try again.")
    }

    override suspend fun restorePurchases(): PurchaseOutcome = runCatching {
        _state.value = _state.value.copy(isLoading = true, message = null)
        val customerInfo = purchases.awaitRestore()
        updateCustomerInfo(customerInfo)
        if (_state.value.isPro) {
            PurchaseOutcome.Succeeded
        } else {
            unavailable("No active Relive Pro purchase was found for this store account.")
        }
    }.getOrElse {
        failed("Purchases could not be restored. Check your connection and try again.")
    }

    override suspend fun refresh() = refreshMutex.withLock {
        val previous = _state.value
        _state.value = previous.copy(isLoading = true, message = null)
        runCatching {
            val offering = purchases.awaitOfferings().current
            val info = purchases.awaitCustomerInfo()
            info to offering?.availablePackages.orEmpty().mapNotNull { packageInfo ->
                relivePurchaseOptionForPackage(packageInfo.identifier, packageInfo.storeProduct.id)
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
            .onFailure {
                _state.value = offeringsFailureState(previous, "Could not check Relive Pro right now. Check your connection and try again.")
            }
        Unit
    }

    private fun updateCustomerInfo(customerInfo: CustomerInfo) {
        _state.value = _state.value.copy(
            isPro = customerInfo.entitlements[ReliveMonetization.entitlementId]?.isActive == true,
            purchasingAvailable = true,
            isLoading = false,
            message = null,
        )
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

internal fun offeringsFailureState(previous: EntitlementState, message: String): EntitlementState = previous.copy(
    purchasingAvailable = true,
    isLoading = false,
    message = message,
)

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
