package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.entitlement.EntitlementPolicy
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.exporting.DiaryPaper
import com.vaibhav.relive.domain.exporting.ExportFormat
import com.vaibhav.relive.domain.exporting.ExportOperationState
import com.vaibhav.relive.domain.exporting.ExportScope
import com.vaibhav.relive.domain.exporting.MagazineOptions
import com.vaibhav.relive.domain.exporting.PdfImageQuality
import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot
import com.vaibhav.relive.domain.exporting.PortableTimelineIdentity
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.AppearanceRepository
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.exporting.MagazineDocument
import com.vaibhav.relive.platform.exporting.ExportCompletion
import com.vaibhav.relive.platform.exporting.ExportCompletionNotifier
import com.vaibhav.relive.platform.exporting.ReliveExportService
import com.vaibhav.relive.platform.exporting.UnavailableExportCompletionNotifier
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

data class ExportUiState(
    val timelines: List<Timeline.Custom> = emptyList(),
    val allMomentCount: Int = 0,
    val timelineMomentCounts: Map<TimelineId, Int> = emptyMap(),
    val format: ExportFormat = ExportFormat.KeepsakePdf,
    val scope: ExportScope = ExportScope.All,
    val startDate: LocalCalendarDate? = null,
    val endDate: LocalCalendarDate? = null,
    val title: String = "My Relive",
    val subtitle: String = "",
    val coverPhotoPath: String? = null,
    val paper: DiaryPaper = DiaryPaper.WarmCream,
    val imageQuality: PdfImageQuality = PdfImageQuality.Standard,
    val selectedMomentCount: Int = 0,
    val operation: ExportOperationState = ExportOperationState.Idle,
    val isPro: Boolean = false,
    val upgradeRequired: Boolean = false,
)

