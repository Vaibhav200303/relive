package com.vaibhav.relive.platform.media

import android.content.Context
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import java.io.File
import java.util.UUID

/**
 * Android [MediaStore] implementation. Files live under
 * `context.filesDir/relive-media/{images,videos,audio,tmp}`. Storage keys are
 * opaque relative paths of the form `images/{uuid}.jpg`; the physical root is
 * an implementation detail resolved via [resolveAbsolutePath].
 */
class AndroidMediaStore(context: Context) : MediaStore {

    private val root: File = File(context.filesDir, "relive-media").apply { mkdirs() }
    private val tmpDir: File = File(root, "tmp").apply { mkdirs() }

    init {
        listOf("images", "videos", "audio").forEach { File(root, it).mkdirs() }
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
        val ext = extensionFor(type)
        val relative = "${subDir(type)}/${UUID.randomUUID()}.$ext"
        return MediaStorageRef(relative)
    }

    override fun resolveAbsolutePath(ref: MediaStorageRef): String =
        File(root, ref.value).absolutePath

    override fun exists(ref: MediaStorageRef): Boolean =
        File(root, ref.value).exists()

    override fun delete(ref: MediaStorageRef) {
        File(root, ref.value).delete()
    }

    override fun sizeBytes(ref: MediaStorageRef): Long {
        val f = File(root, ref.value)
        return if (f.exists()) f.length() else 0L
    }

    fun newTempFile(prefix: String, suffix: String): File =
        File.createTempFile(prefix, suffix, tmpDir)
}
