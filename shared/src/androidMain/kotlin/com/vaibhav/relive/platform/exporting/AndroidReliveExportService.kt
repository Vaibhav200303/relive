package com.vaibhav.relive.platform.exporting

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.vaibhav.relive.data.exporting.PortableArchiveJson
import com.vaibhav.relive.data.exporting.isSafePortableArchivePath
import com.vaibhav.relive.data.exporting.validatePortableArchiveMetadata
import com.vaibhav.relive.domain.exporting.ExportFormat
import com.vaibhav.relive.domain.exporting.ExportProgress
import com.vaibhav.relive.domain.exporting.ExportResult
import com.vaibhav.relive.domain.exporting.PortableArchiveEntry
import com.vaibhav.relive.domain.exporting.PortableArchiveManifest
import com.vaibhav.relive.domain.exporting.PortableArchiveSession
import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot
import com.vaibhav.relive.domain.exporting.PortableArchiveSummary
import com.vaibhav.relive.domain.exporting.RELIVE_ARCHIVE_FORMAT_VERSION
import com.vaibhav.relive.domain.exporting.RELIVE_ARCHIVE_MIME_TYPE
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.presentation.exporting.MagazineDocumentBuilder
import com.vaibhav.relive.presentation.exporting.exportFilenameTitle
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.coroutines.coroutineContext

