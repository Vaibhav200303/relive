package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.presentation.search.SearchViewModel
import com.vaibhav.relive.presentation.search.SearchFilter
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.CalendarGlyph
import com.vaibhav.relive.ui.components.timeline.DateNavigationPicker
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.components.timeline.TimelineMediaSharedTransition
import com.vaibhav.relive.ui.components.timeline.sharedTransitionKey
import com.vaibhav.relive.ui.components.composer.CloseGlyph
import com.vaibhav.relive.ui.components.viewer.MediaViewer
import com.vaibhav.relive.ui.components.viewer.MomentMediaGallery
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.spec
import com.vaibhav.relive.platform.media.ActivePlayback
import com.vaibhav.relive.presentation.viewer.TimelineMediaNavState
import com.vaibhav.relive.presentation.viewer.closeGallery
import com.vaibhav.relive.presentation.viewer.closeViewer
import com.vaibhav.relive.presentation.viewer.openFromCollage
import com.vaibhav.relive.presentation.viewer.openFromGallery
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.platform.system.ReliveBackHandler
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    mediaStore: MediaStore,
    listState: LazyListState,
    clock: Clock,
    onBack: () -> Unit,
    onOpenAllAtMoment: (com.vaibhav.relive.domain.model.MomentId?) -> Unit,
    onCreateMoment: (() -> Unit)? = null,
    wallpaper: TimelineWallpaper = TimelineWallpaper.WarmCream,
    navigationToolbarExpanded: Boolean = true,
    onNavigationToolbarExpand: () -> Unit = {},
    onNavigationToolbarCollapse: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    var navState by remember { mutableStateOf(TimelineMediaNavState.Idle) }
    var showDatePicker by remember { mutableStateOf(false) }
    val clearOrNavigateBack: () -> Unit = {
        if (state.query.isNotBlank()) {
            viewModel.clear()
            keyboard?.hide()
        } else {
            onBack()
        }
        Unit
    }

    ReliveBackHandler(enabled = true, onBack = clearOrNavigateBack)

    LaunchedEffect(state.activeMomentId) {
        state.activeMomentId?.let { id ->
            val index = state.results.indexOfFirst { it.id == id }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }
    LaunchedEffect(state.query) {
        if (state.query.isBlank()) listState.scrollToItem(0)
    }
    LaunchedEffect(state.dateNavigation) {
        state.dateNavigation?.let { navigation ->
            onOpenAllAtMoment(navigation.momentId)
            viewModel.consumeDateNavigation()
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val motion = ReliveTheme.motion
        val reduceMotion = ReliveTheme.reduceMotion
        val gallery = navState.gallery
        val galleryHeroAttachment = gallery?.heroAttachment
        var activeGalleryHeroAttachmentId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(galleryHeroAttachment?.sharedTransitionKey()) {
            galleryHeroAttachment?.let { activeGalleryHeroAttachmentId = it.sharedTransitionKey() }
        }
        LaunchedEffect(gallery, activeGalleryHeroAttachmentId, motion.durations.long2) {
            if (gallery == null && activeGalleryHeroAttachmentId != null) {
                delay(motion.durations.long2.toLong())
                if (navState.gallery == null) activeGalleryHeroAttachmentId = null
            }
        }
        val galleryHeroAttachmentId = galleryHeroAttachment?.sharedTransitionKey() ?: activeGalleryHeroAttachmentId
        val galleryBoundsTransform = remember(motion, reduceMotion) {
            BoundsTransform { _, _ ->
                motion.spec(
                    reduceMotion = reduceMotion,
                    full = tween(
                        durationMillis = motion.durations.long2,
                        easing = motion.easings.emphasized,
                    ),
                )
            }
        }
        val mediaSharedTransition = TimelineMediaSharedTransition(
            scope = this,
            activeAttachmentId = null,
            viewerVisible = false,
            activeGalleryAttachmentId = galleryHeroAttachmentId,
            galleryVisible = galleryHeroAttachment != null,
            reduceMotion = reduceMotion,
            boundsTransform = galleryBoundsTransform,
        )
        Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        SearchHeader(
            query = state.query,
            resultCount = state.resultCount,
            activeIndex = state.activeIndex,
            onBack = clearOrNavigateBack,
            onQueryChange = viewModel::updateQuery,
            onSearch = {
                viewModel.submitQuery()
                keyboard?.hide()
            },
            onClear = viewModel::clear,
            onPrevious = {
                viewModel.selectPrevious()
                keyboard?.show()
            },
            onNext = {
                viewModel.selectNext()
                keyboard?.show()
            },
            onJumpToDate = { showDatePicker = true },
        )
        SearchFilterBar(filter = state.filter, onFilterSelected = viewModel::selectFilter)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = ReliveTheme.dimensions.timeline.horizontalPadding)
                .windowInsetsPadding(WindowInsets.ime),
        ) {
            when {
                state.query.isBlank() -> SearchLanding(
                    recentSearches = state.recentSearches,
                    onSuggestionSelected = viewModel::useSuggestion,
                    onRecentSelected = viewModel::useSuggestion,
                    onRemoveRecent = viewModel::removeRecentSearch,
                    onClearRecent = viewModel::clearRecentSearches,
                )
                state.results.isEmpty() -> SearchEditorialState("No moments found.")
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .floatingToolbarNestedScroll(
                            expanded = navigationToolbarExpanded,
                            onExpand = onNavigationToolbarExpand,
                            onCollapse = onNavigationToolbarCollapse,
                        ),
                    contentPadding = PaddingValues(
                        bottom = if (onCreateMoment != null) {
                            ReliveTheme.dimensions.spacing.huge * 2
                        } else {
                            ReliveTheme.dimensions.spacing.huge
                        },
                    ),
                ) {
                    items(
                        count = state.results.size,
                        key = { state.results[it].id.value },
                    ) { index ->
                        val moment = state.results[index]
                        MomentCard(
                            moment = moment,
                            mediaStore = mediaStore,
                            onToggleFavorite = null,
                            onOpenMedia = { attachments, attachmentIndex ->
                                ActivePlayback.stopActive()
                                navState = navState.openFromCollage(attachments, attachmentIndex)
                            },
                            canEditOrForget = false,
                            onEdit = {},
                            onForget = {},
                            sharedTransition = mediaSharedTransition,
                            hasPreviousMoment = index > 0,
                            hasNextMoment = index < state.results.lastIndex,
                            isActive = state.activeIndex == index,
                        )
                    }
                }
            }
        }
        }
        AnimatedContent(
            targetState = gallery,
            contentKey = { it != null },
            transitionSpec = {
                val fadeSpec = motion.spec<Float>(
                    reduceMotion = reduceMotion,
                    full = tween(
                        durationMillis = motion.durations.long2,
                        easing = motion.easings.emphasized,
                    ),
                )
                fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)
            },
            label = "search-moment-media-gallery-container",
        ) { galleryState ->
            galleryState?.let { openGallery ->
                MomentMediaGallery(
                    state = openGallery,
                    mediaStore = mediaStore,
                    onOpenItem = { index -> navState = navState.openFromGallery(index) },
                    onClose = { navState = navState.closeGallery() },
                    backEnabled = navState.viewer == null,
                    wallpaper = wallpaper,
                    sharedTransition = mediaSharedTransition,
                )
            }
        }
        navState.viewer?.let { viewer ->
            MediaViewer(
                state = viewer,
                mediaStore = mediaStore,
                onIndexChange = { index -> navState = navState.copy(viewer = viewer.withCurrent(index)) },
                onClose = { navState = navState.closeViewer() },
                wallpaper = wallpaper,
            )
        }
    }
    }
    if (showDatePicker) {
        DateNavigationPicker(
            initialDate = RediscoverCalendar.localDate(clock.now()),
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                showDatePicker = false
                viewModel.jumpToDate(date)
            },
        )
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
private fun SearchHeader(
    query: String,
    resultCount: Int,
    activeIndex: Int?,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpToDate: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val previousEnabled = activeIndex != null && activeIndex > 0
    val nextEnabled = activeIndex != null && activeIndex < resultCount - 1
    val haptics = rememberReliveHaptics()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            // No header band: like every root, the search field floats on the canvas gradient.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                start = dims.spacing.xl,
                top = dims.spacing.none,
                end = dims.spacing.xl,
                bottom = dims.spacing.lg,
            ),
    ) {
        val showClear = query.isNotEmpty() && maxWidth >= 400.dp
        val showSearchControls = query.isNotBlank()
        val showCounter = showSearchControls && activeIndex != null && resultCount > 0
        val shape = RoundedCornerShape(dims.radii.pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(colors.surfaceCard)
                .border(dims.stroke.hairline, colors.borderMuted, shape)
                .padding(horizontal = dims.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(dims.minTouchTarget).semantics { contentDescription = "Back" },
            ) {
                BackGlyph(dims.icon.lg, colors.textPrimary, dims.stroke.icon)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search memories" },
                textStyle = ReliveTheme.typography.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = if (showClear) dims.minTouchTarget else dims.spacing.none),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search memories...",
                                style = ReliveTheme.typography.body,
                                color = colors.textMuted,
                            )
                        }
                        innerTextField()
                        if (showClear) {
                            IconButton(
                                onClick = {
                                    haptics.perform(ReliveHapticCue.Action)
                                    onClear()
                                },
                                modifier = Modifier
                                    .size(dims.minTouchTarget)
                                    .align(Alignment.CenterEnd)
                                    .semantics {
                                    contentDescription = "Clear search"
                                },
                            ) {
                                Text("×", color = colors.textPrimary)
                            }
                        }
                    }
                },
            )
            if (showCounter) {
                Text(
                    text = "${activeIndex + 1} / $resultCount",
                    style = ReliveTheme.typography.tag,
                    color = colors.textMuted,
                )
            }
            if (showSearchControls) {
                IconButton(
                    onClick = {
                        haptics.perform(ReliveHapticCue.Selection)
                        onPrevious()
                    },
                    enabled = previousEnabled,
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Previous search result" },
                ) { Text("↑", color = colors.textPrimary) }
                IconButton(
                    onClick = {
                        haptics.perform(ReliveHapticCue.Selection)
                        onNext()
                    },
                    enabled = nextEnabled,
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Next search result" },
                ) { Text("↓", color = colors.textPrimary) }
            } else {
                IconButton(
                    onClick = onJumpToDate,
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .semantics { contentDescription = "Jump to date" },
                ) {
                    CalendarGlyph(dims.icon.lg, colors.textPrimary, dims.stroke.icon)
                }
            }
        }
    }
}

