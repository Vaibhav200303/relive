package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.TimelineHomeSummary
import kotlinx.coroutines.flow.Flow

/** Read-only, bounded projection for the Timeline Home collection cards. */
interface TimelineHomeRepository {
    fun observeSummaries(): Flow<List<TimelineHomeSummary>>
}
