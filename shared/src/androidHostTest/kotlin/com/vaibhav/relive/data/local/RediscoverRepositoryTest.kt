package com.vaibhav.relive.data.local

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.util.Calendar
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RediscoverRepositoryTest {
    private lateinit var fx: TestFixture
    private lateinit var priorZone: TimeZone

    @BeforeTest fun setup() {
        priorZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        fx = TestFixture()
    }

    @AfterTest fun tearDown() {
        fx.close()
        TimeZone.setDefault(priorZone)
    }

    @Test fun on_this_day_excludes_today_and_is_bounded_to_twenty() = runTest {
        repeat(22) { index ->
            fx.moments.insert(sampleMoment("past-$index", utc(2025, 8, 23, 12), title = "past"))
        }
        fx.moments.insert(sampleMoment("today", utc(2026, 8, 23, 8), title = "today"))
        fx.moments.insert(sampleMoment("wrong-day", utc(2024, 8, 22, 8), title = "wrong"))

        val overview = fx.rediscover.observeOverview(query()).first()

        assertEquals(20, overview.onThisDay.size)
        assertFalse(overview.onThisDay.any { it.id.value == "today" })
    }

    @Test fun on_this_day_preview_matches_exact_prior_year_dates_and_stays_bounded() = runTest {
        repeat(12) { index ->
            fx.moments.insert(sampleMoment("past-$index", utc(2025, 8, 23, 12), title = "past"))
        }
        fx.moments.insert(sampleMoment("today", utc(2026, 8, 23, 8), title = "today"))
        fx.moments.insert(sampleMoment("wrong-date", utc(2024, 8, 22, 8), title = "wrong"))

        val previews = fx.rediscover.observeOnThisDayPreviews(
            today = LocalCalendarDate(2026, 8, 23),
            startOfToday = Instant(utc(2026, 8, 23)),
        ).first()

        assertEquals(10, previews.size)
        assertTrue(previews.all { it.localYear == 2025 })
        assertFalse(previews.any { it.id.value == "today" || it.id.value == "wrong-date" })
    }

    @Test fun places_and_tags_are_ranked_from_persisted_usage() = runTest {
        fx.moments.insert(sampleMoment(
            "pune-1", utc(2025, 1, 1), title = "one",
            location = ReliveLocation(locality = "Pune"), tags = listOf(Tag.of("Travel")),
        ))
        fx.moments.insert(sampleMoment(
            "pune-2", utc(2025, 1, 2), title = "two",
            location = ReliveLocation(locality = "pune"), tags = listOf(Tag.of("travel"), Tag.of("friends")),
        ))
        fx.moments.insert(sampleMoment(
            "jal", utc(2025, 1, 3), title = "three",
            location = ReliveLocation(placeName = "NIT Jalandhar"), tags = listOf(Tag.of("friends")),
        ))

        val overview = fx.rediscover.observeOverview(query()).first()

        assertEquals(listOf("Pune", "NIT Jalandhar"), overview.places.map { it.label })
        assertEquals(listOf(2L, 1L), overview.places.map { it.momentCount })
        assertEquals(listOf("friends", "travel"), overview.tags.map { it.canonical })
        assertTrue(overview.tags.all { it.momentCount == 2L })
    }

    @Test fun from_your_past_excludes_on_this_day_and_recent_moments() = runTest {
        fx.moments.insert(sampleMoment("anniversary", utc(2025, 8, 23), title = "anniversary"))
        fx.moments.insert(sampleMoment("old", utc(2025, 2, 3), title = "old"))
        fx.moments.insert(sampleMoment("recent", utc(2026, 8, 1), title = "recent"))

        val overview = fx.rediscover.observeOverview(query()).first()

        assertEquals(listOf("old"), overview.fromYourPast.map { it.id.value })
    }

    @Test fun from_your_past_returns_every_eligible_moment_up_to_ten() = runTest {
        assertTrue(fx.rediscover.observeFromYourPastPreviews(query()).first().isEmpty())

        fx.moments.insert(sampleMoment("one", utc(2025, 1, 1)))
        assertEquals(1, fx.rediscover.observeFromYourPastPreviews(query()).first().size)

        repeat(6) { index -> fx.moments.insert(sampleMoment("seven-$index", utc(2025, 1, index + 2))) }
        assertEquals(7, fx.rediscover.observeFromYourPastPreviews(query()).first().size)

        repeat(3) { index -> fx.moments.insert(sampleMoment("ten-$index", utc(2025, 2, index + 1))) }
        assertEquals(10, fx.rediscover.observeFromYourPastPreviews(query()).first().size)

        repeat(5) { index -> fx.moments.insert(sampleMoment("fifteen-$index", utc(2025, 3, index + 1))) }
        assertEquals(10, fx.rediscover.observeFromYourPastPreviews(query()).first().size)
    }

    @Test fun from_your_past_is_unique_deterministic_and_rotates_daily() = runTest {
        repeat(15) { index -> fx.moments.insert(sampleMoment("old-$index", utc(2025, 1, index + 1))) }

        val today = fx.rediscover.observeFromYourPastPreviews(query()).first().map { it.id.value }
        val sameDay = fx.rediscover.observeFromYourPastPreviews(query()).first().map { it.id.value }
        val tomorrow = fx.rediscover.observeFromYourPastPreviews(query(dailySeed = 20_260_824L)).first().map { it.id.value }

        assertEquals(today, sameDay)
        assertEquals(today.size, today.toSet().size)
        assertEquals(10, today.size)
        assertFalse(today == tomorrow)
    }

    @Test fun from_your_past_excludes_future_recent_and_on_this_day_moments() = runTest {
        fx.moments.insert(sampleMoment("eligible", utc(2025, 1, 1)))
        fx.moments.insert(sampleMoment("recent", utc(2026, 6, 1)))
        fx.moments.insert(sampleMoment("anniversary", utc(2025, 8, 23)))
        fx.moments.insert(sampleMoment("future", utc(2027, 1, 1)))

        assertEquals(listOf("eligible"), fx.rediscover.observeFromYourPastPreviews(query()).first().map { it.id.value })
    }

    @Test fun from_your_past_detail_uses_the_same_daily_order_as_the_shelf() = runTest {
        repeat(15) { index -> fx.moments.insert(sampleMoment("old-$index", utc(2025, 1, index + 1))) }

        val shelf = fx.rediscover.observeFromYourPastPreviews(query()).first().map { it.id.value }
        val detail = fx.rediscover.observeFromYourPastMoments(query()).first().map { it.id.value }

        assertEquals(shelf, detail)
    }

    @Test fun favorites_are_reactive_and_only_return_favorited_moments() = runTest {
        val favorite = sampleMoment("favorite", createdAtMs = 20, isFavorite = true)
        val ordinary = sampleMoment("ordinary", createdAtMs = 30, isFavorite = false)
        fx.moments.insert(favorite)
        fx.moments.insert(ordinary)

        assertEquals(1L, fx.rediscover.observeFavoritesSummary().first().momentCount)
        assertEquals(listOf("favorite"), fx.rediscover.observeFavoriteMoments().first().map { it.id.value })
        assertTrue(fx.timelines.observeCustom().first().isEmpty())

        fx.moments.setFavorite(ordinary.id, true)

        assertEquals(2, fx.rediscover.observeFavoritesSummary().first { it.momentCount == 2L }.momentCount)
        assertEquals(
            listOf("ordinary", "favorite"),
            fx.rediscover.observeFavoriteMoments().first { it.size == 2 }.map { it.id.value },
        )
    }

    @Test fun favorites_preview_is_visual_only_bounded_and_deterministic() = runTest {
        fx.moments.insert(sampleMoment(
            "older", createdAtMs = 10, isFavorite = true,
            attachments = listOf(sampleAttachment("older-photo", sortIndex = 0)),
        ))
        fx.moments.insert(sampleMoment(
            "newer", createdAtMs = 20, isFavorite = true,
            attachments = listOf(
                sampleAttachment("newer-audio", MediaType.Audio, sortIndex = 0),
                sampleAttachment("newer-photo-1", MediaType.Image, sortIndex = 1),
                sampleAttachment("newer-video", MediaType.Video, sortIndex = 2),
                sampleAttachment("newer-photo-2", MediaType.Image, sortIndex = 3),
                sampleAttachment("newer-photo-3", MediaType.Image, sortIndex = 4),
            ),
        ))
        fx.moments.insert(sampleMoment(
            "unfavorite", createdAtMs = 40, isFavorite = false,
            attachments = listOf(sampleAttachment("unrelated", sortIndex = 0)),
        ))

        val summary = fx.rediscover.observeFavoritesSummary().first()

        assertEquals(2L, summary.momentCount)
        assertEquals(
            listOf("newer-photo-1", "newer-video", "newer-photo-2", "newer-photo-3"),
            summary.previewAttachments.map { it.id.value },
        )
        assertTrue(summary.previewAttachments.all { it.type != MediaType.Audio })
    }

    @Test fun audioOnlyFavoritesHaveNoInventedVisualPreview() = runTest {
        fx.moments.insert(sampleMoment(
            "voice", isFavorite = true,
            attachments = listOf(sampleAttachment("voice-audio", MediaType.Audio, sortIndex = 0)),
        ))

        val summary = fx.rediscover.observeFavoritesSummary().first()

        assertEquals(1L, summary.momentCount)
        assertTrue(summary.previewAttachments.isEmpty())
    }

    @Test fun favorite_shelf_is_bounded_and_uses_timeline_order_without_n_plus_one_reads() = runTest {
        repeat(12) { index ->
            fx.moments.insert(sampleMoment(
                id = "favorite-$index",
                createdAtMs = index.toLong(),
                isFavorite = true,
                attachments = listOf(sampleAttachment("attachment-$index", sortIndex = 0)),
            ))
        }

        val previews = fx.rediscover.observeFavoritePreviews().first()

        assertEquals(10, previews.size)
        assertEquals((2..11).map { "favorite-$it" }, previews.map { it.id.value })
        assertEquals((2..11).map { "attachment-$it" }, previews.map { it.attachments.single().id.value })
    }

    private fun query(dailySeed: Long = 20_260_823L): RediscoverQuery = RediscoverQuery(
        today = LocalCalendarDate(2026, 8, 23),
        startOfToday = Instant(utc(2026, 8, 23)),
        recentCutoff = Instant(utc(2026, 5, 25)),
        dailySeed = dailySeed,
    )

    private fun utc(year: Int, month: Int, day: Int, hour: Int = 0): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day, hour, 0, 0)
    }.timeInMillis
}