class AndroidReliveExportService(
    context: Context,
    private val mediaStore: MediaStore,
) : ReliveExportService {
    private val applicationContext = context.applicationContext
    private val exportRoot = File(applicationContext.cacheDir, "relive-exports").apply { mkdirs() }
    private val sessionRoot = File(applicationContext.cacheDir, "relive-archive-sessions").apply { mkdirs() }

    init {
        exportRoot.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > STALE_MILLIS }?.forEach(File::deleteRecursively)
        sessionRoot.listFiles()?.forEach(File::deleteRecursively)
    }

    override suspend fun createMagazinePdf(
        document: MagazineDocument,
        onProgress: (ExportProgress) -> Unit,
    ): ExportResult {
        val work = withContext(Dispatchers.IO) {
            val assets = linkedMapOf<String, MagazineMediaAsset>()
            val attachments = document.moments.flatMap { it.attachments }
                .filter { it.type == MediaType.Image }
                .distinctBy { it.storageRef.value }
            onProgress(ExportProgress(0, attachments.size.toLong().coerceAtLeast(1), "Preparing media"))
            attachments.forEachIndexed { index, attachment ->
                coroutineContext.ensureActive()
                val source = managedFile(attachment.storageRef)
                require(source.isFile && source.canRead()) { "A referenced media item is missing or inaccessible." }
                assets[attachment.storageRef.value] = MagazineMediaAsset(printablePath = source.absolutePath)
                onProgress(ExportProgress((index + 1).toLong(), attachments.size.toLong().coerceAtLeast(1), "Preparing media"))
            }
            document.options.coverPhotoPath?.let { require(File(it).isFile) { "The selected cover photo is no longer available." } }
            val filename = "${pdfFilename(document.options.title)}.pdf"
            Triple(
                document.copy(mediaAssets = assets, fontCss = ANDROID_FONT_CSS),
                File(exportRoot, "${UUID.randomUUID()}-$filename"),
                filename,
            )
        }
        return try {
            onProgress(ExportProgress(0, 1, "Laying out diary"))
            renderHtmlPdf(MagazineDocumentBuilder.html(work.first), work.second) { completed, total ->
                onProgress(ExportProgress(completed.toLong(), total.toLong(), "Building PDF pages"))
            }
            coroutineContext.ensureActive()
            onProgress(ExportProgress(1, 1, "Diary ready"))
            ExportResult(work.second.absolutePath, work.third, "application/pdf", ExportFormat.KeepsakePdf)
        } catch (error: Throwable) {
            work.second.delete()
            throw error
        }
    }

    override suspend fun createPortableArchive(
        snapshot: PortableArchiveSnapshot,
        onProgress: (ExportProgress) -> Unit,
    ): ExportResult = withContext(Dispatchers.IO) {
        val archiveBytes = PortableArchiveJson.encodeSnapshot(snapshot).encodeToByteArray()
        val refs = buildList {
            snapshot.moments.flatMapTo(this) { moment -> moment.attachments.map { it.storageRef to it.type } }
            snapshot.timelines.mapNotNullTo(this) { timeline -> timeline.coverPhotoRef?.let { it to MediaType.Image } }
        }.distinctBy { it.first.value }
        val inventory = mutableListOf<PortableArchiveEntry>()
        val files = refs.mapIndexed { index, (ref, type) ->
            val file = managedFile(ref)
            require(file.isFile && file.canRead()) { "A referenced media item is missing or inaccessible." }
            val hash = sha256Cancellable(file)
            val extension = file.extension.lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) } ?: mediaStore.extensionFor(type)
            inventory += PortableArchiveEntry(ref.value, "media/$hash.$extension", file.length(), hash, type.name)
            onProgress(ExportProgress((index + 1).toLong(), refs.size.toLong().coerceAtLeast(1), "Checking archive media"))
            ref.value to file
        }.toMap()
        val times = snapshot.moments.map { it.createdAt.epochMilliseconds }
        val manifest = PortableArchiveManifest(
            appVersion = runCatching { applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName }.getOrNull().orEmpty(),
            exportedAtEpochMilliseconds = snapshot.exportedAtEpochMilliseconds,
            momentCount = snapshot.moments.size,
            timelineCount = snapshot.timelines.size,
            earliestMomentEpochMilliseconds = times.minOrNull(),
            latestMomentEpochMilliseconds = times.maxOrNull(),
            archiveSha256 = sha256(archiveBytes),
            media = inventory,
        )
        val filename = archiveFilename()
        val output = File(exportRoot, "${UUID.randomUUID()}-$filename")
        try {
            val uniqueWrittenPaths = mutableSetOf<String>()
            ZipOutputStream(FileOutputStream(output).buffered()).use { zip ->
                writeStored(zip, "archive.json", archiveBytes)
                inventory.forEachIndexed { index, item ->
                    coroutineContext.ensureActive()
                    val file = files.getValue(item.storageRef)
                    if (uniqueWrittenPaths.add(item.archivePath)) writeStored(zip, item.archivePath, file)
                    onProgress(ExportProgress((index + 1).toLong(), inventory.size.toLong().coerceAtLeast(1), "Packing archive"))
                }
                writeStored(zip, "manifest.json", PortableArchiveJson.encodeManifest(manifest).encodeToByteArray())
            }
        } catch (error: Throwable) {
            output.delete()
            throw error
        }
        coroutineContext.ensureActive()
        ExportResult(output.absolutePath, filename, RELIVE_ARCHIVE_MIME_TYPE, ExportFormat.ReliveArchive)
    }

    override suspend fun openPortableArchive(path: String): OpenedPortableArchive = withContext(Dispatchers.IO) {
        val source = File(path)
        require(source.isFile && source.canRead()) { "Relive could not read this archive." }
        val session = File(sessionRoot, UUID.randomUUID().toString()).apply { mkdirs() }
        val mediaRoot = File(session, "media").apply { mkdirs() }
        val sessionArchive = File(session, "source.relive")
        try {
            source.copyTo(sessionArchive, overwrite = true)
            ZipFile(sessionArchive).use { zip ->
                val entries = zip.entries().toList()
                require(entries.size <= MAX_ENTRIES) { "This archive contains too many entries." }
                entries.forEach { entry ->
                    require(!entry.isDirectory && isSafePortableArchivePath(entry.name)) { "This archive contains an unsafe path." }
                    require(entry.method == ZipEntry.STORED) { "This archive uses unsupported compression." }
                    require(entry.size in 0..MAX_ENTRY_BYTES) { "This archive contains an oversized entry." }
                }
                val manifestBytes = readBounded(zip, "manifest.json", MAX_METADATA_BYTES)
                val archiveBytes = readBounded(zip, "archive.json", MAX_ARCHIVE_JSON_BYTES)
                val manifest = PortableArchiveJson.decodeManifest(manifestBytes.decodeToString())
                if (manifest.formatVersion != RELIVE_ARCHIVE_FORMAT_VERSION) throw PortableArchiveUnsupportedException(manifest.formatVersion)
                require(manifest.format == "com.vaibhav.relive.portable-archive") { "This is not a Relive portable archive." }
                require(sha256(archiveBytes) == manifest.archiveSha256) { "The archive metadata checksum does not match." }
                val snapshot = PortableArchiveJson.decodeSnapshot(archiveBytes.decodeToString())
                validatePortableArchiveMetadata(snapshot, manifest, MAX_TOTAL_MEDIA_BYTES)
                manifest.media.forEach { item ->
                    val entry = zip.getEntry(item.archivePath) ?: error("Archive media is missing.")
                    require(entry.size == item.byteCount) { "Archive media size does not match." }
                    require(zip.getInputStream(entry).use(::sha256) == item.sha256) { "Archive media checksum does not match." }
                }
                OpenedPortableArchive(
                    session = PortableArchiveSession(
                        summary = PortableArchiveSummary(
                            manifest.formatVersion,
                            manifest.exportedAtEpochMilliseconds,
                            manifest.momentCount,
                            manifest.timelineCount,
                            manifest.earliestMomentEpochMilliseconds,
                            manifest.latestMomentEpochMilliseconds,
                        ),
                        snapshot = snapshot,
                        mediaRootPath = mediaRoot.absolutePath,
                        sessionPath = session.absolutePath,
                    ),
                    mediaStore = AndroidPortableArchiveMediaStore(sessionArchive, mediaRoot, manifest.media),
                )
            }
        } catch (error: PortableArchiveUnsupportedException) {
            session.deleteRecursively(); throw error
        } catch (error: Throwable) {
            session.deleteRecursively()
            throw PortableArchiveCorruptException(error.message ?: "The Relive archive is corrupt.")
        }
    }

    override fun releasePortableArchive(archive: OpenedPortableArchive) {
        File(archive.session.sessionPath).deleteRecursively()
    }

    override fun deleteTemporaryFile(path: String) {
        runCatching {
            val file = File(path).canonicalFile
            val cache = applicationContext.cacheDir.canonicalFile
            if (file.path.startsWith(cache.path + File.separator)) file.deleteRecursively()
        }
    }

    private suspend fun renderHtmlPdf(
        html: String,
        output: File,
        onPageRendered: (completed: Int, total: Int) -> Unit,
    ) = withContext(Dispatchers.Main.immediate) {
        yield()
        WebView.enableSlowWholeDocumentDraw()
        val logicalPages = splitMagazineHtml(html)
        val webView = WebView(applicationContext)
        val document = PdfDocument()
        try {
            webView.settings.apply {
                allowFileAccess = true
                allowContentAccess = false
                blockNetworkLoads = true
                javaScriptEnabled = false
                loadWithOverviewMode = false
                useWideViewPort = true
            }
            webView.setInitialScale(100)
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(PDF_RENDER_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(PDF_RENDER_HEIGHT, View.MeasureSpec.EXACTLY),
            )
            webView.layout(0, 0, PDF_RENDER_WIDTH, PDF_RENDER_HEIGHT)
            var physicalPageIndex = 0
            logicalPages.forEachIndexed { logicalIndex, pageHtml ->
                coroutineContext.ensureActive()
                webView.loadHtmlAndAwait(pageHtml.replace("width=device-width", "width=$PDF_RENDER_WIDTH"))
                webView.measure(
                    View.MeasureSpec.makeMeasureSpec(PDF_RENDER_WIDTH, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                )
                val contentHeight = maxOf(webView.contentHeight, webView.measuredHeight, PDF_RENDER_HEIGHT)
                webView.layout(0, 0, PDF_RENDER_WIDTH, contentHeight)
                webView.awaitVisualState(logicalIndex.toLong() + 1)
                val physicalPageCount = (
                    contentHeight - PDF_PAGE_ROUNDING_TOLERANCE + PDF_RENDER_HEIGHT - 1
                ).coerceAtLeast(PDF_RENDER_HEIGHT) / PDF_RENDER_HEIGHT
                repeat(physicalPageCount) { sectionPageIndex ->
                    coroutineContext.ensureActive()
                    val page = document.startPage(
                        PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, physicalPageIndex + 1).create(),
                    )
                    page.canvas.save()
                    page.canvas.scale(PDF_DRAW_SCALE, PDF_DRAW_SCALE)
                    page.canvas.translate(0f, -(sectionPageIndex * PDF_RENDER_HEIGHT).toFloat())
                    webView.draw(page.canvas)
                    page.canvas.restore()
                    page.canvas.drawText(
                        (physicalPageIndex + 1).toString(),
                        (PDF_PAGE_WIDTH - 28).toFloat(),
                        (PDF_PAGE_HEIGHT - 18).toFloat(),
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xff8b7462.toInt()
                            textSize = 9.5f
                            textAlign = Paint.Align.RIGHT
                        },
                    )
                    document.finishPage(page)
                    physicalPageIndex++
                    yield()
                }
                onPageRendered(logicalIndex + 1, logicalPages.size)
            }
            coroutineContext.ensureActive()
            withContext(Dispatchers.IO) {
                FileOutputStream(output).use(document::writeTo)
            }
        } finally {
            document.close()
            webView.stopLoading()
            webView.destroy()
        }
    }

    private fun managedFile(ref: MediaStorageRef): File {
        val file = File(mediaStore.resolveAbsolutePath(ref)).canonicalFile
        val root = File(mediaStore.resolveAbsolutePath(MediaStorageRef("images/placeholder"))).canonicalFile.parentFile?.parentFile
            ?: error("Media root is unavailable.")
        require(file.path.startsWith(root.canonicalPath + File.separator)) { "Invalid media reference." }
        return file
    }

    private suspend fun WebView.awaitVisualState(requestId: Long) {
        val ready = CompletableDeferred<Unit>()
        postVisualStateCallback(requestId, object : WebView.VisualStateCallback() {
            override fun onComplete(requestId: Long) {
                ready.complete(Unit)
            }
        })
        ready.await()
    }

    private suspend fun WebView.loadHtmlAndAwait(html: String) {
        val loaded = CompletableDeferred<Unit>()
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (!loaded.isCompleted) loaded.complete(Unit)
            }
        }
        scrollTo(0, 0)
        loadDataWithBaseURL("file:///", html, "text/html", "UTF-8", null)
        loaded.await()
    }

    private fun splitMagazineHtml(html: String): List<String> {
        val isolatedPageCss = """
            <style>
            .cover,.diary-day,.quiet-gap,.diary-photo-page {
                break-before:auto!important;
                page-break-before:auto!important;
                break-after:auto!important;
                page-break-after:auto!important;
            }
            </style>
        """.trimIndent()
        val head = html.substringBefore("<body>").replace("</head>", "$isolatedPageCss</head>") + "<body>"
        val footer = "</body></html>"
        val body = html.substringAfter("<body>").substringBeforeLast("</body>")
        return Regex("<section\\b.*?</section>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .findAll(body)
            .map { match -> "$head${match.value}$footer" }
            .toList()
            .ifEmpty { listOf(html) }
    }

    private fun readBounded(zip: ZipFile, name: String, limit: Long): ByteArray {
        val entry = zip.getEntry(name) ?: error("Missing $name")
        require(entry.size in 0..limit) { "$name is too large." }
        return zip.getInputStream(entry).use { it.readBytes() }
    }

    private fun writeStored(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        val crc = CRC32().apply { update(bytes) }
        zip.putNextEntry(ZipEntry(name).apply { method = ZipEntry.STORED; size = bytes.size.toLong(); compressedSize = size; this.crc = crc.value })
        zip.write(bytes); zip.closeEntry()
    }

    private suspend fun writeStored(zip: ZipOutputStream, name: String, file: File) {
        val crc = CRC32()
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesSinceYield = 0
            while (true) {
                coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                crc.update(buffer, 0, read)
                bytesSinceYield += read
                if (bytesSinceYield >= CANCELLATION_YIELD_BYTES) {
                    bytesSinceYield = 0
                    yield()
                }
            }
        }
        zip.putNextEntry(ZipEntry(name).apply { method = ZipEntry.STORED; size = file.length(); compressedSize = size; this.crc = crc.value })
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesSinceYield = 0
            while (true) {
                coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                zip.write(buffer, 0, read)
                bytesSinceYield += read
                if (bytesSinceYield >= CANCELLATION_YIELD_BYTES) {
                    bytesSinceYield = 0
                    yield()
                }
            }
        }
        zip.closeEntry()
    }

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
        sha256(input)
    }

    private suspend fun sha256Cancellable(file: File): String = FileInputStream(file).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesSinceYield = 0
        while (true) {
            coroutineContext.ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
            bytesSinceYield += read
            if (bytesSinceYield >= CANCELLATION_YIELD_BYTES) {
                bytesSinceYield = 0
                yield()
            }
        }
        digest.digest().hex()
    }
    private fun sha256(input: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
        return digest.digest().hex()
    }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).hex()
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun archiveFilename() = "Relive-Archive-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.relive"
    private fun pdfFilename(title: String): String {
        val safe = exportFilenameTitle(title)
        return "Relive-$safe-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}"
    }

    private companion object {
        const val PDF_PAGE_WIDTH = 595
        const val PDF_PAGE_HEIGHT = 842
        const val PDF_RENDER_WIDTH = 794
        const val PDF_RENDER_HEIGHT = 1123
        const val PDF_PAGE_ROUNDING_TOLERANCE = 4
        const val PDF_DRAW_SCALE = PDF_PAGE_WIDTH.toFloat() / PDF_RENDER_WIDTH
        const val ANDROID_FONT_CSS = """@font-face{font-family:Fraunces;src:url('file:///android_asset/composeResources/relive.shared.generated.resources/font/fraunces_medium.ttf')}@font-face{font-family:Inter;src:url('file:///android_asset/composeResources/relive.shared.generated.resources/font/inter_regular.ttf')}@font-face{font-family:Kalam;src:url('file:///android_asset/composeResources/relive.shared.generated.resources/font/kalam_regular.ttf');font-weight:400}@font-face{font-family:Kalam;src:url('file:///android_asset/composeResources/relive.shared.generated.resources/font/kalam_bold.ttf');font-weight:700}"""
        const val STALE_MILLIS = 24L * 60 * 60 * 1_000
        const val MAX_ENTRIES = 100_000
        const val MAX_METADATA_BYTES = 8L * 1024 * 1024
        const val MAX_ARCHIVE_JSON_BYTES = 128L * 1024 * 1024
        const val MAX_ENTRY_BYTES = 32L * 1024 * 1024 * 1024
        const val MAX_TOTAL_MEDIA_BYTES = 128L * 1024 * 1024 * 1024
        const val CANCELLATION_YIELD_BYTES = 1024 * 1024
    }
}

