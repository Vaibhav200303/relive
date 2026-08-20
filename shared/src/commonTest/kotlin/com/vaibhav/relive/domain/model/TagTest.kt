package com.vaibhav.relive.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagTest {

    @Test
    fun label_preserved_verbatim_after_trim() {
        val t = Tag.of("  Family Trip  ")
        assertEquals("Family Trip", t.label)
    }

    @Test
    fun blank_rejected() {
        assertFails { Tag.of("   ") }
        assertNull(Tag.ofOrNull(""))
    }

    @Test
    fun too_long_rejected() {
        val long = "x".repeat(Tag.MAX_LENGTH + 1)
        assertFails { Tag.of(long) }
    }

    @Test
    fun case_and_whitespace_only_change_equal() {
        val a = Tag.of("Family Trip")
        val b = Tag.of("family  trip")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a.label, b.label)
    }

    @Test
    fun distinct_labels_not_equal() {
        assertNotEquals(Tag.of("family"), Tag.of("travel"))
    }

    @Test
    fun ofOrNull_returns_tag_when_valid() {
        assertNotNull(Tag.ofOrNull("ok"))
    }

    @Test
    fun to_string_returns_original_label() {
        assertTrue(Tag.of("Japan 2026").toString() == "Japan 2026")
    }
}
