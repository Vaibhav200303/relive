package com.vaibhav.relive.presentation.date

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual object BackupTimestampFormatter {
    actual fun format(epochMilliseconds: Long, nowEpochMilliseconds: Long): String {
        val value = Calendar.getInstance().apply { timeInMillis = epochMilliseconds }
        val now = Calendar.getInstance().apply { timeInMillis = nowEpochMilliseconds }
        val sameDay = value.get(Calendar.ERA) == now.get(Calendar.ERA) && value.get(Calendar.YEAR) == now.get(Calendar.YEAR) && value.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        val yesterday = Calendar.getInstance().apply { timeInMillis = nowEpochMilliseconds; add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = value.get(Calendar.ERA) == yesterday.get(Calendar.ERA) && value.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && value.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMilliseconds))
        return when {
            sameDay -> "Today, $time"
            isYesterday -> "Yesterday, $time"
            else -> "${SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(epochMilliseconds))}"
        }
    }
}
