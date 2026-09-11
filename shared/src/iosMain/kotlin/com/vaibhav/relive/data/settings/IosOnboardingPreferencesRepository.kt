package com.vaibhav.relive.data.settings

import com.vaibhav.relive.domain.model.OnboardingPreferences
import com.vaibhav.relive.domain.repository.OnboardingPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class IosOnboardingPreferencesRepository : OnboardingPreferencesRepository {
    private val store = NSUserDefaults.standardUserDefaults
    private val mutable = MutableStateFlow(read())
    override val preferences = mutable.asStateFlow()

    override suspend fun complete(version: Int): Result<Unit> = runCatching {
        require(version >= 0)
        val next = maxOf(mutable.value.completedVersion, version)
        store.setInteger(next.toLong(), COMPLETED_VERSION)
        mutable.value = OnboardingPreferences(next)
    }

    private fun read() = OnboardingPreferences(
        completedVersion = store.integerForKey(COMPLETED_VERSION).toInt().coerceAtLeast(0),
    )

    private companion object {
        const val COMPLETED_VERSION = "onboarding.completed_version"
    }
}
