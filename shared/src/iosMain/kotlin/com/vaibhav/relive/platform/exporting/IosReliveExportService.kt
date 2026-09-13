package com.vaibhav.relive.platform.exporting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vaibhav.relive.data.exporting.PortableArchiveJson
import com.vaibhav.relive.data.exporting.StoredZip
import com.vaibhav.relive.data.exporting.StoredZipEntry
import com.vaibhav.relive.data.exporting.isSafePortableArchivePath
import com.vaibhav.relive.data.exporting.sha256
import com.vaibhav.relive.data.exporting.validatePortableArchiveMetadata
import com.vaibhav.relive.domain.exporting.ExportFormat
import com.vaibhav.relive.domain.exporting.ExportProgress
import com.vaibhav.relive.domain.exporting.ExportResult
import com.vaibhav.relive.domain.exporting.PdfImageQuality
import com.vaibhav.relive.domain.exporting.PortableArchiveEntry
import com.vaibhav.relive.domain.exporting.PortableArchiveManifest
import com.vaibhav.relive.domain.exporting.PortableArchiveSession
import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot
import com.vaibhav.relive.domain.exporting.PortableArchiveSummary
import com.vaibhav.relive.domain.exporting.RELIVE_ARCHIVE_FORMAT_VERSION
import com.vaibhav.relive.domain.exporting.RELIVE_ARCHIVE_MIME_TYPE
import com.vaibhav.relive.domain.exporting.RELIVE_ARCHIVE_UTI
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.presentation.exporting.MagazineDocumentBuilder
import com.vaibhav.relive.presentation.exporting.exportFilenameTitle
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSMakeRange
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsBeginPDFContextToFile
import platform.UIKit.UIGraphicsBeginPDFPage
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIMarkupTextPrintFormatter
import platform.UIKit.UIPrintPageRenderer
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalForeignApi::class)
class IosReliveExportService(private val mediaStore: MediaStore) : ReliveExportService {
    private val files = NSFileManager.defaultManager
    private val temporaryRoot = NSURL.fileURLWithPath(NSTemporaryDirectory(), isDirectory = true)
    private val root: NSURL = temporaryRoot.URLByAppendingPathComponent("relive-exports", isDirectory = true)!!
    private val sessions: NSURL = temporaryRoot.URLByAppendingPathComponent("relive-archive-sessions", isDirectory = true)!!

    init {
        files.createDirectoryAtURL(root, true, null, null)
        files.removeItemAtURL(sessions, null)
        files.createDirectoryAtURL(sessions, true, null, null)
    }

