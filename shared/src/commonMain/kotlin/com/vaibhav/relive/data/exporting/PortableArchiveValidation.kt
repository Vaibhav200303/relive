package com.vaibhav.relive.data.exporting

import com.vaibhav.relive.domain.exporting.PortableArchiveManifest
import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot

internal fun validatePortableArchiveMetadata(
    snapshot: PortableArchiveSnapshot,
    manifest: PortableArchiveManifest,
    maxTotalMediaBytes: Long,
) {
    require(snapshot.exportedAtEpochMilliseconds == manifest.exportedAtEpochMilliseconds) { "Archive export dates do not match." }
    require(snapshot.moments.size == manifest.momentCount && snapshot.timelines.size == manifest.timelineCount) { "Archive counts do not match." }
    require(snapshot.moments.distinctBy { it.id }.size == snapshot.moments.size) { "Duplicate Moment id." }
    require(snapshot.timelines.distinctBy { it.id }.size == snapshot.timelines.size) { "Duplicate timeline id." }
    val attachments = snapshot.moments.flatMap { it.attachments }
    require(attachments.distinctBy { it.id }.size == attachments.size) { "Duplicate attachment id." }

    val momentIds = snapshot.moments.mapTo(mutableSetOf()) { it.id }
    val timelineIds = snapshot.timelines.mapTo(mutableSetOf()) { it.id }
    require(snapshot.memberships.all { (moment, memberships) -> moment in momentIds && memberships.all { it in timelineIds } }) {
        "Archive membership is invalid."
    }
    require(snapshot.exportedTimeline.name.isNotBlank() && snapshot.exportedTimeline.name.length <= 100) {
        "Exported timeline name is invalid."
    }
    snapshot.exportedTimeline.customTimelineId?.let { exportedId ->
        val timeline = snapshot.timelines.singleOrNull { it.id == exportedId }
        require(
            timeline != null &&
                timeline.name == snapshot.exportedTimeline.name &&
                timeline.appearance == snapshot.exportedTimeline.appearance &&
                timeline.coverPhotoRef == snapshot.exportedTimeline.coverPhotoRef,
        ) { "Exported timeline identity does not match its timeline." }
        require(snapshot.moments.all { exportedId in snapshot.memberships[it.id].orEmpty() }) {
            "An exported Moment is outside the selected timeline."
        }
    }

    val expectedTypes = linkedMapOf<String, String>()
    attachments.forEach { attachment ->
        val previous = expectedTypes.put(attachment.storageRef.value, attachment.type.name)
        require(previous == null || previous == attachment.type.name) { "A media reference has conflicting types." }
    }
    snapshot.timelines.mapNotNull { it.coverPhotoRef }.forEach { ref ->
        val previous = expectedTypes.put(ref.value, "Image")
        require(previous == null || previous == "Image") { "A timeline cover has a conflicting media type." }
    }
    snapshot.exportedTimeline.coverPhotoRef?.let { ref ->
        val previous = expectedTypes.put(ref.value, "Image")
        require(previous == null || previous == "Image") { "The exported cover has a conflicting media type." }
    }

    require(manifest.media.distinctBy { it.storageRef }.size == manifest.media.size) { "Duplicate media reference." }
    require(manifest.media.mapTo(mutableSetOf()) { it.storageRef } == expectedTypes.keys) { "Archive media inventory does not match." }
    manifest.media.forEach { item ->
        require(isSafePortableStorageRef(item.storageRef) && isSafePortableArchivePath(item.archivePath)) { "This archive contains an unsafe media path." }
        require(item.archivePath.startsWith("media/") && item.byteCount >= 0) { "Archive media metadata is invalid." }
        require(item.sha256.matches(Regex("[0-9a-f]{64}"))) { "Archive media checksum is invalid." }
        require(item.mediaType == expectedTypes[item.storageRef]) { "Archive media type does not match." }
    }
    manifest.media.groupBy { it.archivePath }.values.forEach { aliases ->
        require(aliases.map { Triple(it.byteCount, it.sha256, it.mediaType) }.distinct().size == 1) { "Conflicting deduplicated media entries." }
    }
    require(manifest.media.distinctBy { it.archivePath }.sumOf { it.byteCount } <= maxTotalMediaBytes) { "Archive media is too large." }

    val times = snapshot.moments.map { it.createdAt.epochMilliseconds }
    require(times.minOrNull() == manifest.earliestMomentEpochMilliseconds && times.maxOrNull() == manifest.latestMomentEpochMilliseconds) {
        "Archive date span does not match."
    }
}

internal fun isSafePortableArchivePath(value: String): Boolean =
    value.isNotBlank() &&
        !value.startsWith('/') &&
        !value.contains('\\') &&
        value.split('/').none { it.isBlank() || it == "." || it == ".." }

internal fun isSafePortableStorageRef(value: String): Boolean =
    value.matches(Regex("(?:images|videos|audio)/[^/\\\\.][^/\\\\]*")) && !value.contains("..")
