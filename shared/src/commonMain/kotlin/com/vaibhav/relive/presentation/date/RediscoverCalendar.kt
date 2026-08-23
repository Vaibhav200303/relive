package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.time.Instant

/** Platform calendar seam for Rediscover's current-device-local rules. */
expect object RediscoverCalendar {
    fun localDate(instant: Instant): LocalCalendarDate
    fun startOfDay(date: LocalCalendarDate): Instant
    fun millisecondsUntilNextDay(instant: Instant): Long
    fun nextDayStart(date: LocalCalendarDate): Instant
    fun pickerMillis(date: LocalCalendarDate): Long
    fun dateFromPickerMillis(millis: Long): LocalCalendarDate
}

fun LocalCalendarDate.editorialDayMonth(): String {
    val monthName = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    ).getOrElse(month - 1) { error("Invalid local calendar month: $month") }
    return "$day $monthName"
}

fun anniversaryYearLabel(currentYear: Int, momentYear: Int): String {
    val years = currentYear - momentYear
    require(years > 0) { "Anniversary Moments must be from a previous calendar year" }
    return if (years == 1) "1 YEAR AGO" else "$years YEARS AGO"
}
