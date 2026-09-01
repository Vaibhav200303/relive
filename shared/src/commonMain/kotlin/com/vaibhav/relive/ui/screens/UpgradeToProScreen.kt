package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.ReliveLegalLinks
import com.vaibhav.relive.domain.entitlement.RelivePurchaseProduct
import com.vaibhav.relive.domain.entitlement.RelivePurchaseOption
import com.vaibhav.relive.domain.entitlement.PurchaseOutcome
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader
import com.vaibhav.relive.ui.theme.ReliveTheme
import kotlinx.coroutines.launch

@Composable
fun UpgradeToProScreen(
    entitlementProvider: EntitlementProvider,
    legalLinks: ReliveLegalLinks,
    onBack: () -> Unit,
) {
    val state by entitlementProvider.state.collectAsState()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var purchaseMessage by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas),
    ) {
        ProfilePageHeader("Relive Pro", onBack)
        Column(
            Modifier.fillMaxWidth().padding(ReliveTheme.dimensions.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.lg),
        ) {
            Text("Protect your archive. Make every chapter yours.", style = ReliveTheme.typography.title)
            Text("Relive Pro adds scheduled backup, unlimited timelines, and every palette and wallpaper. Your existing memories always remain yours.", style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
            if (state.isPro) {
                Text("Relive Pro is active.", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.accent)
            } else {
                val canPurchase = state.purchasingAvailable && !state.isLoading && legalLinks.areConfigured
                if (!state.isLoading && state.purchasingAvailable && state.message == null && state.localizedPrices.isEmpty()) {
                    Text("Relive Pro products are not configured for this store yet.", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textMuted)
                }
                RelivePurchaseOption.entries.forEach { option ->
                    val product = state.products[option]
                    Button(
                        enabled = canPurchase && product != null,
                        onClick = { scope.launch { purchaseMessage = entitlementProvider.purchase(option).messageOrNull() } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Text(option.label(product))
                            product?.introductoryOffer?.let { Text(it, style = ReliveTheme.typography.body) }
                        }
                    }
                }
                Text("Subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period.", style = ReliveTheme.typography.body, color = ReliveTheme.colors.textMuted)
                LegalLinks(legalLinks, uriHandler::openUri)
                if (!legalLinks.areConfigured) Text("Purchases are unavailable until the Terms of Service and Privacy Policy links are configured.", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textMuted)
            }
            Button(
                enabled = state.purchasingAvailable && !state.isLoading,
                onClick = { scope.launch { purchaseMessage = entitlementProvider.restorePurchases().messageOrNull() } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restore purchases") }
            state.message?.let { Text(it, style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textMuted) }
            purchaseMessage?.let { Text(it, style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textMuted) }
            if (!state.purchasingAvailable) Text("Purchasing is unavailable until this build receives a RevenueCat public key.", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textMuted)
        }
    }
}

private fun RelivePurchaseOption.label(product: RelivePurchaseProduct?): String {
    val period = product?.period ?: defaultPeriod
    val price = product?.price?.let { " · $it" }.orEmpty()
    return "$displayName · $period$price"
}

private val RelivePurchaseOption.displayName: String
    get() = when (this) {
        RelivePurchaseOption.Monthly -> "Monthly"
        RelivePurchaseOption.Annual -> "Annual"
        RelivePurchaseOption.Lifetime -> "Lifetime"
    }

private val RelivePurchaseOption.defaultPeriod: String
    get() = when (this) {
        RelivePurchaseOption.Monthly -> "1 month"
        RelivePurchaseOption.Annual -> "1 year"
        RelivePurchaseOption.Lifetime -> "lifetime"
    }

@Composable
private fun LegalLinks(links: ReliveLegalLinks, openUri: (String) -> Unit) {
    Row {
        TextButton(enabled = links.termsOfServiceUrl.isNotBlank(), onClick = { openUri(links.termsOfServiceUrl) }) { Text("Terms of Service") }
        TextButton(enabled = links.privacyPolicyUrl.isNotBlank(), onClick = { openUri(links.privacyPolicyUrl) }) { Text("Privacy Policy") }
    }
}

private fun PurchaseOutcome.messageOrNull(): String? = when (this) {
    PurchaseOutcome.Succeeded -> null
    PurchaseOutcome.Cancelled -> "Purchase cancelled."
    is PurchaseOutcome.Unavailable -> message
    is PurchaseOutcome.Failed -> message
}
