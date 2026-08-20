package com.vaibhav.relive.platform.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vaibhav.relive.domain.model.MediaType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.NSItemProvider
import platform.Foundation.NSURL
import platform.Foundation.NSFileManager
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeAudio
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberMediaPickerHandle(mediaStore: MediaStore): MediaPickerHandle {
    val store = mediaStore as? IosMediaStore
        ?: error("iOS media picker requires IosMediaStore")
    return remember(store) {
        object : MediaPickerHandle {
            override suspend fun pickImage(): List<RawMedia> =
                presentPHPicker(store, MediaType.Image)
            override suspend fun pickVideo(): List<RawMedia> =
                presentPHPicker(store, MediaType.Video)
            override suspend fun pickAudio(): List<RawMedia> =
                presentDocumentPicker(store)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun presentPHPicker(store: IosMediaStore, type: MediaType): List<RawMedia> {
    val cfg = PHPickerConfiguration().apply {
        selectionLimit = 1
        filter = when (type) {
            MediaType.Image -> PHPickerFilter.imagesFilter
            MediaType.Video -> PHPickerFilter.videosFilter
            else -> null
        }
    }
    val deferred = CompletableDeferred<List<RawMedia>>()
    val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
        override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
            picker.dismissViewControllerAnimated(true, completion = null)
            val results = didFinishPicking.filterIsInstance<PHPickerResult>()
            if (results.isEmpty()) { deferred.complete(emptyList()); return }
            // Copy each result to tmp file synchronously.
            val out = mutableListOf<RawMedia>()
            var pending = results.size
            results.forEach { res ->
                val provider: NSItemProvider = res.itemProvider
                val ext = store.extensionFor(type)
                val destPath = store.newTempPath(ext)
                provider.loadFileRepresentationForTypeIdentifier(
                    if (type == MediaType.Image) "public.image" else "public.movie",
                ) { url, _ ->
                    if (url != null) {
                        try {
                            NSFileManager.defaultManager.copyItemAtURL(
                                url, NSURL.fileURLWithPath(destPath), error = null,
                            )
                            synchronized(out) { out += RawMedia(type, destPath, ownedByRelive = true) }
                        } catch (_: Throwable) { /* ignore */ }
                    }
                    if (--pending == 0) deferred.complete(out.toList())
                }
            }
        }
    }
    val picker = PHPickerViewController(configuration = cfg)
    picker.delegate = delegate
    presentModal(picker)
    return deferred.await()
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun presentDocumentPicker(store: IosMediaStore): List<RawMedia> {
    val deferred = CompletableDeferred<List<RawMedia>>()
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeAudio))
    val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val urls = didPickDocumentsAtURLs.filterIsInstance<NSURL>()
            val out = mutableListOf<RawMedia>()
            urls.forEach { url ->
                val ext = url.pathExtension ?: "m4a"
                val destPath = store.newTempPath(ext)
                if (url.startAccessingSecurityScopedResource()) {
                    try {
                        NSFileManager.defaultManager.copyItemAtURL(
                            url, NSURL.fileURLWithPath(destPath), error = null,
                        )
                        out += RawMedia(MediaType.Audio, destPath, ownedByRelive = true)
                    } finally { url.stopAccessingSecurityScopedResource() }
                }
            }
            deferred.complete(out)
        }
        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            deferred.complete(emptyList())
        }
    }
    picker.delegate = delegate
    presentModal(picker)
    return deferred.await()
}

@OptIn(ExperimentalForeignApi::class)
private fun presentModal(vc: UIViewController) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    root.presentViewController(vc, animated = true, completion = null)
}

// K/N does not provide `synchronized`; fallback minimal no-op wrapper.
private inline fun <R> synchronized(lock: Any, block: () -> R): R = block()
