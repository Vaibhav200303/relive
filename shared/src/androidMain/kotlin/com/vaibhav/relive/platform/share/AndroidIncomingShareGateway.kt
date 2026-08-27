package com.vaibhav.relive.platform.share

import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.RawMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

/** Android edge adapter for ACTION_SEND and ACTION_SEND_MULTIPLE. */
class AndroidIncomingShareGateway(
    private val context: Context,
    private val scope: CoroutineScope,
) : IncomingShareGateway {
    private val _state = MutableStateFlow<IncomingShareState>(IncomingShareState.Idle)
    override val state: StateFlow<IncomingShareState> = _state.asStateFlow()
    private var retryIntent: Intent? = null

    fun accept(intent: Intent?) {
        val shareIntent = intent ?: return
        if (shareIntent.action !in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) return
        cancel()
        retryIntent = Intent(shareIntent)
        _state.value = IncomingShareState.Reading
        scope.launch {
            val result = withContext(Dispatchers.IO) { read(shareIntent) }
            _state.value = result
        }
    }

    override fun retry() = accept(retryIntent)

    override fun cancel() {
        (_state.value as? IncomingShareState.Ready)?.payload?.deleteTemporaryMedia()
        _state.value = IncomingShareState.Idle
    }

    override fun claim(requestId: String) {
        val ready = _state.value as? IncomingShareState.Ready ?: return
        if (ready.payload.requestId == requestId) _state.value = IncomingShareState.Idle
    }

    private fun read(intent: Intent): IncomingShareState {
        val resolver = context.contentResolver
        val textParts = mutableListOf<String>()
        intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf(String::isNotBlank)?.let(textParts::add)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.takeIf(String::isNotBlank)
        val media = mutableListOf<RawMedia>()
        return try {
            val uris = extractUris(intent)
            val seen = mutableSetOf<String>()
            uris.filter { seen.add(it.toString()) }.forEach { uri ->
                when (val kind = classify(resolver, uri, intent.type)) {
                    ShareKind.Text -> textParts += readText(resolver, uri)
                    is ShareKind.Media -> {
                        if (media.size == MAX_MEDIA_ITEMS) throw ShareReadException("You can share up to $MAX_MEDIA_ITEMS media items at once.")
                        media += copyMedia(resolver, uri, kind.type)
                    }
                    ShareKind.Unsupported -> throw ShareReadException(SUPPORTED_TYPES_MESSAGE)
                }
            }
            val text = textParts.filter(String::isNotBlank).joinToString(separator = "\n\n").takeIf(String::isNotBlank)
            if (text == null && media.isEmpty()) throw ShareReadException(SUPPORTED_TYPES_MESSAGE)
            IncomingShareState.Ready(
                IncomingSharePayload(
                    requestId = UUID.randomUUID().toString(),
                    subject = subject,
                    text = text,
                    media = media,
                ),
            )
        } catch (error: Throwable) {
            media.deleteTemporaryMedia()
            IncomingShareState.Error(
                (error as? ShareReadException)?.message ?: "Relive couldn't read this shared item. Please try again.",
            )
        }
    }

    private fun extractUris(intent: Intent): List<Uri> = buildList {
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let(::add)
        IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let(::addAll)
        val clip: ClipData? = intent.clipData
        repeat(clip?.itemCount ?: 0) { index -> clip?.getItemAt(index)?.uri?.let(::add) }
    }

    private fun classify(resolver: ContentResolver, uri: Uri, fallbackType: String?): ShareKind {
        val mime = resolver.getType(uri) ?: fallbackType
        return when {
            mime == "text/plain" -> ShareKind.Text
            mime?.startsWith("image/") == true -> ShareKind.Media(MediaType.Image)
            mime?.startsWith("video/") == true -> ShareKind.Media(MediaType.Video)
            mime?.startsWith("audio/") == true -> ShareKind.Media(MediaType.Audio)
            else -> ShareKind.Unsupported
        }
    }

    private fun readText(resolver: ContentResolver, uri: Uri): String {
        val bytes = resolver.openInputStream(uri)?.use { input ->
            input.readBytesLimited(MAX_TEXT_BYTES)
        } ?: throw ShareReadException("Relive couldn't read this text file.")
        return bytes.toString(Charsets.UTF_8)
    }

    private fun copyMedia(resolver: ContentResolver, uri: Uri, type: MediaType): RawMedia {
        val directory = File(context.cacheDir, "relive-shares").apply { mkdirs() }
        val target = File.createTempFile("share-", ".${extensionFor(type)}", directory)
        try {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) }
                ?: throw ShareReadException("Relive couldn't read this shared item.")
            return RawMedia(type = type, sourcePath = target.absolutePath, ownedByRelive = true)
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    private fun InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            if (output.size() + read > limit) throw ShareReadException("Shared text is too large to add to a moment.")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private sealed interface ShareKind {
        data object Text : ShareKind
        data class Media(val type: MediaType) : ShareKind
        data object Unsupported : ShareKind
    }

    private class ShareReadException(message: String) : Exception(message)

    private companion object {
        const val MAX_MEDIA_ITEMS = 50
        const val MAX_TEXT_BYTES = 1024 * 1024
        const val SUPPORTED_TYPES_MESSAGE = "Relive supports photos, videos, audio, and text."
    }
}

private fun IncomingSharePayload.deleteTemporaryMedia() = media.deleteTemporaryMedia()

private fun List<RawMedia>.deleteTemporaryMedia() {
    filter(RawMedia::ownedByRelive).forEach { raw -> runCatching { File(raw.sourcePath).delete() } }
}

private fun extensionFor(type: MediaType): String = when (type) {
    MediaType.Image -> "jpg"
    MediaType.Video -> "mp4"
    MediaType.Audio -> "m4a"
}