    override suspend fun createMagazinePdf(document: MagazineDocument, onProgress: (ExportProgress) -> Unit): ExportResult {
        val temporaryAssets = newTemporaryPdfAssetsRoot()
        val filename = pdfFilename(document.options.title)
        // UIDocumentPicker uses the source URL's basename, so keep the user-facing filename
        // clean. ExportViewModel prevents concurrent work and clears the previous result first.
        val output = root.URLByAppendingPathComponent(filename)!!.path!!
        return try {
            val preparedDocument = withContext(Dispatchers.Default) {
                val assets = linkedMapOf<String, MagazineMediaAsset>()
                val attachments = document.moments.flatMap { it.attachments }
                    .filter { it.type == MediaType.Image }
                    .distinctBy { it.storageRef.value }
                val coverSource = document.options.coverPhotoPath
                val totalMedia = (attachments.size + if (coverSource == null) 0 else 1).toLong().coerceAtLeast(1)
                var completedMedia = 0L
                onProgress(ExportProgress(0, totalMedia, "Preparing media"))

                val quality = document.options.imageQuality
                val normalizedAssetPaths = mutableMapOf<String, String>()
                attachments.forEach { attachment ->
                    coroutineContext.ensureActive()
                    require(mediaStore.exists(attachment.storageRef)) { "A referenced media item is missing or inaccessible." }
                    val sourcePath = mediaStore.resolveAbsolutePath(attachment.storageRef)
                    require(files.fileExistsAtPath(sourcePath)) { "A referenced media item is missing or inaccessible." }
                    val printablePath = when (quality) {
                        PdfImageQuality.Standard -> normalizedAssetPaths.getOrPut(sourcePath) {
                            normalizeJpeg(sourcePath, temporaryAssets, quality)
                        }
                        PdfImageQuality.HD -> sourcePath
                    }
                    assets[attachment.storageRef.value] = MagazineMediaAsset(printablePath = printablePath)
                    completedMedia++
                    onProgress(ExportProgress(completedMedia, totalMedia, "Preparing media"))
                }

                val coverPath = coverSource?.let { sourcePath ->
                    coroutineContext.ensureActive()
                    require(files.fileExistsAtPath(sourcePath)) { "The selected cover photo is no longer available." }
                    // A cover is user-selected input, so it is normalized even for an HD
                    // export. This also guarantees that its embedded orientation is baked
                    // into the pixels before the HTML renderer reads it.
                    if (quality == PdfImageQuality.Standard) {
                        normalizedAssetPaths.getOrPut(sourcePath) {
                            normalizeJpeg(sourcePath, temporaryAssets, quality)
                        }
                    } else {
                        normalizeJpeg(sourcePath, temporaryAssets, quality)
                    }
                }.also {
                    if (coverSource != null) {
                        completedMedia++
                        onProgress(ExportProgress(completedMedia, totalMedia, "Preparing media"))
                    }
                }

                document.copy(
                    options = document.options.copy(coverPhotoPath = coverPath),
                    mediaAssets = assets,
                    fontCss = fontCss(),
                )
            }
            onProgress(ExportProgress(0, 1, "Laying out diary"))
            coroutineContext.ensureActive()
            renderPdf(MagazineDocumentBuilder.html(preparedDocument), output)
            coroutineContext.ensureActive()
            onProgress(ExportProgress(1, 1, "Diary ready"))
            ExportResult(output, filename, "application/pdf", ExportFormat.KeepsakePdf)
        } catch (error: Throwable) {
            // A failed renderer can leave a partial file behind; only a completed PDF is
            // retained for the caller.
            files.removeItemAtPath(output, error = null)
            throw error
        } finally {
            // The generated JPEGs are only HTML renderer inputs. The final PDF is
            // deliberately outside this directory and is retained for Save/Share.
            files.removeItemAtPath(temporaryAssets, error = null)
        }
    }

    private fun fontCss(): String {
        val fraunces = NSBundle.mainBundle.pathForResource("fraunces_medium", "ttf")
        val inter = NSBundle.mainBundle.pathForResource("inter_regular", "ttf")
        val kalam = NSBundle.mainBundle.pathForResource("kalam_regular", "ttf")
        val kalamBold = NSBundle.mainBundle.pathForResource("kalam_bold", "ttf")
        return buildString {
            fraunces?.let { append("@font-face{font-family:Fraunces;src:url('file://$it')}") }
            inter?.let { append("@font-face{font-family:Inter;src:url('file://$it')}") }
            kalam?.let { append("@font-face{font-family:Kalam;src:url('file://$it');font-weight:400}") }
            kalamBold?.let { append("@font-face{font-family:Kalam;src:url('file://$it');font-weight:700}") }
        }
    }

