package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.time.Instant
import java.util.Calendar
import java.util.TimeZone

actual object RediscoverCalendar {
    actual fun localDate(instant: Instant): LocalCalendarDate = Calendar.getInstance(TimeZone.getDefault()).apply {
        timeInMillis = instant.epochMilliseconds
    }.let { calendar ->
        LocalCalendarDate(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
        )
    }

    actual fun startOfDay(date: LocalCalendarDate): Instant = Instant(Calendar.getInstance(TimeZone.getDefault()).apply {
        clear()
        set(date.year, date.month - 1, date.day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis)

    actual fun millisecondsUntilNextDay(instant: Instant): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = instant.epochMilliseconds }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return (calendar.timeInMillis - instant.epochMilliseconds).coerceAtLeast(1L)
    }

    actual fun nextDayStart(date: LocalCalendarDate): Instant = Instant(
        Calendar.getInstance(TimeZone.getDefault()).apply {
            clear()
            set(date.year, date.month - 1, date.day, 0, 0, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis,
    )

    actual fun pickerMillis(date: LocalCalendarDate): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(date.year, date.month - 1, date.day, 0, 0, 0)
    }.timeInMillis

    actual fun dateFromPickerMillis(millis: Long): LocalCalendarDate =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }.let {
            LocalCalendarDate(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
}
