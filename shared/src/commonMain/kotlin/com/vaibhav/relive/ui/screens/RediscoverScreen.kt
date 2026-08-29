package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.BehaviorPreferences
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.FavoriteMomentPreview
import com.vaibhav.relive.domain.model.RediscoverQuery
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.repository.TimelineHomeRepository
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.presentation.date.anniversaryYearLabel
import com.vaibhav.relive.presentation.date.editorialDayMonth
import com.vaibhav.relive.presentation.rediscover.RediscoverSectionSpacing
import com.vaibhav.relive.presentation.rediscover.rediscoverSectionLayout
import com.vaibhav.relive.presentation.cardcover.allTimelineCollageBucket
import com.vaibhav.relive.ui.components.navigation.ReliveWordmarkAppBar
import com.vaibhav.relive.ui.components.rediscover.FavoriteMomentCard
import com.vaibhav.relive.ui.components.rediscover.OnThisDayMomentCard
import com.vaibhav.relive.ui.components.timeline.ForwardGlyph
import com.vaibhav.relive.ui.components.timeline.HeartGlyph
import com.vaibhav.relive.ui.theme.ReliveTheme
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** The active root collects only bounded Favorites and On This Day shelves. */
@Composable
fun RediscoverScreen(
    repository: RediscoverRepository,
    timelineHomeRepository: TimelineHomeRepository,
    clock: Clock,
    mediaStore: MediaStore,
    listState: LazyListState,
    onOpenAll: () -> Unit,
    onOpenFavorites: (MomentId?) -> Unit,
    onOpenOnThisDay: (MomentId, LocalCalendarDate) -> Unit,
    onOpenFromYourPast: (MomentId?, RediscoverQuery) -> Unit,
    behaviorPreferences: BehaviorPreferences = BehaviorPreferences(),
    debugControls: (@Composable () -> Unit)? = null,
    onCreateMoment: (() -> Unit)? = null,
    navigationToolbarExpanded: Boolean = true,
    onNavigationToolbarExpand: () -> Unit = {},
    onNavigationToolbarCollapse: () -> Unit = {},
) {
    val timelineSummaries by timelineHomeRepository.observeSummaries().collectAsState(emptyList())
    val allSummary = timelineSummaries.firstOrNull { it.timeline == Timeline.All }
    val previews by repository.observeFavoritePreviews().collectAsState(emptyList())
    var today by remember(clock) { mutableStateOf(RediscoverCalendar.localDate(clock.now())) }
    var collageBucket by remember(clock) { mutableLongStateOf(allTimelineCollageBucket(clock.now())) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        collageBucket = allTimelineCollageBucket(clock.now())
    }
    LaunchedEffect(today) {
        delay(RediscoverCalendar.millisecondsUntilNextDay(clock.now()))
        today = RediscoverCalendar.localDate(clock.now())
    }
    val onThisDayPreviews by repository.observeOnThisDayPreviews(
        today = today,
        startOfToday = RediscoverCalendar.startOfDay(today),
    ).collectAsState(emptyList())
    val allCollageCandidates by timelineHomeRepository.observeAllCollageCandidates(collageBucket)
        .collectAsState(emptyList())
    val fromYourPastQuery = remember(today, clock) {
        val now = clock.now()
        RediscoverQuery(
            today = today,
            startOfToday = RediscoverCalendar.startOfDay(today),
            recentCutoff = now - com.vaibhav.relive.domain.time.Duration.ofDays(90),
            dailySeed = today.year.toLong() * 10_000L + today.month * 100L + today.day,
        )
    }
    val fromYourPastPreviews by repository.observeFromYourPastPreviews(fromYourPastQuery)
        .collectAsState(emptyList())
    var debugOpen by remember { mutableStateOf(false) }
    val dims = ReliveTheme.dimensions
    val sectionLayout = rediscoverSectionLayout(
        favoriteMomentCount = previews.size,
        onThisDayMomentCount = onThisDayPreviews.size,
        fromYourPastMomentCount = fromYourPastPreviews.size,
        showFavorites = behaviorPreferences.showFavorites,
        showOnThisDay = behaviorPreferences.showOnThisDay,
    )
    val bottomPadding = if (onCreateMoment != null) dims.spacing.huge * 2 else dims.spacing.huge

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(ReliveTheme.colors.bgCanvas)
                .floatingToolbarNestedScroll(
                    expanded = navigationToolbarExpanded,
                    onExpand = onNavigationToolbarExpand,
                    onCollapse = onNavigationToolbarCollapse,
                ),
            contentPadding = PaddingValues(bottom = bottomPadding),
        ) {
        item(key = "relive-app-bar") {
            ReliveWordmarkAppBar(title = "Rediscover", style = ReliveTheme.typography.title)
        }
        allSummary?.let { summary ->
            item(key = "all") {
                Box(modifier = Modifier.padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md)) {
                    TimelineHomeCard(
                        summary = summary,
                        mediaStore = mediaStore,
                        allCollageBucket = collageBucket,
                        allCollageCandidates = allCollageCandidates,
                        onClick = onOpenAll,
                    )
                }
            }
        }
        if (sectionLayout.showFavorites) {
            item(key = "favorites-heading") {
                Text(
                    text = "FAVOURITES",
                    style = ReliveTheme.typography.title,
                    color = ReliveTheme.colors.accentMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = debugControls?.let { { debugOpen = true } },
                        )
                        .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md),
                )
            }
            if (sectionLayout.showFavoritesEmptyState) {
                item(key = "favorites-empty") { FavoritesEmptyState() }
            } else {
                item(key = "favorites-shelf") {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = dims.spacing.xl),
                            horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
                        ) {
                            items(previews, key = { it.id.value }) { moment ->
                                FavoriteMomentCard(
                                    moment = moment,
                                    mediaStore = mediaStore,
                                    modifier = Modifier.width(
                                        maxWidth * dims.rediscover.favoriteShelfCardWidthFraction,
                                    ),
                                    onOpen = { onOpenFavorites(moment.id) },
                                )
                            }
                        }
                    }
                }
                item(key = "favorites-show-all") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.sm),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(
                                    minWidth = dims.minTouchTarget,
                                    minHeight = dims.minTouchTarget,
                                )
                                .clickable(role = Role.Button) { onOpenFavorites(null) }
                                .semantics { contentDescription = "Show all favorite moments" },
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Show all",
                                    style = ReliveTheme.typography.action,
                                    color = ReliveTheme.colors.accent,
                                )
                                ForwardGlyph(
                                    size = dims.icon.sm,
                                    color = ReliveTheme.colors.accent,
                                    strokeWidth = dims.stroke.icon,
                                    modifier = Modifier.padding(start = dims.spacing.xs),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (sectionLayout.showOnThisDay) {
            item(key = "on-this-day-heading") {
                Text(
                    text = "ON THIS DAY",
                    style = ReliveTheme.typography.title,
                    color = ReliveTheme.colors.accentMuted,
                    modifier = Modifier.padding(
                        start = dims.spacing.xl,
                        end = dims.spacing.xl,
                        top = dims.spacing.xl,
                    ),
                )
            }
            item(key = "on-this-day-date") {
                Text(
                    text = today.editorialDayMonth(),
                    style = ReliveTheme.typography.title,
                    color = ReliveTheme.colors.textPrimary,
                    modifier = Modifier.padding(horizontal = dims.spacing.xl, vertical = dims.spacing.xs),
                )
            }
            item(key = "on-this-day-shelf") {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = dims.spacing.xl),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
                    ) {
                        items(onThisDayPreviews, key = { it.id.value }) { moment ->
                            OnThisDayMomentCard(
                                moment = moment,
                                anniversaryLabel = anniversaryYearLabel(today.year, moment.localYear),
                                mediaStore = mediaStore,
                                modifier = Modifier.width(maxWidth * dims.rediscover.onThisDayShelfCardWidthFraction),
                                onOpen = { onOpenOnThisDay(moment.id, today) },
                            )
                        }
                    }
                }
            }
        }
        if (sectionLayout.showFromYourPast) {
            item(key = "from-your-past-heading") {
                val topSpacing = when (sectionLayout.fromYourPastSpacing) {
                    RediscoverSectionSpacing.Normal -> dims.spacing.xl
                    RediscoverSectionSpacing.Expanded -> dims.spacing.xxl
                }
                Text(
                    text = "FROM YOUR PAST",
                    style = ReliveTheme.typography.title,
                    color = ReliveTheme.colors.accentMuted,
                    modifier = Modifier.padding(start = dims.spacing.xl, end = dims.spacing.xl, top = topSpacing),
                )
            }
            item(key = "from-your-past-subtitle") {
                Text(
                    text = "A few moments worth seeing again",
                    style = ReliveTheme.typography.subtitle,
                    color = ReliveTheme.colors.textSecondary,
                    modifier = Modifier.padding(
                        start = dims.spacing.xl,
                        end = dims.spacing.xl,
                        top = dims.spacing.xs,
                        bottom = dims.spacing.sm,
                    ),
                )
            }
            item(key = "from-your-past-shelf") {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = dims.spacing.xl),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
                    ) {
                        items(fromYourPastPreviews, key = { it.id.value }) { moment ->
                            FavoriteMomentCard(
                                moment = FavoriteMomentPreview(
                                    id = moment.id,
                                    createdAt = moment.createdAt,
                                    title = moment.title,
                                    content = moment.content,
                                    attachments = moment.attachments,
                                ),
                                mediaStore = mediaStore,
                                modifier = Modifier.width(maxWidth * dims.rediscover.favoriteShelfCardWidthFraction),
                                showFavoriteIndicator = false,
                                semanticDescription = "Open memory from your past",
                                onOpen = { onOpenFromYourPast(moment.id, fromYourPastQuery) },
                            )
                        }
                    }
                }
            }
            item(key = "from-your-past-explore") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.sm),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(
                                minWidth = dims.minTouchTarget,
                                minHeight = dims.minTouchTarget,
                            )
                            .clickable(role = Role.Button) {
                                onOpenFromYourPast(null, fromYourPastQuery)
                            }
                            .semantics { contentDescription = "Explore memories from your past" },
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Explore", style = ReliveTheme.typography.action, color = ReliveTheme.colors.accent)
                            ForwardGlyph(
                                size = dims.icon.sm,
                                color = ReliveTheme.colors.accent,
                                strokeWidth = dims.stroke.icon,
                                modifier = Modifier.padding(start = dims.spacing.xs),
                            )
                        }
                    }
                }
            }
        }
        }
    }
    if (debugOpen) {
        AlertDialog(onDismissRequest = { debugOpen = false }, confirmButton = {}, text = { debugControls?.invoke() })
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
private fun FavoritesEmptyState() {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val shape = RoundedCornerShape(dims.rediscover.cardOuterRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dims.rediscover.favoriteShelfCardHeight)
                .background(colors.surfaceCard, shape)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Favorites. Your favorite memories will appear here."
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HeartGlyph(
                size = dims.icon.lg,
                color = colors.accentMuted,
                strokeWidth = dims.stroke.icon,
                filled = false,
            )
            Text(
                text = "Your favorite memories",
                style = ReliveTheme.typography.action,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = dims.spacing.lg),
            )
            Text(
                text = "Moments you favorite\nwill appear here.",
                style = ReliveTheme.typography.subtitle,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = dims.spacing.sm),
            )
        }
    }
}
