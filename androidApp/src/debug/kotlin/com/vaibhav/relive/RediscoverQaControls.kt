package com.vaibhav.relive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vaibhav.relive.di.ReliveAppContainer
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Duration
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.ProcessedMedia
import com.vaibhav.relive.platform.media.rememberMediaPickerHandle
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.ui.theme.ReliveTheme
import kotlinx.coroutines.launch

/** Debug-source-set-only controls. The release variant does not compile this file. */
@Composable
fun RediscoverQaControls(container: ReliveAppContainer) {
    val scope = rememberCoroutineScope()
    val picker = rememberMediaPickerHandle(container.mediaStore)
    var image by remember { mutableStateOf<ProcessedMedia?>(null) }
    var video by remember { mutableStateOf<ProcessedMedia?>(null) }
    var audio by remember { mutableStateOf<ProcessedMedia?>(null) }
    var status by remember { mutableStateOf("Choose one image, video, and audio for QA seeding.") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ReliveTheme.dimensions.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.xs),
    ) {
        Text("DEBUG · Rediscover QA", style = ReliveTheme.typography.eyebrow, color = ReliveTheme.colors.accentMuted)
        TextButton(onClick = {
            scope.launch {
                val raw = picker.pickImage().firstOrNull() ?: return@launch
                image?.let { container.mediaStore.delete(it.storageRef) }
                image = container.mediaProcessor.process(raw)
                status = "Image ready."
            }
        }) { Text(if (image == null) "Choose QA image" else "Replace QA image") }
        TextButton(onClick = {
            scope.launch {
                val raw = picker.pickVideo().firstOrNull() ?: return@launch
                video?.let { container.mediaStore.delete(it.storageRef) }
                video = container.mediaProcessor.process(raw)
                status = "Video ready."
            }
        }) { Text(if (video == null) "Choose QA video" else "Replace QA video") }
        TextButton(onClick = {
            scope.launch {
                val raw = picker.pickAudio().firstOrNull() ?: return@launch
                audio?.let { container.mediaStore.delete(it.storageRef) }
                audio = container.mediaProcessor.process(raw)
                status = "Audio ready."
            }
        }) { Text(if (audio == null) "Choose QA audio" else "Replace QA audio") }
        TextButton(
            enabled = image != null && video != null && audio != null,
            onClick = {
                scope.launch {
                    val selected = QaMedia(image!!, video!!, audio!!)
                    status = runCatching { RediscoverQaSeeder(container.momentRepository, container.mediaStore, container.clock.now()).seed(selected) }
                        .fold(onSuccess = { "Rediscover QA data seeded." }, onFailure = { "QA seeding failed." })
                }
            },
        ) { Text("Seed Rediscover QA data") }
        TextButton(onClick = {
            scope.launch {
                RediscoverQaSeeder(container.momentRepository, container.mediaStore, container.clock.now()).remove()
                status = "Rediscover QA data removed."
            }
        }) { Text("Remove Rediscover QA data") }
        Text(status, style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textSecondary)
    }
}

internal data class QaMedia(val image: ProcessedMedia, val video: ProcessedMedia, val audio: ProcessedMedia)

/**
 * Fixed, private IDs are the QA ownership marker. They are never inferred from
 * titles, so removal can only target this exact debug dataset.
 */
