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
import java.io.File
import java.util.UUID

@Composable
actual fun rememberMediaPickerHandle(mediaStore: MediaStore): MediaPickerHandle {
    val context = LocalContext.current
    val store = mediaStore as? AndroidMediaStore
        ?: error("Android media picker requires AndroidMediaStore")

    val pendingImage = remember { PendingPick() }
    val pendingVideo = remember { PendingPick() }
    val pendingAudio = remember { PendingPick() }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> pendingImage.deliver(uris = listOfNotNull(uri)) }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> pendingVideo.deliver(uris = listOfNotNull(uri)) }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> pendingAudio.deliver(uris = listOfNotNull(uri)) }

    return remember(context, store) {
        object : MediaPickerHandle {
            override suspend fun pickImage(): List<RawMedia> {
                imageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
                return pendingImage.await().mapNotNull { copyToTmp(context, store, it, MediaType.Image) }
            }
            override suspend fun pickVideo(): List<RawMedia> {
                videoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                )
                return pendingVideo.await().mapNotNull { copyToTmp(context, store, it, MediaType.Video) }
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
