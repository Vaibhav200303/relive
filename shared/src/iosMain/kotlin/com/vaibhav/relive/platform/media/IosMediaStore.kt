package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUUID

/**
 * iOS [MediaStore] using the app's Application Support directory. Keys are
 * opaque relative paths of the form `images/{uuid}.jpg`; physical resolution
 * happens exclusively via [resolveAbsolutePath].
 */
@OptIn(ExperimentalForeignApi::class)
class IosMediaStore : MediaStore {

    private val fileManager = NSFileManager.defaultManager
    private val root: NSURL = run {
        val urls = fileManager.URLsForDirectory(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
        )
        val base = urls.firstOrNull() as? NSURL
            ?: error("No application support directory")
        val dir = base.URLByAppendingPathComponent("relive-media", isDirectory = true)!!
        fileManager.createDirectoryAtURL(dir, withIntermediateDirectories = true, attributes = null, error = null)
        listOf("images", "videos", "audio", "tmp").forEach {
            val sub = dir.URLByAppendingPathComponent(it, isDirectory = true)!!
            fileManager.createDirectoryAtURL(sub, withIntermediateDirectories = true, attributes = null, error = null)
        }
        dir
    }

    override fun extensionFor(type: MediaType): String = when (type) {
        MediaType.Image -> "jpg"
        MediaType.Video -> "mp4"
        MediaType.Audio -> "m4a"
    }

    private fun subDir(type: MediaType): String = when (type) {
        MediaType.Image -> "images"
        MediaType.Video -> "videos"
        MediaType.Audio -> "audio"
    }

    override fun allocateKey(type: MediaType): MediaStorageRef {
        val name = NSUUID().UUIDString + "." + extensionFor(type)
        return MediaStorageRef("${subDir(type)}/$name")
    }

    override fun resolveAbsolutePath(ref: MediaStorageRef): String =
        root.URLByAppendingPathComponent(ref.value)?.path ?: error("Bad ref: ${ref.value}")

    override fun exists(ref: MediaStorageRef): Boolean =
        fileManager.fileExistsAtPath(resolveAbsolutePath(ref))

    override fun delete(ref: MediaStorageRef) {
        fileManager.removeItemAtPath(resolveAbsolutePath(ref), error = null)
    }

    override fun sizeBytes(ref: MediaStorageRef): Long {
        val attrs = fileManager.attributesOfItemAtPath(resolveAbsolutePath(ref), error = null) ?: return 0
        val n = attrs["NSFileSize"] as? NSNumber ?: return 0
        return n.longLongValue
    }

    fun newTempPath(extension: String): String {
        val name = "relive-tmp-${NSUUID().UUIDString}.$extension"
        return root.URLByAppendingPathComponent("tmp/$name")?.path ?: error("Bad tmp path")
    }

    val rootUrl: NSURL get() = root
}
