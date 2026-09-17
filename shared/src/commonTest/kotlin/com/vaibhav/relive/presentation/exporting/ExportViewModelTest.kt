package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.EntitlementState
import com.vaibhav.relive.domain.entitlement.PurchaseOutcome
import com.vaibhav.relive.domain.entitlement.RelivePurchaseOption
import com.vaibhav.relive.domain.exporting.DiaryPaper
import com.vaibhav.relive.domain.exporting.ExportFormat
import com.vaibhav.relive.domain.exporting.ExportOperationState
import com.vaibhav.relive.domain.exporting.ExportProgress
import com.vaibhav.relive.domain.exporting.ExportResult
import com.vaibhav.relive.domain.exporting.ExportScope
import com.vaibhav.relive.domain.exporting.PdfImageQuality
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
import com.vaibhav.relive.platform.exporting.ExportCompletion
import com.vaibhav.relive.platform.exporting.ExportCompletionNotifier
import com.vaibhav.relive.platform.exporting.OpenedPortableArchive
import com.vaibhav.relive.platform.exporting.ReliveExportService
import com.vaibhav.relive.platform.exporting.UnavailableExportCompletionNotifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun constructionDoesNotLoadArchive() = runTest {
        val moments = CountingMomentRepository()
        val timelines = CountingTimelineRepository()
        exportViewModel(
            service = CompletingExportService(),
            momentRepository = moments,
            timelineRepository = timelines,
            prepare = false,
        )
        runCurrent()

        assertEquals(0, moments.listAllCalls)
        assertEquals(0, moments.listInTimelineCalls)
        assertEquals(0, timelines.listCustomCalls)
    }

    @Test
    fun firstPreparationLoadsArchiveOnce() = runTest {
        val moments = CountingMomentRepository()
        val timelines = CountingTimelineRepository()
        val viewModel = exportViewModel(
            service = CompletingExportService(),
            momentRepository = moments,
            timelineRepository = timelines,
            prepare = false,
        )

        viewModel.prepareForEntry()?.join()

        assertEquals(1, moments.listAllCalls)
        assertEquals(1, moments.listInTimelineCalls)
        assertEquals(1, timelines.listCustomCalls)
    }

    @Test
    fun repeatedPreparationDoesNotDuplicateSuccessfulLoad() = runTest {
        val moments = CountingMomentRepository()
        val timelines = CountingTimelineRepository()
        val viewModel = exportViewModel(
            service = CompletingExportService(),
            momentRepository = moments,
            timelineRepository = timelines,
            prepare = false,
        )

        val preparation = viewModel.prepareForEntry()
        assertEquals(preparation, viewModel.prepareForEntry())
        preparation?.join()
        viewModel.prepareForEntry()?.join()

        assertEquals(1, moments.listAllCalls)
        assertEquals(1, moments.listInTimelineCalls)
        assertEquals(1, timelines.listCustomCalls)
    }

    @Test
    fun failedPreparationCanRetry() = runTest {
        val moments = CountingMomentRepository(listAllFailuresRemaining = 1)
        val timelines = CountingTimelineRepository()
        val viewModel = exportViewModel(
            service = CompletingExportService(),
            momentRepository = moments,
            timelineRepository = timelines,
            prepare = false,
        )

        viewModel.prepareForEntry()?.join()
        assertEquals(1, moments.listAllCalls)

        viewModel.prepareForEntry()?.join()

        assertEquals(2, moments.listAllCalls)
        assertEquals(2, timelines.listCustomCalls)
        assertEquals(1, moments.listInTimelineCalls)
        assertEquals(1, viewModel.state.value.allMomentCount)
    }

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

    @Test
    fun selectedDiaryPaperIsPassedToThePdfDocument() = runTest {
        val service = ControllableExportService()
        val viewModel = exportViewModel(service)
        viewModel.state.first { it.isPro && it.selectedMomentCount == 1 }
        viewModel.setPaper(DiaryPaper.Lavender)

        viewModel.createSelectedFormat()
        service.started.await()

        assertEquals(DiaryPaper.Lavender, service.startedDocument?.options?.paper)
        viewModel.cancel()
    }

    @Test
    fun selectedPdfImageQualityIsPassedToThePdfDocument() = runTest {
        assertEquals(PdfImageQuality.Standard, ExportUiState().imageQuality)
        assertEquals(1280, PdfImageQuality.Standard.maxLongEdgePx)
        assertEquals(70, PdfImageQuality.Standard.jpegQualityPercent)
        assertEquals(1920, PdfImageQuality.HD.maxLongEdgePx)
        assertEquals(82, PdfImageQuality.HD.jpegQualityPercent)

        val defaultService = ControllableExportService()
        val defaultViewModel = exportViewModel(defaultService)
        defaultViewModel.state.first { it.isPro && it.selectedMomentCount == 1 }
        defaultViewModel.createSelectedFormat()
        defaultService.started.await()
        assertEquals(PdfImageQuality.Standard, defaultService.startedDocument?.options?.imageQuality)
        defaultViewModel.cancel()

        val service = ControllableExportService()
        val viewModel = exportViewModel(service)
        viewModel.state.first { it.isPro && it.selectedMomentCount == 1 }
        viewModel.setImageQuality(PdfImageQuality.HD)

        viewModel.createSelectedFormat()
        service.started.await()

        assertEquals(PdfImageQuality.HD, service.startedDocument?.options?.imageQuality)
        viewModel.cancel()
    }

    @Test
    fun hiddenCompletionNotifiesOnceAndLeavingProcessingDoesNotCancel() = runTest {
        val service = CompletingExportService()
        val notifier = RecordingExportCompletionNotifier()
        val viewModel = exportViewModel(service, notifier)
        viewModel.state.first { it.isPro && it.selectedMomentCount == 1 }
        viewModel.setScreenVisible(true)
        viewModel.createSelectedFormat()
        service.started.await()
        viewModel.state.first { it.operation is ExportOperationState.Ready }

        assertTrue(viewModel.state.value.operation is ExportOperationState.Ready)
        assertTrue(notifier.completions.isEmpty())
        viewModel.setScreenVisible(false)
        notifier.notified.await()
        viewModel.setScreenVisible(false)

        assertEquals(listOf(ExportCompletion.Ready), notifier.completions)
    }

    @Test
    fun hidingProcessingLeavesGenerationRunningUntilExplicitCancel() = runTest {
        val service = ControllableExportService()
        val viewModel = exportViewModel(service)
        viewModel.state.first { it.isPro && it.selectedMomentCount == 1 }
        viewModel.setScreenVisible(true)
        viewModel.createSelectedFormat()
        service.started.await()

        viewModel.setScreenVisible(false)

        assertEquals(ExportFlowStage.Processing, viewModel.state.value.operation.flowStage())
        assertFalse(service.cancelled.isCompleted)
        viewModel.cancel()
        service.cancelled.await()
    }

    @Test
    fun failedHiddenCompletionNotifiesFailure() = runTest {
        val notifier = RecordingExportCompletionNotifier()
        val viewModel = exportViewModel(FailingExportService(), notifier)
        viewModel.state.first { it.isPro && it.selectedMomentCount == 1 }
        viewModel.setScreenVisible(false)
        viewModel.createSelectedFormat()
        viewModel.state.first { it.operation is ExportOperationState.Failed }
        notifier.notified.await()

        assertEquals(ExportOperationState.Failed("generation failed"), viewModel.state.value.operation)
        assertEquals(listOf(ExportCompletion.Failed), notifier.completions)
    }

    @Test
    fun leavingSetupDeletesAnUnusedCoverWithoutCancellingWork() = runTest {
        val service = ControllableExportService()
        val viewModel = exportViewModel(service)
        viewModel.state.first { it.isPro && it.selectedMomentCount == 1 }
        viewModel.setCoverPhoto("unused-cover.jpg")

        viewModel.exitSetup()

        assertEquals(null, viewModel.state.value.coverPhotoPath)
        assertEquals(listOf("unused-cover.jpg"), service.deletedPaths)
    }

    @Test
    fun completedGenerationDeletesItsCoverAndClearsTheSelection() = runTest {
        val service = CompletingExportService()
        val viewModel = exportViewModel(service)
        viewModel.state.first { it.isPro && it.selectedMomentCount == 1 }
        viewModel.setCoverPhoto("used-cover.jpg")

        viewModel.createSelectedFormat()
        viewModel.state.first { it.operation is ExportOperationState.Ready && it.coverPhotoPath == null }

        assertEquals(listOf("used-cover.jpg"), service.deletedPaths)
    }

    @Test
    fun profileStatusUsesProgressAndTerminalCopy() {
        assertEquals(
            "Creating PDF",
            ExportUiState(operation = ExportOperationState.Preparing(ExportFormat.KeepsakePdf)).profileExportStatus(),
        )
        assertEquals(
            "Creating PDF · 40%",
            ExportUiState(
                operation = ExportOperationState.Working(
                    ExportFormat.KeepsakePdf,
                    ExportProgress(2, 5, "Writing"),
                ),
            ).profileExportStatus(),
        )
        assertEquals(
            "Export ready",
            ExportUiState(
                operation = ExportOperationState.Ready(
                    ExportResult("path", "file.pdf", "application/pdf", ExportFormat.KeepsakePdf),
                ),
            ).profileExportStatus(),
        )
        assertEquals(
            "Export needs attention",
            ExportUiState(operation = ExportOperationState.Failed("failed")).profileExportStatus(),
        )
    }

    @Test
    fun selectedTimelineMomentsAreTheOnlyMomentsPassedToThePdfDocument() = runTest {
        val selectedTimeline = Timeline.Custom(TimelineId("trip"), "Trip")
        val selectedMoment = Moment(MomentId("selected"), Instant(1), title = "Selected")
        val otherMoment = Moment(MomentId("other"), Instant(2), title = "Other")
        val service = ControllableExportService()
        val viewModel = ExportViewModel(
            momentRepository = FakeMomentRepository(
                moments = listOf(selectedMoment, otherMoment),
                timelineMoments = listOf(selectedMoment),
            ),
            timelineRepository = FakeTimelineRepository(listOf(selectedTimeline)),
            appearanceRepository = FakeAppearanceRepository(),
            entitlementProvider = FakeProEntitlementProvider(),
            exportService = service,
            clock = Clock { Instant(3) },
            scope = backgroundScope,
        )
        viewModel.prepareForEntry()
        val loadedState = viewModel.state.first { it.isPro && it.selectedMomentCount == 2 }
        assertEquals(2, loadedState.allMomentCount)
        assertEquals(1, loadedState.timelineMomentCounts[selectedTimeline.id])

        viewModel.selectScope(ExportScope.Custom(selectedTimeline.id, selectedTimeline.name))
        viewModel.state.first { it.selectedMomentCount == 1 }
        viewModel.createSelectedFormat()
        service.started.await()

        assertEquals(listOf(selectedMoment), service.startedDocument?.moments)
        assertEquals("Trip", service.startedDocument?.scopeTitle)
        viewModel.cancel()
    }

    private fun kotlinx.coroutines.test.TestScope.exportViewModel(
        service: ReliveExportService,
        notifier: ExportCompletionNotifier = UnavailableExportCompletionNotifier,
        momentRepository: MomentRepository = FakeMomentRepository(
            listOf(Moment(MomentId("moment"), Instant(1), title = "A memory")),
        ),
        timelineRepository: TimelineRepository = FakeTimelineRepository(),
        prepare: Boolean = true,
    ) = ExportViewModel(
        momentRepository = momentRepository,
        timelineRepository = timelineRepository,
        appearanceRepository = FakeAppearanceRepository(),
        entitlementProvider = FakeProEntitlementProvider(),
        exportService = service,
        clock = Clock { Instant(2) },
        scope = backgroundScope,
        exportCompletionNotifier = notifier,
    ).also { if (prepare) it.prepareForEntry() }
}

