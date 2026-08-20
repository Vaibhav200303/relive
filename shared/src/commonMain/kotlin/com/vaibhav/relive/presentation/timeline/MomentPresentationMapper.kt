package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.presentation.date.EditorialDateFormatter

/**
 * Pure conversion from domain [Moment] to the immutable view-model shape the UI
 * consumes. Held outside the ViewModel so the mapping is unit-testable on its own.
 */
fun Moment.toPresentation(): MomentPresentation = MomentPresentation(
    id = id,
    formattedDate = EditorialDateFormatter.format(createdAt),
    title = title,
    content = content,
    locationLabel = location?.readableLabel(),
    isFavorite = isFavorite,
)

internal fun ReliveLocation.readableLabel(): String? {
    val parts = listOfNotNull(placeName, locality, region, country)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return parts.firstOrNull()
}
