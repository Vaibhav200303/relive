package com.vaibhav.relive.presentation.onboarding

import com.vaibhav.relive.di.InMemoryOnboardingPreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingStateTest {
    @Test
    fun navigationAdvancesAndRetreatsWithinSixScreens() {
        val first = OnboardingState()
        val second = first.next()
        val last = second.next().next().next().next()

        assertTrue(first.isFirstPage)
        assertEquals(1, second.pageIndex)
        assertTrue(last.isLastPage)
        assertEquals(last, last.next())
        assertEquals(first, first.previous())
        assertEquals(4, last.previous().pageIndex)
    }

    @Test
    fun sixScreensMapToTheReferenceOrder() {
        assertEquals(
            listOf(
                OnboardingPage.Welcome,
                OnboardingPage.Capture,
                OnboardingPage.Organize,
                OnboardingPage.ReliveMoments,
                OnboardingPage.Private,
                OnboardingPage.Final,
            ),
            (0 until ONBOARDING_PAGE_COUNT).map { OnboardingState(it).page },
        )
    }

    @Test
    fun primaryAndSkipActionsMatchTheVisibleControls() {
        val welcome = OnboardingState(0)
        val numberedPages = (1..4).map(::OnboardingState)
        val final = OnboardingState(5)

        assertEquals(OnboardingCommand.Advance, welcome.primaryCommand)
        assertFalse(welcome.canSkip)
        numberedPages.forEach { page ->
            assertEquals(OnboardingCommand.Advance, page.primaryCommand)
            assertTrue(page.canSkip)
        }
        assertEquals(OnboardingCommand.Complete, final.primaryCommand)
        assertFalse(final.canSkip)
    }

    @Test
    fun resolutionShowsOnlyIncompleteEmptyInstalls() {
        assertEquals(OnboardingResolution.Show, resolveOnboarding(0, 0, 0))
        assertEquals(OnboardingResolution.CompleteExistingInstall, resolveOnboarding(0, 1, 0))
        assertEquals(OnboardingResolution.CompleteExistingInstall, resolveOnboarding(0, 0, 1))
        assertEquals(
            OnboardingResolution.Hidden,
            resolveOnboarding(CURRENT_ONBOARDING_VERSION, 0, 0),
        )
    }

    @Test
    fun completionIsPersistedMonotonically() = runTest {
        val repository = InMemoryOnboardingPreferencesRepository(initialVersion = 0)

        assertTrue(repository.complete(CURRENT_ONBOARDING_VERSION).isSuccess)
        assertTrue(repository.complete(0).isSuccess)

        assertEquals(
            CURRENT_ONBOARDING_VERSION,
            repository.preferences.value.completedVersion,
        )
    }
}
