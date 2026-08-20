package com.vaibhav.relive.debug

import com.vaibhav.relive.domain.repository.MomentRepository

/**
 * Debug variant: hands the shared container a purely in-memory repository seeded
 * with representative moments so the timeline can be verified visually without
 * touching the on-device SQLite file. This code is compiled only into debug builds.
 */
fun debugMomentRepositoryOverrideOrNull(): MomentRepository = DebugSampleMomentRepository()