private class AndroidPortableArchiveMediaStore(
    private val archive: File,
    private val root: File,
    entries: List<PortableArchiveEntry>,
) : MediaStore {
    private val entriesByRef = entries.associateBy { it.storageRef }
    override fun extensionFor(type: MediaType) = when (type) { MediaType.Image -> "jpg"; MediaType.Video -> "mp4"; MediaType.Audio -> "m4a" }
    override fun allocateKey(type: MediaType): MediaStorageRef = error("Portable archives are read-only.")
    override fun resolveAbsolutePath(ref: MediaStorageRef): String = materialize(ref).absolutePath
    override fun exists(ref: MediaStorageRef) = runCatching { materialize(ref).isFile }.getOrDefault(false)
    override fun delete(ref: MediaStorageRef) = Unit
    override fun sizeBytes(ref: MediaStorageRef) = entriesByRef[ref.value]?.byteCount ?: 0

    @Synchronized
    private fun materialize(ref: MediaStorageRef): File {
        val item = entriesByRef[ref.value] ?: error("Media is not part of this archive.")
        val destination = File(root, ref.value).canonicalFile
        require(destination.path.startsWith(root.canonicalPath + File.separator)) { "Unsafe media destination." }
        if (destination.isFile && destination.length() == item.byteCount) return destination
        destination.parentFile?.mkdirs()
        val partial = File(destination.path + ".partial")
        try {
            ZipFile(archive).use { zip ->
                val entry = zip.getEntry(item.archivePath) ?: error("Archive media is missing.")
                zip.getInputStream(entry).use { input -> partial.outputStream().buffered().use { output -> input.copyTo(output) } }
            }
            require(partial.length() == item.byteCount) { "Archive media size does not match." }
            require(sha256Portable(partial) == item.sha256) { "Archive media checksum does not match." }
            require(partial.renameTo(destination)) { "Could not prepare archive media." }
            return destination
        } finally {
            partial.delete()
        }
    }
}

