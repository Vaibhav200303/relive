package com.vaibhav.relive.presentation.onboarding

const val CURRENT_ONBOARDING_VERSION = 1
const val ONBOARDING_PAGE_COUNT = 4

data class OnboardingState(
    val pageIndex: Int = 0,
) {
    init {
        require(pageIndex in 0 until ONBOARDING_PAGE_COUNT)
    }

    val isFirstPage: Boolean get() = pageIndex == 0
    val isLastPage: Boolean get() = pageIndex == ONBOARDING_PAGE_COUNT - 1

    fun next(): OnboardingState = copy(
        pageIndex = (pageIndex + 1).coerceAtMost(ONBOARDING_PAGE_COUNT - 1),
    )

    fun previous(): OnboardingState = copy(
        pageIndex = (pageIndex - 1).coerceAtLeast(0),
    )
}

enum class OnboardingResolution {
    Show,
    CompleteExistingInstall,
    Hidden,
}

fun resolveOnboarding(
    completedVersion: Int,
    momentCount: Long,
    customTimelineCount: Long,
): OnboardingResolution = when {
    completedVersion >= CURRENT_ONBOARDING_VERSION -> OnboardingResolution.Hidden
    momentCount > 0L || customTimelineCount > 0L -> OnboardingResolution.CompleteExistingInstall
    else -> OnboardingResolution.Show
}