class ExportViewModel(
    private val momentRepository: MomentRepository,
    private val timelineRepository: TimelineRepository,
    private val appearanceRepository: AppearanceRepository,
    private val entitlementProvider: EntitlementProvider,
    private val exportService: ReliveExportService,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val exportCompletionNotifier: ExportCompletionNotifier = UnavailableExportCompletionNotifier,
) {
    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()
    private var allMoments: List<Moment> = emptyList()
    private var generation: Job? = null
    private var generationId: Long = 0
    private var terminalGenerationId: Long? = null
    private var notifiedGenerationId: Long? = null
    private var isScreenVisible = false
    private var preparationJob: Job? = null
    private var preparationSucceeded = false

    init {
        scope.launch {
            entitlementProvider.state.collect { entitlement ->
                _state.update { it.copy(isPro = entitlement.isPro) }
            }
        }
    }

    /** Starts the one-time archive load needed by the Export setup screen. */
    fun prepareForEntry(): Job? {
        if (preparationSucceeded) return null
        preparationJob?.takeIf { it.isActive }?.let { return it }

        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                reload()
                preparationSucceeded = true
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                // Keep the gate open so a later Profile → Export entry can retry a failed load.
            } finally {
                preparationJob = null
            }
        }
        preparationJob = job
        job.start()
        return job
    }

    suspend fun reload() {
        val timelines = timelineRepository.listCustom()
        allMoments = momentRepository.listAll()
        val timelineMomentCounts = timelines.associate { timeline ->
            timeline.id to runCatching {
                momentRepository.listInTimeline(timeline.id).size
            }.getOrDefault(0)
        }
        _state.update {
            it.copy(
                timelines = timelines,
                allMomentCount = allMoments.size,
                timelineMomentCounts = timelineMomentCounts,
            )
        }
        refreshSelection()
    }

    fun selectScope(value: ExportScope) {
        _state.update { current ->
            current.copy(
                scope = value,
                title = when (value) {
                    ExportScope.All -> "My Relive"
                    is ExportScope.Custom -> value.name
                },
            )
        }
        refreshSelection()
    }

    fun setDateRange(start: LocalCalendarDate?, end: LocalCalendarDate?) {
        _state.update { it.copy(startDate = start, endDate = end) }
        refreshSelection()
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value.take(100)) }
    fun setSubtitle(value: String) = _state.update { it.copy(subtitle = value.take(180)) }
    fun setPaper(value: DiaryPaper) = _state.update { it.copy(paper = value) }
    fun setImageQuality(value: PdfImageQuality) = _state.update { it.copy(imageQuality = value) }

    /** Tracks the Export screen only; leaving it never cancels an in-flight generation. */
    fun setScreenVisible(visible: Boolean) {
        isScreenVisible = visible
        if (!visible) notifyTerminalCompletionIfNeeded()
    }

    fun setCoverPhoto(path: String?) {
        _state.value.coverPhotoPath?.takeIf { it != path }?.let(exportService::deleteTemporaryFile)
        _state.update { it.copy(coverPhotoPath = path) }
    }

    fun deleteTemporaryCover(path: String) = exportService.deleteTemporaryFile(path)

    fun clearUpgradeRequired() = _state.update { it.copy(upgradeRequired = false) }

    fun selectFormat(value: ExportFormat) = _state.update { it.copy(format = value) }

    fun createSelectedFormat() = create(_state.value.format)

    private fun create(format: ExportFormat) {
        if (!EntitlementPolicy(entitlementProvider.state.value).mayExport()) {
            _state.update { it.copy(upgradeRequired = true) }
            return
        }
        if (generation?.isActive == true) return
        val activeGenerationId = ++generationId
        terminalGenerationId = null
        notifiedGenerationId = null
        generation = scope.launch {
            val coverToDelete = if (format == ExportFormat.KeepsakePdf) _state.value.coverPhotoPath else null
            updateOperation(activeGenerationId, ExportOperationState.Preparing(format))
            try {
                reload()
                val result = when (format) {
                    ExportFormat.KeepsakePdf -> {
                        val selected = selectedMoments()
                        require(selected.isNotEmpty()) { "Choose a range containing at least one Moment." }
                        val current = _state.value
                        exportService.createMagazinePdf(
                            MagazineDocument(
                                moments = selected.sortedBy { it.createdAt.epochMilliseconds },
                                options = MagazineOptions(
                                    title = current.title.trim().ifEmpty { "My Relive" },
                                    subtitle = current.subtitle.trim().ifEmpty { dateSpan(selected) },
                                    coverPhotoPath = current.coverPhotoPath,
                                    startDate = current.startDate,
                                    endDate = current.endDate,
                                    paper = current.paper,
                                    imageQuality = current.imageQuality,
                                ),
                                scopeTitle = when (val selectedScope = current.scope) {
                                    ExportScope.All -> "All moments"
                                    is ExportScope.Custom -> selectedScope.name
                                },
                            ),
                        ) { progress ->
                            updateOperation(activeGenerationId, ExportOperationState.Working(format, progress))
                        }
                    }
                    ExportFormat.ReliveArchive -> {
                        val current = _state.value
                        val moments = selectedMoments()
                        require(moments.isNotEmpty()) { "Choose a range containing at least one Moment." }
                        val custom = (current.scope as? ExportScope.Custom)?.let { selected ->
                            current.timelines.firstOrNull { it.id == selected.timelineId }
                                ?: error("The selected timeline is no longer available.")
                        }
                        val exportedTimeline = if (custom == null) {
                            PortableTimelineIdentity(
                                appearance = appearanceRepository.preferences.value.allTimelineAppearance,
                            )
                        } else {
                            PortableTimelineIdentity(
                                name = custom.name,
                                customTimelineId = custom.id,
                                appearance = custom.appearance,
                                coverPhotoRef = custom.coverPhotoRef,
                            )
                        }
                        exportService.createPortableArchive(
                            PortableArchiveSnapshot(
                                exportedAtEpochMilliseconds = clock.now().epochMilliseconds,
                                moments = moments,
                                timelines = listOfNotNull(custom),
                                memberships = custom?.let { timeline ->
                                    moments.associate { moment -> moment.id to setOf(timeline.id) }
                                }.orEmpty(),
                                allTimelineAppearance = appearanceRepository.preferences.value.allTimelineAppearance,
                                exportedTimeline = exportedTimeline,
                            ),
                        ) { progress ->
                            updateOperation(activeGenerationId, ExportOperationState.Working(format, progress))
                        }
                    }
                }
                coroutineContext.ensureActive()
                updateOperationAndNotify(activeGenerationId, ExportOperationState.Ready(result), ExportCompletion.Ready)
            } catch (_: CancellationException) {
                updateOperation(activeGenerationId, ExportOperationState.Idle)
            } catch (error: Throwable) {
                updateOperationAndNotify(
                    activeGenerationId,
                    ExportOperationState.Failed(error.message ?: "Export failed."),
                    ExportCompletion.Failed,
                )
            } finally {
                coverToDelete?.let(exportService::deleteTemporaryFile)
                if (coverToDelete != null && generationId == activeGenerationId) {
                    _state.update { it.copy(coverPhotoPath = null) }
                }
                if (generationId == activeGenerationId) generation = null
            }
        }
    }

    fun cancel() {
        val clearsOneOffCover = when (val operation = _state.value.operation) {
            is ExportOperationState.Preparing -> operation.format == ExportFormat.KeepsakePdf
            is ExportOperationState.Working -> operation.format == ExportFormat.KeepsakePdf
            else -> false
        }
        generationId++
        generation?.cancel()
        generation = null
        terminalGenerationId = null
        notifiedGenerationId = null
        _state.update {
            it.copy(
                operation = ExportOperationState.Idle,
                coverPhotoPath = if (clearsOneOffCover) null else it.coverPhotoPath,
            )
        }
    }

    fun clearOperation() {
        (_state.value.operation as? ExportOperationState.Ready)?.result?.path?.let(exportService::deleteTemporaryFile)
        terminalGenerationId = null
        notifiedGenerationId = null
        _state.update { it.copy(operation = ExportOperationState.Idle) }
    }

    /** Leaves setup without affecting any generation owned by the app root. */
    fun exitSetup() {
        if (_state.value.operation.flowStage() != ExportFlowStage.Setup) return
        _state.value.coverPhotoPath?.let(exportService::deleteTemporaryFile)
        _state.update { it.copy(coverPhotoPath = null) }
    }

    fun close() {
        val readyPath = (_state.value.operation as? ExportOperationState.Ready)?.result?.path
        preparationJob?.cancel()
        preparationJob = null
        cancel()
        readyPath?.let(exportService::deleteTemporaryFile)
        _state.value.coverPhotoPath?.let(exportService::deleteTemporaryFile)
        _state.update { it.copy(coverPhotoPath = null) }
    }

    private fun updateOperation(id: Long, operation: ExportOperationState) {
        if (generationId == id) {
            if (operation is ExportOperationState.Ready || operation is ExportOperationState.Failed) {
                terminalGenerationId = id
            } else if (operation is ExportOperationState.Idle) {
                terminalGenerationId = null
            }
            _state.update { it.copy(operation = operation) }
        }
    }

    private suspend fun updateOperationAndNotify(
        id: Long,
        operation: ExportOperationState,
        completion: ExportCompletion,
    ) {
        updateOperation(id, operation)
        notifyTerminalCompletionIfNeeded(id, completion)
    }

    private fun notifyTerminalCompletionIfNeeded() {
        val completion = when (_state.value.operation) {
            is ExportOperationState.Ready -> ExportCompletion.Ready
            is ExportOperationState.Failed -> ExportCompletion.Failed
            else -> null
        } ?: return
        terminalGenerationId?.let { notifyTerminalCompletionIfNeeded(it, completion) }
    }

    private fun notifyTerminalCompletionIfNeeded(id: Long, completion: ExportCompletion) {
        if (isScreenVisible || terminalGenerationId != id || notifiedGenerationId == id) return
        notifiedGenerationId = id
        scope.launch {
            runCatching { exportCompletionNotifier.notify(completion) }
        }
    }

    private fun refreshSelection() {
        scope.launch {
            val count = runCatching { selectedMoments().size }.getOrDefault(0)
            _state.update { it.copy(selectedMomentCount = count) }
        }
    }

    private suspend fun selectedMoments(): List<Moment> {
        val current = _state.value
        val source = when (val selectedScope = current.scope) {
            ExportScope.All -> allMoments
            is ExportScope.Custom -> momentRepository.listInTimeline(selectedScope.timelineId)
        }
        return filterMomentsForMagazine(source, current.startDate, current.endDate) {
            RediscoverCalendar.localDate(it.createdAt)
        }
    }

    private fun dateSpan(moments: List<Moment>): String {
        val years = moments.map { RediscoverCalendar.localDate(it.createdAt).year }
        return when {
            years.isEmpty() -> ""
            years.min() == years.max() -> years.first().toString()
            else -> "${years.min()} - ${years.max()}"
        }
    }
}

fun ExportUiState.profileExportStatus(): String? = when (val current = operation) {
    ExportOperationState.Idle -> null
    is ExportOperationState.Preparing -> current.format.profileExportCreationLabel()
    is ExportOperationState.Working -> {
        val label = current.format.profileExportCreationLabel()
        current.progress.fraction?.let { "$label · ${(it * 100).toInt()}%" } ?: label
    }
    is ExportOperationState.Ready -> "Export ready"
    is ExportOperationState.Failed -> "Export needs attention"
}

private fun ExportFormat.profileExportCreationLabel(): String = when (this) {
    ExportFormat.KeepsakePdf -> "Creating PDF"
    ExportFormat.ReliveArchive -> "Creating archive"
}
