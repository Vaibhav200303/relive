package com.vaibhav.relive

import com.vaibhav.relive.presentation.date.BackupTimestampFormatter
import com.vaibhav.relive.presentation.profile.formatByteSize
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupPresentationFormattingTest {
    @Test fun todayUsesTodayLabel() {
        val now = Calendar.getInstance()
        val value = (now.clone() as Calendar).apply { add(Calendar.MINUTE, -10) }
        assertTrue(BackupTimestampFormatter.format(value.timeInMillis, now.timeInMillis).startsWith("Today, "))
    }

    @Test fun yesterdayUsesYesterdayLabel() {
        val now = Calendar.getInstance()
        val value = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        assertTrue(BackupTimestampFormatter.format(value.timeInMillis, now.timeInMillis).startsWith("Yesterday, "))
    }

    @Test fun olderDateIncludesYear() {
        val now = Calendar.getInstance()
        val value = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -10) }
        assertTrue(BackupTimestampFormatter.format(value.timeInMillis, now.timeInMillis).contains(value.get(Calendar.YEAR).toString()))
    }

    @Test fun byteFormatterAndPluralizationValues() {
        assertEquals("539 KB", formatByteSize(552_314))
        assertEquals("1 moment", "1 moment")
        assertEquals("2 moments", "2 moments")
        assertEquals("0 moments", "0 moments")
    }
}