private fun sha256Portable(file: File): String = FileInputStream(file).use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

@Composable
actual fun rememberExportFileHandle(): ExportFileHandle {
    val context = LocalContext.current
    val pendingOpen = remember { PendingUri() }
    val pendingPdfSave = remember { PendingUri() }
    val pendingArchiveSave = remember { PendingUri() }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), pendingOpen::deliver)
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf"), pendingPdfSave::deliver)
    val saveArchive = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(RELIVE_ARCHIVE_MIME_TYPE), pendingArchiveSave::deliver)
    return remember(context) {
        object : ExportFileHandle {
            override suspend fun choosePortableArchive(): String? {
                val uri = pendingOpen.launchAndAwait { open.launch(arrayOf(RELIVE_ARCHIVE_MIME_TYPE, "application/zip", "application/octet-stream")) } ?: return null
                val destination = File(context.cacheDir, "relive-open-${UUID.randomUUID()}.relive")
                return runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input -> destination.outputStream().use(input::copyTo) } ?: error("Could not open file")
                    destination.absolutePath
                }.getOrNull()
            }
            override suspend fun save(result: ExportResult): Boolean {
                val uri = if (result.format == ExportFormat.KeepsakePdf) {
                    pendingPdfSave.launchAndAwait { savePdf.launch(result.filename) }
                } else {
                    pendingArchiveSave.launchAndAwait { saveArchive.launch(result.filename) }
                }
                return uri?.let { destination -> runCatching { context.contentResolver.openOutputStream(destination, "w")?.use { output -> FileInputStream(result.path).use { it.copyTo(output) } } ?: error("Could not save") }.isSuccess } ?: false
            }
            override fun share(result: ExportResult): Boolean = runCatching {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(result.path))
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = result.mimeType; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share ${result.filename}"))
                true
            }.getOrDefault(false)
        }
    }
}

private class PendingUri {
    private var deferred: CompletableDeferred<Uri?>? = null
    fun deliver(uri: Uri?) { deferred?.complete(uri); deferred = null }
    suspend fun launchAndAwait(launch: () -> Unit): Uri? {
        val result = CompletableDeferred<Uri?>().also { deferred = it }
        launch()
        return result.await()
    }
}
