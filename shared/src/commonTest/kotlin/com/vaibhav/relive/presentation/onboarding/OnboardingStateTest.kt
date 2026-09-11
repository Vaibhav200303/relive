package com.vaibhav.relive.presentation.onboarding

import com.vaibhav.relive.di.InMemoryOnboardingPreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingStateTest {
    @Test
    fun navigationAdvancesAndRetreatsWithinFourPages() {
        val first = OnboardingState()
        val second = first.next()
        val last = second.next().next()

        assertTrue(first.isFirstPage)
        assertEquals(1, second.pageIndex)
        assertTrue(last.isLastPage)
        assertEquals(last, last.next())
        assertEquals(first, first.previous())
        assertEquals(2, last.previous().pageIndex)
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