internal class RediscoverQaSeeder(
    private val repository: MomentRepository,
    private val mediaStore: MediaStore,
    private val now: Instant,
) {
    suspend fun seed(media: QaMedia) {
        val records = records(media)
        val existing = records.mapNotNull { repository.findById(it.id) }
        if (existing.size == records.size) {
            mediaStore.delete(media.image.storageRef)
            mediaStore.delete(media.video.storageRef)
            mediaStore.delete(media.audio.storageRef)
            return
        }
        remove()
        records.forEach { repository.insert(it) }
    }

    suspend fun remove() {
        IDS.mapNotNull { repository.findById(MomentId(it)) }.forEach { moment ->
            repository.delete(moment.id)
            moment.attachments.forEach { mediaStore.delete(it.storageRef) }
        }
    }

    private fun records(media: QaMedia): List<Moment> {
        val today = RediscoverCalendar.localDate(now)
        val onThisDay = listOfNotNull(
            anniversary(today, 1)?.let { dated(ID_PHOTO, it, "[QA] Photo memory", "A photo memory returned from last year.", media.image, "photo-media", tags = listOf("college", "friends", "qa"), place = "QA Pune") },
            anniversary(today, 2)?.let { dated(ID_VIDEO, it, "[QA] Video memory", "A video memory returned from two years ago.", media.video, "video-media", tags = listOf("travel", "qa"), place = "QA Pune") },
            anniversary(today, 3)?.let { dated(ID_TEXT, it, "[QA] First week of college", "That was probably the moment I realized this place had started to feel like home. I remember sitting there thinking how different everything already felt.", null, null, tags = listOf("college", "qa"), place = "QA Jalandhar") },
            anniversary(today, 4)?.let { dated(ID_AUDIO, it, "[QA] Voice memory", "A small voice memory from the past.", media.audio, "audio-media", tags = listOf("friends", "qa"), place = "QA Jalandhar") },
        )
        return onThisDay + listOf(
            dated(ID_OLD_TRIP, past(today, 120), "[QA] Old trip", "A long-ago trip worth returning to.", media.image, "old-trip-media", listOf("travel", "friends", "qa"), "QA Pune"),
            dated(ID_COLLEGE, past(today, 180), "[QA] College evening", "An ordinary evening that stayed with me.", null, null, listOf("college", "friends", "qa"), "QA Jalandhar"),
            dated(ID_RANDOM, past(today, 250), "[QA] Random thought", "A quiet thought from another season.", null, null, listOf("qa"), null),
            dated(ID_WEEKEND, past(today, 320), "[QA] Weekend memory", "A small weekend memory.", null, null, listOf("friends", "qa"), "QA Pune"),
            dated(ID_RECENT, now - Duration.ofDays(10), "[QA] Recent memory", "This should remain excluded from From Your Past.", null, null, listOf("qa"), "QA Pune"),
        )
    }

    private fun anniversary(today: LocalCalendarDate, yearsAgo: Int): Instant? {
        val target = LocalCalendarDate(today.year - yearsAgo, today.month, today.day)
        val instant = RediscoverCalendar.startOfDay(target) + Duration.ofHours(12)
        return instant.takeIf { RediscoverCalendar.localDate(it) == target }
    }

    private fun past(today: LocalCalendarDate, daysAgo: Long): Instant {
        var candidate = now - Duration.ofDays(daysAgo)
        if (RediscoverCalendar.localDate(candidate).let { it.month == today.month && it.day == today.day }) {
            candidate -= Duration.ofDays(1)
        }
        return candidate
    }

    private fun dated(
        id: String,
        createdAt: Instant,
        title: String,
        content: String,
        media: ProcessedMedia?,
        attachmentSuffix: String?,
        tags: List<String>,
        place: String?,
    ) = Moment(
        id = MomentId(id), createdAt = createdAt, title = title, content = content,
        location = place?.let { ReliveLocation(locality = it) },
        tags = tags.map(Tag::of),
        attachments = media?.let {
            listOf(MediaAttachment(MediaAttachmentId("$id-$attachmentSuffix"), it.type, it.storageRef, 0))
        }.orEmpty(),
    )

    private companion object {
        const val ID_PHOTO = "relive-debug-rediscover-qa-photo-v1"
        const val ID_VIDEO = "relive-debug-rediscover-qa-video-v1"
        const val ID_TEXT = "relive-debug-rediscover-qa-text-v1"
        const val ID_AUDIO = "relive-debug-rediscover-qa-audio-v1"
        const val ID_OLD_TRIP = "relive-debug-rediscover-qa-old-trip-v1"
        const val ID_COLLEGE = "relive-debug-rediscover-qa-college-v1"
        const val ID_RANDOM = "relive-debug-rediscover-qa-random-v1"
        const val ID_WEEKEND = "relive-debug-rediscover-qa-weekend-v1"
        const val ID_RECENT = "relive-debug-rediscover-qa-recent-v1"
        val IDS = listOf(ID_PHOTO, ID_VIDEO, ID_TEXT, ID_AUDIO, ID_OLD_TRIP, ID_COLLEGE, ID_RANDOM, ID_WEEKEND, ID_RECENT)
    }
}
