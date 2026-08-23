package com.vaibhav.relive.presentation.rediscover

import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Duration
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import com.vaibhav.relive.presentation.viewer.openAt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RediscoverViewModel(
    private val repository: RediscoverRepository,
    private val clock: Clock,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RediscoverState())
    val state: StateFlow<RediscoverState> = _state.asStateFlow()

    private var overviewJob: Job? = null
    private var midnightJob: Job? = null

    init { refreshTemporalContext() }

    fun refreshTemporalContext() {
        overviewJob?.cancel()
        midnightJob?.cancel()
        val now = clock.now()
        val today = RediscoverCalendar.localDate(now)
        val query = RediscoverQuery(
            today = today,
            startOfToday = RediscoverCalendar.startOfDay(today),
            recentCutoff = now - Duration.ofDays(90),
            dailySeed = today.year.toLong() * 10_000L + today.month * 100L + today.day,
        )
        _state.update { it.copy(content = RediscoverContent.Loading, mediaViewer = null) }
        overviewJob = scope.launch {
            runCatching {
                repository.observeOverview(query).collect { overview ->
                    _state.update {
                        it.copy(
                            content = if (overview.totalMomentCount == 0L) {
                                RediscoverContent.EmptyArchive
                            } else {
                                RediscoverContent.Loaded(overview)
                            },
                        )
                    }
                }
            }.onFailure {
                _state.update { state -> state.copy(content = RediscoverContent.Failed) }
            }
        }
        midnightJob = scope.launch {
            delay(RediscoverCalendar.millisecondsUntilNextDay(now))
            refreshTemporalContext()
        }
    }

    fun openMedia(attachments: List<MediaAttachment>, index: Int = 0) {
        if (attachments.isEmpty()) return
        val presentation = attachments.sortedBy { it.sortIndex }.map {
            MomentAttachmentPresentation(id = it.id.value, storageRef = it.storageRef, type = it.type)
        }
        _state.update { it.copy(mediaViewer = openAt(presentation, index)) }
    }

    fun setViewerIndex(index: Int) {
        _state.update { state -> state.copy(mediaViewer = state.mediaViewer?.withCurrent(index)) }
    }

    fun closeViewer() {
        _state.update { it.copy(mediaViewer = null) }
    }
}
