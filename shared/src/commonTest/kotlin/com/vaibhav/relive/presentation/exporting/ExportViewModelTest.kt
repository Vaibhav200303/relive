package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.EntitlementState
import com.vaibhav.relive.domain.entitlement.PurchaseOutcome
import com.vaibhav.relive.domain.entitlement.RelivePurchaseOption
import com.vaibhav.relive.domain.exporting.ExportFormat
import com.vaibhav.relive.domain.exporting.ExportOperationState
import com.vaibhav.relive.domain.exporting.ExportProgress
import com.vaibhav.relive.domain.exporting.ExportResult
import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot
import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.AppearancePreferences
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.AppearanceRepository
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.exporting.MagazineDocument
import com.vaibhav.relive.platform.exporting.OpenedPortableArchive
import com.vaibhav.relive.platform.exporting.ReliveExportService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportViewModelTest {
    @Test
    fun cancelReturnsToSetupAndCancelsTheActiveExporter() = runTest {
        val service = ControllableExportService()
        val viewModel = exportViewModel(service)
        viewModel.state.first { it.isPro && it.selectedMomentCount == 1 }
        viewModel.setCoverPhoto("temporary-cover.jpg")

        viewModel.createSelectedFormat()
        service.started.await()
        viewModel.cancel()

        service.cancelled.await()
        assertEquals(ExportOperationState.Idle, viewModel.state.value.operation)
        assertEquals(null, viewModel.state.value.coverPhotoPath)
        assertEquals(listOf("temporary-cover.jpg"), service.deletedPaths)
    }

    @Test
    fun cancelledExporterCannotPublishLateProgressOrAResult() = runTest {
        val service = ControllableExportService(publishAfterCancellation = true)
        val viewModel = exportViewModel(service)
        viewModel.state.first { it.isPro && it.selectedMomentCount == 1 }

        viewModel.createSelectedFormat()
        service.started.await()
        viewModel.cancel()
        service.allowCancelledExporterToReturn.complete(Unit)
        service.cancelled.await()

        assertEquals(ExportOperationState.Idle, viewModel.state.value.operation)
    }

    private fun kotlinx.coroutines.test.TestScope.exportViewModel(service: ReliveExportService) = ExportViewModel(
        momentRepository = FakeMomentRepository(
            listOf(Moment(MomentId("moment"), Instant(1), title = "A memory")),
        ),
        timelineRepository = FakeTimelineRepository(),
        appearanceRepository = FakeAppearanceRepository(),
        entitlementProvider = FakeProEntitlementProvider(),
        exportService = service,
        clock = Clock { Instant(2) },
        scope = backgroundScope,
    )
}

private class ControllableExportService(
    private val publishAfterCancellation: Boolean = false,
) : ReliveExportService {
    val started = CompletableDeferred<Unit>()
    val cancelled = CompletableDeferred<Unit>()
    val allowCancelledExporterToReturn = CompletableDeferred<Unit>()
    val deletedPaths = mutableListOf<String>()

    override suspend fun createMagazinePdf(
        document: MagazineDocument,
        onProgress: (ExportProgress) -> Unit,
    ): ExportResult {
        started.complete(Unit)
        return try {
            awaitCancellation()
        } finally {
            if (publishAfterCancellation) {
                withContext(NonCancellable) {
                    allowCancelledExporterToReturn.await()
                    onProgress(ExportProgress(1, 1, "Too late"))
                }
            }
            cancelled.complete(Unit)
        }
    }

    override suspend fun createPortableArchive(
        snapshot: PortableArchiveSnapshot,
        onProgress: (ExportProgress) -> Unit,
    ) = error("Not used")

    override suspend fun openPortableArchive(path: String): OpenedPortableArchive = error("Not used")
    override fun releasePortableArchive(archive: OpenedPortableArchive) = Unit
    override fun deleteTemporaryFile(path: String) {
        deletedPaths += path
    }
}

private class FakeMomentRepository(
    private val moments: List<Moment>,
) : MomentRepository {
    override suspend fun insert(moment: Moment, timelineIds: Set<TimelineId>) = Unit
    override suspend fun findById(id: MomentId): Moment? = moments.firstOrNull { it.id == id }
    override suspend fun updateEditable(moment: Moment) = Unit
    override suspend fun setFavorite(id: MomentId, isFavorite: Boolean) = Unit
    override suspend fun setFeeling(id: MomentId, feeling: MomentFeeling?) = Unit
    override suspend fun delete(id: MomentId) = Unit
    override suspend fun listAll(): List<Moment> = moments
    override fun observeAll(): Flow<List<Moment>> = MutableStateFlow(moments)
    override fun observeSearch(query: String): Flow<List<Moment>> = MutableStateFlow(emptyList())
    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> = moments
    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> = MutableStateFlow(moments)
}

private class FakeTimelineRepository : TimelineRepository {
    override suspend fun createCustom(timeline: Timeline.Custom, createdAt: Instant) = Unit
    override suspend fun findCustom(id: TimelineId): Timeline.Custom? = null
    override suspend fun listCustom(): List<Timeline.Custom> = emptyList()
    override fun observeCustom(): Flow<List<Timeline.Custom>> = MutableStateFlow(emptyList())
    override suspend fun rename(id: TimelineId, newName: String) = Unit
    override suspend fun updateAppearance(id: TimelineId, appearance: TimelineAppearance) = Unit
    override suspend fun updateCoverPhoto(id: TimelineId, coverPhotoRef: MediaStorageRef?) = Unit
    override suspend fun deleteCustom(id: TimelineId) = Unit
    override suspend fun addMembership(momentId: MomentId, timelineId: TimelineId) = Unit
    override suspend fun removeMembership(momentId: MomentId, timelineId: TimelineId) = Unit
    override suspend fun timelinesFor(momentId: MomentId): List<TimelineId> = emptyList()
}

private class FakeAppearanceRepository : AppearanceRepository {
    override val preferences = MutableStateFlow(AppearancePreferences())
    override suspend fun setMode(mode: AppearanceMode) = Result.success(Unit)
    override suspend fun setDefaultTheme(theme: ThemeReference) = Result.success(Unit)
    override suspend fun setAllTimelineAppearance(appearance: TimelineAppearance) = Result.success(Unit)
}

private class FakeProEntitlementProvider : EntitlementProvider {
    override val state = MutableStateFlow(EntitlementState(isPro = true))
    override suspend fun purchase(option: RelivePurchaseOption) = PurchaseOutcome.Succeeded
    override suspend fun restorePurchases() = PurchaseOutcome.Succeeded
}
