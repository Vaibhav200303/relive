package com.vaibhav.relive.domain.exporting

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId

enum class ExportFormat { KeepsakePdf, ReliveArchive }

enum class DiaryPaper(val displayName: String) {
    WarmCream("Warm cream"),
    BlushPink("Blush pink"),
    SageGreen("Sage green"),
    Lavender("Lavender"),
    PowderBlue("Powder blue"),
    SoftPeach("Soft peach"),
}

sealed interface ExportScope {
    data object All : ExportScope
    data class Custom(val timelineId: TimelineId, val name: String) : ExportScope
}

data class MagazineOptions(
    val title: String,
    val subtitle: String,
    val coverPhotoPath: String? = null,
    val startDate: LocalCalendarDate? = null,
    val endDate: LocalCalendarDate? = null,
    val paper: DiaryPaper = DiaryPaper.WarmCream,
)

data class ExportRequest(
    val format: ExportFormat,
    val scope: ExportScope = ExportScope.All,
    val magazine: MagazineOptions? = null,
)

data class ExportProgress(
    val completed: Long,
    val total: Long,
    val phase: String,
) {
    val fraction: Float?
        get() = total.takeIf { it > 0 }?.let { completed.toFloat().div(it).coerceIn(0f, 1f) }
}

data class ExportResult(
    val path: String,
    val filename: String,
    val mimeType: String,
    val format: ExportFormat,
)

data class PortableArchiveSummary(
    val formatVersion: Int,
    val exportedAtEpochMilliseconds: Long,
    val momentCount: Int,
    val timelineCount: Int,
    val earliestMomentEpochMilliseconds: Long?,
    val latestMomentEpochMilliseconds: Long?,
)

data class PortableArchiveSnapshot(
    val exportedAtEpochMilliseconds: Long,
    val moments: List<Moment>,
    val timelines: List<Timeline.Custom>,
    val memberships: Map<MomentId, Set<TimelineId>>,
    val allTimelineAppearance: TimelineAppearance,
    val exportedTimeline: PortableTimelineIdentity = PortableTimelineIdentity(),
)

/** The timeline identity frozen into a portable export, independent of later live edits. */
data class PortableTimelineIdentity(
    val name: String = "All moments",
    val customTimelineId: TimelineId? = null,
    val appearance: TimelineAppearance = TimelineAppearance(),
    val coverPhotoRef: com.vaibhav.relive.domain.model.MediaStorageRef? = null,
)

data class PortableArchiveSession(
    val summary: PortableArchiveSummary,
    val snapshot: PortableArchiveSnapshot,
    val mediaRootPath: String,
    val sessionPath: String,
)

data class PortableArchiveEntry(
    val storageRef: String,
    val archivePath: String,
    val byteCount: Long,
    val sha256: String,
    val mediaType: String,
)

data class PortableArchiveManifest(
    val format: String = "com.vaibhav.relive.portable-archive",
    val formatVersion: Int = RELIVE_ARCHIVE_FORMAT_VERSION,
    val appVersion: String,
    val exportedAtEpochMilliseconds: Long,
    val momentCount: Int,
    val timelineCount: Int,
    val earliestMomentEpochMilliseconds: Long?,
    val latestMomentEpochMilliseconds: Long?,
    val archiveSha256: String,
    val media: List<PortableArchiveEntry>,
)

sealed interface ExportOperationState {
    data object Idle : ExportOperationState
    data class Preparing(val format: ExportFormat) : ExportOperationState
    data class Working(val format: ExportFormat, val progress: ExportProgress) : ExportOperationState
    data class Ready(val result: ExportResult) : ExportOperationState
    data class Failed(val message: String) : ExportOperationState
}

const val RELIVE_ARCHIVE_MIME_TYPE: String = "application/vnd.vaibhav.relive.archive"
const val RELIVE_ARCHIVE_UTI: String = "com.vaibhav.relive.archive"
const val RELIVE_ARCHIVE_FORMAT_VERSION: Int = 1
