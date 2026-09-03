package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.AllPhotosCollectionSummary
import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.model.FavoritesCollectionSummary
import com.vaibhav.relive.domain.model.FromYourPastMomentPreview
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.OnThisDayMomentPreview
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.repository.AppearanceRepository
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.repository.ProfileSettingsRepository
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.repository.TimelineRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.media.MediaProcessor
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.share.IncomingSharePayload
import com.vaibhav.relive.presentation.composer.TimelineComposerDraftStore
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.presentation.home.HOME_GREETING_SUBTITLE
import com.vaibhav.relive.presentation.home.homeGreeting
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.presentation.timeline.SystemCollectionCover
import com.vaibhav.relive.ui.components.rediscover.REDISCOVER_CARD_ALL_PHOTOS
import com.vaibhav.relive.ui.components.rediscover.REDISCOVER_CARD_FAVOURITES
import com.vaibhav.relive.ui.components.rediscover.REDISCOVER_CARD_FROM_YOUR_PAST
import com.vaibhav.relive.ui.components.rediscover.REDISCOVER_CARD_ON_THIS_DAY
import com.vaibhav.relive.ui.components.rediscover.RediscoverCollectionCardModel
import com.vaibhav.relive.ui.components.rediscover.RediscoverCollectionRow
import com.vaibhav.relive.ui.components.rediscover.RediscoverRowHitTester
import com.vaibhav.relive.ui.components.rediscover.resolvedRediscoverCollectionCover
import com.vaibhav.relive.ui.components.timeline.LocalTimelineWallpaperPalette
import com.vaibhav.relive.ui.components.timeline.TimelineWallpaperSurface
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.timelineMomentForegroundColors
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The part of Home's state that has to outlive the screen's own composition.
 *
 * Home is swapped out whenever Profile, a Rediscover collection, the appearance screen or another
 * top-level destination takes over. On the way back the surface is rebuilt from nothing: the
 * backdrop has not been measured, so the header reserves no space, and the archive window is still
 * loading, so the feed is empty. For those first frames the list holds almost no content and
 * `LazyListState` clamps its remembered position to the top — the position is destroyed before the
 * content that justified it comes back, and no amount of hoisting the list state alone can save it.
 *
 * Keeping the measured header height and an unclamped anchor out here is what lets the surface be
 * put back where it was, once it can hold that position again. The Rediscover row has the same
 * problem in miniature — its projections also rebuild from empty — so its carousel state, an
 * unclamped item anchor, and the last-delivered projections live here too. Seeding the rebuilt
 * collectors from those projections is what gives the very first frame the full card list, which
 * the reverse container transform needs: without it the tapped card does not exist yet, and the
 * returning screen has nothing to morph into (ADR-0065).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
class HomeSurfaceState {
    var headerHeightPx: Int by mutableIntStateOf(0)
    var anchorIndex: Int by mutableIntStateOf(0)
    var anchorScrollOffset: Int by mutableIntStateOf(0)

    /** Live card count backing [rediscoverCarousel]'s item lookup; Home keeps it current. */
    var rediscoverCardCount: Int by mutableIntStateOf(0)

    /** The carousel item the row was left on — the row's equivalent of the feed's anchor. */
    var rediscoverAnchorItem: Int by mutableIntStateOf(0)

    /**
     * The Rediscover row's scroll state, hoisted with the rest so a rebuilt Home does not reset
     * the row to its first card after a collection, Profile or another destination takes over.
     */
    val rediscoverCarousel: CarouselState = CarouselState(itemCount = { rediscoverCardCount })

    /** The row's last-delivered projections, seeding a rebuilt Home's collectors. */
    var lastFavoritesSummary: FavoritesCollectionSummary by mutableStateOf(FavoritesCollectionSummary(0, emptyList()))
    var lastAllPhotosSummary: AllPhotosCollectionSummary by mutableStateOf(AllPhotosCollectionSummary(0, emptyList()))
    var lastOnThisDayPreviews: List<OnThisDayMomentPreview> by mutableStateOf(emptyList())
    var lastFromYourPastPreviews: List<FromYourPastMomentPreview> by mutableStateOf(emptyList())
}

@Composable
fun rememberHomeSurfaceState(): HomeSurfaceState = remember { HomeSurfaceState() }

/** How long to wait for the reloaded feed to grow back far enough to hold the anchor. */
private const val HOME_RESTORE_TIMEOUT_MS = 2_000L

