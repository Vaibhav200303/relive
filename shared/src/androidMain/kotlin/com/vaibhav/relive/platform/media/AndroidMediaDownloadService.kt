package com.vaibhav.relive.platform.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore as AndroidProviderMediaStore
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class AndroidMediaDownloadService(
    private val context: Context,
    private val mediaStore: MediaStore,
) : MediaDownloadService {
    override suspend fun download(
        attachments: List<MomentAttachmentPresentation>,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): MediaDownloadResult =
        withContext(Dispatchers.IO) {
            var saved = 0
            onProgress(0, attachments.size)
            attachments.forEachIndexed { index, attachment ->
                coroutineContext.ensureActive()
                if (runCatching { saveOne(attachment, index) }.getOrDefault(false)) saved++
                onProgress(index + 1, attachments.size)
            }
            MediaDownloadResult(saved, attachments.size - saved)
        }

    private fun saveOne(attachment: MomentAttachmentPresentation, index: Int): Boolean {
        val source = File(mediaStore.resolveAbsolutePath(attachment.storageRef))
        if (!source.isFile) return false
        val extension = source.extension.ifBlank { mediaStore.extensionFor(attachment.type) }
        val name = "Relive-${System.currentTimeMillis()}-${index + 1}.$extension"
        val resolver = context.contentResolver
        val collection = when (attachment.type) {
            MediaType.Image -> AndroidProviderMediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.Video -> AndroidProviderMediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.Audio -> AndroidProviderMediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(AndroidProviderMediaStore.MediaColumns.DISPLAY_NAME, name)
            put(AndroidProviderMediaStore.MediaColumns.MIME_TYPE, when (extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "heic", "heif" -> "image/heic"
                "mov" -> "video/quicktime"
                "m4a" -> "audio/mp4"
                "aac" -> "audio/aac"
                "wav" -> "audio/wav"
                else -> when (attachment.type) {
                    MediaType.Image -> "image/jpeg"
                    MediaType.Video -> "video/mp4"
                    MediaType.Audio -> "audio/mpeg"
                }
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(AndroidProviderMediaStore.MediaColumns.RELATIVE_PATH, when (attachment.type) {
                    MediaType.Image -> "Pictures/Relive"
                    MediaType.Video -> "Movies/Relive"
                    MediaType.Audio -> "Music/Relive"
                })
                put(AndroidProviderMediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val destination = resolver.insert(collection, values) ?: return false
        return try {
            resolver.openOutputStream(destination)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(destination, ContentValues().apply { put(AndroidProviderMediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
            true
        } catch (failure: Throwable) {
            resolver.delete(destination, null, null)
            false
        }
    }
}
