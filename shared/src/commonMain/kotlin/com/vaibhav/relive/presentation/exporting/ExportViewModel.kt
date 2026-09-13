package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.entitlement.EntitlementPolicy
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.exporting.ExportFormat
import com.vaibhav.relive.domain.exporting.ExportOperationState
import com.vaibhav.relive.domain.exporting.ExportScope
import com.vaibhav.relive.domain.exporting.MagazineOptions
import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot
import com.vaibhav.relive.domain.exporting.PortableTimelineIdentity
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.repository.AppearanceRepository
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.exporting.MagazineDocument
import com.vaibhav.relive.platform.exporting.ReliveExportService
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
    val format: ExportFormat = ExportFormat.KeepsakePdf,
    val scope: ExportScope = ExportScope.All,
    val startDate: LocalCalendarDate? = null,
    val endDate: LocalCalendarDate? = null,
    val title: String = "My Relive",
    val subtitle: String = "",
    val coverPhotoPath: String? = null,
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
) {
    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()
    private var allMoments: List<Moment> = emptyList()
    private var generation: Job? = null
    private var generationId: Long = 0

    init {
        scope.launch { reload() }
        scope.launch {
            entitlementProvider.state.collect { entitlement ->
                _state.update { it.copy(isPro = entitlement.isPro) }
            }
        }
    }

    suspend fun reload() {
        val timelines = timelineRepository.listCustom()
        allMoments = momentRepository.listAll()
        _state.update { it.copy(timelines = timelines) }
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
                updateOperation(activeGenerationId, ExportOperationState.Ready(result))
            } catch (_: CancellationException) {
                updateOperation(activeGenerationId, ExportOperationState.Idle)
            } catch (error: Throwable) {
                updateOperation(
                    activeGenerationId,
                    ExportOperationState.Failed(error.message ?: "Export failed."),
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
        _state.update {
            it.copy(
                operation = ExportOperationState.Idle,
                coverPhotoPath = if (clearsOneOffCover) null else it.coverPhotoPath,
            )
        }
    }

    fun clearOperation() {
        (_state.value.operation as? ExportOperationState.Ready)?.result?.path?.let(exportService::deleteTemporaryFile)
        _state.update { it.copy(operation = ExportOperationState.Idle) }
    }

    fun close() {
        cancel()
        _state.value.coverPhotoPath?.let(exportService::deleteTemporaryFile)
        _state.update { it.copy(coverPhotoPath = null) }
    }

    private fun updateOperation(id: Long, operation: ExportOperationState) {
        if (generationId == id) _state.update { it.copy(operation = operation) }
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