/** How many pages a restore will wait through before settling for as deep as it got. */
private const val HOME_RESTORE_PAGE_ATTEMPTS = 8

/**
 * How long the Rediscover row's card count must hold still before its anchor is re-seated. The
 * transient rebuild emissions land within a frame or two of each other, so a quarter second of
 * silence means the settled list is in.
 */
private const val HOME_ROW_COUNT_SETTLE_MS = 250L

/**
 * The unified Home surface (ADR-0061): one vertically scrollable container holding the welcome
 * block, the Rediscover collection row, and the All moments feed, in that order.
 *
 * Home is not a wrapper around two screens. It delegates to [TimelineScreen] with
 * `isHomeSurface = true`, passing its own header items into the same `LazyColumn` that renders the
 * feed — so there is exactly one scroll container and one scroll position. Everything that makes
 * Home behave differently from a timeline detail screen (newest-first feed, composer at the head,
 * no entry scroll, no scroll after a save) is expressed by that one flag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    momentRepository: MomentRepository,
    timelineRepository: TimelineRepository,
    appearanceRepository: AppearanceRepository,
    rediscoverRepository: RediscoverRepository,
    profileSettingsRepository: ProfileSettingsRepository,
    clock: Clock,
    idGenerator: IdGenerator,
    mediaStore: MediaStore,
    mediaProcessor: MediaProcessor,
    listState: LazyListState,
    /** Hoisted with [listState]: together they are what Home is restored from. */
    surfaceState: HomeSurfaceState = rememberHomeSurfaceState(),
    /**
     * Every open callback carries the cover the tapped card was wearing, so the collection screen
     * opens under the same image and the container transform between them is continuous
     * (ADR-0065).
     */
    onOpenFavorites: (MomentId?, SystemCollectionCover) -> Unit,
    onOpenOnThisDay: (MomentId?, LocalCalendarDate, SystemCollectionCover) -> Unit,
    onOpenFromYourPast: (MomentId?, RediscoverQuery, SystemCollectionCover) -> Unit,
    onOpenAllPhotos: (SystemCollectionCover) -> Unit,
    /**
     * Container-transform bounds for the Rediscover card being opened (ADR-0065). Supplied by the
     * navigation host, which owns the shared-transition and animated-visibility scopes; the
     * default leaves every card with no shared element.
     */
    rediscoverCardModifier: @Composable (RediscoverCollectionCardModel) -> Modifier = { Modifier },
    draftStore: TimelineComposerDraftStore? = null,
    /**
     * Incremented by `+ New`. Each new value expands the inline composer in place; a counter rather
     * than a flag because Home is persistent and a latched boolean would only ever fire once.
     */
    expandComposerRequest: Int = 0,
    /** Invoked once a pending [expandComposerRequest] has been acted on, so the owner can clear it. */
    onExpandComposerRequestHandled: (() -> Unit)? = null,
    openComposerOnEnter: Boolean = false,
    incomingShare: IncomingSharePayload? = null,
    onIncomingShareApplied: ((String) -> Unit)? = null,
    onOpenTimelineTheme: (() -> Unit)? = null,
    /** Home is a root, so its app bar carries the Profile affordance where a detail carries Back. */
    onOpenProfile: (() -> Unit)? = null,
    navigationToolbarExpanded: Boolean = true,
    onNavigationToolbarExpand: () -> Unit = {},
    onNavigationToolbarCollapse: () -> Unit = {},
    behaviorPreferences: BehaviorPreferences = BehaviorPreferences(),
    wallpaper: TimelineWallpaper = TimelineWallpaper.WarmCream,
    onComposerExpandedChanged: ((Boolean) -> Unit)? = null,
    onFocusedAllMomentsChanged: ((Boolean) -> Unit)? = null,
) {
    val profileSettings by profileSettingsRepository.settings.collectAsState()
    val greeting = homeGreeting(profileSettings.displayName)
    val density = LocalDensity.current

    // Geometry of the sheet riding over the welcome area. The backdrop measures itself — the
    // welcome content alone, below the floating wordmark-and-profile strip — and the list's own
    // scroll position drives how far the sheet has covered it: one scroll container, one scroll
    // position (ADR-0061); only the layering changes. The whole mechanism plays out beneath the
    // strip, so the sheet's travel is exactly the measured height and its focused resting edge is
    // the strip's lower edge, never the top of the screen.
    val headerHeightPx = surfaceState.headerHeightPx
    val homeControlsInset = homeControlsInset()
    val scrolledIntoHeader by rememberScrolledIntoBackdrop(listState) { surfaceState.headerHeightPx }

    // Put the surface back before anything starts recording a new position, so the clamped-to-top
    // frames of a rebuild are never mistaken for somewhere the person actually scrolled to.
    var isRestored by remember { mutableStateOf(false) }
    LaunchedEffect(listState, surfaceState) {
        val index = surfaceState.anchorIndex
        val offset = surfaceState.anchorScrollOffset
        if (index > 0 || offset > 0) {
            // The archive window reloads at its first page, so a deep anchor needs paging to catch
            // up before the feed can hold it. Parking at the end of what is loaded is what asks for
            // the next page; repeat until the anchor is reachable or the feed stops growing, then
            // land as deep as it actually got — never block recording on an unreachable anchor.
            var attempts = 0
            while (attempts < HOME_RESTORE_PAGE_ATTEMPTS) {
                attempts++
                val loaded = listState.layoutInfo.totalItemsCount
                if (loaded > index) break
                if (loaded > 0) listState.scrollToItem(loaded - 1)
                val grew = withTimeoutOrNull(HOME_RESTORE_TIMEOUT_MS) {
                    snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > loaded }
                }
                if (grew == null) break
            }
            val available = listState.layoutInfo.totalItemsCount
            if (available > 0) listState.scrollToItem(index.coerceAtMost(available - 1), offset)
        }
        isRestored = true
    }
    LaunchedEffect(listState, surfaceState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (!isRestored) return@collect
                surfaceState.anchorIndex = index
                surfaceState.anchorScrollOffset = offset
            }
    }
    val isFocused = headerHeightPx > 0 && scrolledIntoHeader >= headerHeightPx

    // Home is the reference implementation of the three-position sliding backdrop described in
    // [BackdropExpansionState]: the welcome block and Rediscover row are the backdrop, All moments
    // is the sheet riding over them (ADR-0061). Custom timeline detail runs the same mechanism over
    // its cover photo (ADR-0062).
    val motion = ReliveTheme.motion
    val settleDurationMillis = motion.durations.standardMillis
    val settleEasing = motion.easings.standard
    val expansion = rememberBackdropExpansionState()
    val expansionConnection = rememberBackdropExpansionConnection(expansion)
    BackdropSettleEffect(
        listState = listState,
        backdropHeightPx = headerHeightPx,
        scrolledIntoBackdrop = { scrolledIntoHeader },
    )

    // Rediscover row sources. Every one of these is a bounded projection; none of them reads the
    // archive (ADR-0061). Each collector is seeded from the hoisted last-delivered value, so a
    // rebuilt Home renders the full row on its first frame instead of watching the cards trickle
    // back in — which is also what gives the reverse container transform its target (ADR-0065).
    val favorites by rediscoverRepository.observeFavoritesSummary()
        .collectAsState(surfaceState.lastFavoritesSummary)
    val allPhotos by rediscoverRepository.observeAllPhotosSummary()
        .collectAsState(surfaceState.lastAllPhotosSummary)

    var today by remember(clock) { mutableStateOf(RediscoverCalendar.localDate(clock.now())) }
    LaunchedEffect(today) {
        delay(RediscoverCalendar.millisecondsUntilNextDay(clock.now()))
        today = RediscoverCalendar.localDate(clock.now())
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        today = RediscoverCalendar.localDate(clock.now())
    }

    val onThisDayPreviews by rediscoverRepository.observeOnThisDayPreviews(
        today = today,
        startOfToday = RediscoverCalendar.startOfDay(today),
    ).collectAsState(surfaceState.lastOnThisDayPreviews)

    val fromYourPastQuery = remember(today, clock) {
        val now = clock.now()
        RediscoverQuery(
            today = today,
            startOfToday = RediscoverCalendar.startOfDay(today),
            recentCutoff = now - com.vaibhav.relive.domain.time.Duration.ofDays(90),
            dailySeed = today.year.toLong() * 10_000L + today.month * 100L + today.day,
        )
    }
    val fromYourPastPreviews by rediscoverRepository.observeFromYourPastPreviews(fromYourPastQuery)
        .collectAsState(surfaceState.lastFromYourPastPreviews)
    SideEffect {
        surfaceState.lastFavoritesSummary = favorites
        surfaceState.lastAllPhotosSummary = allPhotos
        surfaceState.lastOnThisDayPreviews = onThisDayPreviews
        surfaceState.lastFromYourPastPreviews = fromYourPastPreviews
    }

    // Each card resolves its cover from the same inputs the card composable renders it from, so
    // what travels to the opened collection is exactly the image on screen (ADR-0065). Every
    // collection opens at its top: no card carries a selected moment.
    val cards = buildList {
        if (behaviorPreferences.showFavorites) {
            val cover = resolvedRediscoverCollectionCover(favorites.previewAttachments, "collection-favourites")
            add(
                RediscoverCollectionCardModel(
                    key = REDISCOVER_CARD_FAVOURITES,
                    title = "Favourites",
                    coverAttachments = favorites.previewAttachments,
                    coverSeed = "collection-favourites",
                    onOpen = { onOpenFavorites(null, cover) },
                ),
            )
        }
        // On This Day and From Your Past drop out of the row entirely when they have nothing to
        // show; the row closes up rather than reserving a gap.
        if (behaviorPreferences.showOnThisDay && onThisDayPreviews.isNotEmpty()) {
            val cover = resolvedRediscoverCollectionCover(emptyList(), "collection-on-this-day")
            add(
                RediscoverCollectionCardModel(
                    key = REDISCOVER_CARD_ON_THIS_DAY,
                    title = "On This Day",
                    coverAttachments = emptyList(),
                    coverSeed = "collection-on-this-day",
                    onOpen = { onOpenOnThisDay(null, today, cover) },
                ),
            )
        }
        if (fromYourPastPreviews.isNotEmpty()) {
            val cover = resolvedRediscoverCollectionCover(emptyList(), "collection-from-your-past")
            add(
                RediscoverCollectionCardModel(
                    key = REDISCOVER_CARD_FROM_YOUR_PAST,
                    title = "From Your Past",
                    coverAttachments = emptyList(),
                    coverSeed = "collection-from-your-past",
                    onOpen = { onOpenFromYourPast(null, fromYourPastQuery, cover) },
                ),
            )
        }
        val allPhotosCover = resolvedRediscoverCollectionCover(allPhotos.previewAttachments, "collection-all-photos")
        add(
            RediscoverCollectionCardModel(
                key = REDISCOVER_CARD_ALL_PHOTOS,
                title = "All Photos",
                coverAttachments = allPhotos.previewAttachments,
                coverSeed = "collection-all-photos",
                onOpen = { onOpenAllPhotos(allPhotosCover) },
            ),
        )
    }

    // One carousel state serves two nodes. The carousel itself lives in the backdrop, but the
    // feed's transparent window is drawn over it and wins the hit test at rest (the topmost
    // pointer-input branch is chosen at pointer-down), so a horizontal drag on the row would die
    // in the window. The window instead proxies horizontal drags into this state, while vertical
    // drags fall through to the feed's own scrolling as before. The state itself is hoisted in
    // [surfaceState] so the row holds its position across navigation; its item count is fed from
    // here because the hoisted state outlives any one card list.
    val rediscoverCarouselState = surfaceState.rediscoverCarousel
    SideEffect { surfaceState.rediscoverCardCount = cards.size }
    // The row is put back the way the feed is: the projections rebuild from empty on return, so
    // over the first frames the card count climbs back up, and every step of that climb can shove
    // the hoisted pager off its position. Wait until the row can hold the anchor and the count has
    // stopped moving, seat the anchor once, and only then start recording — the transient frames
    // are never mistaken for somewhere the person actually browsed to.
    var isRowRestored by remember { mutableStateOf(false) }
    LaunchedEffect(surfaceState) {
        val anchor = surfaceState.rediscoverAnchorItem
        if (anchor > 0) {
            withTimeoutOrNull(HOME_RESTORE_TIMEOUT_MS) {
                snapshotFlow { surfaceState.rediscoverCardCount }.first { it > anchor }
            }
            var settled = surfaceState.rediscoverCardCount
            while (true) {
                val next = withTimeoutOrNull(HOME_ROW_COUNT_SETTLE_MS) {
                    snapshotFlow { surfaceState.rediscoverCardCount }.first { it != settled }
                } ?: break
                settled = next
            }
            // With the collectors seeded from the last-delivered projections the position is
            // normally undisturbed; the seat is only for genuine drift, so the healthy path never
            // jumps the row mid-transform.
            val available = surfaceState.rediscoverCardCount
            if (available > 0 && surfaceState.rediscoverCarousel.currentItem != anchor) {
                surfaceState.rediscoverCarousel.scrollToItem(anchor.coerceAtMost(available - 1))
            }
        }
        isRowRestored = true
    }
    LaunchedEffect(surfaceState) {
        snapshotFlow { surfaceState.rediscoverCarousel.currentItem }.collect { item ->
            if (isRowRestored) surfaceState.rediscoverAnchorItem = item
        }
    }
    val rediscoverCarouselFling = CarouselDefaults.singleAdvanceFlingBehavior(rediscoverCarouselState)
    val rediscoverDragReversed = ScrollableDefaults.reverseDirection(
        LocalLayoutDirection.current,
        Orientation.Horizontal,
        false,
    )
    // The window owns taps over the row for the same hit-test reason it owns horizontal drags, so
    // it forwards them here against the cards' registered visible bounds — without this, a tap on
    // a card at rest dies in the window and the row only opens when the backdrop is expanded.
    val rediscoverRowHitTester = remember { RediscoverRowHitTester() }

    TimelineScreen(
        momentRepository = momentRepository,
        timelineRepository = timelineRepository,
        appearanceRepository = appearanceRepository,
        rediscoverRepository = rediscoverRepository,
        clock = clock,
        idGenerator = idGenerator,
        mediaStore = mediaStore,
        mediaProcessor = mediaProcessor,
        draftStore = draftStore,
        initialTimeline = CurrentTimeline.All,
        openComposerOnEnter = openComposerOnEnter,
        incomingShare = incomingShare,
        onIncomingShareApplied = onIncomingShareApplied,
        onOpenTimelineTheme = onOpenTimelineTheme,
        behaviorPreferences = behaviorPreferences,
        onComposerExpandedChanged = onComposerExpandedChanged,
        isHomeSurface = true,
        // Opening the composer seats it at the top of the feed, which is only meaningful once
        // All moments is back on screen.
        onExpandingComposer = {
            if (expansion.expansionPx > 0f) {
                animateExpansionTo(expansion, 0f, settleDurationMillis, settleEasing)
            }
        },
        expandComposerRequest = expandComposerRequest,
        onExpandComposerRequestHandled = onExpandComposerRequestHandled,
        listState = listState,
        homeHeaderCount = HOME_HEADER_ITEM_COUNT,
        isFocusedAllMoments = isFocused,
        onFocusedAllMomentsChanged = onFocusedAllMomentsChanged,
        homeAppBarLeading = if (onOpenProfile == null) null else {
            {
                IconButton(
                    onClick = onOpenProfile,
                    modifier = Modifier
                        .size(ReliveTheme.dimensions.minTouchTarget)
                        .semantics { contentDescription = "Open Profile" },
                ) {
                    ProfileAffordanceGlyph(profileSettings.profilePhoto, mediaStore)
                }
            }
        },
        listModifier = Modifier
            // The sheet rests below the floating wordmark-and-profile strip rather than at the
            // top of the screen, so the feed is inset to match: its first pixel is the sheet's
            // top edge in the focused state, and the strip above it stays the canvas gradient's.
            .padding(top = homeControlsInset)
            // The feed slides down past the bottom edge with its sheet, so what travels off must
            // stop being drawn. Clipping the feed rather than the whole content area leaves the
            // backdrop free to bleed to the screen edges.
            .clipToBounds()
            .floatingToolbarNestedScroll(
                expanded = navigationToolbarExpanded,
                onExpand = onNavigationToolbarExpand,
                onCollapse = onNavigationToolbarCollapse,
            )
            .nestedScroll(expansionConnection)
            // The feed rides with the sheet it sits on, so the timeline leaves and re-enters by
            // the bottom edge as one surface instead of standing still while its ground moves.
            .graphicsLayer { translationY = expansion.expansionPx },
        homeBackdrop = {
            HomeBackdrop(
                greeting = greeting,
                cards = cards,
                carouselState = rediscoverCarouselState,
                hitTester = rediscoverRowHitTester,
                cardContainerModifier = rediscoverCardModifier,
                mediaStore = mediaStore,
                scrolledIntoHeader = scrolledIntoHeader,
                headerHeightPx = headerHeightPx,
                expansionPx = expansion.expansionPx,
                wallpaper = wallpaper,
                // The measured height is hoisted so a rebuilt Home can reserve the backdrop's
                // space immediately, and mirrored into the expansion state, which needs it to know
                // how far the sheet may travel.
                onHeaderMeasured = {
                    surfaceState.headerHeightPx = it
                    expansion.backdropHeightPx = it
                },
                onViewportMeasured = { expansion.viewportHeightPx = it },
            )
        },
        homeHeader = {
            // A transparent window onto the backdrop. The welcome block and Rediscover row are
            // drawn behind the list, so this reserves their space without scrolling them. The
            // feed is already inset by the floating strip, so window bottom and sheet top stay
            // the same line throughout. The window also owns the hit test over the backdrop, so
            // it forwards horizontal drags to the Rediscover carousel and taps to the card under
            // them; vertical drags pass to the list around it as always.
            item(key = "home-backdrop-window") {
                var windowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(with(density) { headerHeightPx.toDp() })
                        .onGloballyPositioned { windowCoordinates = it }
                        .pointerInput(rediscoverRowHitTester) {
                            detectTapGestures { offset ->
                                windowCoordinates?.let { coordinates ->
                                    rediscoverRowHitTester.openAt(coordinates.localToRoot(offset))
                                }
                            }
                        }
                        .scrollable(
                            state = rediscoverCarouselState,
                            orientation = Orientation.Horizontal,
                            reverseDirection = rediscoverDragReversed,
                            flingBehavior = rediscoverCarouselFling,
                        ),
                )
            }
            item(key = "home-all-moments-heading") {
                // This heading is the first thing on the All moments sheet, and that sheet keeps
                // its own wallpaper whatever the app's appearance mode is. So it takes its colour
                // from the wallpaper the way the timeline's own content does — the app palette
                // would put dark mode's near-white type on a light cream ground.
                SectionHeading(
                    text = "All moments",
                    color = timelineMomentForegroundColors(
                        colors = ReliveTheme.colors,
                        wallpaper = LocalTimelineWallpaperPalette.current,
                    ).textPrimary,
                )
            }
        },
    )
}

