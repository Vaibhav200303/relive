package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.FavoritesCollectionSummary
import com.vaibhav.relive.domain.model.RediscoverOverview
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.MomentDateNavigationScope
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.domain.time.Instant
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {

    private var previousDefault: TimeZone? = null

    @BeforeTest
    fun setup() {
        previousDefault = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterTest
    fun teardown() {
        previousDefault?.let { TimeZone.setDefault(it) }
    }

    @Test
    fun allIsSelectedByDefault() = runTest {
        val vm = newViewModel()

        assertEquals(CurrentTimeline.All, vm.state.value.currentTimeline)
        assertEquals(TimelineMomentsState.Empty, vm.state.value.moments)
    }

    @Test
    fun selectingCustomTimelineSwitchesObservedMomentSet() = runTest {
        val familyId = TimelineId("family")
        val allMoment = moment("all", 2L)
        val familyMoment = moment("family-moment", 1L)
        val moments = FakeMomentRepository(
            initialAll = listOf(allMoment, familyMoment),
            initialByTimeline = mapOf(familyId to listOf(familyMoment)),
        )
        val vm = newViewModel(momentRepository = moments)

        assertEquals(
            listOf("family-moment", "all"),
            loadedMoments(vm).map { it.id.value },
        )

        vm.selectTimeline(CurrentTimeline.Custom(familyId))

        assertEquals(CurrentTimeline.Custom(familyId), vm.state.value.currentTimeline)
        assertEquals(listOf("family-moment"), loadedMoments(vm).map { it.id.value })
    }

    @Test
    fun customTimelinePresentationIsOldestFirst() = runTest {
        val travelId = TimelineId("travel")
        val moments = FakeMomentRepository(
            initialByTimeline = mapOf(
                travelId to listOf(
                    moment("newest", 3L),
                    moment("middle", 2L),
                    moment("oldest", 1L),
                ),
            ),
        )
        val vm = newViewModel(momentRepository = moments)

        vm.selectTimeline(CurrentTimeline.Custom(travelId))

        assertEquals(
            listOf("oldest", "middle", "newest"),
            loadedMoments(vm).map { it.id.value },
        )
    }

    @Test
    fun sameMomentKeepsSameIdAcrossAllAndCustomWithoutDuplication() = runTest {
        val familyId = TimelineId("family")
        val shared = moment("shared", 1L)
        val moments = FakeMomentRepository(
            initialAll = listOf(shared),
            initialByTimeline = mapOf(familyId to listOf(shared)),
        )
        val vm = newViewModel(momentRepository = moments)

        val allIds = loadedMoments(vm).map { it.id }
        vm.selectTimeline(CurrentTimeline.Custom(familyId))
        val customIds = loadedMoments(vm).map { it.id }

        assertEquals(listOf(MomentId("shared")), allIds)
        assertEquals(allIds, customIds)
        assertEquals(allIds.size, allIds.toSet().size)
        assertEquals(customIds.size, customIds.toSet().size)
    }

    @Test
    fun favoriteChangeRoutesThroughMomentRepository() = runTest {
        val moments = FakeMomentRepository(initialAll = listOf(moment("favorite", 1L)))
        val vm = newViewModel(momentRepository = moments)

        vm.setFavorite(MomentId("favorite"), true)

        assertEquals(listOf(MomentId("favorite") to true), moments.favoriteChanges)
        assertTrue(loadedMoments(vm).single().isFavorite)
    }

    @Test
    fun selectedAllMomentLoadsMembershipsAndAddsOneCustomTimeline() = runTest {
        val family = Timeline.Custom(TimelineId("family"), "Family")
        val travel = Timeline.Custom(TimelineId("travel"), "Travel")
        val timelines = FakeTimelineRepository()
        timelines.createCustom(family, Instant(1L))
        timelines.createCustom(travel, Instant(2L))
        timelines.addMembership(MomentId("target"), family.id)
        val vm = newViewModel(
            momentRepository = FakeMomentRepository(initialAll = listOf(moment("target", 1L))),
            timelineRepository = timelines,
        )

        vm.selectMomentForActions(MomentId("target")) { error("membership load should succeed") }

        assertEquals(MomentId("target"), vm.state.value.momentActions.selectedMomentId)
        assertEquals(setOf(family.id), vm.state.value.momentActions.assignedTimelineIds)
        vm.showTimelineAssignmentPicker()
        assertTrue(vm.state.value.momentActions.isAssignmentPickerVisible)

        var succeeded: Boolean? = null
        vm.addSelectedMomentToTimeline(travel.id) { succeeded = it }

        assertEquals(true, succeeded)
        assertEquals(setOf(family.id, travel.id), timelines.timelinesFor(MomentId("target")).toSet())
        assertEquals(null, vm.state.value.momentActions.selectedMomentId)
    }

    @Test
    fun failedTimelineAssignmentKeepsPickerAndSelectionForRetry() = runTest {
        val timeline = Timeline.Custom(TimelineId("family"), "Family")
        val timelines = FakeTimelineRepository(membershipFailure = IllegalStateException("db"))
        timelines.createCustom(timeline, Instant(1L))
        val vm = newViewModel(
            momentRepository = FakeMomentRepository(initialAll = listOf(moment("target", 1L))),
            timelineRepository = timelines,
        )
        vm.selectMomentForActions(MomentId("target")) { error("membership load should succeed") }
        vm.showTimelineAssignmentPicker()

        var succeeded: Boolean? = null
        vm.addSelectedMomentToTimeline(timeline.id) { succeeded = it }

        assertEquals(false, succeeded)
        assertEquals(MomentId("target"), vm.state.value.momentActions.selectedMomentId)
        assertTrue(vm.state.value.momentActions.isAssignmentPickerVisible)
        assertFalse(vm.state.value.momentActions.isAssigning)
    }

    @Test
    fun emptyCustomTimelineYieldsEmptyState() = runTest {
        val vm = newViewModel()

        vm.selectTimeline(CurrentTimeline.Custom(TimelineId("empty")))

        assertEquals(TimelineMomentsState.Empty, vm.state.value.moments)
    }

    @Test
    fun forgetDeletesEveryMembershipBeforeTheSuccessCallback() = runTest {
        val family = TimelineId("family")
        val travel = TimelineId("travel")
        val target = moment("target", 1L).copy(
            attachments = listOf(MediaAttachment(MediaAttachmentId("a"), MediaType.Image, MediaStorageRef("a.jpg"), 0)),
        )
        val other = moment("other", 2L)
        val moments = FakeMomentRepository(
            initialAll = listOf(other, target),
            initialByTimeline = mapOf(family to listOf(target), travel to listOf(target)),
        )
        val vm = newViewModel(momentRepository = moments)
        var deleted: Moment? = null

        vm.forget(target, onDeleted = { deleted = it }, onFailure = { error("unexpected failure") })

        assertEquals(target, deleted)
        assertEquals(listOf(other), moments.listAll())
        assertTrue(moments.listInTimeline(family).isEmpty())
        assertTrue(moments.listInTimeline(travel).isEmpty())
        assertEquals(listOf(MomentId("target")), moments.deleted)
    }

    @Test
    fun forgetFailureLeavesDataAndDoesNotCallSuccessCallback() = runTest {
        val family = TimelineId("family")
        val target = moment("target", 1L)
        val moments = FakeMomentRepository(
            initialAll = listOf(target),
            initialByTimeline = mapOf(family to listOf(target)),
            deleteFailure = IllegalStateException("db"),
        )
        val vm = newViewModel(momentRepository = moments)
        var success = false
        var failure = false

        vm.forget(target, onDeleted = { success = true }, onFailure = { failure = true })

        assertFalse(success)
        assertTrue(failure)
        assertEquals(listOf(target), moments.listAll())
        assertEquals(listOf(target), moments.listInTimeline(family))
    }

    @Test
    fun forgetRechecksEligibilityAtConfirmationTime() = runTest {
        var now = Instant(4 * 24 * 60 * 60 * 1000L - 1)
        val target = moment("target", 0L)
        val moments = FakeMomentRepository(initialAll = listOf(target))
        val vm = newViewModel(momentRepository = moments, clock = Clock { now })
        now = Instant(4 * 24 * 60 * 60 * 1000L)
        var failure = false

        vm.forget(target, onDeleted = { error("expired Forget must not delete") }, onFailure = { failure = true })

        assertTrue(failure)
        assertEquals(listOf(target), moments.listAll())
    }

    @Test
    fun readOnlySystemCollectionRejectsFavoriteAndForgetMutations() = runTest {
        val target = moment("favorite", 1L)
        val moments = FakeMomentRepository(initialAll = listOf(target))
        val vm = newViewModel(
            momentRepository = moments,
            mode = TimelineMode.ReadOnlySystemCollection("Favorites"),
        )
        var forgetRejected = false

        vm.setFavorite(target.id, true)
        vm.forget(target, onDeleted = { error("Read-only collection must not delete") }, onFailure = { forgetRejected = true })

        assertTrue(moments.favoriteChanges.isEmpty())
        assertTrue(moments.deleted.isEmpty())
        assertTrue(forgetRejected)
    }

    @Test
    fun allTimelineCalendarSelectsTheFirstMomentOnTheExactDate() = runTest {
        val date = com.vaibhav.relive.domain.model.LocalCalendarDate(2026, 8, 24)
        val start = com.vaibhav.relive.presentation.date.RediscoverCalendar.startOfDay(date)
        val moments = FakeMomentRepository(
            initialAll = listOf(
                moment("later", start.epochMilliseconds + 2_000L),
                moment("first", start.epochMilliseconds + 1_000L),
            ),
        )
        val vm = newViewModel(momentRepository = moments)

        vm.jumpToDate(date)

        assertEquals(MomentId("first"), vm.state.value.dateNavigation?.momentId)
        assertEquals(listOf<MomentDateNavigationScope>(MomentDateNavigationScope.All), moments.dateNavigationScopes)
    }

    @Test
    fun customTimelineCalendarKeepsTheCustomScopeForAnExactDate() = runTest {
        val family = TimelineId("family")
        val date = com.vaibhav.relive.domain.model.LocalCalendarDate(2026, 8, 24)
        val start = com.vaibhav.relive.presentation.date.RediscoverCalendar.startOfDay(date)
        val customMoment = moment("family", start.epochMilliseconds + 1_000L)
        val moments = FakeMomentRepository(
            initialAll = listOf(moment("all", start.epochMilliseconds), customMoment),
            initialByTimeline = mapOf(family to listOf(customMoment)),
        )
        val vm = newViewModel(momentRepository = moments)
        vm.selectTimeline(CurrentTimeline.Custom(family))

        vm.jumpToDate(date)

        assertEquals(CurrentTimeline.Custom(family), vm.state.value.currentTimeline)
        assertEquals(MomentId("family"), vm.state.value.dateNavigation?.momentId)
        assertEquals(listOf<MomentDateNavigationScope>(MomentDateNavigationScope.Custom(family)), moments.dateNavigationScopes)
    }

    @Test
    fun calendarMissingDateSelectsTheNextAvailableMoment() = runTest {
        val date = com.vaibhav.relive.domain.model.LocalCalendarDate(2026, 8, 24)
        val next = com.vaibhav.relive.presentation.date.RediscoverCalendar.nextDayStart(date)
        val moments = FakeMomentRepository(initialAll = listOf(moment("next", next.epochMilliseconds)))
        val vm = newViewModel(momentRepository = moments)

        vm.jumpToDate(date)

        assertEquals(MomentId("next"), vm.state.value.dateNavigation?.momentId)
        assertEquals("No moments on 24 August — showing 25 August.", vm.state.value.dateNavigation?.message)
    }

    @Test
    fun calendarMissingDateWithNoNextSelectsThePreviousMoment() = runTest {
        val date = com.vaibhav.relive.domain.model.LocalCalendarDate(2026, 8, 24)
        val start = com.vaibhav.relive.presentation.date.RediscoverCalendar.startOfDay(date)
        val moments = FakeMomentRepository(initialAll = listOf(moment("previous", start.epochMilliseconds - 1L)))
        val vm = newViewModel(momentRepository = moments)

        vm.jumpToDate(date)

        assertEquals(MomentId("previous"), vm.state.value.dateNavigation?.momentId)
        assertEquals("No moments on 24 August — showing 23 August.", vm.state.value.dateNavigation?.message)
    }

    @Test
    fun calendarEmptyTimelineKeepsTheSnackbarFallback() = runTest {
        val vm = newViewModel()

        vm.jumpToDate(com.vaibhav.relive.domain.model.LocalCalendarDate(2026, 8, 24))

        assertEquals(null, vm.state.value.dateNavigation?.momentId)
        assertEquals("No moments in this timeline yet.", vm.state.value.dateNavigation?.message)
    }

    @Test
    fun customTimelineCalendarDoesNotResolveAnAllOnlyMoment() = runTest {
        val family = TimelineId("family")
        val date = com.vaibhav.relive.domain.model.LocalCalendarDate(2026, 8, 24)
        val start = com.vaibhav.relive.presentation.date.RediscoverCalendar.startOfDay(date)
        val familyNext = moment("family-next", com.vaibhav.relive.presentation.date.RediscoverCalendar.nextDayStart(date).epochMilliseconds)
        val moments = FakeMomentRepository(
            initialAll = listOf(moment("all-exact", start.epochMilliseconds), familyNext),
            initialByTimeline = mapOf(family to listOf(familyNext)),
        )
        val vm = newViewModel(momentRepository = moments)
        vm.selectTimeline(CurrentTimeline.Custom(family))

        vm.jumpToDate(date)

        assertEquals(MomentId("family-next"), vm.state.value.dateNavigation?.momentId)
        assertEquals("No moments on 24 August — showing 25 August.", vm.state.value.dateNavigation?.message)
    }

    private fun TestScope.newViewModel(
        momentRepository: FakeMomentRepository = FakeMomentRepository(),
        timelineRepository: FakeTimelineRepository = FakeTimelineRepository(),
        clock: Clock = Clock { Instant(42L) },
        mode: TimelineMode = TimelineMode.Editable,
    ): TimelineViewModel = TimelineViewModel(
        momentRepository = momentRepository,
        timelineRepository = timelineRepository,
        rediscoverRepository = FakeRediscoverRepository(),
        clock = clock,
        scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        mode = mode,
    )

    private fun loadedMoments(vm: TimelineViewModel): List<MomentPresentation> =
        assertIs<TimelineMomentsState.Loaded>(vm.state.value.moments).moments

    private fun moment(id: String, createdAt: Long): Moment = Moment(
        id = MomentId(id),
        createdAt = Instant(createdAt),
        title = id,
    )
}

private class FakeRediscoverRepository : RediscoverRepository {
    override fun observeOverview(query: RediscoverQuery): Flow<RediscoverOverview> = error("Not used")
    override fun observeFavoritesSummary(): Flow<FavoritesCollectionSummary> = error("Not used")
    override fun observeFavoriteMoments(): Flow<List<Moment>> = MutableStateFlow(emptyList())
    override fun observeFavoritePreviews(limit: Int) = error("Not used")
    override fun observeOnThisDayPreviews(
        today: com.vaibhav.relive.domain.model.LocalCalendarDate,
        startOfToday: Instant,
        limit: Int,
    ) = error("Not used")
    override fun observeOnThisDayMoments(
        today: com.vaibhav.relive.domain.model.LocalCalendarDate,
        startOfToday: Instant,
    ): Flow<List<Moment>> = MutableStateFlow(emptyList())
    override fun observeFromYourPastPreviews(query: RediscoverQuery) = error("Not used")
    override fun observeFromYourPastMoments(query: RediscoverQuery): Flow<List<Moment>> = MutableStateFlow(emptyList())
}

private class FakeMomentRepository(
    initialAll: List<Moment> = emptyList(),
    initialByTimeline: Map<TimelineId, List<Moment>> = emptyMap(),
    private val deleteFailure: Throwable? = null,
) : MomentRepository {
    private val all = MutableStateFlow(initialAll)
    private val byTimeline = initialByTimeline
        .mapValuesTo(mutableMapOf()) { MutableStateFlow(it.value) }

    val favoriteChanges = mutableListOf<Pair<MomentId, Boolean>>()
    val deleted = mutableListOf<MomentId>()

    override suspend fun insert(moment: Moment, timelineIds: Set<TimelineId>) {
        all.value = newestFirst(all.value + moment)
        timelineIds.forEach { timelineId ->
            val flow = timelineFlow(timelineId)
            flow.value = newestFirst(flow.value + moment)
        }
    }

    override suspend fun findById(id: MomentId): Moment? = all.value.firstOrNull { it.id == id }

    override suspend fun updateEditable(moment: Moment) {
        all.value = all.value.map { if (it.id == moment.id) moment else it }
        byTimeline.values.forEach { flow ->
            flow.value = flow.value.map { if (it.id == moment.id) moment else it }
        }
    }

    override suspend fun setFavorite(id: MomentId, isFavorite: Boolean) {
        favoriteChanges += id to isFavorite
        all.value = all.value.map {
            if (it.id == id) it.copy(isFavorite = isFavorite) else it
        }
        byTimeline.values.forEach { flow ->
            flow.value = flow.value.map {
                if (it.id == id) it.copy(isFavorite = isFavorite) else it
            }
        }
    }

    override suspend fun delete(id: MomentId) {
        deleteFailure?.let { throw it }
        deleted += id
        all.value = all.value.filterNot { it.id == id }
        byTimeline.values.forEach { flow -> flow.value = flow.value.filterNot { it.id == id } }
    }

    override suspend fun listAll(): List<Moment> = all.value

    override fun observeAll(): Flow<List<Moment>> = all.asStateFlow()

    override fun observeSearch(query: String): Flow<List<Moment>> = MutableStateFlow(emptyList())

    val dateNavigationScopes = mutableListOf<MomentDateNavigationScope>()

    override suspend fun findDateNavigationTarget(
        scope: MomentDateNavigationScope,
        dayStart: Instant,
        nextDayStart: Instant,
    ): Moment? {
        dateNavigationScopes += scope
        val scopedMoments = when (scope) {
            MomentDateNavigationScope.All -> all.value
            is MomentDateNavigationScope.Custom -> timelineFlow(scope.timelineId).value
        }
        val chronological = scopedMoments.sortedWith(
            compareBy<Moment> { it.createdAt.epochMilliseconds }.thenBy { it.id.value },
        )
        return chronological.firstOrNull { it.createdAt >= dayStart && it.createdAt < nextDayStart }
            ?: chronological.firstOrNull { it.createdAt >= nextDayStart }
            ?: chronological.lastOrNull { it.createdAt < dayStart }
    }

    override suspend fun listInTimeline(timelineId: TimelineId): List<Moment> =
        timelineFlow(timelineId).value

    override fun observeInTimeline(timelineId: TimelineId): Flow<List<Moment>> =
        timelineFlow(timelineId).asStateFlow()

    private fun timelineFlow(id: TimelineId): MutableStateFlow<List<Moment>> =
        byTimeline.getOrPut(id) { MutableStateFlow(emptyList()) }

    private fun newestFirst(moments: List<Moment>): List<Moment> = moments.sortedWith(
        compareByDescending<Moment> { it.createdAt }.thenByDescending { it.id.value },
    )
}

private class FakeTimelineRepository(
    private val createFailure: Throwable? = null,
    private val membershipFailure: Throwable? = null,
) : TimelineRepository {
    private val timelines = MutableStateFlow<List<Timeline.Custom>>(emptyList())
    private val memberships = mutableMapOf<MomentId, MutableSet<TimelineId>>()

    val created = mutableListOf<Pair<Timeline.Custom, Instant>>()

    override suspend fun createCustom(timeline: Timeline.Custom, createdAt: Instant) {
        createFailure?.let { throw it }
        created += timeline to createdAt
        timelines.value = timelines.value + timeline
    }

    override suspend fun findCustom(id: TimelineId): Timeline.Custom? =
        timelines.value.firstOrNull { it.id == id }

    override suspend fun listCustom(): List<Timeline.Custom> = timelines.value

    override fun observeCustom(): Flow<List<Timeline.Custom>> = timelines.asStateFlow()

    override suspend fun rename(id: TimelineId, newName: String) {
        timelines.value = timelines.value.map { if (it.id == id) it.copy(name = newName) else it }
    }

    override suspend fun updateTheme(id: TimelineId, theme: ThemeReference?) {
        timelines.value = timelines.value.map { if (it.id == id) it.copy(theme = theme) else it }
    }

    override suspend fun updateCoverPhoto(id: TimelineId, coverPhotoRef: MediaStorageRef?) {
        timelines.value = timelines.value.map { if (it.id == id) it.copy(coverPhotoRef = coverPhotoRef) else it }
    }

    override suspend fun deleteCustom(id: TimelineId) {
        timelines.value = timelines.value.filterNot { it.id == id }
        memberships.values.forEach { it.remove(id) }
    }

    override suspend fun addMembership(momentId: MomentId, timelineId: TimelineId) {
        membershipFailure?.let { throw it }
        memberships.getOrPut(momentId) { mutableSetOf() }.add(timelineId)
    }

    override suspend fun removeMembership(momentId: MomentId, timelineId: TimelineId) {
        memberships[momentId]?.remove(timelineId)
    }

    override suspend fun timelinesFor(momentId: MomentId): List<TimelineId> =
        memberships[momentId]?.toList().orEmpty()
}
