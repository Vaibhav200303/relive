package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.presentation.search.SearchViewModel
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.CalendarGlyph
import com.vaibhav.relive.ui.components.timeline.DateNavigationPicker
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.components.viewer.MediaViewer
import com.vaibhav.relive.ui.components.viewer.MomentMediaGallery
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.platform.media.ActivePlayback
import com.vaibhav.relive.presentation.viewer.TimelineMediaNavState
import com.vaibhav.relive.presentation.viewer.closeGallery
import com.vaibhav.relive.presentation.viewer.closeViewer
import com.vaibhav.relive.presentation.viewer.openFromCollage
import com.vaibhav.relive.presentation.viewer.openFromGallery
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.time.Clock

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
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var navState by remember { mutableStateOf(TimelineMediaNavState.Idle) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
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

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas)) {
        SearchHeader(
            query = state.query,
            resultCount = state.resultCount,
            activeIndex = state.activeIndex,
            focusRequester = focusRequester,
            onBack = onBack,
            onQueryChange = viewModel::updateQuery,
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
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = ReliveTheme.dimensions.timeline.horizontalPadding)
                .windowInsetsPadding(WindowInsets.ime),
        ) {
            when {
                state.query.isBlank() -> SearchEditorialState("Find anything you've saved.")
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
                            hasPreviousMoment = index > 0,
                            isActive = state.activeIndex == index,
                        )
                    }
                }
            }
        }
        }
        navState.gallery?.let { gallery ->
            MomentMediaGallery(
                state = gallery,
                mediaStore = mediaStore,
                onOpenItem = { index -> navState = navState.openFromGallery(index) },
                onClose = { navState = navState.closeGallery() },
                backEnabled = navState.viewer == null,
                wallpaper = wallpaper,
            )
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
    focusRequester: FocusRequester,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
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
            .background(colors.bgHeader)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.sm),
    ) {
        val showClear = query.isNotEmpty() && maxWidth >= 400.dp
        val showSearchControls = query.isNotBlank()
        val showCounter = showSearchControls && activeIndex != null && resultCount > 0
        val shape = RoundedCornerShape(dims.radii.pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.search.containerHeight)
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
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = "Search memories" },
                textStyle = ReliveTheme.typography.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                singleLine = true,
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
private fun SearchEditorialState(text: String) {
    Text(
        text = text,
        style = ReliveTheme.typography.subtitle,
        color = ReliveTheme.colors.textSecondary,
        modifier = Modifier.padding(top = ReliveTheme.dimensions.spacing.huge),
    )
}
