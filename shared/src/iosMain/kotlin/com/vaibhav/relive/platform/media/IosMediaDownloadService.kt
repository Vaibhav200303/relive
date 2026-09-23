package com.vaibhav.relive.platform.media

import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosMediaDownloadService(private val mediaStore: MediaStore) : MediaDownloadService {
    override suspend fun download(
        attachments: List<MomentAttachmentPresentation>,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): MediaDownloadResult {
        if (attachments.isEmpty()) return MediaDownloadResult(0, 0)
        onProgress(0, attachments.size)
        val downloadable = attachments.filter { it.type == MediaType.Image || it.type == MediaType.Video }
        val saved = suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                changeBlock = {
                    downloadable.forEach { attachment ->
                        val url = NSURL.fileURLWithPath(mediaStore.resolveAbsolutePath(attachment.storageRef))
                        when (attachment.type) {
                            MediaType.Image -> PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(url)
                            MediaType.Video -> PHAssetChangeRequest.creationRequestForAssetFromVideoAtFileURL(url)
                            MediaType.Audio -> Unit
                        }
                    }
                },
                completionHandler = { success, _ ->
                    onProgress(attachments.size, attachments.size)
                    continuation.resume(if (success) downloadable.size else 0)
                },
            )
        }
        return MediaDownloadResult(saved, attachments.size - saved)
    }
}
