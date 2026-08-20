package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.presentation.timeline.AllTimelineUiState
import com.vaibhav.relive.presentation.timeline.AllTimelineViewModel
import com.vaibhav.relive.ui.components.timeline.EmptyTimelinePlaceholder
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.components.timeline.TimelineHeader
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * The built-in All timeline: header, cream canvas, continuous left rail with brown
 * dots, and the moment stack. No composer, no bottom controls, no plus marker in
 * Phase 2; a future phase introduces the composer.
 */
@Composable
fun AllTimelineScreen(momentRepository: MomentRepository) {
    val scope = rememberCoroutineScope()
    val viewModel: AllTimelineViewModel = remember(momentRepository, scope) {
        AllTimelineViewModel(momentRepository, scope)
    }
    val uiState by viewModel.state.collectAsState()

    AllTimelineContent(
        uiState = uiState,
        onToggleFavorite = viewModel::setFavorite,
        onMenuClick = { /* Settings surface arrives in Phase 8 */ },
        onSearchClick = { /* Search surface arrives in Phase 7 */ },
    )
}

@Composable
fun AllTimelineContent(
    uiState: AllTimelineUiState,
    onToggleFavorite: (com.vaibhav.relive.domain.model.MomentId, Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
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
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            when (uiState) {
                AllTimelineUiState.Loading -> Unit
                AllTimelineUiState.Empty -> EmptyTimelinePlaceholder()
                is AllTimelineUiState.Loaded -> {
                    // Continuous rail: a hairline running down the gutter, behind
                    // the moment stack. Individual dots are drawn by MomentCard.
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            bottom = dims.spacing.huge,
                        ),
                    ) {
                        items(
                            items = uiState.moments,
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
                    }
                }
            }
        }
    }
}
