package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.entitlement.RelivePurchaseOption
import com.vaibhav.relive.domain.entitlement.RelivePurchaseProduct

internal fun defaultRelivePurchaseOption(
    products: Map<RelivePurchaseOption, RelivePurchaseProduct>,
): RelivePurchaseOption? = when {
    RelivePurchaseOption.Annual in products -> RelivePurchaseOption.Annual
    else -> RelivePurchaseOption.entries.firstOrNull { it in products }
}

internal fun relivePurchaseCtaLabel(
    option: RelivePurchaseOption,
    product: RelivePurchaseProduct,
): String = when {
    !product.introductoryOffer.isNullOrBlank() -> "Try for free"
    option == RelivePurchaseOption.Lifetime -> "Unlock forever"
    else -> "Continue"
}

internal fun relivePurchaseOptionToSubmit(
    selected: RelivePurchaseOption?,
    products: Map<RelivePurchaseOption, RelivePurchaseProduct>,
): RelivePurchaseOption? = selected?.takeIf { it in products }

internal enum class ReliveProFeature(
    val headline: String,
    val supportingText: String,
) {
    AutomaticBackup(
        headline = "Keep your archive protected",
        supportingText = "Schedule automatic backups and choose when your memories are secured.",
    ),
    UnlimitedTimelines(
        headline = "Create every chapter",
        supportingText = "Build unlimited timelines for every season, person, place, and story.",
    ),
    PremiumAppearance(
        headline = "Make every timeline yours",
        supportingText = "Unlock every premium palette and wallpaper in your private archive.",
    ),
}