private class CompletingExportService : ReliveExportService {
    val started = CompletableDeferred<Unit>()
    val deletedPaths = mutableListOf<String>()

    override suspend fun createMagazinePdf(document: MagazineDocument, onProgress: (ExportProgress) -> Unit): ExportResult {
        started.complete(Unit)
        return ExportResult("output.pdf", "output.pdf", "application/pdf", ExportFormat.KeepsakePdf)
    }

    override suspend fun createPortableArchive(snapshot: PortableArchiveSnapshot, onProgress: (ExportProgress) -> Unit) = error("Not used")
    override suspend fun openPortableArchive(path: String): OpenedPortableArchive = error("Not used")
    override fun releasePortableArchive(archive: OpenedPortableArchive) = Unit
    override fun deleteTemporaryFile(path: String) {
        deletedPaths += path
    }
}

private class FailingExportService : ReliveExportService {
    override suspend fun createMagazinePdf(document: MagazineDocument, onProgress: (ExportProgress) -> Unit): ExportResult = error("generation failed")
    override suspend fun createPortableArchive(snapshot: PortableArchiveSnapshot, onProgress: (ExportProgress) -> Unit) = error("Not used")
    override suspend fun openPortableArchive(path: String): OpenedPortableArchive = error("Not used")
    override fun releasePortableArchive(archive: OpenedPortableArchive) = Unit
    override fun deleteTemporaryFile(path: String) = Unit
}

