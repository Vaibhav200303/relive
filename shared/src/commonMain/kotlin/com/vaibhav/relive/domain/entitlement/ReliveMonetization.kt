package com.vaibhav.relive.domain.entitlement

import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.TimelineWallpaper

/** Central launch configuration. Change product IDs or Free appearance here, not in feature UI. */
object ReliveMonetization {
    const val entitlementId = "relive_pro"
    const val monthlyProductId = "monthly"
    const val annualProductId = "yearly"
    const val lifetimeProductId = "lifetime"
    const val freeCustomTimelineLimit = 3

    val freePalettes = setOf(ThemeReference.InkLilac, ThemeReference.TealSaffron)
    val freeWallpapers = setOf(TimelineWallpaper.WarmCream, TimelineWallpaper.SageGreen)
}

enum class RelivePurchaseOption(val productId: String) {
    Monthly(ReliveMonetization.monthlyProductId),
    Annual(ReliveMonetization.annualProductId),
    Lifetime(ReliveMonetization.lifetimeProductId),
}

internal fun relivePurchaseOptionForProductId(productId: String): RelivePurchaseOption? =
    RelivePurchaseOption.entries.firstOrNull { it.productId == productId }

data class EntitlementState(
    val isPro: Boolean = false,
    val purchasingAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val localizedPrices: Map<RelivePurchaseOption, String> = emptyMap(),
    val products: Map<RelivePurchaseOption, RelivePurchaseProduct> = emptyMap(),
)

/** Store-localized information shown before a customer starts a purchase. */
data class RelivePurchaseProduct(
    val price: String,
    val period: String?,
    val introductoryOffer: String? = null,
)

/** Release-supplied destinations required on every purchase surface. */
data class ReliveLegalLinks(
    val termsOfServiceUrl: String = "",
    val privacyPolicyUrl: String = "",
) {
    val areConfigured: Boolean
        get() = termsOfServiceUrl.isNotBlank() && privacyPolicyUrl.isNotBlank()
}

sealed interface PurchaseOutcome {
    data object Succeeded : PurchaseOutcome
    data object Cancelled : PurchaseOutcome
    data class Unavailable(val message: String) : PurchaseOutcome
    data class Failed(val message: String) : PurchaseOutcome
}

interface EntitlementProvider {
    val state: kotlinx.coroutines.flow.StateFlow<EntitlementState>
    suspend fun purchase(option: RelivePurchaseOption): PurchaseOutcome
    suspend fun restorePurchases(): PurchaseOutcome
}

class EntitlementPolicy(private val state: EntitlementState) {
    fun mayCreateCustomTimeline(existingCount: Int): Boolean = state.isPro || existingCount < ReliveMonetization.freeCustomTimelineLimit
    fun maySelectPalette(value: ThemeReference): Boolean = state.isPro || value in ReliveMonetization.freePalettes
    fun maySelectWallpaper(value: TimelineWallpaper): Boolean = state.isPro || value in ReliveMonetization.freeWallpapers
    fun mayScheduleBackup(): Boolean = state.isPro
}
