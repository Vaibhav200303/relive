package com.vaibhav.relive.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mood insights aggregation (PRODUCT_SPEC §10A, ADR-0066). Every window here is expressed in
 * device-local calendar days, so the calculator can be pinned exactly without a platform clock.
 */
class MoodInsightsCalculatorTest {

    // 2026-09-03 is a Thursday, so its Sunday–Saturday week starts 2026-08-30.
    private val today = LocalCalendarDate(2026, 9, 3)

    private fun sample(date: LocalCalendarDate, feeling: MomentFeeling?) = MoodDaySample(date, feeling)

    @Test
    fun `epoch day round trips and knows its weekday`() {
        // 1970-01-01 was a Thursday: index 4 with Sunday as 0.
        assertEquals(0L, LocalCalendarDate(1970, 1, 1).toEpochDay())
        assertEquals(4, LocalCalendarDate(1970, 1, 1).dayOfWeekIndex())
        assertEquals(0, LocalCalendarDate(2026, 8, 30).dayOfWeekIndex(), "2026-08-30 is a Sunday")
        assertEquals(4, today.dayOfWeekIndex(), "2026-09-03 is a Thursday")

        // Month, year and leap-day boundaries must survive the round trip exactly.
        listOf(
            LocalCalendarDate(2024, 2, 29),
            LocalCalendarDate(2024, 3, 1),
            LocalCalendarDate(2025, 12, 31),
            LocalCalendarDate(2026, 1, 1),
        ).forEach { date ->
            assertEquals(date, localDateOfEpochDay(date.toEpochDay()), "round trip $date")
        }
        assertEquals(LocalCalendarDate(2024, 3, 1), LocalCalendarDate(2024, 2, 29).plusDays(1))
        assertEquals(LocalCalendarDate(2024, 2, 29), LocalCalendarDate(2024, 3, 1).plusDays(-1))
    }

    @Test
    fun `weeks are bucketed Sunday to Saturday around today`() {
        val insights = MoodInsightsCalculator.calculate(
            samples = listOf(
                // This week: Sunday 08-30 and Thursday 09-03.
                sample(LocalCalendarDate(2026, 8, 30), MomentFeeling.Great),
                sample(today, MomentFeeling.Great),
                // Last week: Sunday 08-23 through Saturday 08-29.
                sample(LocalCalendarDate(2026, 8, 23), MomentFeeling.Low),
                sample(LocalCalendarDate(2026, 8, 29), MomentFeeling.Good),
                // The Saturday before last week — outside both windows.
                sample(LocalCalendarDate(2026, 8, 22), MomentFeeling.Great),
            ),
            today = today,
        )

        assertEquals(MomentFeeling.Great, insights.thisWeek?.verdict)
        assertEquals(3f, insights.thisWeek?.averageScore)
        // (1 + 2) / 2 = 1.5, which is below the Good boundary.
        assertEquals(1.5f, insights.lastWeek?.averageScore)
        assertEquals(MomentFeeling.Low, insights.lastWeek?.verdict)
    }

    @Test
    fun `verdict boundaries are exact`() {
        assertEquals(MomentFeeling.Great, MoodInsightsCalculator.verdictOf(2.5f))
        assertEquals(MomentFeeling.Good, MoodInsightsCalculator.verdictOf(2.49f))
        assertEquals(MomentFeeling.Good, MoodInsightsCalculator.verdictOf(1.75f))
        assertEquals(MomentFeeling.Low, MoodInsightsCalculator.verdictOf(1.74f))
    }

    @Test
    fun `unfelt windows report nothing rather than a fabricated value`() {
        val insights = MoodInsightsCalculator.calculate(
            samples = listOf(
                // Moments exist, but none of them carries a feeling.
                sample(today, null),
                sample(LocalCalendarDate(2026, 8, 25), null),
            ),
            today = today,
        )

        assertNull(insights.thisWeek)
        assertNull(insights.lastWeek)
        assertTrue(insights.weekDays.all { it.averageScore == null && it.verdict == null })
        assertTrue(insights.months.all { it.averageScore == null })
        assertTrue(insights.splitCounts.isEmpty())
        assertEquals(false, insights.hasAnyFeeling)
        // Unfelt Moments still count as Moments.
        assertEquals(2, insights.momentCount)
    }

    @Test
    fun `week days cover Sunday through Saturday of this week only`() {
        val insights = MoodInsightsCalculator.calculate(
            samples = listOf(
                sample(LocalCalendarDate(2026, 8, 30), MomentFeeling.Good),
                sample(LocalCalendarDate(2026, 9, 5), MomentFeeling.Great),
                // Belongs to last week, so it must not appear in this week's days.
                sample(LocalCalendarDate(2026, 8, 29), MomentFeeling.Low),
            ),
            today = today,
        )

        assertEquals(7, insights.weekDays.size)
        assertEquals(LocalCalendarDate(2026, 8, 30), insights.weekDays.first().date)
        assertEquals(LocalCalendarDate(2026, 9, 5), insights.weekDays.last().date)
        assertEquals(MomentFeeling.Good, insights.weekDays[0].verdict)
        assertEquals(MomentFeeling.Great, insights.weekDays[6].verdict)
        assertTrue(insights.weekDays.subList(1, 6).all { it.averageScore == null })
    }

