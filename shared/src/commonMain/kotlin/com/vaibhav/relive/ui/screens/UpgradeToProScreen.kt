package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.PurchaseOutcome
import com.vaibhav.relive.domain.entitlement.ReliveLegalLinks
import com.vaibhav.relive.domain.entitlement.RelivePurchaseOption
import com.vaibhav.relive.domain.entitlement.RelivePurchaseProduct
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.profile.ReliveProFeature
import com.vaibhav.relive.presentation.profile.defaultRelivePurchaseOption
import com.vaibhav.relive.presentation.profile.relivePurchaseCtaLabel
import com.vaibhav.relive.presentation.profile.relivePurchaseOptionToSubmit
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.reliveLateralPagerSnapSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val FeatureAdvanceDelayMillis = 5_000L

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
    var selectedOption by remember { mutableStateOf<RelivePurchaseOption?>(null) }

    LaunchedEffect(state.products) {
        if (selectedOption !in state.products) {
            selectedOption = defaultRelivePurchaseOption(state.products)
        }
    }
    ReliveBackHandler(enabled = true, onBack = onBack)

    Column(
        Modifier
            .fillMaxSize()
            .background(ReliveTheme.colors.canvasBrush()),
    ) {
        ProfilePageHeader("Relive Pro", onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = ReliveTheme.dimensions.spacing.huge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProFeatureShowcase()
            if (state.isPro) {
                ActiveProCard()
            } else {
                PurchasePanel(
                    products = state.products,
                    selectedOption = selectedOption,
                    onSelect = {
                        purchaseMessage = null
                        selectedOption = it
                    },
                    canPurchase = state.purchasingAvailable && !state.isLoading && legalLinks.areConfigured,
                    isLoading = state.isLoading,
                    onPurchase = {
                        val option = relivePurchaseOptionToSubmit(selectedOption, state.products)
                            ?: return@PurchasePanel
                        scope.launch {
                            purchaseMessage = entitlementProvider.purchase(option).messageOrNull()
                        }
                    },
                )

                Text(
                    "Subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period.",
                    style = ReliveTheme.typography.tag,
                    color = ReliveTheme.colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        horizontal = ReliveTheme.dimensions.spacing.xl,
                        vertical = ReliveTheme.dimensions.spacing.lg,
                    ),
                )
                LegalLinks(legalLinks, uriHandler::openUri)

                if (!state.isLoading && state.purchasingAvailable && state.message == null && state.products.isEmpty()) {
                    ProStatusMessage("Relive Pro products are not configured for this store yet.")
                }
                if (!legalLinks.areConfigured) {
                    ProStatusMessage("Purchases are unavailable until the Terms of Service and Privacy Policy links are configured.")
                }
            }

            TextButton(
                enabled = state.purchasingAvailable && !state.isLoading,
                onClick = {
                    scope.launch {
                        purchaseMessage = entitlementProvider.restorePurchases().messageOrNull()
                    }
                },
                modifier = Modifier.heightIn(min = ReliveTheme.dimensions.minTouchTarget),
            ) {
                Text("Restore purchases")
            }
            state.message?.let { ProStatusMessage(it) }
            purchaseMessage?.let { ProStatusMessage(it) }
            if (!state.purchasingAvailable) {
                ProStatusMessage("Purchasing is unavailable until this build receives a RevenueCat public key.")
            }
        }
    }
}

