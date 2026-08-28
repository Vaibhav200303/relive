package com.vaibhav.relive

import com.vaibhav.relive.domain.backup.BackupManifest
import com.vaibhav.relive.domain.backup.BackupSummary
import com.vaibhav.relive.domain.backup.PackagedBackup
import com.vaibhav.relive.platform.backup.backupAuthLog
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class AndroidDriveAppDataClient {
    fun upload(bundle: PackagedBackup, token: String, onBundleProgress: (Long) -> Unit = {}): BackupSummary {
        backupAuthLog("Drive token present=${token.isNotBlank()} scope=drive.appdata destination=appDataFolder")
        backupAuthLog("Drive upload started generation=${bundle.manifest.generation} bytes=${bundle.byteCount}")
        val metadata = JSONObject().apply {
            put("name", "relive-backup-${bundle.manifest.generation}.bundle")
            put("parents", org.json.JSONArray().put("appDataFolder"))
            put("appProperties", JSONObject().apply { put("kind", "relive-backup-bundle"); put("generation", bundle.manifest.generation); put("formatVersion", bundle.manifest.formatVersion.toString()) })
        }
        val id = uploadFile(metadata, File(bundle.path), token, onBundleProgress)
        verifyRemoteBundle(id, bundle.byteCount, token)
        val manifestMetadata = JSONObject().apply { put("name", "relive-backup-${bundle.manifest.generation}.manifest.json"); put("parents", JSONArray().put("appDataFolder")); put("appProperties", JSONObject().apply { put("kind", "relive-backup-manifest"); put("generation", bundle.manifest.generation); put("formatVersion", bundle.manifest.formatVersion.toString()) }) }
        val manifestId = uploadBytes(manifestMetadata, JSONObject().apply {
            put("formatVersion", bundle.manifest.formatVersion); put("generation", bundle.manifest.generation)
            put("createdAt", bundle.manifest.createdAt); put("momentCount", bundle.manifest.momentCount)
            put("logicalBytes", bundle.manifest.logicalBytes); put("bundleId", id); put("bundleSha256", bundle.manifest.bundleSha256)
        }.toString().toByteArray(), token)
        val indexMetadata = JSONObject().apply { put("name", "relive-backup-index-${bundle.manifest.generation}.json"); put("parents", org.json.JSONArray().put("appDataFolder")); put("appProperties", JSONObject().apply { put("kind", "relive-backup-index"); put("generation", bundle.manifest.generation) }) }
        uploadBytes(indexMetadata, JSONObject().apply { put("generation", bundle.manifest.generation); put("createdAt", bundle.manifest.createdAt); put("bundleId", id); put("manifestId", manifestId); put("formatVersion", bundle.manifest.formatVersion) }.toString().toByteArray(), token)
        backupAuthLog("Drive upload completed generation=${bundle.manifest.generation}")
        return BackupSummary(bundle.manifest, id)
    }

    fun discover(token: String): BackupSummary? {
        val files = listFiles(token)
        val indexes = files.filter { it.optJSONObject("appProperties")?.optString("kind") == "relive-backup-index" }
            .sortedByDescending { runCatching { JSONObject(it.optString("_body")).optLong("createdAt") }.getOrDefault(0L) }
        backupAuthLog("Drive discovery listed files=${files.size} indexes=${indexes.size}")
        for (index in indexes) {
            val body = index.optString("_body", "")
            if (body.isBlank()) continue
            val indexJson = runCatching { JSONObject(body) }.getOrNull() ?: continue
            val bundle = files.firstOrNull { it.optString("id") == indexJson.optString("bundleId") } ?: continue
            val manifestFile = files.firstOrNull { it.optString("id") == indexJson.optString("manifestId") } ?: continue
            val manifest = runCatching { JSONObject(manifestFile.optString("_body")) }.getOrNull() ?: continue
            if (manifest.optString("bundleId") != bundle.optString("id")) continue
            if (manifest.optString("bundleSha256").isBlank()) continue
            val parsed = BackupManifest(
                formatVersion = manifest.optInt("formatVersion", -1),
                generation = manifest.optString("generation"),
                createdAt = manifest.optLong("createdAt", 0),
                momentCount = manifest.optLong("momentCount", 0),
                logicalBytes = manifest.optLong("logicalBytes", 0),
                bundleSha256 = manifest.optString("bundleSha256"),
            )
            if (parsed.formatVersion in 1..2 && parsed.generation.isNotBlank()) {
                backupAuthLog("Drive discovery found a compatible promoted generation")
                return BackupSummary(parsed, bundle.optString("id"))
            }
        }
        backupAuthLog("Drive discovery found no compatible promoted generation")
        return null
    }

    fun download(summary: BackupSummary, destination: File, token: String, onProgress: (Long) -> Unit = {}) {
        val connection = (URL("https://www.googleapis.com/drive/v3/files/${summary.remoteBundleId}?alt=media").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; setRequestProperty("Authorization", "Bearer $token")
        }
        requireSuccess(connection, "backup download")
        destination.outputStream().buffered().use { output ->
            connection.inputStream.buffered().use { input ->
                // Match the upload reporting granularity so download progress is
                // visibly live even for modest archives.
                val buffer = ByteArray(256 * 1024)
                var completed = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    completed += read
                    onProgress(completed)
                }
            }
        }
    }

    /** Promotion does not begin until Drive reports the immutable bundle's full size. */
    private fun verifyRemoteBundle(id: String, expectedBytes: Long, token: String) {
        val connection = (URL("https://www.googleapis.com/drive/v3/files/$id?fields=id,size").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        requireSuccess(connection, "bundle verification")
        val remoteBytes = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optLong("size", -1L)
        require(remoteBytes == expectedBytes) { "Drive reported an incomplete backup upload." }
    }

    private fun listFiles(token: String): List<JSONObject> {
        val url = URL("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=trashed%3Dfalse&fields=files(id,name,size,md5Checksum,appProperties,createdTime,modifiedTime)&pageSize=1000")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; setRequestProperty("Authorization", "Bearer $token")
        }
        requireSuccess(connection, "backup discovery list")
        val files = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("files") ?: JSONArray()
        return (0 until files.length()).map {
            val file = files.getJSONObject(it)
            val kind = file.optJSONObject("appProperties")?.optString("kind")
            if (kind == "relive-backup-index" || kind == "relive-backup-manifest") {
                file.put("_body", downloadText(file.getString("id"), token))
            }
            file
        }
    }

    private fun downloadText(id: String, token: String): String {
        val connection = (URL("https://www.googleapis.com/drive/v3/files/$id?alt=media").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; setRequestProperty("Authorization", "Bearer $token")
        }
        requireSuccess(connection, "backup discovery metadata")
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun requestSession(metadata: JSONObject, size: Long, token: String): String {
        val connection = (URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("X-Upload-Content-Type", "application/octet-stream")
            setRequestProperty("X-Upload-Content-Length", size.toString())
        }
        connection.outputStream.use { it.write(metadata.toString().toByteArray()) }
        requireSuccess(connection, "resumable session")
        return connection.getHeaderField("Location") ?: error("Drive did not return an upload session")
    }

    private fun uploadBytes(metadata: JSONObject, bytes: ByteArray, token: String): String {
        val session = requestSession(metadata, bytes.size.toLong(), token)
        val connection = (URL(session).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"; doOutput = true; setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/octet-stream"); setRequestProperty("Content-Length", bytes.size.toString())
        }
        connection.outputStream.use { it.write(bytes) }
        requireSuccess(connection, "metadata upload")
        return JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getString("id")
    }

    /** Streams an archive in Drive resumable chunks; the complete bundle is never held in memory. */
    private fun uploadFile(metadata: JSONObject, file: File, token: String, onProgress: (Long) -> Unit): String {
        val total = file.length()
        val session = requestSession(metadata, total, token)
        // Drive resumable chunks must be 256 KiB aligned (except the final
        // chunk). This smaller valid unit gives the UI confirmed live progress
        // instead of appearing stuck during an 8 MiB request.
        val chunkSize = 256L * 1024L
        var offset = 0L
        FileInputStream(file).buffered().use { input ->
            while (offset < total) {
                val requested = minOf(chunkSize, total - offset).toInt()
                val chunk = ByteArray(requested)
                var read = 0
                while (read < requested) {
                    val n = input.read(chunk, read, requested - read)
                    if (n < 0) break
                    read += n
                }
                check(read > 0) { "Unexpected end of backup bundle" }
                val end = offset + read - 1
                val connection = (URL(session).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"; doOutput = true
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/octet-stream")
                    setRequestProperty("Content-Length", read.toString())
                    setRequestProperty("Content-Range", "bytes $offset-$end/$total")
                }
                connection.outputStream.use { it.write(chunk, 0, read) }
                when (connection.responseCode) {
                    200, 201 -> {
                        // A small archive can complete in its first chunk, so it
                        // never receives a 308. Emit its confirmed completion too.
                        onProgress(total)
                        return JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getString("id")
                    }
                    308 -> {
                        val range = connection.getHeaderField("Range")
                        offset = if (range?.contains("-") == true) range.substringAfterLast('-').toLong() + 1 else end + 1
                        onProgress(offset)
                    }
                    else -> requireSuccess(connection, "resumable upload chunk")
                }
            }
        }
        error("Drive resumable upload ended without a committed file")
    }

    private fun requireSuccess(connection: HttpURLConnection, operation: String) {
        val code = connection.responseCode
        if (code in 200..299) return
        val body = runCatching {
            (connection.errorStream ?: connection.inputStream).bufferedReader().use { it.readText() }
        }.getOrDefault("")
        // Google error payloads contain only API diagnostics here; never include
        // authorization headers, tokens, bundle bytes, or user content.
        val parsed = runCatching { JSONObject(body).optJSONObject("error") }.getOrNull()
        val reason = parsed?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason").orEmpty()
        val message = parsed?.optString("message").orEmpty()
        val safeBody = body.replace(Regex("[\\r\\n\\t]+"), " ").take(512)
        backupAuthLog("Drive $operation rejected http=$code reason=${reason.ifBlank { "unknown" }} message=${message.ifBlank { "none" }}${if (reason.isBlank() && message.isBlank()) " body=$safeBody" else ""}")
        val userMessage = when {
            code == 401 -> "Google Drive authorization expired. Reconnect your Google account."
            code == 403 && reason.contains("insufficient", ignoreCase = true) -> "Google Drive access was not granted. Reconnect and allow Relive's private app storage."
            code == 403 -> "Google Drive rejected access. Check Drive API setup and the selected Google account."
            operation.contains("discovery") -> "Google Drive backup discovery could not be completed. Please try again."
            operation.contains("download") -> "Google Drive backup download could not be completed. Please try again."
            else -> "Google Drive backup could not be completed. Please try again."
        }
        throw IllegalStateException(userMessage)
    }
}
