package com.vaibhav.relive.platform.exporting

import androidx.compose.runtime.Composable
import com.vaibhav.relive.domain.exporting.ExportProgress
import com.vaibhav.relive.domain.exporting.ExportResult
import com.vaibhav.relive.domain.exporting.PortableArchiveSession
import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.exporting.MagazineOptions
import com.vaibhav.relive.platform.media.MediaStore

data class MagazineDocument(
    val moments: List<Moment>,
    val options: MagazineOptions,
    val scopeTitle: String,
    val mediaAssets: Map<String, MagazineMediaAsset> = emptyMap(),
    val fontCss: String = "",
)

data class MagazineMediaAsset(
    val printablePath: String? = null,
    val durationMilliseconds: Long? = null,
    val waveform: FloatArray? = null,
)

data class OpenedPortableArchive(
    val session: PortableArchiveSession,
    val mediaStore: MediaStore,
)

interface ReliveExportService {
    suspend fun createMagazinePdf(
        document: MagazineDocument,
        onProgress: (ExportProgress) -> Unit = {},
    ): ExportResult

    suspend fun createPortableArchive(
        snapshot: PortableArchiveSnapshot,
        onProgress: (ExportProgress) -> Unit = {},
    ): ExportResult

    suspend fun openPortableArchive(path: String): OpenedPortableArchive

    fun releasePortableArchive(archive: OpenedPortableArchive)

    fun deleteTemporaryFile(path: String)
}

interface ExportFileHandle {
    suspend fun choosePortableArchive(): String?
    suspend fun save(result: ExportResult): Boolean
    fun share(result: ExportResult): Boolean
}

@Composable
expect fun rememberExportFileHandle(): ExportFileHandle

class ExportUnavailableException(message: String) : IllegalStateException(message)
class PortableArchiveCorruptException(message: String) : IllegalArgumentException(message)
class PortableArchiveUnsupportedException(val version: Int) : IllegalArgumentException("Unsupported Relive archive version $version")

object UnavailableReliveExportService : ReliveExportService {
    private fun unavailable(): Nothing = throw ExportUnavailableException("Export is unavailable on this platform.")
    override suspend fun createMagazinePdf(document: MagazineDocument, onProgress: (ExportProgress) -> Unit) = unavailable()
    override suspend fun createPortableArchive(snapshot: PortableArchiveSnapshot, onProgress: (ExportProgress) -> Unit) = unavailable()
    override suspend fun openPortableArchive(path: String) = unavailable()
    override fun releasePortableArchive(archive: OpenedPortableArchive) = Unit
    override fun deleteTemporaryFile(path: String) = Unit
}
