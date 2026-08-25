package com.vaibhav.relive.presentation.date

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

@OptIn(ExperimentalForeignApi::class)
actual object BackupTimestampFormatter {
    actual fun format(epochMilliseconds: Long, nowEpochMilliseconds: Long): String {
        val date = NSDate.dateWithTimeIntervalSince1970(epochMilliseconds / 1000.0)
        val now = NSDate.dateWithTimeIntervalSince1970(nowEpochMilliseconds / 1000.0)
        val calendar = NSCalendar.currentCalendar
        val same = calendar.isDate(date, inSameDayAsDate = now)
        val yesterday = calendar.dateByAddingUnit(platform.Foundation.NSCalendarUnitDay, -1, toDate = now, options = 0u)
        val formatter = NSDateFormatter().apply { locale = NSLocale.currentLocale; dateFormat = "h:mm a" }
        return when {
            same -> "Today, ${formatter.stringFromDate(date)}"
            yesterday != null && calendar.isDate(date, inSameDayAsDate = yesterday) -> "Yesterday, ${formatter.stringFromDate(date)}"
            else -> { formatter.dateFormat = "d MMM yyyy, h:mm a"; formatter.stringFromDate(date) }
        }
    }
}
