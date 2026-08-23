package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.domain.model.FavoritesCollectionSummary
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.ui.components.navigation.ReliveWordmarkAppBar
import com.vaibhav.relive.ui.components.rediscover.FavoritesSystemCollectionCard
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * The active Rediscover root intentionally collects only the Favorites system
 * collection. The deferred calendar, past, place, and tag projections remain
 * available through [RediscoverRepository] without doing work for this screen.
 */
@Composable
fun RediscoverScreen(
    repository: RediscoverRepository,
    mediaStore: MediaStore,
    listState: LazyListState,
    onOpenFavorites: () -> Unit,
    debugControls: (@Composable () -> Unit)? = null,
) {
    val summary by repository.observeFavoritesSummary().collectAsState(
        FavoritesCollectionSummary(momentCount = 0, previewAttachments = emptyList()),
    )
    var debugOpen by remember { mutableStateOf(false) }
    val dims = ReliveTheme.dimensions

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(ReliveTheme.colors.bgCanvas),
        contentPadding = PaddingValues(bottom = dims.spacing.huge),
    ) {
        item(key = "relive-app-bar") { ReliveWordmarkAppBar() }
        item(key = "favorites-system-collection") {
            FavoritesSystemCollectionCard(
                summary = summary,
                mediaStore = mediaStore,
                onOpen = onOpenFavorites,
                onDebugLongPress = debugControls?.let { { debugOpen = true } },
                modifier = Modifier.padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md),
            )
        }
    }
    if (debugOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { debugOpen = false },
            confirmButton = {},
            text = { debugControls?.invoke() },
        )
    }
}
