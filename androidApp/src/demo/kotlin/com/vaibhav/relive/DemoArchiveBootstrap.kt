package com.vaibhav.relive

import android.content.Context
import com.vaibhav.relive.di.ReliveAppContainer
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.presentation.onboarding.CURRENT_ONBOARDING_VERSION
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Seeds the judge flavor through the production repositories and app-owned media store. */
object DemoArchiveBootstrap {
    private val seedMarkerId = MomentId("shipaton-demo-on-this-day-v1")

    fun prepare(context: Context, container: ReliveAppContainer, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            // A clean demo install should show the real onboarding before its showcase archive.
            // Waiting here also prevents App's existing-install migration from mistaking seed data
            // for a person's archive and suppressing those screens.
            container.onboardingPreferencesRepository.preferences.first {
                it.completedVersion >= CURRENT_ONBOARDING_VERSION
            }
            seedArchive(context, container)
        }
    }

    private suspend fun seedArchive(context: Context, container: ReliveAppContainer) {
        val childTimelineId = TimelineId("shipaton-demo-timeline-small-wins-v1")
        val migrateSmallWins = container.timelineRepository.findCustom(childTimelineId)?.name == "Small wins"
        val timelines = listOf(
            DemoTimeline("shipaton-demo-timeline-adventures-v1", "Adventures", TimelineWallpaper.SoftPeach),
            DemoTimeline("shipaton-demo-timeline-together-v1", "Together", TimelineWallpaper.BlushPink),
            DemoTimeline(childTimelineId.value, "Aarav's little years", TimelineWallpaper.PowderBlue),
        )
        timelines.forEach { demo ->
            val id = TimelineId(demo.id)
            if (container.timelineRepository.findCustom(id) == null) {
                container.timelineRepository.createCustom(
                    Timeline.Custom(id, demo.name, TimelineAppearance(wallpaper = demo.wallpaper)),
                    Instant(System.currentTimeMillis() - 400L * DAY_MS),
                )
            }
        }
        if (migrateSmallWins) {
            container.timelineRepository.rename(childTimelineId, "Aarav's little years")
            container.timelineRepository.updateAppearance(
                childTimelineId,
                TimelineAppearance(wallpaper = TimelineWallpaper.PowderBlue),
            )
        }

        val media = mapOf(
            "on-this-day" to installAsset(context, container, "mountain-sunrise.png", "on-this-day.png"),
            "campus" to installAsset(context, container, "campus-picnic.png", "campus.png"),
            "dinner" to installAsset(context, container, "family-dinner.png", "dinner.png"),
            "cover-adventures" to installAsset(context, container, "mountain-sunrise.png", "cover-adventures.png"),
            "cover-together" to installAsset(context, container, "family-dinner.png", "cover-together.png"),
            "cover-little-years" to installAsset(context, container, "child-picnic.png", "cover-little-years-v2.png"),
            "child-rain-boots" to installAsset(context, container, "child-rain-boots.png", "child-rain-boots.png"),
            "child-breakfast-words" to installAsset(context, container, "child-breakfast-words.png", "child-breakfast-words.png"),
            "child-picnic" to installAsset(context, container, "child-picnic.png", "child-picnic.png"),
            "child-block-tower" to installAsset(context, container, "child-block-tower.png", "child-block-tower.png"),
            "last-week-ride" to installAsset(context, container, "jacaranda-bike-ride.png", "last-week-ride-v2.png"),
            "first-trip" to installAsset(context, container, "coastal-overlook.png", "first-trip-v2.png"),
            "season-sunlit-park" to installAsset(context, container, "season-sunlit-park.png", "season-sunlit-park-v2.png"),
            "season-rainy-window" to installAsset(context, container, "season-rainy-window.png", "season-rainy-window-v2.png"),
            "season-frosty-morning" to installAsset(context, container, "season-frosty-morning.png", "season-frosty-morning-v2.png"),
        )
        val timelineCovers = mapOf(
            "shipaton-demo-timeline-adventures-v1" to media.getValue("cover-adventures"),
            "shipaton-demo-timeline-together-v1" to media.getValue("cover-together"),
            "shipaton-demo-timeline-small-wins-v1" to media.getValue("cover-little-years"),
        )
        timelineCovers.forEach { (timelineId, coverPhotoRef) ->
            container.timelineRepository.updateCoverPhoto(TimelineId(timelineId), coverPhotoRef)
        }
        val moments = demoMoments(media)
        if (migrateSmallWins) migrateSmallWinsMoments(container, moments)
        migrateDuplicateMomentMedia(container, moments)
        moments.forEach { seeded ->
            if (container.momentRepository.findById(seeded.moment.id) == null) {
                container.momentRepository.insert(
                    seeded.moment,
                    seeded.timelineIds.map(::TimelineId).toSet(),
                )
            }
        }
    }

    private suspend fun migrateDuplicateMomentMedia(
        container: ReliveAppContainer,
        replacements: List<SeededMoment>,
    ) {
        replacements
            .filter { it.moment.id.value in DISTINCT_MEDIA_MOMENT_IDS }
            .forEach { seeded ->
                val existing = container.momentRepository.findById(seeded.moment.id) ?: return@forEach
                if (existing.attachments.none { it.storageRef.value in RETIRED_DUPLICATE_MEDIA_REFS }) return@forEach
                container.momentRepository.updateEditable(
                    seeded.moment.copy(
                        createdAt = existing.createdAt,
                        updatedAt = Instant(maxOf(System.currentTimeMillis(), existing.createdAt.epochMilliseconds)),
                    ),
                )
            }
    }

    private suspend fun migrateSmallWinsMoments(
        container: ReliveAppContainer,
        replacements: List<SeededMoment>,
    ) {
        replacements
            .filter { it.moment.id.value in CHILD_MOMENT_IDS }
            .forEach { seeded ->
                val existing = container.momentRepository.findById(seeded.moment.id) ?: return@forEach
                val replacement = seeded.moment.copy(
                    createdAt = existing.createdAt,
                    updatedAt = Instant(maxOf(System.currentTimeMillis(), existing.createdAt.epochMilliseconds)),
                )
                container.momentRepository.updateEditable(replacement)
                container.momentRepository.setFeeling(replacement.id, replacement.feeling)
            }
    }

    private fun installAsset(
        context: Context,
        container: ReliveAppContainer,
        assetName: String,
        installedName: String,
    ): MediaStorageRef {
        val ref = MediaStorageRef("images/demo-$installedName")
        val destination = File(container.mediaStore.resolveAbsolutePath(ref))
        if (!destination.isFile) {
            destination.parentFile?.mkdirs()
            val temporary = File(destination.parentFile, ".${destination.name}.tmp")
            context.assets.open("demo/$assetName").use { input ->
                temporary.outputStream().buffered().use(input::copyTo)
            }
            check(temporary.renameTo(destination)) { "Could not install demo media." }
        }
        return ref
    }

    private fun demoMoments(media: Map<String, MediaStorageRef>): List<SeededMoment> {
        val now = System.currentTimeMillis()
        val adventures = "shipaton-demo-timeline-adventures-v1"
        val together = "shipaton-demo-timeline-together-v1"
        val littleYears = "shipaton-demo-timeline-small-wins-v1"
        return listOf(
            SeededMoment(
                moment(
                    id = "shipaton-demo-current-window-v1",
                    createdAt = currentWeekMoment(hoursAgo = 2),
                    title = "Puddles in new rain boots",
                    content = "Aarav found every puddle on the walk home and gave each one the same delighted jump.",
                    feeling = MomentFeeling.Great,
                    location = "Riverside walk",
                    tags = listOf("aarav", "outside", "little things"),
                    attachments = listOf(attachment("child-rain-boots", media.getValue("child-rain-boots"))),
                ),
                setOf(littleYears),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-current-call-v1",
                    createdAt = currentWeekMoment(hoursAgo = 5),
                    title = "A call that ran long",
                    content = "We meant to catch up for ten minutes. An hour later, we were still swapping plans for the next little adventure.",
                    feeling = MomentFeeling.Good,
                    tags = listOf("friends", "little things"),
                ),
                setOf(together, adventures),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-current-lunch-v1",
                    createdAt = currentWeekMoment(hoursAgo = 9),
                    title = "Lunch between errands",
                    content = "A warm bowl of noodles, a corner table, and five quiet minutes with nowhere else to be.",
                    feeling = MomentFeeling.Good,
                    location = "Market lane",
                    tags = listOf("food", "everyday"),
                ),
                setOf(together),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-last-week-ride-v1",
                    createdAt = lastWeekMoment(dayOffset = 1, hour = 18),
                    title = "The long way home",
                    content = "We skipped the busy road and took the lane with the jacarandas instead. It added ten minutes and made the whole evening better.",
                    feeling = MomentFeeling.Great,
                    tags = listOf("friends", "outside"),
                    attachments = listOf(attachment("last-week-ride", media.getValue("last-week-ride"))),
                ),
                setOf(adventures, together),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-last-week-kitchen-v1",
                    createdAt = lastWeekMoment(dayOffset = 3, hour = 20),
                    title = "Music in the kitchen",
                    content = "Dinner took twice as long because every song turned into a small dance break. No one minded.",
                    feeling = MomentFeeling.Great,
                    location = "Home",
                    tags = listOf("family", "food"),
                ),
                setOf(together),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-last-week-reset-v1",
                    createdAt = lastWeekMoment(dayOffset = 5, hour = 8),
                    title = "Three new words at breakfast",
                    content = "Between bites of toast, Aarav named the moon, the spoon, and our dog. We wrote them down before the day got busy.",
                    feeling = MomentFeeling.Good,
                    tags = listOf("aarav", "words", "milestone"),
                    attachments = listOf(attachment("child-breakfast-words", media.getValue("child-breakfast-words"))),
                ),
                setOf(littleYears),
            ),
            SeededMoment(
                moment(
                    id = seedMarkerId.value,
                    createdAt = onThisDayLastYear(),
                    title = "The day the trail went quiet",
                    content = "We reached the lake just as the mist began to lift. Nobody spoke for a while; it felt better to let the morning say everything.",
                    favorite = true,
                    feeling = MomentFeeling.Great,
                    location = "Pine Lake trail",
                    tags = listOf("nature", "travel"),
                    attachments = listOf(attachment("on-this-day", media.getValue("on-this-day"))),
                ),
                setOf(adventures),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-campus-v1",
                    createdAt = Instant(now - 18L * DAY_MS),
                    title = "A picnic made for running",
                    content = "Aarav barely stopped for strawberries. The whole afternoon was a loop between the blanket, the trees, and our open arms.",
                    favorite = true,
                    feeling = MomentFeeling.Great,
                    location = "University gardens",
                    tags = listOf("aarav", "family", "outside"),
                    attachments = listOf(attachment("child-picnic", media.getValue("child-picnic"))),
                ),
                setOf(together, littleYears),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-dinner-v1",
                    createdAt = Instant(now - 52L * DAY_MS),
                    title = "Sunday at the long table",
                    content = "Everyone brought something. The recipes were approximate, the plates did not match, and dinner lasted long after the candles burned low.",
                    favorite = true,
                    feeling = MomentFeeling.Good,
                    location = "Home",
                    tags = listOf("family", "food"),
                    attachments = listOf(attachment("dinner", media.getValue("dinner"))),
                ),
                setOf(together),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-photo-journal-v1",
                    createdAt = Instant(now - 96L * DAY_MS),
                    title = "Three views of a changing season",
                    content = "The same week held sunlight, rain, and the first properly cold morning.",
                    feeling = MomentFeeling.Good,
                    tags = listOf("everyday", "photography"),
                    attachments = listOf(
                        attachment("season-1", media.getValue("season-sunlit-park"), 0),
                        attachment("season-2", media.getValue("season-rainy-window"), 1),
                        attachment("season-3", media.getValue("season-frosty-morning"), 2),
                    ),
                ),
                setOf(adventures),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-achievement-v1",
                    createdAt = Instant(now - 145L * DAY_MS),
                    title = "The tallest block tower yet",
                    content = "Six blocks high, then seven. When it tumbled, Aarav laughed, gathered every piece, and started again.",
                    feeling = MomentFeeling.Great,
                    tags = listOf("aarav", "play", "milestone"),
                    attachments = listOf(attachment("child-block-tower", media.getValue("child-block-tower"))),
                ),
                setOf(littleYears),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-long-thought-v1",
                    createdAt = Instant(now - 230L * DAY_MS),
                    title = "A note for future us",
                    content = "Today you wanted the blue cup, one more story, and to hold both our hands on the stairs. None of it looked remarkable while it was happening, which is exactly why I want to keep it.",
                    tags = listOf("aarav", "everyday", "family"),
                ),
                setOf(littleYears),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-first-trip-v1",
                    createdAt = Instant(now - 420L * DAY_MS),
                    title = "Wrong turn, better view",
                    content = "The map said ten minutes. Forty minutes later we found the overlook we did not know we were looking for.",
                    location = "North ridge",
                    tags = listOf("travel", "friends"),
                    attachments = listOf(attachment("first-trip", media.getValue("first-trip"))),
                ),
                setOf(adventures, together),
            ),
            SeededMoment(
                moment(
                    id = "shipaton-demo-quiet-morning-v1",
                    createdAt = Instant(now - 620L * DAY_MS),
                    title = "",
                    content = "Coffee before the city woke up.",
                    tags = listOf("everyday"),
                ),
                emptySet(),
            ),
        )
    }

    private fun moment(
        id: String,
        createdAt: Instant,
        title: String,
        content: String,
        favorite: Boolean = false,
        feeling: MomentFeeling? = null,
        location: String? = null,
        tags: List<String> = emptyList(),
        attachments: List<MediaAttachment> = emptyList(),
    ) = Moment(
        id = MomentId(id),
        createdAt = createdAt,
        title = title,
        content = content,
        isFavorite = favorite,
        feeling = feeling,
        location = location?.let { ReliveLocation(placeName = it) },
        tags = tags.map(Tag::of),
        attachments = attachments,
    )

    private fun attachment(name: String, ref: MediaStorageRef, sortIndex: Int = 0) = MediaAttachment(
        id = MediaAttachmentId("shipaton-demo-attachment-$name-v1"),
        type = MediaType.Image,
        storageRef = ref,
        sortIndex = sortIndex,
    )

    private fun onThisDayLastYear(): Instant {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Instant(calendar.timeInMillis)
    }

    /** Three seed entries stay in the viewer's current calendar week. */
    private fun currentWeekMoment(hoursAgo: Int): Instant {
        val now = System.currentTimeMillis()
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return Instant(maxOf(weekStart, now - hoursAgo * HOUR_MS))
    }

    /** A local Sunday–Saturday offset inside the week preceding the viewer's current week. */
    private fun lastWeekMoment(dayOffset: Int, hour: Int): Instant {
        val calendar = Calendar.getInstance().apply {
            val daysSinceSunday = get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            add(Calendar.DAY_OF_YEAR, -(daysSinceSunday + 7 - dayOffset))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Instant(calendar.timeInMillis)
    }

    private data class DemoTimeline(val id: String, val name: String, val wallpaper: TimelineWallpaper)
    private data class SeededMoment(val moment: Moment, val timelineIds: Set<String>)
    private val CHILD_MOMENT_IDS = setOf(
        "shipaton-demo-current-window-v1",
        "shipaton-demo-last-week-reset-v1",
        "shipaton-demo-campus-v1",
        "shipaton-demo-achievement-v1",
        "shipaton-demo-long-thought-v1",
    )
    private val DISTINCT_MEDIA_MOMENT_IDS = setOf(
        "shipaton-demo-last-week-ride-v1",
        "shipaton-demo-photo-journal-v1",
        "shipaton-demo-first-trip-v1",
    )
    private val RETIRED_DUPLICATE_MEDIA_REFS = setOf(
        "images/demo-ridge.png",
        "images/demo-season-campus.png",
        "images/demo-season-mountains.png",
        "images/demo-season-dinner.png",
    )
    private const val DAY_MS = 86_400_000L
    private const val HOUR_MS = 3_600_000L
}
