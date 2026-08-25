package com.vaibhav.relive

import android.content.Context
import com.vaibhav.relive.data.local.db.ReliveDatabase
import com.vaibhav.relive.domain.backup.BackupManifest
import com.vaibhav.relive.domain.backup.BackupPackageStore
import com.vaibhav.relive.domain.backup.PackagedBackup
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.platform.media.AndroidMediaStore
import com.vaibhav.relive.platform.backup.backupAuthLog
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Android v1 logical snapshot; never copies the open SQLite file. */
class AndroidBackupPackageStore(
    private val context: Context,
    private val database: ReliveDatabase,
    private val mediaStore: AndroidMediaStore,
) : BackupPackageStore {
    fun momentCount(): Long = database.momentsQueries.countMoments().executeAsOne()
    fun customTimelineCount(): Int = database.customTimelinesQueries.selectAllCustomTimelines().executeAsList().size

    suspend fun restore(bundlePath: String) = withContext(Dispatchers.IO) {
        val journal = File(context.filesDir, "relive-restore-journal")
        val staging = File(context.filesDir, "relive-restore-staging")
        staging.deleteRecursively(); staging.mkdirs()
        journal.writeText("STAGED")
        var archiveBytes: ByteArray? = null
        var manifestBytes: ByteArray? = null
        val mediaFiles = mutableMapOf<String, File>()
        val entryNames = mutableSetOf<String>()
        ZipInputStream(FileInputStream(bundlePath).buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && entry.name.indexOf("..") < 0 && !entry.name.startsWith("/") && !entry.name.contains('\\')) { "Invalid restore path" }
                require(entryNames.add(entry.name)) { "Duplicate restore entry" }
                when {
                    entry.name == "archive.json" -> archiveBytes = zip.readBytes()
                    entry.name == "manifest.json" -> manifestBytes = zip.readBytes()
                    entry.name.startsWith("media/") -> {
                        require(entry.name.removePrefix("media/").matches(Regex("[0-9a-f]{64}"))) { "Invalid media entry" }
                        val out = File(staging, entry.name); out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) }
                        mediaFiles[entry.name.removePrefix("media/")] = out
                    }
                    else -> error("Unknown restore entry")
                }
                zip.closeEntry()
            }
        }
        val manifest = JSONObject(manifestBytes?.toString(Charsets.UTF_8) ?: error("Missing backup manifest"))
        require(manifest.optInt("formatVersion") == 1) { "Unsupported backup format" }
        val rawArchive = archiveBytes ?: error("Missing backup archive")
        require(manifest.optString("archiveSha256") == sha256(rawArchive)) { "Archive checksum mismatch" }
        val payload = JSONObject(rawArchive.toString(Charsets.UTF_8))
        val inventory = manifest.optJSONArray("media") ?: JSONArray()
        val newRefs = mutableMapOf<String, String>()
        val createdFiles = mutableListOf<File>()
        val oldRefs = database.mediaAttachmentsQueries.selectArchiveAttachmentRefs().executeAsList()
            .map { it.storage_ref }
        for (i in 0 until inventory.length()) {
            val item = inventory.getJSONObject(i); val hash = item.getString("sha256")
            require(hash.matches(Regex("[0-9a-f]{64}"))) { "Invalid media checksum" }
            require(item.optString("storageRef").isSafeStorageRef()) { "Invalid media reference" }
            val source = mediaFiles[hash] ?: error("Missing media entry")
            require(sha256(source) == hash) { "Media checksum mismatch" }
            val type = payload.getJSONArray("attachments").let { attachments ->
                (0 until attachments.length()).asSequence().map { attachments.getJSONObject(it) }
                    .firstOrNull { it.optString("storageRef") == item.optString("storageRef") }?.optString("mediaType") ?: "image"
            }
            val folder = when (type.lowercase()) { "video" -> "videos"; "audio" -> "audio"; else -> "images" }
            val extension = when (type.lowercase()) { "video" -> "mp4"; "audio" -> "m4a"; else -> "jpg" }
            val ref = "$folder/restore_$hash.$extension"
            val destination = File(mediaStore.resolveAbsolutePath(MediaStorageRef(ref))).canonicalFile
            val root = File(mediaStore.resolveAbsolutePath(MediaStorageRef("images/."))).canonicalFile.parentFile
            require(root != null && destination.path.startsWith(root.path + File.separator)) { "Invalid media destination" }
            if (destination.exists()) require(sha256(destination) == hash) { "Media destination collision" }
            else { destination.parentFile?.mkdirs(); source.copyTo(destination); createdFiles += destination }
            newRefs[item.getString("storageRef")] = ref
        }
        journal.writeText("PROMOTION_STARTED")
        try {
            database.transaction {
                database.customTimelinesQueries.deleteAllCustomTimelines()
                database.momentsQueries.deleteAllMoments()
                database.momentTagsQueries.deleteAllMomentTags()
                database.tagsQueries.deleteAllTags()
                val moments = payload.getJSONArray("moments")
                for (i in 0 until moments.length()) {
                    val m = moments.getJSONObject(i)
                    database.momentsQueries.insertMoment(m.getString("id"), m.getLong("createdAt"), if (m.isNull("updatedAt")) null else m.optLong("updatedAt"), m.optString("title"), m.optString("content"), if (m.optBoolean("favorite")) 1L else 0L, if (m.isNull("locationLat")) null else m.optDouble("locationLat"), if (m.isNull("locationLon")) null else m.optDouble("locationLon"), m.optNullableString("locationDisplayName"), m.optNullableString("locationLocality"), m.optNullableString("locationRegion"), m.optNullableString("locationCountry"))
                }
                val timelines = payload.getJSONArray("timelines")
                for (i in 0 until timelines.length()) { val t = timelines.getJSONObject(i); database.customTimelinesQueries.insertCustomTimeline(t.getString("id"), t.getString("name"), t.optNullableString("theme"), t.getLong("createdAt")) }
                val tags = payload.getJSONArray("tags")
                for (i in 0 until tags.length()) { val t = tags.getJSONObject(i); database.tagsQueries.insertTagIfAbsent(t.getString("canonical"), t.getString("label")) }
                val memberships = payload.getJSONArray("memberships")
                for (i in 0 until memberships.length()) { val m = memberships.getJSONObject(i); database.membershipsQueries.insertMembership(m.getString("momentId"), m.getString("timelineId")) }
                val links = payload.optJSONArray("momentTags") ?: JSONArray()
                for (i in 0 until links.length()) { val l = links.getJSONObject(i); database.momentTagsQueries.insertMomentTag(l.getString("momentId"), l.getString("tagCanonical")) }
                val attachments = payload.getJSONArray("attachments")
                for (i in 0 until attachments.length()) { val a = attachments.getJSONObject(i); database.mediaAttachmentsQueries.insertMediaAttachment(a.getString("id"), a.getString("momentId"), a.getString("mediaType"), newRefs[a.getString("storageRef")] ?: error("Missing media reference"), a.getLong("sortIndex")) }
            }
            journal.writeText("PROMOTED")
            // Only after the database transaction commits do we remove media that
            // belonged solely to the replaced archive. A failed restore therefore
            // leaves the old archive and its files intact.
            val restoredRefs = newRefs.values.toSet()
            oldRefs.filterNot(restoredRefs::contains).forEach { oldRef ->
                runCatching { File(mediaStore.resolveAbsolutePath(MediaStorageRef(oldRef))).delete() }
            }
            journal.delete(); staging.deleteRecursively()
        } catch (t: Throwable) {
            createdFiles.forEach { it.delete() }; journal.delete(); staging.deleteRecursively(); throw t
        }
    }
    override suspend fun create(): PackagedBackup = withContext(Dispatchers.IO) {
        backupAuthLog("archive snapshot started")
        val generation = UUID.randomUUID().toString()
        val mediaRefs = database.mediaAttachmentsQueries.selectArchiveAttachmentRefs().executeAsList()
            .map { it.storage_ref }.distinct().sorted()
        val mediaInventory = JSONArray()
        val payload = JSONObject()
        payload.put("moments", JSONArray().apply {
            database.momentsQueries.selectAllMoments().executeAsList().forEach { row ->
                put(JSONObject().apply {
                    put("id", row.id); put("createdAt", row.created_at); put("updatedAt", row.updated_at)
                    put("title", row.title); put("content", row.content); put("favorite", row.is_favorite)
                    put("locationLat", row.location_lat); put("locationLon", row.location_lon)
                    put("locationDisplayName", row.location_display_name); put("locationLocality", row.location_locality)
                    put("locationRegion", row.location_region); put("locationCountry", row.location_country)
                })
            }
        })
        payload.put("timelines", JSONArray().apply { database.customTimelinesQueries.selectAllCustomTimelines().executeAsList().forEach { put(JSONObject().apply { put("id", it.id); put("name", it.name); put("theme", it.theme); put("createdAt", it.created_at) }) } })
        payload.put("memberships", JSONArray().apply {
            database.membershipsQueries.selectAllMemberships().executeAsList().forEach {
                put(JSONObject().apply { put("momentId", it.moment_id); put("timelineId", it.timeline_id) })
            }
        })
        payload.put("tags", JSONArray().apply { database.tagsQueries.selectAllTags().executeAsList().forEach { put(JSONObject().apply { put("canonical", it.canonical); put("label", it.label) }) } })
        payload.put("momentTags", JSONArray().apply {
            database.momentTagsQueries.selectAllMomentTags().executeAsList().forEach {
                put(JSONObject().apply { put("momentId", it.moment_id); put("tagCanonical", it.tag_canonical) })
            }
        })
        payload.put("attachments", JSONArray().apply {
            database.mediaAttachmentsQueries.selectAllAttachments().executeAsList().forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("momentId", it.moment_id); put("mediaType", it.media_type)
                    put("storageRef", it.storage_ref); put("sortIndex", it.sort_index)
                })
            }
        })
        // Profile creation time is installation metadata guarded by an immutable
        // trigger. Format v1 deliberately excludes it rather than exporting a
        // value that restore cannot safely honor.
        payload.put("excluded", JSONArray(listOf("profile_creation_metadata", "oauth_identity_and_tokens", "client_configuration", "active_jobs_and_resumable_sessions", "drafts", "navigation_and_playback_state", "caches", "regenerable_thumbnails")))
        val archiveBytes = payload.toString().toByteArray(Charsets.UTF_8)
        val temp = File.createTempFile("relive-backup-$generation-", ".bundle", context.cacheDir)
        ZipOutputStream(temp.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("archive.json")); zip.write(archiveBytes); zip.closeEntry()
            val writtenMedia = mutableSetOf<String>()
            mediaRefs.forEach { ref ->
                val file = File(mediaStore.resolveAbsolutePath(MediaStorageRef(ref))).canonicalFile
                val root = File(mediaStore.resolveAbsolutePath(MediaStorageRef("images/."))).canonicalFile.parentFile
                require(root != null && (file == root || file.path.startsWith(root.path + File.separator))) { "Invalid media reference" }
                require(file.isFile && file.canRead()) { "Referenced media is missing or inaccessible" }
                val digest = sha256(file)
                mediaInventory.put(JSONObject().apply { put("storageRef", ref); put("sha256", digest); put("bytes", file.length()) })
                if (writtenMedia.add(digest)) {
                    zip.putNextEntry(ZipEntry("media/${digest}")); FileInputStream(file).use { it.copyTo(zip) }; zip.closeEntry()
                }
            }
            val manifest = JSONObject().apply {
                put("formatVersion", 1); put("generation", generation); put("createdAt", System.currentTimeMillis())
                put("schemaVersion", 2); put("momentCount", database.momentsQueries.countMoments().executeAsOne()); put("media", mediaInventory)
                put("archiveSha256", sha256(archiveBytes))
            }
            zip.putNextEntry(ZipEntry("manifest.json")); zip.write(manifest.toString().toByteArray(Charsets.UTF_8)); zip.closeEntry()
            backupAuthLog("archive package completed generation=$generation moments=${database.momentsQueries.countMoments().executeAsOne()}")
        }
        val manifest = BackupManifest(1, generation, System.currentTimeMillis(), database.momentsQueries.countMoments().executeAsOne(), temp.length(), sha256(temp))
        PackagedBackup(temp.absolutePath, manifest, temp.length())
    }

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun JSONObject.optNullableString(name: String): String? = if (isNull(name)) null else optString(name).takeIf { it.isNotEmpty() }

    private fun String.isSafeStorageRef(): Boolean =
        matches(Regex("(?:images|videos|audio)/[^/\\\\.][^/\\\\]*")) && !contains("..")
}
