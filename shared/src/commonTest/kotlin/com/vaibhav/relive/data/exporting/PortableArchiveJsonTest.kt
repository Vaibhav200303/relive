package com.vaibhav.relive.data.exporting

import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot
import com.vaibhav.relive.domain.exporting.PortableTimelineIdentity
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class PortableArchiveJsonTest {
    @Test
    fun snapshot_round_trip_preserves_relationships_and_unicode() {
        val moment = Moment(MomentId("m1"), Instant(1_000), title = "京都 & friends", content = "<a>\nsecond", tags = listOf(Tag.of("Travel")))
        val timeline = Timeline.Custom(TimelineId("t1"), "Japan")
        val source = PortableArchiveSnapshot(
            2_000,
            listOf(moment),
            listOf(timeline),
            mapOf(moment.id to setOf(timeline.id)),
            TimelineAppearance(),
            PortableTimelineIdentity("Japan", timeline.id, TimelineAppearance(wallpaper = TimelineWallpaper.Lavender)),
        )
        val decoded = PortableArchiveJson.decodeSnapshot(PortableArchiveJson.encodeSnapshot(source))
        assertEquals(source, decoded)
    }

    @Test
    fun duplicate_json_keys_are_rejected() {
        assertFails { PortableArchiveJson.decodeManifest("{\"format\":\"a\",\"format\":\"b\"}") }
    }
}
