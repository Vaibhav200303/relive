package com.vaibhav.relive.domain.model

/**
 * Local aggregation for the Mood insights surface (PRODUCT_SPEC §10A, ADR-0064).
 * Everything here is pure arithmetic over day-resolved samples: the presentation
 * layer resolves each Moment's `createdAt` to a device-local [LocalCalendarDate]
 * through its platform calendar seam and hands the result in, so this calculator
 * stays platform-free and exactly testable.
 */

/** One Moment reduced to its device-local calendar day and optional feeling. */
data class MoodDaySample(
    val date: LocalCalendarDate,
    val feeling: MomentFeeling?,
)

/** A calendar week's verdict, present only when the week has at least one felt Moment. */
data class MoodWeekSummary(
    val averageScore: Float,
    val verdict: MomentFeeling,
)

/** One day of the current week's curve; scores are null on days with no felt Moments. */
data class MoodDayPoint(
    val date: LocalCalendarDate,
    val averageScore: Float?,
    val verdict: MomentFeeling?,
)

/** One calendar month of the six-month curve; scores are null for unfelt months. */
data class MoodMonthPoint(
    val year: Int,
    val month: Int,
    val averageScore: Float?,
    val verdict: MomentFeeling?,
)

data class MoodInsights(
    /** The Sunday–Saturday week before the one containing today. */
    val lastWeek: MoodWeekSummary?,
    /** The Sunday–Saturday week containing today. */
    val thisWeek: MoodWeekSummary?,
    /** Exactly seven entries, Sunday first, for the week containing today. */
    val weekDays: List<MoodDayPoint>,
    /** Exactly six entries, oldest first, ending at the month containing today. */
    val months: List<MoodMonthPoint>,
    /** Felt Moments per feeling over the last 28 days including today. */
    val splitCounts: Map<MomentFeeling, Int>,
    /** All Moments — felt or not — over the last 28 days including today. */
    val momentCount: Int,
    /** Consecutive days with at least one Moment, ending today or yesterday. */
    val streakDays: Int,
) {
    val hasAnyFeeling: Boolean =
        lastWeek != null || thisWeek != null || months.any { it.averageScore != null }
}

object MoodInsightsCalculator {

    /** Verdict boundaries shared by the bar, the curves, and the month points. */
    fun verdictOf(averageScore: Float): MomentFeeling = when {
        averageScore >= 2.5f -> MomentFeeling.Great
        averageScore >= 1.75f -> MomentFeeling.Good
        else -> MomentFeeling.Low
    }

    /** Days of history the sample projection must cover: six calendar months plus margin. */
    const val HISTORY_DAYS: Long = 190L

    private const val SPLIT_WINDOW_DAYS = 28L
    private const val MONTH_BUCKETS = 6

    fun calculate(samples: List<MoodDaySample>, today: LocalCalendarDate): MoodInsights {
        val todayEpoch = today.toEpochDay()
        val thisWeekStart = todayEpoch - today.dayOfWeekIndex()
        val lastWeekStart = thisWeekStart - 7

        val feltScoresByDay = HashMap<Long, MutableList<Int>>()
        val momentDays = HashSet<Long>()
        var momentsInWindow = 0
        val splitCounts = HashMap<MomentFeeling, Int>()
        val splitWindowStart = todayEpoch - (SPLIT_WINDOW_DAYS - 1)

        for (sample in samples) {
            val day = sample.date.toEpochDay()
            momentDays.add(day)
            if (day in splitWindowStart..todayEpoch) {
                momentsInWindow++
                sample.feeling?.let { splitCounts[it] = (splitCounts[it] ?: 0) + 1 }
            }
            sample.feeling?.let { feeling ->
                feltScoresByDay.getOrPut(day) { mutableListOf() }.add(feeling.score)
            }
        }

        fun weekSummary(weekStart: Long): MoodWeekSummary? {
            val scores = (weekStart until weekStart + 7).flatMap { feltScoresByDay[it].orEmpty() }
            if (scores.isEmpty()) return null
            val average = scores.sum().toFloat() / scores.size
            return MoodWeekSummary(averageScore = average, verdict = verdictOf(average))
        }

        val weekDays = (0 until 7).map { offset ->
            val day = thisWeekStart + offset
            val scores = feltScoresByDay[day].orEmpty()
            val average = if (scores.isEmpty()) null else scores.sum().toFloat() / scores.size
            MoodDayPoint(
                date = localDateOfEpochDay(day),
                averageScore = average,
                verdict = average?.let(::verdictOf),
            )
        }

        val feltScoresByMonth = HashMap<Int, MutableList<Int>>()
        for (sample in samples) {
            val feeling = sample.feeling ?: continue
            val bucket = sample.date.year * 12 + (sample.date.month - 1)
            feltScoresByMonth.getOrPut(bucket) { mutableListOf() }.add(feeling.score)
        }
        val currentMonthBucket = today.year * 12 + (today.month - 1)
        val months = ((MONTH_BUCKETS - 1) downTo 0).map { back ->
            val bucket = currentMonthBucket - back
            val scores = feltScoresByMonth[bucket].orEmpty()
            val average = if (scores.isEmpty()) null else scores.sum().toFloat() / scores.size
            MoodMonthPoint(
                year = bucket.floorDiv(12),
                month = bucket.mod(12) + 1,
                averageScore = average,
                verdict = average?.let(::verdictOf),
            )
        }

        var streak = 0
        var cursor = if (todayEpoch in momentDays) todayEpoch else todayEpoch - 1
        while (cursor in momentDays) {
            streak++
            cursor--
        }

        return MoodInsights(
            lastWeek = weekSummary(lastWeekStart),
            thisWeek = weekSummary(thisWeekStart),
            weekDays = weekDays,
            months = months,
            splitCounts = splitCounts,
            momentCount = momentsInWindow,
            streakDays = streak,
        )
    }
}

/*
 * Proleptic-Gregorian civil-date arithmetic (Howard Hinnant's days_from_civil /
 * civil_from_days), so week and streak math needs no platform calendar. Day zero
 * is 1970-01-01, a Thursday.
 */

fun LocalCalendarDate.toEpochDay(): Long {
    val adjustedYear = year - if (month <= 2) 1 else 0
    val era = (if (adjustedYear >= 0) adjustedYear else adjustedYear - 399) / 400
    val yearOfEra = adjustedYear - era * 400
    val dayOfYear = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146097L + dayOfEra - 719468L
}

fun localDateOfEpochDay(epochDay: Long): LocalCalendarDate {
    val shifted = epochDay + 719468L
    val era = (if (shifted >= 0) shifted else shifted - 146096) / 146097
    val dayOfEra = shifted - era * 146097
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
    val year = yearOfEra + era * 400
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear + 2) / 153
    val day = (dayOfYear - (153 * monthPrime + 2) / 5 + 1).toInt()
    val month = (monthPrime + if (monthPrime < 10) 3 else -9).toInt()
    return LocalCalendarDate(
        year = (year + if (month <= 2) 1 else 0).toInt(),
        month = month,
        day = day,
    )
}

fun LocalCalendarDate.plusDays(days: Long): LocalCalendarDate =
    localDateOfEpochDay(toEpochDay() + days)

/** 0 = Sunday … 6 = Saturday, matching the Mood insights week (PRODUCT_SPEC §10A). */
fun LocalCalendarDate.dayOfWeekIndex(): Int = (toEpochDay() + 4).mod(7L).toInt()
