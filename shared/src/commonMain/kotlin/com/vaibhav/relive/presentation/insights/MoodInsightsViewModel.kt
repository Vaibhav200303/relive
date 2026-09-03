package com.vaibhav.relive.presentation.insights

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.MoodDaySample
import com.vaibhav.relive.domain.model.MoodInsights
import com.vaibhav.relive.domain.model.MoodInsightsCalculator
import com.vaibhav.relive.domain.model.plusDays
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.domain.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * State holder for the Home mood bar and the in-place Mood insights (PRODUCT_SPEC §10A).
 *
 * Reads only the bounded `(createdAt, feeling)` sample projection — never Moments —
 * resolves each sample to its device-local calendar day through the platform
 * calendar seam, and delegates every number to the pure [MoodInsightsCalculator].
 * [refreshToday] re-anchors the windows when the local day rolls over.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoodInsightsViewModel(
    private val momentRepository: MomentRepository,
    private val clock: Clock,
    scope: CoroutineScope,
) {

    private val today = MutableStateFlow(RediscoverCalendar.localDate(clock.now()))

    private val _insights = MutableStateFlow<MoodInsights?>(null)
    val insights: StateFlow<MoodInsights?> = _insights.asStateFlow()

    init {
        scope.launch {
            today
                .flatMapLatest { day ->
                    val cutoff = RediscoverCalendar.startOfDay(
                        day.plusDays(-MoodInsightsCalculator.HISTORY_DAYS),
                    )
                    momentRepository.observeFeelingSamplesSince(cutoff).map { samples -> day to samples }
                }
                .collect { (day, samples) ->
                    val daySamples = samples.map { sample ->
                        MoodDaySample(
                            date = RediscoverCalendar.localDate(sample.createdAt),
                            feeling = sample.feeling,
                        )
                    }
                    _insights.value = MoodInsightsCalculator.calculate(daySamples, day)
                }
        }
    }

    fun refreshToday() {
        today.value = RediscoverCalendar.localDate(clock.now())
    }

    fun currentDay(): LocalCalendarDate = today.value
}

/** Uppercase three-letter month label for the Mood over time axis. */
fun moodMonthLabel(month: Int): String = listOf(
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
).getOrElse(month - 1) { error("Invalid month: $month") }

/** Uppercase day labels for the Weekly mood axis, Sunday first (PRODUCT_SPEC §10A). */
val MOOD_WEEKDAY_LABELS: List<String> =
    listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

/** The one-word verdict shown under each mood-bar face. */
fun moodVerdictLabel(feeling: com.vaibhav.relive.domain.model.MomentFeeling): String =
    when (feeling) {
        com.vaibhav.relive.domain.model.MomentFeeling.Great -> "Great!"
        com.vaibhav.relive.domain.model.MomentFeeling.Good -> "Good"
        com.vaibhav.relive.domain.model.MomentFeeling.Low -> "Low"
    }
