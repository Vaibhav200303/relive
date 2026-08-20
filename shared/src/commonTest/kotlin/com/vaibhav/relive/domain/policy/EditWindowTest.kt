package com.vaibhav.relive.domain.policy

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Duration
import com.vaibhav.relive.domain.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val CREATED = Instant(1_700_000_000_000L)

private fun moment(updatedAt: Instant? = null): Moment = Moment(
    id = MomentId("m"),
    createdAt = CREATED,
    updatedAt = updatedAt,
    title = "t",
    content = "c",
)

private fun clockAt(instant: Instant): Clock = Clock { instant }

class EditWindowTest {

    @Test
    fun editable_at_creation_moment() {
        val m = moment()
        assertTrue(EditWindow.isEditable(m, CREATED))
        assertTrue(EditWindow.isForgettable(m, CREATED))
    }

    @Test
    fun editable_at_one_day_in() {
        val now = CREATED + Duration.ofDays(1)
        assertTrue(EditWindow.isEditable(moment(), now))
    }

    @Test
    fun editable_just_before_boundary() {
        val now = CREATED + Duration.ofDays(4) - Duration.ofMilliseconds(1)
        assertTrue(EditWindow.isEditable(moment(), now))
    }

    @Test
    fun not_editable_at_exact_boundary() {
        val boundary = CREATED + Duration.ofDays(4)
        assertFalse(EditWindow.isEditable(moment(), boundary))
        assertFalse(EditWindow.isForgettable(moment(), boundary))
    }

    @Test
    fun not_editable_after_boundary() {
        val after = CREATED + Duration.ofDays(4) + Duration.ofMilliseconds(1)
        assertFalse(EditWindow.isEditable(moment(), after))
    }

    @Test
    fun expiresAt_is_createdAt_plus_four_days() {
        val expected = CREATED + Duration.ofDays(4)
        assertEquals(expected, EditWindow.expiresAt(moment()))
    }

    @Test
    fun updatedAt_does_not_extend_window() {
        val editedNear = CREATED + Duration.ofDays(3)
        val m = moment(updatedAt = editedNear)
        val afterOriginalWindow = CREATED + Duration.ofDays(4)
        assertFalse(EditWindow.isEditable(m, afterOriginalWindow))
        // Even far past original window while updatedAt is recent → still not editable.
        val farLater = CREATED + Duration.ofDays(10)
        assertFalse(EditWindow.isEditable(m.copy(updatedAt = farLater - Duration.ofHours(1)), farLater))
    }

    @Test
    fun one_ms_before_createdAt_not_editable() {
        val before = CREATED - Duration.ofMilliseconds(1)
        assertFalse(EditWindow.isEditable(moment(), before))
        assertFalse(EditWindow.isForgettable(moment(), before))
    }

    @Test
    fun far_before_createdAt_not_editable() {
        val skewed = CREATED - Duration.ofHours(1)
        assertFalse(EditWindow.isEditable(moment(), skewed))
        assertFalse(EditWindow.isForgettable(moment(), skewed))
    }

    @Test
    fun clock_overload_matches_instant_overload() {
        val now = CREATED + Duration.ofDays(2)
        assertEquals(
            EditWindow.isEditable(moment(), now),
            EditWindow.isEditable(moment(), clockAt(now)),
        )
    }

    @Test
    fun forgettable_matches_editable() {
        val now = CREATED + Duration.ofDays(2)
        val m = moment()
        assertEquals(EditWindow.isEditable(m, now), EditWindow.isForgettable(m, now))
    }
}
