package com.vaibhav.relive.data.local

import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimelineHomeRepositoryTest {
    private lateinit var fx: TestFixture

    @BeforeTest fun setup() { fx = TestFixture() }
    @AfterTest fun tearDown() { fx.close() }

    @Test fun summaries_are_scoped_counted_and_visually_bounded() = runTest {
        val family = TimelineId("family")
        val trips = TimelineId("trips")
        fx.timelines.createCustom(sampleCustomTimeline(family.value, "Family"), Instant(1))
        fx.timelines.createCustom(sampleCustomTimeline(trips.value, "Trips"), Instant(2))
        fx.moments.insert(
            sampleMoment(
                id = "new", createdAtMs = 30, title = "new",
                attachments = listOf(
                    sampleAttachment("n0", MediaType.Image, sortIndex = 0),
                    sampleAttachment("n1", MediaType.Video, sortIndex = 1),
                    sampleAttachment("n2", MediaType.Image, sortIndex = 2),
                ),
            ),
            setOf(family, trips),
        )
        fx.moments.insert(
            sampleMoment(
                id = "old", createdAtMs = 10, title = "old",
                attachments = listOf(
                    sampleAttachment("o0", MediaType.Image, sortIndex = 0),
                    sampleAttachment("audio", MediaType.Audio, sortIndex = 1),
                    sampleAttachment("o1", MediaType.Image, sortIndex = 2),
                ),
            ),
            setOf(family),
        )

        val summaries = fx.timelineHome.observeSummaries().first()
        val all = summaries.single { it.timeline == Timeline.All }
        val familySummary = summaries.single { (it.timeline as? Timeline.Custom)?.id == family }
        val tripsSummary = summaries.single { (it.timeline as? Timeline.Custom)?.id == trips }

        assertEquals(2, all.momentCount)
        assertEquals(2, familySummary.momentCount)
        assertEquals(1, tripsSummary.momentCount)
        assertEquals(listOf("n0", "n1", "n2", "o0"), all.previewAttachments.map { it.id.value })
        assertEquals(listOf("n0", "n1", "n2", "o0"), familySummary.previewAttachments.map { it.id.value })
        assertEquals(listOf("n0", "n1", "n2"), tripsSummary.previewAttachments.map { it.id.value })
        assertTrue(all.previewAttachments.none { it.type == MediaType.Audio })
    }

    @Test fun empty_and_audio_only_timelines_have_no_visual_preview() = runTest {
        val empty = TimelineId("empty")
        val audio = TimelineId("audio")
        fx.timelines.createCustom(sampleCustomTimeline(empty.value, "Empty"), Instant(1))
        fx.timelines.createCustom(sampleCustomTimeline(audio.value, "Audio"), Instant(2))
        fx.moments.insert(
            sampleMoment("voice", title = "voice", attachments = listOf(sampleAttachment("a", MediaType.Audio, sortIndex = 0))),
            setOf(audio),
        )
        val summaries = fx.timelineHome.observeSummaries().first()
        val emptySummary = summaries.single { (it.timeline as? Timeline.Custom)?.id == empty }
        val audioSummary = summaries.single { (it.timeline as? Timeline.Custom)?.id == audio }
        assertEquals(0, emptySummary.momentCount)
        assertTrue(emptySummary.previewAttachments.isEmpty())
        assertEquals(1, audioSummary.momentCount)
        assertTrue(audioSummary.previewAttachments.isEmpty())
    }
}
