package com.vaibhav.relive.data.settings

import android.content.Context
import com.vaibhav.relive.domain.model.OnboardingPreferences
import com.vaibhav.relive.domain.repository.OnboardingPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AndroidOnboardingPreferencesRepository(context: Context) : OnboardingPreferencesRepository {
    private val store = context.applicationContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())
    override val preferences = mutable.asStateFlow()

    override suspend fun complete(version: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(version >= 0)
            val next = maxOf(mutable.value.completedVersion, version)
            check(store.edit().putInt(COMPLETED_VERSION, next).commit()) {
                "Onboarding preference commit failed"
            }
            mutable.value = OnboardingPreferences(next)
        }
    }

    private fun read() = OnboardingPreferences(
        completedVersion = store.getInt(COMPLETED_VERSION, 0).coerceAtLeast(0),
    )

    private companion object {
        const val STORE_NAME = "relive_onboarding"
        const val COMPLETED_VERSION = "onboarding.completed_version"
    }
}
