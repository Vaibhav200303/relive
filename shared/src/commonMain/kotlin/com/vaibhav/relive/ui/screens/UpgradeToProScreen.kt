package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

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

    fun restorePurchases() {
        if (!state.purchasingAvailable || state.isLoading) return
        purchaseMessage = "Restoring purchases…"
        scope.launch {
            purchaseMessage = entitlementProvider.restorePurchases().restoreMessageOrNull()
        }
    }

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
            ProHero()
            ProFeatureShowcase()
            if (state.isPro) {
                ActiveProCard()
                RestorePurchases(
                    enabled = state.purchasingAvailable && !state.isLoading,
                    onRestore = ::restorePurchases,
                )
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
                PurchaseAssurances()
                RestorePurchases(
                    enabled = state.purchasingAvailable && !state.isLoading,
                    onRestore = ::restorePurchases,
                )
                RenewalCopy()
                LegalLinks(legalLinks, uriHandler::openUri)

                if (!state.isLoading && state.purchasingAvailable && state.message == null && state.products.isEmpty()) {
                    ProStatusMessage("Relive Pro products are not configured for this store yet.")
                }
                if (!legalLinks.areConfigured) {
                    ProStatusMessage("Purchases are unavailable until the Terms of Service and Privacy Policy links are configured.")
                }
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
private fun ProHero() {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.tint)
                    .padding(horizontal = dims.spacing.md, vertical = dims.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = ProfileIcons.Crown,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(dims.icon.sm),
                )
                Text("RELIVE PRO", style = ReliveTheme.typography.eyebrow, color = colors.accent)
            }
            Text(
                text = "More room\nfor what matters",
                style = ReliveTheme.typography.title,
                color = colors.textPrimary,
                modifier = Modifier
                    .padding(top = dims.spacing.sm)
                    .semantics { heading() },
            )
            Text(
                text = "Unlock powerful features to make your memories safer, richer, and yours.",
                style = ReliveTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = dims.spacing.xs),
            )
        }
        Spacer(Modifier.width(dims.spacing.sm))
        ProHeroArtwork()
    }
}

@Composable
private fun ProHeroArtwork() {
    val dims = ReliveTheme.dimensions
    val pro = dims.pro
    val colors = ReliveTheme.colors
    Box(
        modifier = Modifier.size(pro.heroArtworkWidth, pro.heroArtworkHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset(x = dims.spacing.sm, y = dims.spacing.md)
                .rotate(8f)
                .size(pro.heroPhotoWidth, pro.heroPhotoHeight)
                .clip(RoundedCornerShape(dims.radii.small))
                .background(colors.surfaceCard)
                .border(dims.stroke.cardOuter, colors.borderMuted, RoundedCornerShape(dims.radii.small)),
        )
        Box(
            modifier = Modifier
                .offset(x = -dims.spacing.sm, y = dims.spacing.lg)
                .rotate(-8f)
                .size(pro.heroPhotoWidth, pro.heroPhotoHeight)
                .clip(RoundedCornerShape(dims.radii.small))
                .background(colors.surfaceCard)
                .border(dims.stroke.cardOuter, colors.border, RoundedCornerShape(dims.radii.small))
                .padding(dims.spacing.xs),
            contentAlignment = Alignment.TopCenter,
        ) {
            ProLandscapeThumbnail(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pro.heroPhotoImageHeight)
                    .clip(RoundedCornerShape(dims.radii.xs)),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(pro.crownBadgeSize)
                .clip(CircleShape)
                .background(colors.surfaceCard)
                .border(dims.stroke.iconBold, colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ProfileIcons.Crown,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(dims.icon.md),
            )
        }
    }
}