    private fun newTemporaryPdfAssetsRoot(): String {
        val path = temporaryRoot.URLByAppendingPathComponent(
            "relive-pdf-assets-${NSUUID().UUIDString}",
            isDirectory = true,
        )!!.path!!
        require(files.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)) {
            "Could not prepare PDF media."
        }
        return path
    }

    private fun normalizeJpeg(
        sourcePath: String,
        destinationRoot: String,
        quality: PdfImageQuality,
    ): String {
        val data = files.contentsAtPath(sourcePath) ?: error("Cannot read image")
        val source = UIImage.imageWithData(data) ?: error("Cannot decode image")
        val pixels = source.size.useContents {
            width * source.scale to height * source.scale
        }
        require(pixels.first > 0.0 && pixels.second > 0.0) { "Cannot decode image dimensions" }
        val longEdge = maxOf(pixels.first, pixels.second)
        val scale = minOf(1.0, quality.maxLongEdgePx.toDouble() / longEdge)
        val targetWidth = maxOf(1.0, kotlin.math.floor(pixels.first * scale))
        val targetHeight = maxOf(1.0, kotlin.math.floor(pixels.second * scale))
        val targetSize = CGSizeMake(targetWidth, targetHeight)
        UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
        val normalized = try {
            source.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
            UIGraphicsGetImageFromCurrentImageContext() ?: error("Cannot normalize image")
        } finally {
            UIGraphicsEndImageContext()
        }
        val jpeg = UIImageJPEGRepresentation(normalized, quality.jpegQualityPercent / 100.0) ?: error("Cannot encode JPEG")
        val destination = NSURL.fileURLWithPath(destinationRoot, isDirectory = true)
            .URLByAppendingPathComponent("asset-${NSUUID().UUIDString}.jpg")!!.path!!
        require(files.createFileAtPath(destination, contents = jpeg, attributes = null)) {
            "Could not write PDF media."
        }
        return destination
    }

    override suspend fun createPortableArchive(snapshot: PortableArchiveSnapshot, onProgress: (ExportProgress) -> Unit): ExportResult {
        val archiveBytes = PortableArchiveJson.encodeSnapshot(snapshot).encodeToByteArray()
        val refs = buildList {
            snapshot.moments.flatMapTo(this) { moment -> moment.attachments.map { it.storageRef to it.type } }
            snapshot.timelines.mapNotNullTo(this) { timeline -> timeline.coverPhotoRef?.let { it to MediaType.Image } }
        }.distinctBy { it.first.value }
        val mediaData = linkedMapOf<String, ByteArray>()
        val inventory = refs.mapIndexed { index, (ref, type) ->
            val data = files.contentsAtPath(mediaStore.resolveAbsolutePath(ref))?.toByteArray() ?: error("A referenced media item is missing or inaccessible.")
            val hash = sha256(data)
            val ext = mediaStore.resolveAbsolutePath(ref).substringAfterLast('.', mediaStore.extensionFor(type)).lowercase()
            val path = "media/$hash.$ext"
            if (path !in mediaData) mediaData[path] = data
            onProgress(ExportProgress((index + 1).toLong(), refs.size.toLong().coerceAtLeast(1), "Checking archive media"))
            PortableArchiveEntry(ref.value, path, data.size.toLong(), hash, type.name)
        }
        val times = snapshot.moments.map { it.createdAt.epochMilliseconds }
        val manifest = PortableArchiveManifest(
            appVersion = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")?.toString().orEmpty(),
            exportedAtEpochMilliseconds = snapshot.exportedAtEpochMilliseconds,
            momentCount = snapshot.moments.size,
            timelineCount = snapshot.timelines.size,
            earliestMomentEpochMilliseconds = times.minOrNull(),
            latestMomentEpochMilliseconds = times.maxOrNull(),
            archiveSha256 = sha256(archiveBytes),
            media = inventory,
        )
        val zip = StoredZip.encode(buildList {
            add(StoredZipEntry("archive.json", archiveBytes))
            mediaData.forEach { (name, data) -> add(StoredZipEntry(name, data)) }
            add(StoredZipEntry("manifest.json", PortableArchiveJson.encodeManifest(manifest).encodeToByteArray()))
        })
        val output = root.URLByAppendingPathComponent(archiveFilename())!!.path!!
        writeBytes(output, zip)
        onProgress(ExportProgress(1, 1, "Archive ready"))
        return ExportResult(output, output.substringAfterLast('/'), RELIVE_ARCHIVE_MIME_TYPE, ExportFormat.ReliveArchive)
    }

    override suspend fun openPortableArchive(path: String): OpenedPortableArchive {
        val bytes = files.contentsAtPath(path)?.toByteArray() ?: error("Relive could not read this archive.")
        try {
            val entries = StoredZip.decode(bytes).associateBy { it.name }
            require(entries.keys.all(::isSafePortableArchivePath)) { "This archive contains an unsafe path." }
            val manifest = PortableArchiveJson.decodeManifest(entries["manifest.json"]?.bytes?.decodeToString() ?: error("Missing manifest.json"))
            if (manifest.formatVersion != RELIVE_ARCHIVE_FORMAT_VERSION) throw PortableArchiveUnsupportedException(manifest.formatVersion)
            require(manifest.format == "com.vaibhav.relive.portable-archive") { "This is not a Relive portable archive." }
            val archiveData = entries["archive.json"]?.bytes ?: error("Missing archive.json")
            require(sha256(archiveData) == manifest.archiveSha256) { "The archive metadata checksum does not match." }
            val snapshot = PortableArchiveJson.decodeSnapshot(archiveData.decodeToString())
            validatePortableArchiveMetadata(snapshot, manifest, MAX_TOTAL_MEDIA_BYTES)
            val media = manifest.media.associate { item ->
                val data = entries[item.archivePath]?.bytes ?: error("Archive media is missing.")
                require(data.size.toLong() == item.byteCount && sha256(data) == item.sha256) { "Archive media checksum does not match." }
                item.storageRef to data
            }
            val sessionRoot = sessions.URLByAppendingPathComponent(NSUUID().UUIDString, true)!!
            files.createDirectoryAtURL(sessionRoot, true, null, null)
            val session = PortableArchiveSession(
                PortableArchiveSummary(manifest.formatVersion, manifest.exportedAtEpochMilliseconds, manifest.momentCount, manifest.timelineCount, manifest.earliestMomentEpochMilliseconds, manifest.latestMomentEpochMilliseconds),
                snapshot,
                sessionRoot.path!!,
                sessionRoot.path!!,
            )
            return OpenedPortableArchive(session, IosPortableArchiveMediaStore(sessionRoot, media))
        } catch (error: PortableArchiveUnsupportedException) {
            throw error
        } catch (error: Throwable) {
            throw PortableArchiveCorruptException(error.message ?: "The Relive archive is corrupt.")
        }
    }

    override fun releasePortableArchive(archive: OpenedPortableArchive) { files.removeItemAtPath(archive.session.sessionPath, null) }
    override fun deleteTemporaryFile(path: String) { if (path.startsWith(NSTemporaryDirectory())) files.removeItemAtPath(path, null) }

    private fun renderPdf(html: String, path: String) {
        val formatter = UIMarkupTextPrintFormatter(markupText = html)
        val paper = CGRectMake(0.0, 0.0, 595.2, 841.8)
        val renderer = object : UIPrintPageRenderer() {
            override fun paperRect() = paper
            override fun printableRect() = paper
        }
        renderer.addPrintFormatter(formatter, startingAtPageAtIndex = 0)
        UIGraphicsBeginPDFContextToFile(path, paper, null)
        renderer.prepareForDrawingPages(NSMakeRange(0u, renderer.numberOfPages.toULong()))
        repeat(renderer.numberOfPages.toInt()) { page -> UIGraphicsBeginPDFPage(); renderer.drawPageAtIndex(page.toLong(), paper) }
        UIGraphicsEndPDFContext()
    }

    private fun stamp(): String = NSDateFormatter().apply { dateFormat = "yyyy-MM-dd"; locale = NSLocale("en_US_POSIX") }.stringFromDate(NSDate())
    private fun archiveFilename() = "Relive-Archive-${stamp()}.relive"
    private fun pdfFilename(title: String) = "Relive-${exportFilenameTitle(title)}-${stamp()}.pdf"

}