@Composable
private fun SearchLanding(
    recentSearches: List<String>,
    onSuggestionSelected: (String) -> Unit,
    onRecentSelected: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val suggestions = listOf("beach", "birthday", "college", "friends", "food", "notes")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dims.spacing.huge * 2),
    ) {
        item {
            Text(
                text = "Find anything you've saved.",
                style = ReliveTheme.typography.subtitle,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = dims.spacing.md, bottom = dims.spacing.md),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.none)) {
                suggestions.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                    ) {
                        row.forEach { suggestion ->
                            SearchSuggestionChip(
                                label = suggestion,
                                onClick = { onSuggestionSelected(suggestion) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = dims.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recent searches", style = ReliveTheme.typography.body, color = colors.textPrimary)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Clear all",
                        style = ReliveTheme.typography.caption,
                        color = colors.accent,
                        modifier = Modifier.clickable(onClick = onClearRecent).padding(dims.spacing.sm),
                    )
                }
            }
            items(recentSearches, key = { it }) { query ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRecentSelected(query) }
                            .height(dims.minTouchTarget),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = ProfileIcons.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(dims.icon.md),
                            tint = colors.textSecondary,
                        )
                        Spacer(Modifier.width(dims.spacing.md))
                        Text(query, style = ReliveTheme.typography.body, color = colors.textSecondary, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { onRemoveRecent(query) },
                            modifier = Modifier.size(dims.minTouchTarget).semantics {
                                contentDescription = "Remove $query from recent searches"
                            },
                        ) {
                            CloseGlyph(dims.icon.md, colors.textMuted, dims.stroke.iconBold)
                        }
                    }
                    HorizontalDivider(color = colors.borderMuted, thickness = dims.stroke.hairline)
                }
            }
        }
    }
}

