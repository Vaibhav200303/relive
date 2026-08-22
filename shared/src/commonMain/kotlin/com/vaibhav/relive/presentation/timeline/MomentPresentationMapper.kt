package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.presentation.date.EditorialDateFormatter
import com.vaibhav.relive.presentation.date.EditorialTimeFormatter

fun Moment.toPresentation(): MomentPresentation = MomentPresentation(
    id = id,
    formattedDate = EditorialDateFormatter.format(createdAt),
    formattedTime = EditorialTimeFormatter.format(createdAt),
    title = title,
    content = content,
    locationLabel = location?.readableLabel(),
    isFavorite = isFavorite,
    tags = tags,
    attachments = attachments
        .sortedBy { it.sortIndex }
        .map { MomentAttachmentPresentation(storageRef = it.storageRef, type = it.type) },
)

internal fun ReliveLocation.readableLabel(): String? {
    val parts = listOfNotNull(placeName, locality, region, country)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return parts.firstOrNull()
}
