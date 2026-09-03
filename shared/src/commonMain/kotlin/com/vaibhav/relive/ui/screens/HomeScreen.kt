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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
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
import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.MomentId
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
import com.vaibhav.relive.presentation.date.editorialDayMonth
import com.vaibhav.relive.presentation.home.HOME_GREETING_SUBTITLE
import com.vaibhav.relive.presentation.home.homeGreeting
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import com.vaibhav.relive.ui.components.rediscover.RediscoverCollectionCardModel
import com.vaibhav.relive.ui.components.rediscover.RediscoverCollectionRow
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
 * put back where it was, once it can hold that position again.
 */
@Stable
class HomeSurfaceState {
    var headerHeightPx: Int by mutableIntStateOf(0)
    var anchorIndex: Int by mutableIntStateOf(0)
    var anchorScrollOffset: Int by mutableIntStateOf(0)
}

@Composable
fun rememberHomeSurfaceState(): HomeSurfaceState = remember { HomeSurfaceState() }

/** How long to wait for the reloaded feed to grow back far enough to hold the anchor. */
private const val HOME_RESTORE_TIMEOUT_MS = 2_000L

/** How many pages a restore will wait through before settling for as deep as it got. */
private const val HOME_RESTORE_PAGE_ATTEMPTS = 8

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
    onOpenFavorites: (MomentId?) -> Unit,
    onOpenOnThisDay: (MomentId, LocalCalendarDate) -> Unit,
    onOpenFromYourPast: (MomentId?, RediscoverQuery) -> Unit,
    onOpenAllPhotos: () -> Unit,
    draftStore: TimelineComposerDraftStore? = null,
    /**
     * Incremented by `+ New`. Each new value expands the inline composer in place; a counter rather
     * than a flag because Home is persistent and a latched boolean would only ever fire once.
     */
    expandComposerRequest: Int = 0,
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

    // Geometry of the sheet riding over the welcome area. The backdrop measures itself, and the
    // list's own scroll position drives how far the sheet has covered it — one scroll container,
    // one scroll position (ADR-0061); only the layering changes.
    val headerHeightPx = surfaceState.headerHeightPx
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
    // archive (ADR-0061).
    val favorites by rediscoverRepository.observeFavoritesSummary()
        .collectAsState(com.vaibhav.relive.domain.model.FavoritesCollectionSummary(0, emptyList()))
    val allPhotos by rediscoverRepository.observeAllPhotosSummary()
        .collectAsState(com.vaibhav.relive.domain.model.AllPhotosCollectionSummary(0, emptyList()))

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
    ).collectAsState(emptyList())

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
        .collectAsState(emptyList())

    val cards = buildList {
        if (behaviorPreferences.showFavorites) {
            add(
                RediscoverCollectionCardModel(
                    key = "favourites",
                    title = "Favourites",
                    subtitle = momentCountLabel(favorites.momentCount),
                    coverAttachments = favorites.previewAttachments,
                    coverSeed = "collection-favourites",
                    onOpen = { onOpenFavorites(null) },
                ),
            )
        }
        // On This Day and From Your Past drop out of the row entirely when they have nothing to
        // show; the row closes up rather than reserving a gap.
        if (behaviorPreferences.showOnThisDay && onThisDayPreviews.isNotEmpty()) {
            add(
                RediscoverCollectionCardModel(
                    key = "on-this-day",
                    title = "On This Day",
                    subtitle = today.editorialDayMonth(),
                    coverAttachments = emptyList(),
                    coverSeed = "collection-on-this-day",
                    onOpen = {
                        onThisDayPreviews.firstOrNull()?.let { onOpenOnThisDay(it.id, today) }
                    },
                ),
            )
        }
        if (fromYourPastPreviews.isNotEmpty()) {
            add(
                RediscoverCollectionCardModel(
                    key = "from-your-past",
                    title = "From Your Past",
                    subtitle = momentCountLabel(fromYourPastPreviews.size.toLong()),
                    coverAttachments = emptyList(),
                    coverSeed = "collection-from-your-past",
                    onOpen = { onOpenFromYourPast(null, fromYourPastQuery) },
                ),
            )
        }
        add(
            RediscoverCollectionCardModel(
                key = "all-photos",
                title = "All Photos",
                subtitle = momentCountLabel(allPhotos.momentCount),
                coverAttachments = allPhotos.previewAttachments,
                coverSeed = "collection-all-photos",
                onOpen = onOpenAllPhotos,
            ),
        )
    }

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
            // drawn behind the list, so this reserves their space without scrolling them.
            item(key = "home-backdrop-window") {
                Spacer(Modifier.fillMaxWidth().height(with(density) { headerHeightPx.toDp() }))
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
 * How far the welcome content drifts down into the room the expanded state opens up. A fraction,
 * not the whole distance: the block should look like it is settling into the space rather than
 * being dragged along by the sheet.
 */
private const val HOME_WELCOME_EXPAND_DRIFT = 0.12f

/**
 * The welcome block and Rediscover row, drawn behind the scrolling list, plus the opaque surface
 * the All moments sheet rides on.
 *
 * The sheet's top edge sits at `headerHeight - scrolledIntoHeader`, so scrolling down raises it
 * over the welcome area and scrolling up lowers it again — one continuous, reversible surface
 * (ADR-0061) where the timeline visibly covers what is above it rather than sliding in lockstep.
 */
@Composable
private fun HomeBackdrop(
    greeting: String,
    cards: List<RediscoverCollectionCardModel>,
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
            // The welcome layer trails the sheet, so without this it would ride up past the top of
            // the content area and show through the translucent app bar.
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
                // No wrapper padding here: the greeting, its subtitle and the section heading
                // carry their own `spacing.xl` inset, which is exactly the Rediscover row's
                // content padding, so all of them start on the Favourites card's leading edge.
                WelcomeBlock(greeting)
                SectionHeading("Relive your memories")
                RediscoverCollectionRow(cards = cards, mediaStore = mediaStore)
                Spacer(Modifier.height(dims.spacing.xl))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        0,
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

private fun momentCountLabel(count: Long): String =
    if (count == 1L) "1 moment" else "$count moments"

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
