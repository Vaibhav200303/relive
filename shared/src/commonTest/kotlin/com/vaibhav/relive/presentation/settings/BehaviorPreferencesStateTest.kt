package com.vaibhav.relive.presentation.settings

import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.repository.BehaviorPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BehaviorPreferencesStateTest {
    @Test
    fun everySetterPublishesAndRelaunchRestoresPersistedValues() = runTest {
        val repository = FakeBehaviorPreferencesRepository()
        val viewModel = BehaviorPreferencesViewModel(repository, backgroundScope)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }

        viewModel.setConfirmBeforeDiscarding(false)
        viewModel.setShowLocations(false)
        viewModel.setShowTags(false)
        viewModel.setShowOnThisDay(false)
        viewModel.setShowFavorites(false)
        runCurrent()

        assertEquals(
            BehaviorPreferences(
                confirmBeforeDiscarding = false,
                showLocations = false,
                showTags = false,
                showOnThisDay = false,
                showFavorites = false,
            ),
            viewModel.state.value.preferences,
        )

        val relaunched = BehaviorPreferencesViewModel(repository, backgroundScope)
        assertEquals(viewModel.state.value.preferences, relaunched.state.value.preferences)
        collectJob.cancel()
    }

    @Test
    fun failedWriteKeepsOldValueAndReportsError() = runTest {
        val repository = FakeBehaviorPreferencesRepository().apply { failWrites = true }
        val viewModel = BehaviorPreferencesViewModel(repository, backgroundScope)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }

        viewModel.setShowTags(false)
        runCurrent()

        assertTrue(viewModel.state.value.preferences.showTags)
        assertEquals("Could not save preferences.", viewModel.state.value.errorMessage)
        collectJob.cancel()
    }
}

private class FakeBehaviorPreferencesRepository : BehaviorPreferencesRepository {
    override val preferences = MutableStateFlow(BehaviorPreferences())
    var failWrites: Boolean = false

    override suspend fun setConfirmBeforeDiscarding(enabled: Boolean): Result<Unit> = write {
        preferences.value = preferences.value.copy(confirmBeforeDiscarding = enabled)
    }

    override suspend fun setShowLocations(enabled: Boolean): Result<Unit> = write {
        preferences.value = preferences.value.copy(showLocations = enabled)
    }

    override suspend fun setShowTags(enabled: Boolean): Result<Unit> = write {
        preferences.value = preferences.value.copy(showTags = enabled)
    }

    override suspend fun setShowOnThisDay(enabled: Boolean): Result<Unit> = write {
        preferences.value = preferences.value.copy(showOnThisDay = enabled)
    }

    override suspend fun setShowFavorites(enabled: Boolean): Result<Unit> = write {
        preferences.value = preferences.value.copy(showFavorites = enabled)
    }

    private fun write(block: () -> Unit): Result<Unit> = if (failWrites) {
        Result.failure(IllegalStateException("failed"))
    } else {
        runCatching(block)
    }
}
