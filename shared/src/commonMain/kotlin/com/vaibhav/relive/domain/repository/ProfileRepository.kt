package com.vaibhav.relive.domain.repository

import com.vaibhav.relive.domain.model.ProfileSnapshot
import kotlinx.coroutines.flow.Flow

/** Read-only Profile data. Counts are database projections, not hydrated Moments. */
interface ProfileRepository {
    fun observeProfile(): Flow<ProfileSnapshot>
}