    @Test
    fun `a day averages every feeling recorded on it`() {
        val insights = MoodInsightsCalculator.calculate(
            samples = listOf(
                sample(today, MomentFeeling.Great),
                sample(today, MomentFeeling.Low),
            ),
            today = today,
        )

        // (3 + 1) / 2 = 2 → Good, on the day and in the week alike.
        assertEquals(2f, insights.weekDays[4].averageScore)
        assertEquals(MomentFeeling.Good, insights.weekDays[4].verdict)
        assertEquals(2f, insights.thisWeek?.averageScore)
    }

    @Test
    fun `months are the six calendar buckets ending at today`() {
        val insights = MoodInsightsCalculator.calculate(
            samples = listOf(
                sample(LocalCalendarDate(2026, 4, 10), MomentFeeling.Good),
                sample(LocalCalendarDate(2026, 9, 1), MomentFeeling.Great),
                // Older than the six buckets, so it must be excluded.
                sample(LocalCalendarDate(2026, 3, 31), MomentFeeling.Low),
            ),
            today = today,
        )

        assertEquals(6, insights.months.size)
        assertEquals(2026 to 4, insights.months.first().year to insights.months.first().month)
        assertEquals(2026 to 9, insights.months.last().year to insights.months.last().month)
        assertEquals(2f, insights.months.first().averageScore)
        assertEquals(3f, insights.months.last().averageScore)
        assertNull(insights.months[1].averageScore, "May had nothing felt")
    }

    @Test
    fun `month buckets cross the year boundary`() {
        val insights = MoodInsightsCalculator.calculate(
            samples = listOf(sample(LocalCalendarDate(2025, 11, 4), MomentFeeling.Great)),
            today = LocalCalendarDate(2026, 2, 10),
        )

        assertEquals(listOf(9, 10, 11, 12, 1, 2), insights.months.map { it.month })
        assertEquals(listOf(2025, 2025, 2025, 2025, 2026, 2026), insights.months.map { it.year })
        assertEquals(3f, insights.months[2].averageScore, "November 2025 holds the sample")
    }

    @Test
    fun `the split window counts felt moments over the last 28 days`() {
        val insights = MoodInsightsCalculator.calculate(
            samples = listOf(
                sample(today, MomentFeeling.Great),
                sample(today.plusDays(-27), MomentFeeling.Great),
                // The 28-day window is inclusive of today, so day -28 falls outside it.
                sample(today.plusDays(-28), MomentFeeling.Great),
                sample(today.plusDays(-3), MomentFeeling.Low),
                sample(today.plusDays(-4), null),
            ),
            today = today,
        )

        assertEquals(2, insights.splitCounts[MomentFeeling.Great])
        assertEquals(1, insights.splitCounts[MomentFeeling.Low])
        assertNull(insights.splitCounts[MomentFeeling.Good])
        // Four samples fall in the window, including the unfelt one.
        assertEquals(4, insights.momentCount)
    }

    @Test
    fun `streaks run back from today and count unfelt moments too`() {
        val insights = MoodInsightsCalculator.calculate(
            samples = listOf(
                sample(today, MomentFeeling.Good),
                sample(today.plusDays(-1), null),
                sample(today.plusDays(-2), MomentFeeling.Great),
                // Day -3 is missing, so the streak stops at three.
                sample(today.plusDays(-4), MomentFeeling.Great),
            ),
            today = today,
        )

        assertEquals(3, insights.streakDays)
        assertTrue(insights.hasAnyFeeling)
    }

    @Test
    fun `a streak that ended yesterday still counts`() {
        val insights = MoodInsightsCalculator.calculate(
            samples = listOf(
                sample(today.plusDays(-1), MomentFeeling.Good),
                sample(today.plusDays(-2), MomentFeeling.Good),
            ),
            today = today,
        )

        assertEquals(2, insights.streakDays)
    }

    @Test
    fun `an empty archive produces a complete, empty shape`() {
        val insights = MoodInsightsCalculator.calculate(samples = emptyList(), today = today)

        assertNull(insights.thisWeek)
        assertNull(insights.lastWeek)
        assertEquals(7, insights.weekDays.size)
        assertEquals(6, insights.months.size)
        assertEquals(0, insights.momentCount)
        assertEquals(0, insights.streakDays)
        assertEquals(false, insights.hasAnyFeeling)
        assertNotNull(insights.splitCounts)
    }
}
