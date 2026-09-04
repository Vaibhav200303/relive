package com.vaibhav.relive.domain.entitlement

import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.TimelineWallpaper
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EntitlementPolicyTest {
    private val free = EntitlementPolicy(EntitlementState())
    private val pro = EntitlementPolicy(EntitlementState(isPro = true))

    @Test fun freeTierAllowsThreeCustomTimelinesButNotAFourth() {
        assertTrue(free.mayCreateCustomTimeline(2))
        assertFalse(free.mayCreateCustomTimeline(3))
    }

    @Test fun proAllowsUnlimitedTimelinesAndScheduledBackup() {
        assertTrue(pro.mayCreateCustomTimeline(100))
        assertTrue(pro.mayScheduleBackup())
    }

    @Test fun freeTierOnlyAllowsApprovedAppearance() {
        assertEquals(setOf(ThemeReference.Sunrise, ThemeReference.Sunset), ReliveMonetization.freePalettes)
        assertEquals(setOf(TimelineWallpaper.WarmCream, TimelineWallpaper.BlushPink), ReliveMonetization.freeWallpapers)
        assertTrue(free.maySelectPalette(ThemeReference.Sunrise))
        assertTrue(free.maySelectPalette(ThemeReference.Sunset))
        assertFalse(free.maySelectPalette(ThemeReference.InkLilac))
        assertFalse(free.maySelectPalette(ThemeReference.EmberAqua))
        assertTrue(free.maySelectWallpaper(TimelineWallpaper.WarmCream))
        assertTrue(free.maySelectWallpaper(TimelineWallpaper.BlushPink))
        assertFalse(free.maySelectWallpaper(TimelineWallpaper.SageGreen))
    }

    @Test fun purchasesRequireBothConfiguredLegalLinks() {
        assertFalse(ReliveLegalLinks().areConfigured)
        assertFalse(ReliveLegalLinks(termsOfServiceUrl = "https://example.com/terms").areConfigured)
        assertTrue(
            ReliveLegalLinks(
                termsOfServiceUrl = "https://example.com/terms",
                privacyPolicyUrl = "https://example.com/privacy",
            ).areConfigured,
        )
    }

    @Test fun testStoreProductIdsMapToTheThreePurchaseOptions() {
        assertEquals(RelivePurchaseOption.Monthly, relivePurchaseOptionForProductId("monthly"))
        assertEquals(RelivePurchaseOption.Annual, relivePurchaseOptionForProductId("yearly"))
        assertEquals(RelivePurchaseOption.Lifetime, relivePurchaseOptionForProductId("lifetime"))
    }

    @Test fun mismatchedProductIdsAreIgnored() {
        assertNull(relivePurchaseOptionForProductId("relive_pro_monthly"))
        assertNull(relivePurchaseOptionForProductId("monthly_extra"))
    }

    @Test fun entitlementUsesTheExactReliveProIdentifier() {
        assertEquals("relive_pro", ReliveMonetization.entitlementId)
    }

    @Test fun emptyOfferingHasNoProductsButIsNotAnError() {
        val state = EntitlementState(purchasingAvailable = true)
        assertTrue(state.products.isEmpty())
        assertNull(state.message)
    }

    @Test fun getOfferingsFailurePreservesTheSafeErrorMessage() {
        val state = offeringsFailureState("Network unavailable")
        assertEquals("Network unavailable", state.message)
        assertTrue(state.purchasingAvailable)
    }
}
