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

    @Test fun export_creation_requires_pro() {
        assertFalse(free.mayExport())
        assertTrue(pro.mayExport())
    }

    @Test fun freeTierAllowsThreeCustomTimelinesButNotAFourth() {
        assertTrue(free.mayCreateCustomTimeline(2))
        assertFalse(free.mayCreateCustomTimeline(3))
    }

    @Test fun proAllowsUnlimitedTimelinesAndScheduledBackup() {
        assertTrue(pro.mayCreateCustomTimeline(100))
        assertTrue(pro.mayScheduleBackup())
    }

    @Test fun freeTierOnlyAllowsApprovedAppearance() {
        assertEquals(
            setOf(ThemeReference.WarmJournal, ThemeReference.Sunrise, ThemeReference.Sunset),
            ReliveMonetization.freePalettes,
        )
        assertEquals(setOf(TimelineWallpaper.WarmCream, TimelineWallpaper.BlushPink), ReliveMonetization.freeWallpapers)
        assertTrue(free.maySelectPalette(ThemeReference.WarmJournal))
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

    @Test fun standardRevenueCatPackagesMapAcrossStoreProductIdentifiers() {
        assertEquals(RelivePurchaseOption.Monthly, relivePurchaseOptionForPackage("\$rc_monthly", "monthly"))
        assertEquals(RelivePurchaseOption.Annual, relivePurchaseOptionForPackage("\$rc_annual", "yearly"))
        assertEquals(RelivePurchaseOption.Lifetime, relivePurchaseOptionForPackage("\$rc_lifetime", "lifetime"))
    }

    @Test fun productionProductIdsMapForCustomRevenueCatPackages() {
        assertEquals(RelivePurchaseOption.Monthly, relivePurchaseOptionForPackage("monthly-plan", "relive_pro_monthly"))
        assertEquals(RelivePurchaseOption.Annual, relivePurchaseOptionForPackage("annual-plan", "relive_pro_annual"))
        assertEquals(RelivePurchaseOption.Lifetime, relivePurchaseOptionForPackage("lifetime-plan", "relive_pro_lifetime"))
        assertNull(relivePurchaseOptionForPackage("unknown", "monthly_extra"))
    }

    @Test fun entitlementUsesTheExactReliveProIdentifier() {
        assertEquals("relive_pro", ReliveMonetization.entitlementId)
        assertEquals("relive_pro_monthly", ReliveMonetization.monthlyProductId)
        assertEquals("relive_pro_annual", ReliveMonetization.annualProductId)
        assertEquals("relive_pro_lifetime", ReliveMonetization.lifetimeProductId)
    }

    @Test fun emptyOfferingHasNoProductsButIsNotAnError() {
        val state = EntitlementState(purchasingAvailable = true)
        assertTrue(state.products.isEmpty())
        assertNull(state.message)
    }

    @Test fun getOfferingsFailurePreservesTheSafeErrorMessage() {
        val state = offeringsFailureState(
            previous = EntitlementState(isPro = true, isLoading = true),
            message = "Network unavailable",
        )
        assertEquals("Network unavailable", state.message)
        assertTrue(state.purchasingAvailable)
        assertTrue(state.isPro)
        assertFalse(state.isLoading)
    }

    @Test fun releaseConfigurationRejectsTestStoreKeys() {
        assertTrue(isRevenueCatKeyUsable("test_public", allowTestStore = true))
        assertFalse(isRevenueCatKeyUsable("test_public", allowTestStore = false))
        assertTrue(isRevenueCatKeyUsable("goog_public", allowTestStore = false))
        assertFalse(isRevenueCatKeyUsable("RELIVE_REVENUECAT_ANDROID_PUBLIC_API_KEY", allowTestStore = false))
    }
}
