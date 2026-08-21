package com.vaibhav.relive.platform.media

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.vaibhav.relive.domain.model.MediaType
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

/**
 * Android picker handles. Uses the system Photo Picker for images/videos and
 * `OpenMultipleDocuments` for audio, so the app never requests broad storage
 * permission. All entry points accept multi-selection and preserve the order
 * the picker returns.
 */
@Composable
actual fun rememberMediaPickerHandle(mediaStore: MediaStore): MediaPickerHandle {
    val context = LocalContext.current
    val store = mediaStore as? AndroidMediaStore
        ?: error("Android media picker requires AndroidMediaStore")

    val pendingImages = remember { PendingPick() }
    val pendingVideos = remember { PendingPick() }
    val pendingAudio = remember { PendingPick() }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MULTI_SELECT_MAX),
    ) { uris: List<Uri> -> pendingImages.deliver(uris) }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MULTI_SELECT_MAX),
    ) { uris: List<Uri> -> pendingVideos.deliver(uris) }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> -> pendingAudio.deliver(uris) }

    return remember(context, store) {
        object : MediaPickerHandle {
            override suspend fun pickImage(): List<RawMedia> {
                imageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
                return pendingImages.await().mapNotNull { copyToTmp(context, store, it, MediaType.Image) }
            }
            override suspend fun pickVideo(): List<RawMedia> {
                videoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                )
                return pendingVideos.await().mapNotNull { copyToTmp(context, store, it, MediaType.Video) }
            }
            override suspend fun pickAudio(): List<RawMedia> {
                audioLauncher.launch(arrayOf("audio/*"))
                return pendingAudio.await().mapNotNull { copyToTmp(context, store, it, MediaType.Audio) }
            }
        }
    }
}

private class PendingPick {
    private var deferred: CompletableDeferred<List<Uri>>? = null
    fun deliver(uris: List<Uri>) { deferred?.complete(uris); deferred = null }
    suspend fun await(): List<Uri> {
        val d = CompletableDeferred<List<Uri>>().also { deferred = it }
        return d.await()
    }
}

private fun copyToTmp(
    context: Context,
    store: AndroidMediaStore,
    uri: Uri,
    type: MediaType,
): RawMedia? {
    val ext = store.extensionFor(type)
    val tmp = store.newTempFile("relive-src-${UUID.randomUUID()}-", ".$ext")
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        RawMedia(type = type, sourcePath = tmp.absolutePath, ownedByRelive = true)
    } catch (_: Throwable) {
        tmp.delete()
        null
    }
}

/** Upper bound matching the Photo Picker cap on modern Android. */
private const val MULTI_SELECT_MAX = 50
