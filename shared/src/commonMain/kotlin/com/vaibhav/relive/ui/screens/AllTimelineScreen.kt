package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock
import com.vaibhav.relive.presentation.composer.MomentComposerState
import com.vaibhav.relive.presentation.composer.MomentComposerViewModel
import com.vaibhav.relive.presentation.timeline.AllTimelineUiState
import com.vaibhav.relive.presentation.timeline.AllTimelineViewModel
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import com.vaibhav.relive.ui.components.composer.MomentComposer
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.components.timeline.TimelineHeader
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * The built-in All timeline: header, cream canvas, a continuous left rail with
 * brown dots, and the moment stack. Reads chronologically top-to-bottom
 * (oldest first, newest last), with the inline composer pinned as the final
 * timeline item beneath the newest saved moment. Presentation-layer reversal
 * of the newest-first repository contract lives in [AllTimelineViewModel].
 */
@Composable
fun AllTimelineScreen(
    momentRepository: MomentRepository,
    clock: Clock,
    idGenerator: IdGenerator,
) {
    val scope = rememberCoroutineScope()
    val timelineViewModel: AllTimelineViewModel = remember(momentRepository, scope) {
        AllTimelineViewModel(momentRepository, scope)
    }
    val composerViewModel: MomentComposerViewModel = remember(momentRepository, clock, idGenerator, scope) {
        MomentComposerViewModel(momentRepository, clock, idGenerator, scope)
    }
    val timelineState by timelineViewModel.state.collectAsState()
    val composerState by composerViewModel.state.collectAsState()

    AllTimelineContent(
        timelineState = timelineState,
        composerState = composerState,
        clock = clock,
        onToggleFavorite = timelineViewModel::setFavorite,
        onMenuClick = { /* Settings arrives in Phase 8. */ },
        onSearchClick = { /* Search arrives in Phase 7. */ },
        onTitleChange = composerViewModel::updateTitle,
        onContentChange = composerViewModel::updateContent,
        onPendingTagChange = composerViewModel::updatePendingTagInput,
        onCommitPendingTag = composerViewModel::commitPendingTag,
        onRemoveTag = composerViewModel::removeTag,
        onToggleAddMedia = composerViewModel::toggleAddMedia,
        onReset = composerViewModel::reset,
        onKeepMoment = composerViewModel::keepMoment,
    )
}

@Composable
fun AllTimelineContent(
    timelineState: AllTimelineUiState,
    composerState: MomentComposerState,
    clock: Clock,
    onToggleFavorite: (MomentId, Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onPendingTagChange: (String) -> Unit,
    onCommitPendingTag: () -> Unit,
    onRemoveTag: (com.vaibhav.relive.domain.model.Tag) -> Unit,
    onToggleAddMedia: () -> Unit,
    onReset: () -> Unit,
    onKeepMoment: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas),
    ) {
        TimelineHeader(onMenuClick = onMenuClick, onSearchClick = onSearchClick)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dims.timeline.horizontalPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            val moments: List<MomentPresentation> = when (timelineState) {
                AllTimelineUiState.Loading -> emptyList()
                AllTimelineUiState.Empty -> emptyList()
                is AllTimelineUiState.Loaded -> timelineState.moments
            }

            // Continuous rail: a hairline running down the gutter, behind the
            // moment stack. Individual dots are drawn by MomentCard; the plus
            // marker is drawn by MomentComposer.
            val railInset = remember(dims.timeline.contentInset, dims.timeline.railWidth) {
                (dims.timeline.contentInset - dims.timeline.railWidth) / 2
            }
            Box(
                modifier = Modifier
                    .offset(x = railInset)
                    .width(dims.timeline.railWidth)
                    .fillMaxHeight()
                    .background(colors.borderMuted)
                    .align(Alignment.TopStart),
            )
            val listState = rememberLazyListState()
            // Composer sits at the end of the list; scrolling to `moments.size`
            // reveals both the newest moment and the composer.
            var lastSeenCount by remember { mutableIntStateOf(-1) }
            LaunchedEffect(moments.size) {
                val target = moments.size
                if (lastSeenCount == -1) {
                    // Initial snapshot: land at the latest end without an
                    // animated journey through the whole timeline.
                    listState.scrollToItem(target)
                } else if (target > lastSeenCount) {
                    // New moment persisted; keep composer + newest in view.
                    listState.animateScrollToItem(target)
                }
                lastSeenCount = target
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = dims.spacing.huge),
            ) {
                items(
                    items = moments,
                    key = { it.id.value },
                ) { moment ->
                    MomentCard(
                        moment = moment,
                        onToggleFavorite = { newValue ->
                            onToggleFavorite(moment.id, newValue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "composer") {
                    MomentComposer(
                        state = composerState,
                        clock = clock,
                        onTitleChange = onTitleChange,
                        onContentChange = onContentChange,
                        onPendingTagChange = onPendingTagChange,
                        onCommitPendingTag = onCommitPendingTag,
                        onRemoveTag = onRemoveTag,
                        onToggleAddMedia = onToggleAddMedia,
                        onReset = onReset,
                        onKeepMoment = onKeepMoment,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
