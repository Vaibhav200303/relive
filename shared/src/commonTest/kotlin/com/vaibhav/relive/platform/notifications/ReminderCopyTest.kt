package com.vaibhav.relive.platform.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderCopyTest {
    @Test
    fun sameSeedIsDeterministic() {
        assertEquals(
            selectReminderCopy(ReminderKind.Capture, 5),
            selectReminderCopy(ReminderKind.Capture, 5),
        )
    }

    @Test
    fun copyRotatesAcrossConsecutiveSeeds() {
        val titles = (0 until 4).map { selectReminderCopy(ReminderKind.Capture, it).title }.toSet()
        assertTrue(titles.size > 1, "consecutive days should not all show the same capture copy")
    }

    @Test
    fun negativeSeedStaysInBounds() {
        // Must not throw or return an out-of-range entry.
        val copy = selectReminderCopy(ReminderKind.Rediscover, -3)
        assertTrue(copy.title.isNotBlank() && copy.body.isNotBlank())
    }

    @Test
    fun everyVariantIsNonEmptyAndCarriesNoArchiveField() {
        // Guards the ADR-0046 privacy rule: reminder copy is static text with no interpolation
        // slot, so a Moment's title/text/location can never be substituted in.
        for (kind in ReminderKind.entries) {
            for (seed in 0 until 8) {
                val copy = selectReminderCopy(kind, seed)
                assertTrue(copy.title.isNotBlank(), "title blank for $kind seed $seed")
                assertTrue(copy.body.isNotBlank(), "body blank for $kind seed $seed")
                assertTrue('{' !in copy.title && '}' !in copy.title, "title has a template slot")
                assertTrue('{' !in copy.body && '}' !in copy.body, "body has a template slot")
            }
        }
    }
}
