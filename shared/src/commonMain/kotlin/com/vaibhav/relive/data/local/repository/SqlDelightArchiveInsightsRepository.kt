package com.vaibhav.relive.data.local.repository

import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.domain.model.ArchiveInsights
import com.vaibhav.relive.domain.model.ArchiveAttachmentReference
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.calculateArchiveInsights
import com.vaibhav.relive.domain.repository.ArchiveInsightsRepository
import com.vaibhav.relive.platform.media.MediaStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightArchiveInsightsRepository(
    private val database: ReliveDatabase,
    private val mediaStore: MediaStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ArchiveInsightsRepository {
    override suspend fun load(): ArchiveInsights = withContext(dispatcher) {
        calculateArchiveInsights(
            momentCount = database.momentsQueries.countMoments().executeAsOne(),
            attachments = database.mediaAttachmentsQueries.selectArchiveAttachmentRefs().executeAsList().map {
                ArchiveAttachmentReference(mediaType = it.media_type, storageRef = it.storage_ref)
            },
            inspect = { rawRef ->
                runCatching { MediaStorageRef(rawRef) }.getOrNull()
                    ?.let(mediaStore::inspectManagedFile)
                    ?: com.vaibhav.relive.domain.model.ArchiveFileInspection.Inaccessible
            },
        )
    }
}
