package com.vaibhav.relive.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveStoragePresentationTest {
    @Test
    fun percentage_rounds_the_category_share() {
        assertEquals(100, archivePercentage(bytes = 15_800L, totalBytes = 15_800L))
        assertEquals(33, archivePercentage(bytes = 1L, totalBytes = 3L))
    }

    @Test
    fun percentage_is_zero_without_measurable_bytes() {
        assertEquals(0, archivePercentage(bytes = 0L, totalBytes = 42L))
        assertEquals(0, archivePercentage(bytes = 42L, totalBytes = 0L))
    }
}
