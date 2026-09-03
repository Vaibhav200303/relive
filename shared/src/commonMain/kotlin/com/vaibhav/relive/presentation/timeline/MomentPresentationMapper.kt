package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.presentation.date.EditorialDateFormatter
import com.vaibhav.relive.presentation.date.EditorialTimeFormatter

fun Moment.toPresentation(): MomentPresentation = MomentPresentation(
    id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    formattedDate = EditorialDateFormatter.format(createdAt),
    formattedTime = EditorialTimeFormatter.format(createdAt),
    title = title,
    content = content,
    locationLabel = location?.readableLabel(),
    location = location,
    isFavorite = isFavorite,
    feeling = feeling,
    tags = tags,
    attachments = attachments
        .sortedBy { it.sortIndex }
        .map { MomentAttachmentPresentation(id = it.id.value, storageRef = it.storageRef, type = it.type) },
)

internal fun ReliveLocation.readableLabel(): String? {
    val parts = listOfNotNull(placeName, locality, region, country)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return parts.firstOrNull()?.let(::formatLocationLabel)
}

internal fun formatLocationLabel(value: String): String = value
    .trim()
    .replaceFirstChar { it.uppercase() }

/** Reconstructs the domain entity held by a timeline item for an edit/forget action. */
fun MomentPresentation.toMoment(): Moment = Moment(
    id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    title = title,
    content = content,
    isFavorite = isFavorite,
    feeling = feeling,
    location = location,
    tags = tags,
    attachments = attachments.mapIndexed { index, attachment ->
        MediaAttachment(
            id = MediaAttachmentId(attachment.id),
            type = attachment.type,
            storageRef = attachment.storageRef,
            sortIndex = index,
        )
    },
)
