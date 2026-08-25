package com.vaibhav.relive.presentation.date

expect object BackupTimestampFormatter {
    fun format(epochMilliseconds: Long, nowEpochMilliseconds: Long): String
}