@OptIn(ExperimentalForeignApi::class)
private class IosPortableArchiveMediaStore(private val root: NSURL, private val data: Map<String, ByteArray>) : MediaStore {
    override fun extensionFor(type: MediaType) = when (type) { MediaType.Image -> "jpg"; MediaType.Video -> "mp4"; MediaType.Audio -> "m4a" }
    override fun allocateKey(type: MediaType): MediaStorageRef = error("Portable archives are read-only.")
    override fun resolveAbsolutePath(ref: MediaStorageRef): String {
        val bytes = data[ref.value] ?: error("Media is not part of this archive.")
        val path = root.URLByAppendingPathComponent(ref.value)!!.path!!
        val parent = path.substringBeforeLast('/')
        NSFileManager.defaultManager.createDirectoryAtPath(parent, true, null, null)
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) writeBytes(path, bytes)
        return path
    }
    override fun exists(ref: MediaStorageRef) = data.containsKey(ref.value)
    override fun delete(ref: MediaStorageRef) = Unit
    override fun sizeBytes(ref: MediaStorageRef) = data[ref.value]?.size?.toLong() ?: 0
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberExportFileHandle(): ExportFileHandle = remember {
    object : ExportFileHandle {
        override suspend fun choosePortableArchive(): String? {
            val type = UTType.typeWithIdentifier(RELIVE_ARCHIVE_UTI) ?: UTType.typeWithIdentifier("public.data")!!
            val deferred = CompletableDeferred<String?>()
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(type))
            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                    val source = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                    if (source == null) { deferred.complete(null); return }
                    val output = NSURL.fileURLWithPath(NSTemporaryDirectory(), isDirectory = true).URLByAppendingPathComponent("open-${NSUUID().UUIDString}.relive")!!
                    val accessed = source.startAccessingSecurityScopedResource()
                    try { NSFileManager.defaultManager.copyItemAtURL(source, output, null); deferred.complete(output.path) }
                    catch (_: Throwable) { deferred.complete(null) }
                    finally { if (accessed) source.stopAccessingSecurityScopedResource() }
                }
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) { deferred.complete(null) }
            }
            picker.delegate = delegate
            present(picker)
            return deferred.await()
        }
        override suspend fun save(result: ExportResult): Boolean {
            val picker = UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(result.path)))
            present(picker)
            return true
        }
        override fun share(result: ExportResult): Boolean {
            present(UIActivityViewController(listOf(NSURL.fileURLWithPath(result.path)), null))
            return true
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun present(controller: platform.UIKit.UIViewController) {
    var root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    while (root.presentedViewController != null) root = root.presentedViewController!!
    root.presentViewController(controller, true, null)
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = bytes?.readBytes(length.toInt()) ?: ByteArray(0)

@OptIn(ExperimentalForeignApi::class)
private fun writeBytes(path: String, bytes: ByteArray) {
    val file = fopen(path, "wb") ?: error("Could not create export file.")
    try {
        val written = bytes.usePinned { pinned -> fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file) }
        require(written == bytes.size.toULong()) { "Could not finish export file." }
    } finally {
        fclose(file)
    }
}

private const val MAX_TOTAL_MEDIA_BYTES = 8L * 1024 * 1024 * 1024

/** SwiftUI document-open bridge; the external security-scoped URL is copied before returning. */
object IosPortableArchiveIngress {
    val requestBus = PortableArchiveRequestBus()

    fun openPath(path: String) {
        val output = NSURL.fileURLWithPath(NSTemporaryDirectory(), isDirectory = true)
            .URLByAppendingPathComponent("incoming-${NSUUID().UUIDString}.relive")!!
        NSFileManager.defaultManager.copyItemAtURL(NSURL.fileURLWithPath(path), output, null)
        output.path?.let(requestBus::open)
    }
}
