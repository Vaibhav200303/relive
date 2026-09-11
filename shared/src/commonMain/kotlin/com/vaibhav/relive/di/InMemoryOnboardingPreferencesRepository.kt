package com.vaibhav.relive.di

import com.vaibhav.relive.domain.model.OnboardingPreferences
import com.vaibhav.relive.domain.repository.OnboardingPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class InMemoryOnboardingPreferencesRepository(
    initialVersion: Int = CURRENT_DEFAULT_VERSION,
) : OnboardingPreferencesRepository {
    private val mutable = MutableStateFlow(OnboardingPreferences(initialVersion.coerceAtLeast(0)))
    override val preferences = mutable.asStateFlow()

    override suspend fun complete(version: Int): Result<Unit> = runCatching {
        require(version >= 0)
        val next = maxOf(mutable.value.completedVersion, version)
        mutable.value = OnboardingPreferences(next)
    }

    private companion object {
        // Tests and previews that do not supply install preferences should retain the historical
        // direct-to-Home behavior. Platform containers supply the persistent version-0 stores.
        const val CURRENT_DEFAULT_VERSION = 1
    }
}
