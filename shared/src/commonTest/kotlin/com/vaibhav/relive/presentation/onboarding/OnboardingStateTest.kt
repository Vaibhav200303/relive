package com.vaibhav.relive.presentation.onboarding

import com.vaibhav.relive.di.InMemoryOnboardingPreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingStateTest {
    @Test
    fun navigationAdvancesAndRetreatsWithinFiveChapters() {
        val first = OnboardingState()
        val second = first.next()
        val last = second.next().next().next()

        assertTrue(first.isFirstPage)
        assertEquals(1, second.pageIndex)
        assertTrue(last.isLastPage)
        assertEquals(last, last.next())
        assertEquals(first, first.previous())
        assertEquals(3, last.previous().pageIndex)
    }

    @Test
    fun fiveChaptersMapToTheReferenceOrder() {
        assertEquals(
            listOf(
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
        val chapters = (0..3).map(::OnboardingState)
        val final = OnboardingState(4)

        chapters.forEach { page ->
            assertEquals(OnboardingCommand.Advance, page.primaryCommand)
            assertTrue(page.canSkip)
        }
        assertEquals(OnboardingCommand.Complete, final.primaryCommand)
        assertFalse(final.canSkip)
    }

    @Test
    fun repeatedNavigationInputRemainsBoundedAndDeterministic() {
        val rapidForward = generateSequence(OnboardingState()) { it.next() }.take(20).last()
        val rapidBack = generateSequence(rapidForward) { it.previous() }.take(20).last()

        assertEquals(OnboardingState(ONBOARDING_PAGE_COUNT - 1), rapidForward)
        assertEquals(OnboardingState(), rapidBack)
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
