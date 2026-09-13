package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.Moment

fun exportFilenameTitle(value: String): String = value
    .trim()
    .replace(Regex("[^A-Za-z0-9]+"), "-")
    .trim('-')
    .take(60)
    .ifEmpty { "My-Relive" }

internal fun filterMomentsForMagazine(
    moments: List<Moment>,
    start: LocalCalendarDate?,
    end: LocalCalendarDate?,
    dateOf: (Moment) -> LocalCalendarDate,
): List<Moment> {
    require(start == null || end == null || start <= end) { "The start date must be on or before the end date." }
    return moments.filter { moment ->
        val date = dateOf(moment)
        (start == null || date >= start) && (end == null || date <= end)
    }.sortedBy { it.createdAt.epochMilliseconds }
}

private operator fun LocalCalendarDate.compareTo(other: LocalCalendarDate): Int =
    compareValuesBy(this, other, LocalCalendarDate::year, LocalCalendarDate::month, LocalCalendarDate::day)
