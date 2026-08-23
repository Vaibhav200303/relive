package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.repository.RediscoverRepository
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.ui.components.navigation.ReliveWordmarkAppBar
import com.vaibhav.relive.ui.components.rediscover.FavoriteMomentCard
import com.vaibhav.relive.ui.theme.ReliveTheme

/** The active root collects only the bounded Favorites shelf. */
@Composable
fun RediscoverScreen(
    repository: RediscoverRepository,
    mediaStore: MediaStore,
    listState: LazyListState,
    onOpenFavorites: (MomentId?) -> Unit,
    debugControls: (@Composable () -> Unit)? = null,
) {
    val previews by repository.observeFavoritePreviews().collectAsState(emptyList())
    var debugOpen by remember { mutableStateOf(false) }
    val dims = ReliveTheme.dimensions

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas),
        contentPadding = PaddingValues(bottom = dims.spacing.huge),
    ) {
        item(key = "relive-app-bar") { ReliveWordmarkAppBar() }
        item(key = "favorites-heading") {
            Text(
                text = "FAVOURITES",
                style = ReliveTheme.typography.title,
                color = ReliveTheme.colors.accentMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = {}, onLongClick = debugControls?.let { { debugOpen = true } })
                    .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md),
            )
        }
        if (previews.isEmpty()) {
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
                                modifier = Modifier.width(maxWidth * dims.rediscover.favoriteShelfCardWidthFraction),
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
                        Text(
                            text = "Show all",
                            style = ReliveTheme.typography.action,
                            color = ReliveTheme.colors.accent,
                        )
                    }
                }
            }
        }
    }
    if (debugOpen) {
        AlertDialog(onDismissRequest = { debugOpen = false }, confirmButton = {}, text = { debugControls?.invoke() })
    }
}

@Composable
private fun FavoritesEmptyState() {
    val dims = ReliveTheme.dimensions
    Text(
        text = "No favorite moments yet.\nMoments you favorite will appear here.",
        style = ReliveTheme.typography.subtitle,
        color = ReliveTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md),
    )
}
