package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.ArchiveInsights

/** Loads archive statistics without exposing database or filesystem details to presentation. */
interface ArchiveInsightsRepository {
    suspend fun load(): ArchiveInsights
}