@Composable
private fun ProLandscapeThumbnail(modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    Canvas(modifier = modifier.background(colors.tint)) {
        drawCircle(
            color = colors.spark.copy(alpha = 0.34f),
            radius = size.minDimension * 0.10f,
            center = Offset(size.width * 0.72f, size.height * 0.28f),
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, size.height * 0.76f)
                lineTo(size.width * 0.30f, size.height * 0.49f)
                lineTo(size.width * 0.52f, size.height * 0.67f)
                lineTo(size.width * 0.73f, size.height * 0.43f)
                lineTo(size.width, size.height * 0.70f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
            color = colors.accentMuted.copy(alpha = 0.42f),
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, size.height * 0.88f)
                lineTo(size.width * 0.23f, size.height * 0.68f)
                lineTo(size.width * 0.45f, size.height * 0.80f)
                lineTo(size.width * 0.68f, size.height * 0.59f)
                lineTo(size.width, size.height * 0.82f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
            color = colors.accentMuted.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun ProFeatureShowcase() {
    val features = ReliveProFeature.entries
    val initialPage = features.indexOf(ReliveProFeature.AutomaticBackup)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { features.size })
    val reduceMotion = ReliveTheme.reduceMotion
    val motion = ReliveTheme.motion
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val featureHeight = dims.pro.featureStageHeight * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = motion.reliveLateralPagerSnapSpec(reduceMotion),
    )
    LaunchedEffect(pagerState) {
        if (pagerState.currentPage != initialPage) {
            pagerState.scrollToPage(initialPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dims.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            flingBehavior = fling,
            contentPadding = PaddingValues(horizontal = dims.pro.featurePeekInset),
            pageSpacing = dims.spacing.sm,
            modifier = Modifier
                .fillMaxWidth()
                .height(featureHeight),
        ) { page ->
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue.coerceIn(0f, 1f)
            val focus = 1f - pageOffset
            ProFeatureCard(
                feature = features[page],
                focus = focus,
                modifier = Modifier
                    .padding(vertical = dims.pro.featureFocusInset)
                    .graphicsLayer {
                        val focusedScale = dims.pro.featureRestingScale +
                            ((1f - dims.pro.featureRestingScale) * focus)
                        scaleX = focusedScale
                        scaleY = focusedScale
                        alpha = dims.pro.featureRestingAlpha +
                            ((1f - dims.pro.featureRestingAlpha) * focus)
                    }
                    .zIndex(focus),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
            modifier = Modifier
                .padding(top = dims.spacing.md)
                .semantics {
                    contentDescription = "Feature ${pagerState.currentPage + 1} of ${features.size}"
                },
        ) {
            features.indices.forEach { index ->
                Box(
                    Modifier
                        .size(
                            if (index == pagerState.currentPage) {
                                dims.timeline.dotSize
                            } else {
                                dims.spacing.sm
                            },
                        )
                        .clip(CircleShape)
                        .background(if (index == pagerState.currentPage) colors.accent else colors.border),
                )
            }
        }
    }
}

@Composable
private fun ProFeatureCard(
    feature: ReliveProFeature,
    focus: Float,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(dims.radii.largeIncreased))
            .background(colors.surfaceCard)
            .border(dims.stroke.cardOuter, colors.borderMuted, RoundedCornerShape(dims.radii.largeIncreased))
            .padding(horizontal = dims.spacing.lg, vertical = dims.spacing.lg)
            .semantics { contentDescription = "${feature.headline}. ${feature.supportingText}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FeatureIcon(feature.icon)
        Spacer(Modifier.height(dims.spacing.md))
        Text(
            text = feature.headline,
            style = ReliveTheme.typography.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = feature.supportingText,
            style = ReliveTheme.typography.caption,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = dims.spacing.xs),
        )
        Spacer(Modifier.weight(1f))
        ProFeatureStory(
            feature = feature,
            modifier = Modifier.graphicsLayer {
                alpha = ((focus - 0.32f) / 0.68f).coerceIn(0f, 1f)
                translationY = (1f - focus) * dims.pro.featureStoryTravel.toPx()
            },
        )
    }
}

@Composable
private fun ProFeatureStory(
    feature: ReliveProFeature,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dims.pro.featureStoryHeight),
        contentAlignment = Alignment.Center,
    ) {
        when (feature) {
            ReliveProFeature.AutomaticBackup -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                ) {
                    StoryBubble(icon = ProfileIcons.Phone, small = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(if (index == 2) dims.spacing.sm else dims.spacing.xs)
                                    .clip(CircleShape)
                                    .background(if (index == 2) colors.accent else colors.accentMuted),
                            )
                        }
                    }
                    StoryBubble(icon = ProfileIcons.CloudOutline)
                }
            }

            ReliveProFeature.UnlimitedTimelines -> {
                listOf(-dims.spacing.xl to -8f, dims.spacing.none to 0f, dims.spacing.xl to 8f)
                    .forEachIndexed { index, (offset, rotation) ->
                        Column(
                            modifier = Modifier
                                .offset(x = offset)
                                .rotate(rotation)
                                .size(dims.pro.featureStoryCardWidth, dims.pro.featureStoryHeight)
                                .clip(RoundedCornerShape(dims.radii.medium))
                                .background(if (index == 1) colors.surfaceCard else colors.tint)
                                .border(dims.stroke.cardOuter, colors.border, RoundedCornerShape(dims.radii.medium))
                                .padding(dims.spacing.sm),
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(dims.spacing.xs)
                                    .clip(CircleShape)
                                    .background(colors.accentMuted),
                            )
                            Spacer(Modifier.height(dims.spacing.xs))
                            Box(
                                Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(dims.spacing.xs)
                                    .clip(CircleShape)
                                    .background(colors.border),
                            )
                        }
                    }
            }

            ReliveProFeature.PremiumAppearance -> {
                Row(horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm)) {
                    listOf(colors.accent, colors.spark, colors.accentMuted, colors.tint).forEach { color ->
                        Box(
                            Modifier
                                .size(dims.icon.lg)
                                .clip(CircleShape)
                                .background(color)
                                .border(dims.stroke.hairline, colors.border, CircleShape),
                        )
                    }
                }
            }

            ReliveProFeature.KeepsakeExports -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
                ) {
                    StoryBubble(icon = ProfileIcons.Pdf, small = true)
                    Icon(
                        imageVector = ProfileIcons.Export,
                        contentDescription = null,
                        tint = colors.accentMuted,
                        modifier = Modifier.size(dims.icon.md),
                    )
                    StoryBubble(icon = ProfileIcons.Archive)
                }
            }
        }
    }
}

