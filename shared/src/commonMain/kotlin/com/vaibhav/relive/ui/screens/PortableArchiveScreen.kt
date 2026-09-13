package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.exporting.OpenedPortableArchive
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.timeline.toPresentation
import com.vaibhav.relive.presentation.date.EditorialDateFormatter
import com.vaibhav.relive.presentation.viewer.TimelineMediaNavState
import com.vaibhav.relive.presentation.viewer.closeGallery
import com.vaibhav.relive.presentation.viewer.closeViewer
import com.vaibhav.relive.presentation.viewer.openFromCollage
import com.vaibhav.relive.presentation.viewer.openFromGallery
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.components.timeline.TimelineCoverHero
import com.vaibhav.relive.ui.components.timeline.TimelineWallpaperSurface
import com.vaibhav.relive.ui.components.viewer.MediaViewer
import com.vaibhav.relive.ui.components.viewer.MomentMediaGallery
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush

@Composable
fun PortableArchiveScreen(archive: OpenedPortableArchive, onClose: () -> Unit) {
    val snapshot = archive.session.snapshot
    var showingTimeline by remember(archive) { mutableStateOf(false) }
    var nav by remember(archive) { mutableStateOf(TimelineMediaNavState.Idle) }
    val identity = snapshot.exportedTimeline
    val moments = remember(snapshot) {
        snapshot.moments
            .sortedByDescending { it.createdAt.epochMilliseconds }
            .map { it.toPresentation() }
    }
    val closeLayer = {
        when {
            nav.viewer != null -> nav = nav.closeViewer()
            nav.gallery != null -> nav = nav.closeGallery()
            showingTimeline -> showingTimeline = false
            else -> onClose()
        }
    }
    ReliveBackHandler(enabled = true, onBack = closeLayer)

    TimelineWallpaperSurface(identity.appearance.wallpaper, Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            if (!showingTimeline) {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        TimelineCoverHero(
                            name = identity.name,
                            coverPhotoRef = identity.coverPhotoRef,
                            mediaStore = archive.mediaStore,
                            onBack = onClose,
                        )
                    }
                    item {
                        Column(Modifier.padding(ReliveTheme.dimensions.spacing.xl), verticalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.sm)) {
                        Text("A read-only memory library", style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
                        Text("Exported ${EditorialDateFormatter.format(Instant(archive.session.summary.exportedAtEpochMilliseconds))}", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textSecondary)
                        val first = archive.session.summary.earliestMomentEpochMilliseconds
                        val last = archive.session.summary.latestMomentEpochMilliseconds
                        if (first != null && last != null) {
                            Text("${EditorialDateFormatter.format(Instant(first))} — ${EditorialDateFormatter.format(Instant(last))}", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textSecondary)
                        }
                        Text("${archive.session.summary.momentCount} ${if (archive.session.summary.momentCount == 1) "Moment" else "Moments"}", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textSecondary)
                        Text("Nothing here can change your live Relive archive.", style = ReliveTheme.typography.tag, color = ReliveTheme.colors.textSecondary)
                        Button(onClick = { showingTimeline = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("View ${identity.name}")
                        }
                    }
                }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    item { ProfilePageHeader(identity.name, closeLayer) }
                    itemsIndexed(moments, key = { _, item -> item.id.value }) { index, moment ->
                        MomentCard(
                            moment = moment,
                            mediaStore = archive.mediaStore,
                            onToggleFavorite = null,
                            onOpenMedia = { attachments, tapped -> nav = nav.openFromCollage(attachments, tapped) },
                            canEditOrForget = false,
                            onEdit = {},
                            onForget = {},
                            hasPreviousMoment = index > 0,
                            hasNextMoment = index < moments.lastIndex,
                            modifier = Modifier.padding(horizontal = ReliveTheme.dimensions.spacing.lg),
                        )
                    }
                }
            }
            nav.gallery?.let { gallery -> MomentMediaGallery(gallery, archive.mediaStore, { nav = nav.openFromGallery(it) }, { nav = nav.closeGallery() }) }
            nav.viewer?.let { viewer -> MediaViewer(viewer, archive.mediaStore, {}, { nav = nav.closeViewer() }) }
        }
    }
}

@Composable
fun PortableArchiveErrorScreen(message: String, onClose: () -> Unit) {
    ReliveBackHandler(enabled = true, onBack = onClose)
    Column(
        Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush()).padding(ReliveTheme.dimensions.spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Archive unavailable", style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
        Text(message, style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary, modifier = Modifier.padding(vertical = ReliveTheme.dimensions.spacing.lg))
        Button(onClick = onClose) { Text("Return to Relive") }
    }
}
