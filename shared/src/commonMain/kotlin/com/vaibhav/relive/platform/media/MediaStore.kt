package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType

/**
 * App-owned media storage. Stores only optimized/normalized files produced by
 * [MediaProcessor]. Never touches the user's external source files. Callers
 * refer to stored blobs exclusively through opaque relative [MediaStorageRef]
 * keys; the physical location is a platform detail.
 */
interface MediaStore {

    /** Filesystem extension for a media type (e.g. "jpg", "mp4", "m4a"). */
    fun extensionFor(type: MediaType): String

    /** Reserves an opaque relative key for a new blob of the given [type]. */
    fun allocateKey(type: MediaType): MediaStorageRef

    /** Absolute path resolver for internal platform code only. */
    fun resolveAbsolutePath(ref: MediaStorageRef): String

    /** True if a blob is present at [ref]. */
    fun exists(ref: MediaStorageRef): Boolean

    /** Deletes the blob at [ref]. Safe to call on missing refs. */
    fun delete(ref: MediaStorageRef)

    /** Byte size of the stored blob, or 0 if missing. */
    fun sizeBytes(ref: MediaStorageRef): Long
}
