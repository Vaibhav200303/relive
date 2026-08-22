package com.vaibhav.relive.platform.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.vaibhav.relive.domain.model.MediaStorageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Platform playback surfaces. Both are Composables so no platform player
 * object crosses into shared code — implementations render a native surface
 * and manage lifecycle themselves.
 */
@Composable
expect fun RelivedImage(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier)

/**
 * Composer callers pass [posterFallbackPath] — the pre-processing source
 * file — so the ready surface can show an instant poster frame from the
 * already-warm source-thumbnail cache while the ready file's own frame is
 * being extracted. Non-composer callers (viewer, gallery) pass null to
 * preserve existing behavior.
 */
@Composable
expect fun RelivedVideo(
    ref: MediaStorageRef,
    mediaStore: MediaStore,
    modifier: Modifier,
    posterFallbackPath: String? = null,
)

@Composable
expect fun RelivedAudio(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier)

/**
 * Adaptive-collage tile variants (ADR-0019). These render fixed-shape
 * grid cells: images are center-cropped without distortion, videos show a
 * still frame with a Play affordance and do NOT instantiate a video
 * player, audio shows a black canvas with a compact Play/Pause + duration
 * (the real waveform is Stage 2).
 *
 * Perf contract (ADR performance pass): implementations MUST NOT perform
 * expensive decode/probe/player construction synchronously during
 * composition. Move that work to a background dispatcher and cache results
 * keyed by [MediaStorageRef].
 */
@Composable
expect fun RelivedImageTile(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier)

@Composable
expect fun RelivedVideoTile(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier)

@Composable
expect fun RelivedAudioTile(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier)

/**
 * Full-screen audio viewer surface (ADR-0019 §8). Preserves the Stage 2
 * waveform visual identity: black canvas → bounded centered real waveform
 * → real playback-synchronized horizontal window → minimal Play/Pause +
 * duration. Larger than the tile but must NOT stretch edge-to-edge — the
 * black negative space is intentional. Owns its own lazy player and
 * releases on Compose disposal; participates in [ActivePlayback].
 */
@Composable
expect fun RelivedAudioViewer(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier)

/**
 * Best-effort natural aspect ratio (width / height) for an image ref, or
 * null when it cannot be determined without a full decode. Callers MUST
 * invoke this off the main thread; the [rememberImageAspectRatioFor]
 * Composable is the safe entry point during composition.
 */
expect fun readImageAspectRatio(ref: MediaStorageRef, mediaStore: MediaStore): Float?

/**
 * Best-effort duration in milliseconds for a media ref via a lightweight
 * metadata read. Must be called off the main thread; the
 * [rememberMediaDurationMs] Composable is the safe entry point.
 */
expect fun readMediaDurationMs(ref: MediaStorageRef, mediaStore: MediaStore): Long?

/**
 * Best-effort natural pixel dimensions for an image ref after orientation
 * has been applied. Callers MUST invoke off the main thread; the
 * [rememberImageNaturalSizeFor] Composable is the safe entry point.
 * Returned width/height are the display-oriented pixel sizes.
 */
expect fun readImageNaturalSize(ref: MediaStorageRef, mediaStore: MediaStore): NaturalSizePx?

/**
 * Best-effort natural pixel dimensions for a video ref after track
 * preferred-transform rotation has been applied. Off-main-thread only;
 * use [rememberVideoNaturalSizeFor] during composition.
 */
expect fun readVideoNaturalSize(ref: MediaStorageRef, mediaStore: MediaStore): NaturalSizePx?

/**
 * Lightweight metadata-only probe of the video at [path] (no full decode).
 * Applies track rotation/preferredTransform so returned width/height match
 * the frame the player will show. Off-main-thread only; use
 * [rememberVideoNaturalSizeForPath] during composition. Enables the composer
 * to size the Processing placeholder to the same bounds as the eventual
 * ready video before compression finishes.
 */
expect fun readVideoNaturalSizeFromPath(path: String): NaturalSizePx?

/**
 * Renders a representative frame extracted from the video at [sourcePath]
 * (the ORIGINAL pre-processing file). Used by the composer so a video
 * placeholder shows the user what they picked while transcoding runs.
 *
 * Extraction contract: lightweight metadata-only frame grab (Android
 * MediaMetadataRetriever, iOS AVAssetImageGenerator). MUST run off the
 * main thread, release retriever/generator immediately, and be cached
 * keyed by [sourcePath] so recompositions do not re-decode. No MediaPlayer
 * / AVPlayer construction. Returns silently (empty layout) when
 * [sourcePath] is null or extraction fails so the caller's own background
 * shows through.
 */
@Composable
expect fun VideoSourceThumbnail(sourcePath: String?, modifier: Modifier)

/**
 * Best-effort real-audio amplitude envelope for [ref] with roughly
 * [targetBuckets] normalized peaks (`0f..1f`). Returns null on any decode
 * failure so the tile can fall back to a flat baseline (ADR-0019 §12).
 * MUST NOT be called on the main thread; the [rememberWaveformFor]
 * Composable is the safe entry point during composition.
 */
