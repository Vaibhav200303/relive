package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType

/**
 * Normalizes a raw source blob into an optimized, app-owned copy stored via
 * [MediaStore]. Themes are never baked in here — the output is theme-neutral.
 *
 * Failure semantics: on exception the processor removes any partial output it
 * wrote and rethrows. It never leaves an orphan file behind.
 *
 * Image profile: orientation-corrected, max long edge ~1920 px, JPEG ~82%
 * quality; downscale-only.
 * Video profile: max 720p, H.264 + AAC, moderate bitrate; no upscaling.
 * Audio profile: AAC/M4A at voice-appropriate bitrate.
 */
interface MediaProcessor {
    suspend fun process(raw: RawMedia): ProcessedMedia
}

data class ProcessedMedia(
    val type: MediaType,
    val storageRef: MediaStorageRef,
    val durationMs: Long? = null,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
)
