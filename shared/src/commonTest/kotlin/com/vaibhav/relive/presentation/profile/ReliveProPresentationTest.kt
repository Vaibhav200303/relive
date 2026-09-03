package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.entitlement.RelivePurchaseOption
import com.vaibhav.relive.domain.entitlement.RelivePurchaseProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReliveProPresentationTest {
    @Test
    fun annualIsTheDefaultWhenItIsAvailable() {
        val products = mapOf(
            RelivePurchaseOption.Monthly to product("\$4.99", "1 month"),
            RelivePurchaseOption.Annual to product("\$39.99", "1 year"),
            RelivePurchaseOption.Lifetime to product("\$99.99", null),
        )

        assertEquals(RelivePurchaseOption.Annual, defaultRelivePurchaseOption(products))
    }

    @Test
    fun firstAvailablePlanIsUsedWhenAnnualIsUnavailable() {
        val products = mapOf(
            RelivePurchaseOption.Lifetime to product("\$99.99", null),
            RelivePurchaseOption.Monthly to product("\$4.99", "1 month"),
        )

        assertEquals(RelivePurchaseOption.Monthly, defaultRelivePurchaseOption(products))
        assertNull(defaultRelivePurchaseOption(emptyMap()))
    }

    @Test
    fun introductoryOfferUsesTrialCallToAction() {
        val annual = product("\$39.99", "1 year", "7 days free, then \$39.99 per year")

        assertEquals("Try for free", relivePurchaseCtaLabel(RelivePurchaseOption.Annual, annual))
    }

    @Test
    fun lifetimeAndStandardSubscriptionsUseTheirOwnCallsToAction() {
        assertEquals(
            "Unlock forever",
            relivePurchaseCtaLabel(RelivePurchaseOption.Lifetime, product("\$99.99", null)),
        )
        assertEquals(
            "Continue",
            relivePurchaseCtaLabel(RelivePurchaseOption.Monthly, product("\$4.99", "1 month")),
        )
    }

    @Test
    fun purchaseSubmissionReturnsExactlyTheSelectedAvailablePlan() {
        val products = mapOf(
            RelivePurchaseOption.Monthly to product("\$4.99", "1 month"),
            RelivePurchaseOption.Annual to product("\$39.99", "1 year"),
        )

        assertEquals(
            RelivePurchaseOption.Monthly,
            relivePurchaseOptionToSubmit(RelivePurchaseOption.Monthly, products),
        )
        assertNull(relivePurchaseOptionToSubmit(RelivePurchaseOption.Lifetime, products))
        assertNull(relivePurchaseOptionToSubmit(null, products))
    }

    private fun product(
        price: String,
        period: String?,
        offer: String? = null,
    ) = RelivePurchaseProduct(price = price, period = period, introductoryOffer = offer)
}
