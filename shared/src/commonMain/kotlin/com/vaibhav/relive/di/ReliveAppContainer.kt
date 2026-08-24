package com.vaibhav.relive.di

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.repository.AppearanceRepository
import com.vaibhav.relive.domain.repository.ArchiveInsightsRepository
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.ProfileRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.repository.TimelineHomeRepository
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.media.MediaProcessor
import com.vaibhav.relive.platform.media.MediaStore

/**
 * Shared app-level dependency container. Platform entry points construct this
 * once (see `androidApp`'s `MainActivity` and iosMain's `MainViewController`)
 * and hand the same shape to `App`.
 *
 * Media capture handles (picker, camera surface, audio recorder) are
 * composition-scoped and remembered inside the UI layer via the
 * `platform/media` expect-composables, so they are not fields of the
 * container. The container carries the process-lifetime pieces:
 * `MediaStore` (app-owned file storage) and `MediaProcessor` (compression /
 * normalization).
 */
class ReliveAppContainer(
    val appearanceRepository: AppearanceRepository,
    val archiveInsightsRepository: ArchiveInsightsRepository,
    val momentRepository: MomentRepository,
    val timelineRepository: TimelineRepository,
    val timelineHomeRepository: TimelineHomeRepository,
    val rediscoverRepository: RediscoverRepository,
    val profileRepository: ProfileRepository,
    val clock: Clock,
    val idGenerator: IdGenerator,
    val mediaStore: MediaStore,
    val mediaProcessor: MediaProcessor,
)
