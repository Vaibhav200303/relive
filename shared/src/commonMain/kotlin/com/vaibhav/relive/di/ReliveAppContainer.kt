package com.vaibhav.relive.di

import com.vaibhav.relive.domain.repository.MomentRepository

/**
 * Shared app-level dependency container. Platform entry points construct this once
 * (see `androidApp`'s `MainActivity` and iosMain's `MainViewController`) and hand
 * the same shape to `App`. Later phases add fields here (timeline repository,
 * media store, location provider, entitlement provider …) without changing platform
 * bootstrap code.
 */
class ReliveAppContainer(
    val momentRepository: MomentRepository,
)
