package com.vaibhav.relive.presentation.settings

import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.repository.AppearanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceStateTest {
    @Test
    fun modeResolutionFollowsSystemOnlyWhenRequested() {
        assertTrue(resolveDarkMode(AppearanceMode.System, systemDark = true))
        assertFalse(resolveDarkMode(AppearanceMode.System, systemDark = false))
        assertFalse(resolveDarkMode(AppearanceMode.Light, systemDark = true))
        assertTrue(resolveDarkMode(AppearanceMode.Dark, systemDark = false))
    }

    @Test
    fun timelineOverrideWinsWithoutChangingGlobalFallback() {
        assertEquals(
            ThemeReference.Evergreen,
            resolveTimelineTheme(ThemeReference.Evergreen, ThemeReference.Rosewood),
        )
        assertEquals(ThemeReference.Rosewood, resolveTimelineTheme(null, ThemeReference.Rosewood))
    }

    @Test
    fun viewModelPublishesPersistedUpdatesAndWriteFailures() = runTest {
        val repository = FakeAppearanceRepository()
        val viewModel = AppearanceViewModel(repository, backgroundScope)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }
        assertEquals(AppearancePreferences(), viewModel.state.value.preferences)

        viewModel.setMode(AppearanceMode.Dark)
        runCurrent()
        assertEquals(AppearanceMode.Dark, viewModel.state.value.preferences.mode)

        val restoredViewModel = AppearanceViewModel(repository, backgroundScope)
        assertEquals(AppearanceMode.Dark, restoredViewModel.state.value.preferences.mode)

        repository.failWrites = true
        viewModel.setDefaultTheme(ThemeReference.BlueHour)
        runCurrent()
        assertEquals(ThemeReference.WarmJournal, viewModel.state.value.preferences.defaultTheme)
        assertEquals("Could not save appearance.", viewModel.state.value.errorMessage)
        collectJob.cancel()
    }
}

private class FakeAppearanceRepository : AppearanceRepository {
    override val preferences = MutableStateFlow(AppearancePreferences())
    var failWrites: Boolean = false

    override suspend fun setMode(mode: AppearanceMode): Result<Unit> = write {
        preferences.value = preferences.value.copy(mode = mode)
    }

    override suspend fun setDefaultTheme(theme: ThemeReference): Result<Unit> = write {
        preferences.value = preferences.value.copy(defaultTheme = theme)
    }

    private fun write(block: () -> Unit): Result<Unit> = if (failWrites) {
        Result.failure(IllegalStateException("failed"))
    } else {
        runCatching(block)
    }
}