private class RecordingExportCompletionNotifier : ExportCompletionNotifier {
    val completions = mutableListOf<ExportCompletion>()
    val notified = CompletableDeferred<Unit>()
    override suspend fun notify(completion: ExportCompletion) {
        completions += completion
        notified.complete(Unit)
    }
}

private class ControllableExportService(
    private val publishAfterCancellation: Boolean = false,
) : ReliveExportService {
    val started = CompletableDeferred<Unit>()
    val cancelled = CompletableDeferred<Unit>()
    val allowCancelledExporterToReturn = CompletableDeferred<Unit>()
    val deletedPaths = mutableListOf<String>()
    var startedDocument: MagazineDocument? = null

    override suspend fun createMagazinePdf(
        document: MagazineDocument,
        onProgress: (ExportProgress) -> Unit,
    ): ExportResult {
        startedDocument = document
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
    private val timelineMoments: List<Moment> = moments,
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
    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> = timelineMoments
    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> = MutableStateFlow(timelineMoments)
}

private class CountingMomentRepository(
    private val moments: List<Moment> = listOf(Moment(MomentId("counted"), Instant(1), title = "A memory")),
    listAllFailuresRemaining: Int = 0,
) : MomentRepository by FakeMomentRepository(moments) {
    private var remainingFailures = listAllFailuresRemaining
    var listAllCalls = 0
        private set
    var listInTimelineCalls = 0
        private set

    override suspend fun listAll(): List<Moment> {
        listAllCalls += 1
        if (remainingFailures > 0) {
            remainingFailures -= 1
            error("archive load failed")
        }
        return moments
    }

    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> {
        listInTimelineCalls += 1
        return moments
    }
}

private class FakeTimelineRepository(
    private val timelines: List<Timeline.Custom> = emptyList(),
) : TimelineRepository {
    override suspend fun createCustom(timeline: Timeline.Custom, createdAt: Instant) = Unit
    override suspend fun findCustom(id: TimelineId): Timeline.Custom? = timelines.firstOrNull { it.id == id }
    override suspend fun listCustom(): List<Timeline.Custom> = timelines
    override fun observeCustom(): Flow<List<Timeline.Custom>> = MutableStateFlow(timelines)
    override suspend fun rename(id: TimelineId, newName: String) = Unit
    override suspend fun updateAppearance(id: TimelineId, appearance: TimelineAppearance) = Unit
    override suspend fun updateCoverPhoto(id: TimelineId, coverPhotoRef: MediaStorageRef?) = Unit
    override suspend fun deleteCustom(id: TimelineId) = Unit
    override suspend fun addMembership(momentId: MomentId, timelineId: TimelineId) = Unit
    override suspend fun removeMembership(momentId: MomentId, timelineId: TimelineId) = Unit
    override suspend fun timelinesFor(momentId: MomentId): List<TimelineId> = emptyList()
}

private class CountingTimelineRepository(
    private val timelines: List<Timeline.Custom> = listOf(Timeline.Custom(TimelineId("counted"), "Counted")),
) : TimelineRepository by FakeTimelineRepository(timelines) {
    var listCustomCalls = 0
        private set

    override suspend fun listCustom(): List<Timeline.Custom> {
        listCustomCalls += 1
        return timelines
    }
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