@Composable
private fun ProFeatureShowcase() {
    val features = ReliveProFeature.entries
    val pagerState = rememberPagerState(pageCount = { features.size })
    val reduceMotion = ReliveTheme.reduceMotion
    val motion = ReliveTheme.motion
    val featureHeight = (292f * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = motion.reliveLateralPagerSnapSpec(reduceMotion),
    )

    LaunchedEffect(pagerState, reduceMotion) {
        if (!reduceMotion) {
            snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
                .collectLatest { (page, isScrolling) ->
                    if (!isScrolling) {
                        delay(FeatureAdvanceDelayMillis)
                        if (!pagerState.isScrollInProgress) {
                            pagerState.animateScrollToPage((page + 1) % features.size)
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ReliveTheme.dimensions.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "YOUR MEMORIES, WITHOUT LIMITS",
            style = ReliveTheme.typography.eyebrow,
            color = ReliveTheme.colors.accent,
            modifier = Modifier.semantics { heading() },
        )
        HorizontalPager(
            state = pagerState,
            flingBehavior = fling,
            modifier = Modifier
                .fillMaxWidth()
                .height(featureHeight),
        ) { page ->
            val feature = features[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ReliveTheme.dimensions.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ProFeatureIllustration(feature)
                Spacer(Modifier.height(ReliveTheme.dimensions.spacing.lg))
                Text(
                    text = feature.headline,
                    style = ReliveTheme.typography.title,
                    color = ReliveTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = feature.supportingText,
                    style = ReliveTheme.typography.body,
                    color = ReliveTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = ReliveTheme.dimensions.spacing.sm),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.sm),
            modifier = Modifier.semantics {
                contentDescription = "Feature ${pagerState.currentPage + 1} of ${features.size}"
            },
        ) {
            features.indices.forEach { index ->
                Box(
                    Modifier
                        .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) {
                                ReliveTheme.colors.accent
                            } else {
                                ReliveTheme.colors.border
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun ProFeatureIllustration(feature: ReliveProFeature) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .semantics { contentDescription = feature.headline },
        contentAlignment = Alignment.Center,
    ) {
        when (feature) {
            ReliveProFeature.AutomaticBackup -> {
                Box(
                    Modifier
                        .size(116.dp, 82.dp)
                        .clip(RoundedCornerShape(dims.radii.largeIncreased))
                        .background(colors.surfaceCard)
                        .border(dims.stroke.cardOuter, colors.border, RoundedCornerShape(dims.radii.largeIncreased)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ProfileIcons.Backup,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Box(Modifier.offset(x = 50.dp, y = 32.dp).size(24.dp).clip(CircleShape).background(colors.spark))
            }

            ReliveProFeature.UnlimitedTimelines -> {
                listOf(-42.dp to -7f, 0.dp to 0f, 42.dp to 7f).forEachIndexed { index, (offset, rotation) ->
                    Column(
                        modifier = Modifier
                            .offset(x = offset)
                            .rotate(rotation)
                            .size(72.dp, 104.dp)
                            .clip(RoundedCornerShape(dims.radii.medium))
                            .background(if (index == 1) colors.surfaceCard else colors.tint)
                            .border(dims.stroke.cardOuter, colors.border, RoundedCornerShape(dims.radii.medium))
                            .padding(dims.spacing.sm),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(colors.accentMuted))
                        Spacer(Modifier.height(dims.spacing.xs))
                        Box(Modifier.fillMaxWidth(0.7f).height(5.dp).clip(CircleShape).background(colors.border))
                    }
                }
            }

            ReliveProFeature.PremiumAppearance -> {
                Box(
                    Modifier
                        .size(164.dp, 104.dp)
                        .clip(RoundedCornerShape(dims.radii.largeIncreased))
                        .background(colors.surfaceCard)
                        .border(dims.stroke.cardOuter, colors.border, RoundedCornerShape(dims.radii.largeIncreased)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm)) {
                    listOf(colors.accent, colors.spark, colors.accentMuted, colors.tint).forEach { color ->
                        Box(Modifier.size(28.dp).clip(CircleShape).background(color).border(dims.stroke.hairline, colors.border, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchasePanel(
    products: Map<RelivePurchaseOption, RelivePurchaseProduct>,
    selectedOption: RelivePurchaseOption?,
    onSelect: (RelivePurchaseOption) -> Unit,
    canPurchase: Boolean,
    isLoading: Boolean,
    onPurchase: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val selectedProduct = selectedOption?.let(products::get)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = dims.spacing.lg, end = dims.spacing.lg, top = dims.spacing.xxl)
            .clip(RoundedCornerShape(topStart = dims.radii.xl, topEnd = dims.radii.xl))
            .background(ReliveTheme.colors.surfaceCard)
            .padding(dims.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
    ) {
        Text(
            "Choose your plan",
            style = ReliveTheme.typography.subtitle,
            color = ReliveTheme.colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        RelivePurchaseOption.entries.forEach { option ->
            PlanCard(
                option = option,
                product = products[option],
                selected = option == selectedOption,
                onSelect = { onSelect(option) },
            )
        }
        Button(
            enabled = canPurchase && selectedProduct != null,
            onClick = onPurchase,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dims.icon.md),
                    color = ReliveTheme.colors.textOnAccent,
                    strokeWidth = dims.stroke.icon,
                )
            } else {
                Text(
                    selectedProduct?.let { product ->
                        relivePurchaseCtaLabel(requireNotNull(selectedOption), product)
                    } ?: "Continue",
                    style = ReliveTheme.typography.prominentAction,
                )
            }
        }
        selectedProduct?.introductoryOffer?.takeIf { it.isNotBlank() }?.let { offer ->
            Text(
                offer,
                style = ReliveTheme.typography.tag,
                color = ReliveTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlanCard(
    option: RelivePurchaseOption,
    product: RelivePurchaseProduct?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val shape = RoundedCornerShape(dims.radii.largeIncreased)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(shape)
            .background(if (selected) colors.tint else Color.Transparent)
            .border(
                width = if (selected) dims.stroke.iconBold else dims.stroke.cardOuter,
                color = if (selected) colors.accent else colors.border,
                shape = shape,
            )
            .selectable(
                selected = selected,
                enabled = product != null,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = dims.spacing.lg, vertical = dims.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(option.displayName, style = ReliveTheme.typography.subtitle, color = colors.textPrimary)
                Text(
                    product?.period ?: option.defaultPeriod,
                    style = ReliveTheme.typography.tag,
                    color = colors.textMuted,
                )
            }
            product?.price?.let { price ->
                Text(price, style = ReliveTheme.typography.subtitle, color = colors.textPrimary)
            }
        }
        if (option == RelivePurchaseOption.Annual || !product?.introductoryOffer.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(top = dims.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
            ) {
                if (option == RelivePurchaseOption.Annual) PlanBadge("Best value")
                product?.introductoryOffer?.takeIf { it.isNotBlank() }?.let { PlanBadge("Free trial") }
            }
        }
    }
}

@Composable
private fun PlanBadge(text: String) {
    Text(
        text = text,
        style = ReliveTheme.typography.tag,
        color = ReliveTheme.colors.textOnAccent,
        modifier = Modifier
            .clip(CircleShape)
            .background(ReliveTheme.colors.accent)
            .padding(horizontal = ReliveTheme.dimensions.spacing.sm, vertical = ReliveTheme.dimensions.spacing.xs),
    )
}

@Composable
private fun ActiveProCard() {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.xxl)
            .clip(RoundedCornerShape(dims.radii.largeIncreased))
            .background(ReliveTheme.colors.surfaceCard)
            .border(dims.stroke.icon, ReliveTheme.colors.accent, RoundedCornerShape(dims.radii.largeIncreased))
            .padding(dims.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        Text("Relive Pro is active", style = ReliveTheme.typography.title, color = ReliveTheme.colors.accent)
        Text(
            "Automatic backup, unlimited timelines, and every appearance are unlocked.",
            style = ReliveTheme.typography.body,
            color = ReliveTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LegalLinks(links: ReliveLegalLinks, openUri: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ReliveTheme.dimensions.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(
            enabled = links.termsOfServiceUrl.isNotBlank(),
            onClick = { openUri(links.termsOfServiceUrl) },
        ) {
            Text("Terms of Service")
        }
        TextButton(
            enabled = links.privacyPolicyUrl.isNotBlank(),
            onClick = { openUri(links.privacyPolicyUrl) },
        ) {
            Text("Privacy Policy")
        }
    }
}

@Composable
private fun ProStatusMessage(message: String) {
    Text(
        message,
        style = ReliveTheme.typography.tag,
        color = ReliveTheme.colors.textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = ReliveTheme.dimensions.spacing.xl, vertical = ReliveTheme.dimensions.spacing.xs),
    )
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
        RelivePurchaseOption.Lifetime -> "One-time purchase"
    }

private fun PurchaseOutcome.messageOrNull(): String? = when (this) {
    PurchaseOutcome.Succeeded -> null
    PurchaseOutcome.Cancelled -> "Purchase cancelled."
    is PurchaseOutcome.Unavailable -> message
    is PurchaseOutcome.Failed -> message
}
