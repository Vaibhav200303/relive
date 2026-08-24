package com.vaibhav.relive.domain.model

data class ArchiveAttachmentReference(
    val mediaType: String,
    val storageRef: String,
)

fun calculateArchiveInsights(
    momentCount: Long,
    attachments: List<ArchiveAttachmentReference>,
    inspect: (String) -> ArchiveFileInspection,
): ArchiveInsights {
    val counts = LongArray(ArchiveMediaCategory.entries.size)
    val categoriesByRef = linkedMapOf<String, MutableSet<ArchiveMediaCategory>>()
    attachments.forEach { attachment ->
        val category = archiveCategoryFor(attachment.mediaType)
        counts[category.ordinal] = saturatedAdd(counts[category.ordinal], 1L)
        categoriesByRef.getOrPut(attachment.storageRef) { linkedSetOf() }.add(category)
    }

    val bytes = LongArray(ArchiveMediaCategory.entries.size)
    var unavailable = 0L
    categoriesByRef.forEach { (ref, categories) ->
        when (val inspection = inspect(ref)) {
            is ArchiveFileInspection.Available -> {
                val category = if (categories.size == 1) categories.first() else ArchiveMediaCategory.Other
                bytes[category.ordinal] = saturatedAdd(bytes[category.ordinal], inspection.bytes)
            }
            ArchiveFileInspection.Missing,
            ArchiveFileInspection.Inaccessible,
            -> unavailable = saturatedAdd(unavailable, 1L)
        }
    }

    return ArchiveInsights(
        momentCount = momentCount,
        attachmentCount = attachments.size.toLong(),
        photo = ArchiveMediaCategorySummary(counts[ArchiveMediaCategory.Photo.ordinal], bytes[ArchiveMediaCategory.Photo.ordinal]),
        video = ArchiveMediaCategorySummary(counts[ArchiveMediaCategory.Video.ordinal], bytes[ArchiveMediaCategory.Video.ordinal]),
        audio = ArchiveMediaCategorySummary(counts[ArchiveMediaCategory.Audio.ordinal], bytes[ArchiveMediaCategory.Audio.ordinal]),
        other = ArchiveMediaCategorySummary(counts[ArchiveMediaCategory.Other.ordinal], bytes[ArchiveMediaCategory.Other.ordinal]),
        unavailableFileCount = unavailable,
    )
}

private fun archiveCategoryFor(raw: String): ArchiveMediaCategory = when (raw) {
    "Image" -> ArchiveMediaCategory.Photo
    "Video" -> ArchiveMediaCategory.Video
    "Audio" -> ArchiveMediaCategory.Audio
    else -> ArchiveMediaCategory.Other
}
