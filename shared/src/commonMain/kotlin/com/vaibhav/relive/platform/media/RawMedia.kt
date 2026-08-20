package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaType

/**
 * A pre-normalization media handle. May point at an app-owned temporary copy
 * of an external source (never the external source itself); the processor
 * is responsible for removing this after producing the optimized blob.
 */
data class RawMedia(
    val type: MediaType,
    val sourcePath: String,
    val ownedByRelive: Boolean,
)
