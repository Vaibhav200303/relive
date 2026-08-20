package com.vaibhav.relive.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TimelineTest {

    @Test
    fun all_is_singleton_and_not_a_custom_timeline() {
        val a: Timeline = Timeline.All
        val b: Timeline = Timeline.All
        assertSame(a, b)
        assertTrue(a !is Timeline.Custom)
    }

    @Test
    fun custom_timeline_requires_non_blank_name() {
        assertFails { Timeline.Custom(TimelineId("t"), "   ") }
    }

    @Test
    fun custom_timeline_name_length_capped() {
        val over = "x".repeat(Timeline.Custom.MAX_NAME_LENGTH + 1)
        assertFails { Timeline.Custom(TimelineId("t"), over) }
    }

    @Test
    fun custom_timeline_theme_optional() {
        val t = Timeline.Custom(TimelineId("t-japan"), "Japan 2026")
        assertEquals(null, t.theme)
        val themed = t.copy(theme = ThemeReference.FilmMemory)
        assertNotNull(themed.theme)
    }

    @Test
    fun membership_binds_moment_to_custom_timeline_only() {
        val mem = CustomTimelineMembership(TimelineId("tl"), MomentId("m"))
        assertEquals("tl", mem.timelineId.value)
        assertEquals("m", mem.momentId.value)
    }

    @Test
    fun all_membership_is_never_modeled_as_a_row() {
        // Compile-time proof: CustomTimelineMembership only exposes TimelineId, and
        // Timeline.All has no id. Any attempt to reference it as a membership target
        // would fail to compile. The runtime check below just documents intent.
        val allType: Timeline = Timeline.All
        assertTrue(allType !is Timeline.Custom)
    }
}