expect fun extractWaveform(
    ref: MediaStorageRef,
    mediaStore: MediaStore,
    targetBuckets: Int,
): FloatArray?

/**
 * Composable that returns the aspect ratio for [ref], reading from
 * [MediaPresentationCache] immediately on cache hit and probing on a
 * background dispatcher on cache miss. Result is null until the probe
 * completes (or if the probe fails).
 */
@Composable
fun rememberImageAspectRatioFor(ref: MediaStorageRef, mediaStore: MediaStore): Float? {
    val cached = MediaPresentationCache.aspectRatio(ref)
    val state = produceState(initialValue = cached, key1 = ref.value) {
        if (value != null) return@produceState
        val v = withContext(Dispatchers.Default) { readImageAspectRatio(ref, mediaStore) }
        if (v != null && v > 0f) {
            MediaPresentationCache.putAspectRatio(ref, v)
            value = v
        }
    }
    return state.value
}

/**
 * Composable that returns the media duration in ms for [ref], reading from
 * [MediaPresentationCache] immediately on cache hit and probing on a
 * background dispatcher on cache miss.
 */
@Composable
fun rememberMediaDurationMs(ref: MediaStorageRef, mediaStore: MediaStore): Long? {
    val cached = MediaPresentationCache.durationMs(ref)
    val state = produceState(initialValue = cached, key1 = ref.value) {
        if (value != null) return@produceState
        val v = withContext(Dispatchers.Default) { readMediaDurationMs(ref, mediaStore) }
        if (v != null && v > 0L) {
            MediaPresentationCache.putDurationMs(ref, v)
            value = v
        }
    }
    return state.value
}

/**
 * Returns the cached waveform envelope for [ref], extracting it once on a
 * background dispatcher on cache miss. Result is null until extraction
 * completes (or if extraction fails), so the waveform tile shows a flat
 * baseline while decoding and on error.
 */
/**
 * Composable that returns the natural pixel size for an image [ref].
 * Reads from [MediaPresentationCache] on hit and probes on a background
 * dispatcher on miss. Null until the probe completes / on failure.
 */
@Composable
fun rememberImageNaturalSizeFor(ref: MediaStorageRef, mediaStore: MediaStore): NaturalSizePx? {
    val cached = MediaPresentationCache.naturalSizePx(ref)
    val state = produceState(initialValue = cached, key1 = ref.value) {
        if (value != null) return@produceState
        val v = withContext(Dispatchers.Default) { readImageNaturalSize(ref, mediaStore) }
        if (v != null && v.widthPx > 0 && v.heightPx > 0) {
            MediaPresentationCache.putNaturalSizePx(ref, v)
            value = v
        }
    }
    return state.value
}

/**
 * Composable that returns the natural pixel size for a video [ref].
 * Reads from [MediaPresentationCache] on hit and probes on a background
 * dispatcher on miss.
 */
@Composable
fun rememberVideoNaturalSizeFor(ref: MediaStorageRef, mediaStore: MediaStore): NaturalSizePx? {
    val cached = MediaPresentationCache.naturalSizePx(ref)
    val state = produceState(initialValue = cached, key1 = ref.value) {
        if (value != null) return@produceState
        val v = withContext(Dispatchers.Default) { readVideoNaturalSize(ref, mediaStore) }
        if (v != null && v.widthPx > 0 && v.heightPx > 0) {
            MediaPresentationCache.putNaturalSizePx(ref, v)
            value = v
        }
    }
    return state.value
}

/**
 * Composable that returns the natural pixel size for a video whose bytes
 * still live at [sourcePath] (pre-processing). Reads from
 * [MediaPresentationCache] on hit and probes on a background dispatcher on
 * miss. Null until the probe completes / on failure or when [sourcePath]
 * is null.
 */
@Composable
fun rememberVideoNaturalSizeForPath(sourcePath: String?): NaturalSizePx? {
    if (sourcePath == null) return null
    val cached = MediaPresentationCache.naturalSizePxForPath(sourcePath)
    val state = produceState(initialValue = cached, key1 = sourcePath) {
        if (value != null) return@produceState
        val v = withContext(Dispatchers.Default) { readVideoNaturalSizeFromPath(sourcePath) }
        if (v != null && v.widthPx > 0 && v.heightPx > 0) {
            MediaPresentationCache.putNaturalSizePxForPath(sourcePath, v)
            value = v
        }
    }
    return state.value
}

@Composable
fun rememberWaveformFor(
    ref: MediaStorageRef,
    mediaStore: MediaStore,
    targetBuckets: Int = WaveformProcessor.DEFAULT_BUCKETS,
): FloatArray? {
    val cached = WaveformCache.get(ref)
    val state = produceState(initialValue = cached, key1 = ref.value) {
        if (value != null) return@produceState
        val v = withContext(Dispatchers.Default) { extractWaveform(ref, mediaStore, targetBuckets) }
        if (v != null && v.isNotEmpty()) {
            WaveformCache.put(ref, v)
            value = v
        }
    }
    return state.value
}