@Composable
private fun StoryBubble(icon: ImageVector, small: Boolean = false) {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .size(if (small) dims.pro.featureStoryBubbleSmall else dims.pro.featureStoryBubble)
            .clip(if (small) RoundedCornerShape(dims.radii.medium) else CircleShape)
            .background(ReliveTheme.colors.tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ReliveTheme.colors.accent,
            modifier = Modifier.size(if (small) dims.icon.md else dims.icon.lg),
        )
    }
}

@Composable
private fun FeatureIcon(icon: ImageVector) {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = Modifier
            .size(dims.pro.featureIconSurfaceSize)
            .clip(RoundedCornerShape(dims.radii.largeIncreased))
            .background(ReliveTheme.colors.tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ReliveTheme.colors.accent,
            modifier = Modifier.size(dims.pro.featureIconSize),
        )
    }
}

private val ReliveProFeature.icon: ImageVector
    get() = when (this) {
        ReliveProFeature.UnlimitedTimelines -> ProfileIcons.Layers
        ReliveProFeature.AutomaticBackup -> ProfileIcons.CloudOutline
        ReliveProFeature.PremiumAppearance -> ProfileIcons.Palette
        ReliveProFeature.KeepsakeExports -> ProfileIcons.Pdf
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
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        Text(
            "Choose your plan",
            style = ReliveTheme.typography.title,
            color = ReliveTheme.colors.textPrimary,
            modifier = Modifier
                .padding(bottom = dims.spacing.xs)
                .semantics { heading() },
        )
        RelivePurchaseOption.entries.forEach { option ->
            PlanCard(
                option = option,
                product = products[option],
                selected = option == selectedOption,
                onSelect = { onSelect(option) },
            )
        }
        Spacer(Modifier.height(dims.spacing.xs))
        Button(
            enabled = canPurchase && selectedProduct != null,
            onClick = onPurchase,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ReliveTheme.colors.accent,
                contentColor = ReliveTheme.colors.textOnAccent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dims.pro.primaryActionHeight),
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
    val shape = RoundedCornerShape(dims.radii.medium)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.pro.planRowMinHeight)
            .clip(shape)
            .background(if (selected) colors.tint else colors.surfaceCard)
            .border(
                width = if (selected) dims.stroke.icon else dims.stroke.cardOuter,
                color = if (selected) colors.accent else colors.borderMuted,
                shape = shape,
            )
            .selectable(
                selected = selected,
                enabled = product != null,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = dims.spacing.lg, vertical = dims.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionIndicator(selected)
        Spacer(Modifier.width(dims.spacing.lg))
        Column(Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(option.displayName, style = ReliveTheme.typography.body, color = colors.textPrimary)
                if (option == RelivePurchaseOption.Annual) PlanBadge("Best value")
            }
            Text(
                product?.period ?: option.defaultPeriod,
                style = ReliveTheme.typography.tag,
                color = colors.textMuted,
            )
        }
        product?.price?.let { price ->
            Column(horizontalAlignment = Alignment.End) {
                Text(price, style = ReliveTheme.typography.body, color = colors.textPrimary)
                Text(option.billingCaption, style = ReliveTheme.typography.tag, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Box(
        modifier = Modifier
            .size(dims.pro.selectionIndicatorSize)
            .clip(CircleShape)
            .border(
                if (selected) dims.stroke.iconBold else dims.stroke.icon,
                if (selected) colors.accent else colors.textMuted,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(dims.pro.selectionIndicatorDotSize)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
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
private fun PurchaseAssurances() {
    val items = listOf(
        ProfileIcons.Lock to "Secure purchase",
        ProfileIcons.Security to "Cancel anytime",
        ProfileIcons.Favorite to "Private by design",
    )
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        items.forEach { (icon, label) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = ReliveTheme.colors.accent,
                    modifier = Modifier.size(dims.icon.md),
                )
                Text(
                    label,
                    style = ReliveTheme.typography.tag,
                    color = ReliveTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RenewalCopy() {
    Text(
        "Subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period.",
        style = ReliveTheme.typography.tag,
        color = ReliveTheme.colors.textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(
            horizontal = ReliveTheme.dimensions.spacing.xxl,
            vertical = ReliveTheme.dimensions.spacing.lg,
        ),
    )
}

@Composable
private fun RestorePurchases(enabled: Boolean, onRestore: () -> Unit) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f), color = ReliveTheme.colors.borderMuted)
        TextButton(
            enabled = enabled,
            onClick = onRestore,
            modifier = Modifier.heightIn(min = dims.minTouchTarget),
        ) {
            Text("Restore purchases", style = ReliveTheme.typography.tag)
        }
        HorizontalDivider(Modifier.weight(1f), color = ReliveTheme.colors.borderMuted)
    }
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
            "Automatic backup, unlimited timelines, every premium appearance, and Keepsake exports are unlocked.",
            style = ReliveTheme.typography.body,
            color = ReliveTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LegalLinks(links: ReliveLegalLinks, openUri: (String) -> Unit) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dims.spacing.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            enabled = links.termsOfServiceUrl.isNotBlank(),
            onClick = { openUri(links.termsOfServiceUrl) },
        ) {
            Text("Terms of Service", style = ReliveTheme.typography.tag)
        }
        Text("•", style = ReliveTheme.typography.tag, color = ReliveTheme.colors.textMuted)
        TextButton(
            enabled = links.privacyPolicyUrl.isNotBlank(),
            onClick = { openUri(links.privacyPolicyUrl) },
        ) {
            Text("Privacy Policy", style = ReliveTheme.typography.tag)
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
        modifier = Modifier.padding(
            horizontal = ReliveTheme.dimensions.spacing.xl,
            vertical = ReliveTheme.dimensions.spacing.xs,
        ),
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

private val RelivePurchaseOption.billingCaption: String
    get() = when (this) {
        RelivePurchaseOption.Monthly -> "per month"
        RelivePurchaseOption.Annual -> "per year"
        RelivePurchaseOption.Lifetime -> "One-time"
    }

private fun PurchaseOutcome.messageOrNull(): String? = when (this) {
    PurchaseOutcome.Succeeded -> null
    PurchaseOutcome.Cancelled -> null
    is PurchaseOutcome.Unavailable -> message
    is PurchaseOutcome.Failed -> message
}

private fun PurchaseOutcome.restoreMessageOrNull(): String? = when (this) {
    PurchaseOutcome.Succeeded -> "Purchases restored successfully."
    PurchaseOutcome.Cancelled -> null
    is PurchaseOutcome.Unavailable -> message
    is PurchaseOutcome.Failed -> message
}
