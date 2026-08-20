package com.vaibhav.relive.di

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock

/**
 * Shared app-level dependency container. Platform entry points construct this once
 * (see `androidApp`'s `MainActivity` and iosMain's `MainViewController`) and hand
 * the same shape to `App`. Later phases add fields here (timeline repository,
 * media store, location provider, entitlement provider …) without changing platform
 * bootstrap code.
 *
 * [clock] and [idGenerator] are the composer's identity/time seams — kept on the
 * container so shared UI stays platform-free and tests can substitute both
 * deterministically.
 */
class ReliveAppContainer(
    val momentRepository: MomentRepository,
    val clock: Clock,
    val idGenerator: IdGenerator,
)