/** The backdrop window and the `All moments` heading. Keep in step with [HomeScreen]. */
private const val HOME_HEADER_ITEM_COUNT = 2

/**
 * How far down the screen Home's sheet comes to rest: clear of the status bar and the floating
 * wordmark-and-profile strip. Read by the feed's inset, the window item reserving the backdrop's
 * space, and the welcome block's own top clearance, so the sheet's resting edge, the feed's first
 * pixel and the welcome block all agree on where the strip ends. The strip itself (in
 * `HomeFloatingHeaderActions`) is a status-bar inset plus a touch target with `sm` padding either
 * side — mirrored here rather than measured, like the sliding cover's controls inset.
 */
@Composable
private fun homeControlsInset(): Dp {
    val dims = ReliveTheme.dimensions
    return WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
        dims.minTouchTarget + dims.spacing.sm * 2
}

/**
 * How far the welcome content drifts down into the room the expanded state opens up. A fraction,
 * not the whole distance: the block should look like it is settling into the space rather than
 * being dragged along by the sheet.
 */
private const val HOME_WELCOME_EXPAND_DRIFT = 0.12f

/**
 * The welcome block and Rediscover row, drawn behind the scrolling list, plus the opaque surface
 * the All moments sheet rides on.
 *
 * The whole layer lives below the floating wordmark-and-profile strip: the welcome block starts
 * under it, is clipped at its lower edge as the parallax carries it up, and the sheet comes to
 * rest against that same line in the focused state rather than running under the strip to the top
 * of the screen. The strip itself stays the canvas gradient's at every scroll position.
 *
 * The sheet's top edge sits at `inset + headerHeight - scrolledIntoHeader`, so scrolling down
 * raises it over the welcome area and scrolling up lowers it again — one continuous, reversible
 * surface (ADR-0061) where the timeline visibly covers what is above it rather than sliding in
 * lockstep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeBackdrop(
    greeting: String,
    cards: List<RediscoverCollectionCardModel>,
    carouselState: CarouselState,
    hitTester: RediscoverRowHitTester,
    cardContainerModifier: @Composable (RediscoverCollectionCardModel) -> Modifier,
    mediaStore: MediaStore,
    scrolledIntoHeader: Int,
    headerHeightPx: Int,
    expansionPx: Float,
    wallpaper: TimelineWallpaper,
    onHeaderMeasured: (Int) -> Unit,
    onViewportMeasured: (Int) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val controlsInset = homeControlsInset()
    val controlsInsetPx = with(LocalDensity.current) { controlsInset.roundToPx() }
    val sheetShape = RoundedCornerShape(
        topStart = dims.radii.xl,
        topEnd = dims.radii.xl,
    )
    val covered = if (headerHeightPx > 0) {
        (scrolledIntoHeader.toFloat() / headerHeightPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .bleedHorizontal(dims.timeline.horizontalPadding)
            // The welcome layer trails the sheet, so without this it would ride up past the top
            // edge of the content area.
            .clipToBounds()
            .onGloballyPositioned { onViewportMeasured(it.size.height) }
            // The welcome area follows the app's global appearance — palette, mode and canvas
            // treatment — not the All timeline's own wallpaper. A timeline's appearance governs its
            // own band, which on Home is the All moments sheet below (PRODUCT_SPEC §11). Painting
            // the app canvas across the whole layer also makes the sheet's edge legible as it rides
            // up, and gives the expanded state a ground of its own once the sheet has left.
            .background(colors.canvasBrush()),
    ) {
        Box(
            // The welcome layer's own window: it starts below the floating strip, and clipping
            // here (not at the content area's edge) is what stops the parallaxed greeting from
            // riding up behind the floating wordmark. Full-size so the downward drift of the
            // expanded state is never cut.
            modifier = Modifier
                .fillMaxSize()
                .padding(top = controlsInset)
                .clipToBounds(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { onHeaderMeasured(it.size.height) }
                    .graphicsLayer {
                        // Trails the sheet instead of matching it, which is what makes the timeline
                        // read as passing in front rather than pushing.
                        translationY = -scrolledIntoHeader * BACKDROP_PARALLAX +
                            expansionPx * HOME_WELCOME_EXPAND_DRIFT
                        alpha = 1f - covered * 0.4f
                    },
            ) {
                Column {
                    // No horizontal wrapper padding: the greeting, its subtitle and the section
                    // heading carry their own `spacing.xl` inset, which is exactly the Rediscover
                    // row's content padding, so all of them start on the Favourites card's leading
                    // edge.
                    WelcomeBlock(greeting)
                    SectionHeading("Relive your memories")
                    RediscoverCollectionRow(
                        cards = cards,
                        mediaStore = mediaStore,
                        state = carouselState,
                        hitTester = hitTester,
                        cardContainerModifier = cardContainerModifier,
                    )
                    Spacer(Modifier.height(dims.spacing.xl))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        0,
                        controlsInsetPx +
                            (headerHeightPx - scrolledIntoHeader).coerceAtLeast(0) +
                            expansionPx.roundToInt(),
                    )
                }
                .shadow(
                    elevation = dims.timelineHome.cardElevation * covered,
                    shape = sheetShape,
                    clip = false,
                    ambientColor = colors.shadow,
                    spotColor = colors.shadow,
                )
                .background(colors.bgCanvas, sheetShape)
                .clip(sheetShape),
        ) {
            // The sheet carries the same wallpaper as the surface it covers, so raising it reads as
            // one material moving rather than a flat panel sliding over a decorated background.
            TimelineWallpaperSurface(
                wallpaper = wallpaper,
                modifier = Modifier.fillMaxSize(),
            ) {}
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun Modifier.floatingToolbarNestedScroll(
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
): Modifier = with(FloatingToolbarDefaults) {
    floatingToolbarVerticalNestedScroll(
        expanded = expanded,
        onExpand = onExpand,
        onCollapse = onCollapse,
    )
}

@Composable
private fun WelcomeBlock(greeting: String) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.lg),
    ) {
        Text(
            text = greeting,
            style = ReliveTheme.typography.display,
            color = ReliveTheme.colors.textPrimary,
        )
        Text(
            text = HOME_GREETING_SUBTITLE,
            style = ReliveTheme.typography.subtitle,
            color = ReliveTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun SectionHeading(text: String, color: Color = ReliveTheme.colors.textPrimary) {
    val dims = ReliveTheme.dimensions
    Text(
        text = text,
        style = ReliveTheme.typography.title,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md),
    )
}
