package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaType

/**
 * A pre-normalization media handle. May point at an app-owned temporary copy
 * of an external source (never the external source itself); the processor
 * is responsible for removing this after producing the optimized blob.
 *
 * Optional editor intents ([trimStartMs] / [trimEndMs] / [muteAudio]) are
 * populated by capture surfaces that offer a review editor. They are applied
 * once by [MediaProcessor] on the final export; the source file itself is
 * never rewritten while the user is still editing.
 */
data class RawMedia(
    val type: MediaType,
    val sourcePath: String,
    val ownedByRelive: Boolean,
    val trimStartMs: Long? = null,
    val trimEndMs: Long? = null,
    val muteAudio: Boolean = false,
)
