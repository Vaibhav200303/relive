package com.vaibhav.relive.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchiveVisualWeightsTest {
    @Test
    fun weights_sum_to_one_and_keep_tiny_non_zero_categories_visible() {
        val weights = archiveVisualWeights(listOf(995L, 4L, 1L), totalBytes = 1_000L)

        assertEquals(1f, weights.sum(), absoluteTolerance = 0.0001f)
        assertTrue(weights[2] >= 0.02f)
        assertEquals(0f, archiveVisualWeights(listOf(995L, 0L, 5L), 1_000L)[1])
    }

    @Test
    fun single_category_keeps_the_full_bar() {
        assertEquals(listOf(1f), archiveVisualWeights(listOf(42L), totalBytes = 42L))
    }
}
