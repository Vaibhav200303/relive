package com.vaibhav.relive.platform.media

import com.vaibhav.relive.presentation.timeline.MomentAttachmentPresentation

/** Copies Relive-owned media into the user's platform media library. */
interface MediaDownloadService {
    suspend fun download(
        attachments: List<MomentAttachmentPresentation>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): MediaDownloadResult
}

data class MediaDownloadResult(
    val savedCount: Int,
    val failedCount: Int,
) {
    val isSuccess: Boolean get() = savedCount > 0 && failedCount == 0
}

object UnavailableMediaDownloadService : MediaDownloadService {
    override suspend fun download(
        attachments: List<MomentAttachmentPresentation>,
        onProgress: (completed: Int, total: Int) -> Unit,
    ) =
        MediaDownloadResult(savedCount = 0, failedCount = attachments.size)
}