@Composable
private fun SearchFilterBar(filter: SearchFilter, onFilterSelected: (SearchFilter) -> Unit) {
    val dims = ReliveTheme.dimensions
    Row(
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.timeline.horizontalPadding, vertical = dims.spacing.xs),
    ) {
        SearchFilter.entries.forEach { option ->
            SearchFilterChip(
                filter = option,
                selected = option == filter,
                onClick = { onFilterSelected(option) },
            )
        }
    }
}

@Composable
private fun SearchFilterChip(filter: SearchFilter, selected: Boolean, onClick: () -> Unit) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val label = when (filter) {
        SearchFilter.All -> "All"
        SearchFilter.Tags -> "Tags"
        SearchFilter.Places -> "Places"
    }
    Box(
        modifier = Modifier
            .height(dims.minTouchTarget)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(dims.radii.pill))
                .background(if (selected) colors.accent else colors.surfaceCard)
                .border(
                    dims.stroke.hairline,
                    if (selected) colors.accent else colors.borderMuted,
                    RoundedCornerShape(dims.radii.pill),
                )
                .padding(horizontal = dims.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
        ) {
            if (filter != SearchFilter.All) {
                Icon(
                    imageVector = if (filter == SearchFilter.Tags) ProfileIcons.Tag else ProfileIcons.Location,
                    contentDescription = null,
                    modifier = Modifier.size(dims.icon.sm),
                    tint = if (selected) colors.textOnAccent else colors.textSecondary,
                )
            }
            Text(
                label,
                style = ReliveTheme.typography.tag,
                color = if (selected) colors.textOnAccent else colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SearchSuggestionChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Box(
        modifier = Modifier
            .then(modifier)
            .height(dims.minTouchTarget)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(dims.radii.pill))
                .background(colors.surfaceCard)
                .padding(horizontal = dims.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            Icon(ProfileIcons.Search, null, Modifier.size(dims.icon.md), colors.accent)
            Text(label, style = ReliveTheme.typography.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun SearchEditorialState(text: String) {
    Text(
        text = text,
        style = ReliveTheme.typography.subtitle,
        color = ReliveTheme.colors.textSecondary,
        modifier = Modifier.padding(top = ReliveTheme.dimensions.spacing.huge),
    )
}
