package com.vaibhav.relive.domain.model

/** Read-only summary of the persisted media that belongs to the Relive archive. */
data class ArchiveInsights(
    val momentCount: Long,
    val attachmentCount: Long,
    val photo: ArchiveMediaCategorySummary,
    val video: ArchiveMediaCategorySummary,
    val audio: ArchiveMediaCategorySummary,
    val other: ArchiveMediaCategorySummary,
    val unavailableFileCount: Long,
) {
    val totalBytes: Long = saturatedAdd(
        saturatedAdd(photo.bytes, video.bytes),
        saturatedAdd(audio.bytes, other.bytes),
    )
}

data class ArchiveMediaCategorySummary(
    val attachmentCount: Long = 0,
    val bytes: Long = 0,
)

enum class ArchiveMediaCategory {
    Photo,
    Video,
    Audio,
    Other,
}

sealed interface ArchiveFileInspection {
    data class Available(val bytes: Long) : ArchiveFileInspection
    data object Missing : ArchiveFileInspection
    data object Inaccessible : ArchiveFileInspection
}

internal fun saturatedAdd(left: Long, right: Long): Long =
    if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
