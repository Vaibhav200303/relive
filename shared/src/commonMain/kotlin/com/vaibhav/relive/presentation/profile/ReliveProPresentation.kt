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
    UnlimitedTimelines(
        headline = "More timelines",
        supportingText = "Create as many timelines as your stories need.",
    ),
    AutomaticBackup(
        headline = "Automatic backups",
        supportingText = "Keep your memories safe, without thinking about it.",
    ),
    PremiumAppearance(
        headline = "All appearances",
        supportingText = "Unlock every premium palette and wallpaper.",
    ),
    KeepsakeExports(
        headline = "Keepsake exports",
        supportingText = "Create beautiful PDFs and portable Relive archives.",
    ),
}
