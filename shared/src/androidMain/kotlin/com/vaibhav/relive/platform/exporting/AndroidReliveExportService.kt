package com.vaibhav.relive.platform.exporting

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
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
import com.vaibhav.relive.domain.exporting.PdfImageQuality
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
        val filename = "${pdfFilename(document.options.title)}.pdf"
        val exportId = UUID.randomUUID().toString()
        val assetRoot = File(exportRoot, "assets-$exportId")
        val output = File(exportRoot, "$exportId-$filename")
        return try {
            withContext(Dispatchers.IO) {
                require(assetRoot.mkdirs() || assetRoot.isDirectory) { "Could not prepare export storage." }
            }
            val work = withContext(Dispatchers.IO) {
                prepareMagazineWork(document, assetRoot, onProgress)
            }
            onProgress(ExportProgress(0, 1, "Laying out diary"))
            renderHtmlPdf(MagazineDocumentBuilder.html(work), output) { completed, total ->
                onProgress(ExportProgress(completed.toLong(), total.toLong(), "Building PDF pages"))
            }
            coroutineContext.ensureActive()
            onProgress(ExportProgress(1, 1, "Diary ready"))
            ExportResult(output.absolutePath, filename, "application/pdf", ExportFormat.KeepsakePdf)
        } catch (error: Throwable) {
            output.delete()
            throw error
        } finally {
            // All generated JPEGs are per-export inputs only. Never remove or rewrite a
            // managed Moment image, and clean up on success, failure, and cancellation.
            assetRoot.deleteRecursively()
        }
    }

    private suspend fun prepareMagazineWork(
        document: MagazineDocument,
        assetRoot: File,
        onProgress: (ExportProgress) -> Unit,
    ): MagazineDocument {
        val attachments = document.moments.flatMap { it.attachments }
            .filter { it.type == MediaType.Image }
            .distinctBy { it.storageRef.value }
        val hasCover = document.options.coverPhotoPath != null
        val total = (attachments.size + if (hasCover) 1 else 0).toLong().coerceAtLeast(1)
        val imageQuality = document.options.imageQuality
        val standard = imageQuality == PdfImageQuality.Standard
        val maxLongEdge = imageQuality.maxLongEdgePx
        val jpegQuality = imageQuality.jpegQualityPercent
        val assets = linkedMapOf<String, MagazineMediaAsset>()
        val preparedPaths = mutableMapOf<String, String>()

        onProgress(ExportProgress(0, total, "Preparing media"))
        attachments.forEachIndexed { index, attachment ->
            coroutineContext.ensureActive()
            val source = managedFile(attachment.storageRef)
            require(source.isFile && source.canRead()) { "A referenced media item is missing or inaccessible." }
            val printablePath = if (standard) {
                preparedPaths.getOrPut(source.absolutePath) {
                    val destination = File(assetRoot, "attachment-$index.jpg")
                    reencodePdfImage(source, destination, maxLongEdge, jpegQuality)
                    destination.absolutePath
                }
            } else {
                // Moment images have already been normalized by AndroidMediaProcessor. Keep this
                // path unchanged in HD so the source remains untouched and no duplicate is made.
                source.absolutePath
            }
            assets[attachment.storageRef.value] = MagazineMediaAsset(printablePath = printablePath)
            onProgress(ExportProgress((index + 1).toLong(), total, "Preparing media"))
        }

        val coverPath = document.options.coverPhotoPath?.let { rawPath ->
            coroutineContext.ensureActive()
            val source = File(rawPath)
            require(source.isFile && source.canRead()) { "The selected cover photo is no longer available." }
            val prepared = if (standard) {
                preparedPaths.getOrPut(source.absolutePath) {
                    val destination = File(assetRoot, "cover.jpg")
                    reencodePdfImage(source, destination, maxLongEdge, jpegQuality)
                    destination.absolutePath
                }
            } else {
                val destination = File(assetRoot, "cover.jpg")
                reencodePdfImage(source, destination, maxLongEdge, jpegQuality)
                destination.absolutePath
            }
            onProgress(ExportProgress(attachments.size.toLong() + 1, total, "Preparing cover"))
            prepared
        }

        return document.copy(
            options = document.options.copy(coverPhotoPath = coverPath),
            mediaAssets = assets,
            fontCss = ANDROID_FONT_CSS,
        )
    }

    /**
     * Reads only a sampled bitmap, applies all EXIF orientation transforms, and writes an
     * orientation-baked JPEG into the export workspace. The source file is never opened for
     * writing. Keeping the intermediate bitmaps short-lived is important for large camera files.
     */
    private fun reencodePdfImage(source: File, destination: File, maxLongEdge: Int, quality: Int) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Failed to decode image." }
        val sample = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxLongEdge)
        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: throw IllegalStateException("Failed to decode image.")
        var oriented: Bitmap? = null
        var scaled: Bitmap? = null
        try {
            oriented = applyExifOrientation(decoded, source)
            scaled = downscaleIfNeeded(oriented, maxLongEdge)
            destination.parentFile?.mkdirs()
            FileOutputStream(destination).use { stream ->
                require(scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                    "Failed to encode image."
                }
            }
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            scaled?.takeIf { it !== oriented && it !== decoded }?.recycle()
            oriented?.takeIf { it !== decoded }?.recycle()
            decoded.recycle()
        }
    }

    private fun sampleSizeFor(width: Int, height: Int, maxLongEdge: Int): Int {
        val longEdge = maxOf(width, height)
        var sample = 1
        while (longEdge / (sample * 2) >= maxLongEdge) sample *= 2
        return sample
    }

    private fun applyExifOrientation(bitmap: Bitmap, source: File): Bitmap {
        val orientation = runCatching {
            ExifInterface(source.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun downscaleIfNeeded(bitmap: Bitmap, maxLongEdge: Int): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge <= maxLongEdge) return bitmap
        val scale = maxLongEdge.toFloat() / longEdge
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
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
                val pagePalette = magazinePdfPalette(pageHtml)
                webView.setBackgroundColor(pagePalette.paperColor)
                // A previous long section can otherwise leave WebView measured to its old height
                // while the next section loads, creating blank trailing slices for the new page.
                webView.measure(
                    View.MeasureSpec.makeMeasureSpec(PDF_RENDER_WIDTH, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(PDF_RENDER_HEIGHT, View.MeasureSpec.EXACTLY),
                )
                webView.layout(0, 0, PDF_RENDER_WIDTH, PDF_RENDER_HEIGHT)
                webView.loadHtmlAndAwait(pageHtml.replace("width=device-width", "width=$PDF_RENDER_WIDTH"))
                webView.awaitVisualState(logicalIndex.toLong() * 2 + 1)
                webView.measure(
                    View.MeasureSpec.makeMeasureSpec(PDF_RENDER_WIDTH, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                )
                val contentHeight = maxOf(webView.contentHeight, webView.measuredHeight, PDF_RENDER_HEIGHT)
                webView.layout(0, 0, PDF_RENDER_WIDTH, contentHeight)
                webView.awaitVisualState(logicalIndex.toLong() * 2 + 2)
                val physicalPageCount = magazinePhysicalPageCount(
                    contentHeight = contentHeight,
                    pageHeight = PDF_RENDER_HEIGHT,
                    roundingTolerance = PDF_PAGE_ROUNDING_TOLERANCE,
                )
                repeat(physicalPageCount) { sectionPageIndex ->
                    coroutineContext.ensureActive()
                    val page = document.startPage(
                        PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, physicalPageIndex + 1).create(),
                    )
                    page.canvas.drawColor(pagePalette.paperColor)
                    val contentScale = minOf(
                        (PDF_PAGE_WIDTH - PDF_CONTENT_HORIZONTAL_INSET * 2) / PDF_RENDER_WIDTH.toFloat(),
                        (PDF_PAGE_HEIGHT - PDF_CONTENT_VERTICAL_INSET * 2) / PDF_RENDER_HEIGHT.toFloat(),
                    )
                    val contentWidth = PDF_RENDER_WIDTH * contentScale
                    val contentHeight = PDF_RENDER_HEIGHT * contentScale
                    val contentLeft = (PDF_PAGE_WIDTH - contentWidth) / 2f
                    val contentTop = (PDF_PAGE_HEIGHT - contentHeight) / 2f
                    page.canvas.save()
                    page.canvas.clipRect(
                        contentLeft,
                        contentTop,
                        contentLeft + contentWidth,
                        contentTop + contentHeight,
                    )
                    page.canvas.translate(contentLeft, contentTop)
                    page.canvas.scale(contentScale, contentScale)
                    page.canvas.translate(0f, -(sectionPageIndex * PDF_RENDER_HEIGHT).toFloat())
                    webView.draw(page.canvas)
                    page.canvas.restore()
                    drawMagazinePageFrame(
                        canvas = page.canvas,
                        paperColor = pagePalette.paperColor,
                        accentColor = pagePalette.accentColor,
                    )
                    page.canvas.drawText(
                        (physicalPageIndex + 1).toString(),
                        (PDF_PAGE_WIDTH - PDF_PAGE_NUMBER_INSET).toFloat(),
                        (PDF_PAGE_HEIGHT - PDF_PAGE_NUMBER_INSET).toFloat(),
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = pagePalette.accentColor
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

    private fun drawMagazinePageFrame(
        canvas: android.graphics.Canvas,
        paperColor: Int,
        accentColor: Int,
    ) {
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
            alpha = 220
        }
        val innerPaint = Paint(outerPaint).apply {
            strokeWidth = 0.9f
            alpha = 120
        }
        val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paperColor
            style = Paint.Style.FILL
        }
        val outer = RectF(
            PDF_FRAME_OUTER_INSET,
            PDF_FRAME_OUTER_INSET,
            PDF_PAGE_WIDTH - PDF_FRAME_OUTER_INSET,
            PDF_PAGE_HEIGHT - PDF_FRAME_OUTER_INSET,
        )
        val inner = RectF(
            PDF_FRAME_INNER_INSET,
            PDF_FRAME_INNER_INSET,
            PDF_PAGE_WIDTH - PDF_FRAME_INNER_INSET,
            PDF_PAGE_HEIGHT - PDF_FRAME_INNER_INSET,
        )
        canvas.drawRoundRect(outer, 8f, 8f, outerPaint)
        canvas.drawRoundRect(inner, 6f, 6f, innerPaint)

        val centerX = PDF_PAGE_WIDTH / 2f
        listOf(PDF_FRAME_OUTER_INSET, PDF_PAGE_HEIGHT - PDF_FRAME_OUTER_INSET).forEach { centerY ->
            canvas.drawCircle(centerX, centerY, 5.2f, paperPaint)
            canvas.save()
            canvas.rotate(45f, centerX, centerY)
            canvas.drawRect(centerX - 3.2f, centerY - 3.2f, centerX + 3.2f, centerY + 3.2f, innerPaint)
            canvas.restore()
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
        const val PDF_PAGE_ROUNDING_TOLERANCE = 36
        const val PDF_CONTENT_HORIZONTAL_INSET = 22f
        const val PDF_CONTENT_VERTICAL_INSET = 24f
        const val PDF_FRAME_OUTER_INSET = 10f
        const val PDF_FRAME_INNER_INSET = 15f
        const val PDF_PAGE_NUMBER_INSET = 28
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

internal fun splitMagazineHtml(html: String): List<String> {
    val isolatedPageCss = """
        <style>
        .cover,.diary-day,.diary-continuation,.quiet-gap,.diary-photo-page {
            break-before:auto!important;
            page-break-before:auto!important;
            break-after:auto!important;
            page-break-after:auto!important;
        }
        </style>
    """.trimIndent()
    val bodyTag = Regex("<body\\b[^>]*>", RegexOption.IGNORE_CASE).find(html) ?: return listOf(html)
    val closingBody = Regex("</body\\s*>", RegexOption.IGNORE_CASE).findAll(html).lastOrNull()
        ?: return listOf(html)
    if (closingBody.range.first <= bodyTag.range.last) return listOf(html)

    val head = html.substring(0, bodyTag.range.first)
        .replace("</head>", "$isolatedPageCss</head>") + bodyTag.value
    val body = html.substring(bodyTag.range.last + 1, closingBody.range.first)
    val footer = "</body></html>"
    return Regex("<section\\b.*?</section>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        .findAll(body)
        .map { match -> "$head${match.value}$footer" }
        .toList()
        .ifEmpty { listOf(html) }
}

internal fun magazinePhysicalPageCount(
    contentHeight: Int,
    pageHeight: Int,
    roundingTolerance: Int,
): Int {
    require(contentHeight >= 0 && pageHeight > 0 && roundingTolerance >= 0)
    val effectiveHeight = (contentHeight - roundingTolerance).coerceAtLeast(pageHeight)
    return (effectiveHeight + pageHeight - 1) / pageHeight
}

internal data class MagazinePdfPalette(
    val paperColor: Int,
    val accentColor: Int,
)

internal fun magazinePdfPalette(html: String): MagazinePdfPalette = MagazinePdfPalette(
    paperColor = cssHexColor(html, "paper") ?: 0xfff3ebdd.toInt(),
    accentColor = cssHexColor(html, "accent") ?: 0xff744b31.toInt(),
)

private fun cssHexColor(html: String, variable: String): Int? {
    val value = Regex("--${variable}:#([0-9a-fA-F]{6})").find(html)?.groupValues?.get(1) ?: return null
    return (0xff000000L or value.toLong(16)).toInt()
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
