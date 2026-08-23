package com.vaibhav.relive.presentation.date

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.time.Instant
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970

actual object RediscoverCalendar {
    actual fun localDate(instant: Instant): LocalCalendarDate {
        val calendar = NSCalendar.currentCalendar.apply { timeZone = NSTimeZone.localTimeZone }
        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochMilliseconds / 1000.0)
        val components = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
            fromDate = date,
        )
        return LocalCalendarDate(components.year.toInt(), components.month.toInt(), components.day.toInt())
    }

    actual fun startOfDay(date: LocalCalendarDate): Instant {
        val calendar = NSCalendar.currentCalendar.apply { timeZone = NSTimeZone.localTimeZone }
        val components = NSDateComponents().apply {
            year = date.year.toLong()
            month = date.month.toLong()
            day = date.day.toLong()
        }
        val resolved = calendar.dateFromComponents(components)
            ?: error("Unable to resolve local calendar day")
        return Instant((resolved.timeIntervalSince1970 * 1000).toLong())
    }

    actual fun millisecondsUntilNextDay(instant: Instant): Long {
        val today = localDate(instant)
        val calendar = NSCalendar.currentCalendar.apply { timeZone = NSTimeZone.localTimeZone }
        val start = NSDate.dateWithTimeIntervalSince1970(startOfDay(today).epochMilliseconds / 1000.0)
        val next = calendar.dateByAddingUnit(
            NSCalendarUnitDay,
            value = 1,
            toDate = start,
            options = 0u,
        )
            ?: return 60_000L
        return ((next.timeIntervalSince1970 * 1000).toLong() - instant.epochMilliseconds).coerceAtLeast(1L)
    }

    actual fun nextDayStart(date: LocalCalendarDate): Instant {
        val calendar = NSCalendar.currentCalendar.apply { timeZone = NSTimeZone.localTimeZone }
        val start = NSDate.dateWithTimeIntervalSince1970(startOfDay(date).epochMilliseconds / 1000.0)
        val next = calendar.dateByAddingUnit(NSCalendarUnitDay, 1, start, 0u)
            ?: error("Unable to resolve next local calendar day")
        return Instant((next.timeIntervalSince1970 * 1000).toLong())
    }

    actual fun pickerMillis(date: LocalCalendarDate): Long {
        val calendar = NSCalendar.currentCalendar.apply { timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)!! }
        val components = NSDateComponents().apply { year = date.year.toLong(); month = date.month.toLong(); day = date.day.toLong() }
        return ((calendar.dateFromComponents(components) ?: error("Unable to resolve picker date")).timeIntervalSince1970 * 1000).toLong()
    }

    actual fun dateFromPickerMillis(millis: Long): LocalCalendarDate {
        val calendar = NSCalendar.currentCalendar.apply { timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)!! }
        val components = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
            fromDate = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0),
        )
        return LocalCalendarDate(components.year.toInt(), components.month.toInt(), components.day.toInt())
    }
}
