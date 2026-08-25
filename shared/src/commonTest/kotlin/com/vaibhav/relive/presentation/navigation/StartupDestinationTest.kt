package com.vaibhav.relive.presentation.navigation

import com.vaibhav.relive.domain.model.StartDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class StartupDestinationTest {
    @Test
    fun timelinesIsUsedByDefault() {
        assertEquals(
            StartDestination.Timelines,
            resolveStartupDestination(StartDestination.Timelines),
        )
    }

    @Test
    fun rediscoverPreferenceIsUsedWithoutAnOverride() {
        assertEquals(
            StartDestination.Rediscover,
            resolveStartupDestination(StartDestination.Rediscover),
        )
    }

    @Test
    fun authoritativeRestorationOrDeepLinkOverrideWins() {
        assertEquals(
            StartDestination.Timelines,
            resolveStartupDestination(
                preferred = StartDestination.Rediscover,
                authoritativeOverride = StartDestination.Timelines,
            ),
        )
    }
}
