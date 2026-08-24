package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.model.ArchiveInsights
import com.vaibhav.relive.domain.model.ArchiveMediaCategorySummary
import com.vaibhav.relive.domain.repository.ArchiveInsightsRepository
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MediaStorageViewModelTest {
    @Test fun loads_archive_insights_on_entry() = runTest {
        val viewModel = MediaStorageViewModel(FakeRepository(), backgroundScope)

        viewModel.loadOnEntry()
        runCurrent()

        assertIs<MediaStorageState.Loaded>(viewModel.state.value)
    }

    @Test fun exposes_error_and_recovers_on_retry() = runTest {
        val repository = FakeRepository(fail = true)
        val viewModel = MediaStorageViewModel(repository, backgroundScope)

        viewModel.loadOnEntry()
        runCurrent()
        assertEquals(MediaStorageState.Error, viewModel.state.value)

        repository.fail = false
        viewModel.refresh()
        runCurrent()
        assertIs<MediaStorageState.Loaded>(viewModel.state.value)
    }

    private class FakeRepository(var fail: Boolean = false) : ArchiveInsightsRepository {
        override suspend fun load(): ArchiveInsights {
            check(!fail)
            return ArchiveInsights(
                momentCount = 0,
                attachmentCount = 0,
                photo = ArchiveMediaCategorySummary(),
                video = ArchiveMediaCategorySummary(),
                audio = ArchiveMediaCategorySummary(),
                other = ArchiveMediaCategorySummary(),
                unavailableFileCount = 0,
            )
        }
    }
}
