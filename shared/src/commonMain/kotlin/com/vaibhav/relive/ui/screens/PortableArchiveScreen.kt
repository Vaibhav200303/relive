package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.vaibhav.relive.ui.icons.ProfileIcons
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
                Column(Modifier.fillMaxSize()) {
                    TimelineCoverHero(
                        name = identity.name,
                        subtitle = "A collection of moments, big and small.",
                        coverPhotoRef = identity.coverPhotoRef,
                        mediaStore = archive.mediaStore,
                        onBack = onClose,
                    )
                    PortableArchiveWelcomeCard(
                        archive = archive,
                        timelineName = identity.name,
                        onViewTimeline = { showingTimeline = true },
                        modifier = Modifier.weight(1f),
                    )
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
private fun PortableArchiveWelcomeCard(
    archive: OpenedPortableArchive,
    timelineName: String,
    onViewTimeline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val cardInk = Color(0xFF352A1E)
    val cardInkSecondary = Color(0xFF74695F)
    val cardTint = Color(0xFFFFF8E8)
    val summary = archive.session.summary
    val first = summary.earliestMomentEpochMilliseconds
    val last = summary.latestMomentEpochMilliseconds
    val exported = EditorialDateFormatter.format(Instant(summary.exportedAtEpochMilliseconds))
    val range = if (first != null && last != null) {
        "${EditorialDateFormatter.format(Instant(first))} —\n${EditorialDateFormatter.format(Instant(last))}"
    } else {
        "No dated moments"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.md)
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(dims.spacing.xl))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dims.radii.xl),
            color = Color.White,
            shadowElevation = 0.dp,
            border = BorderStroke(dims.stroke.hairline, Color(0xFFE9E3DA)),
        ) {
            Column(Modifier.padding(dims.spacing.lg), verticalArrangement = Arrangement.spacedBy(dims.spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArchiveIconBubble(ProfileIcons.Archive, colors.accent, cardTint, 44.dp)
                    Spacer(Modifier.width(dims.spacing.md))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
                        Text(
                            "A read-only memory library",
                            style = ReliveTheme.typography.body,
                            color = cardInk,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "A snapshot of your Relive moments, exactly as they were on this date.",
                            style = ReliveTheme.typography.tag,
                            color = cardInkSecondary,
                        )
                    }
                    Text("✦", color = colors.spark, style = ReliveTheme.typography.subtitle)
                }

                HorizontalDivider(color = Color(0xFFE9E3DA))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm)) {
                    ArchiveFact(ProfileIcons.Calendar, "Exported", exported, colors.accent, cardTint, cardInk, cardInkSecondary, Modifier.weight(1f))
                    ArchiveFact(ProfileIcons.Calendar, "Time range", range, colors.spark, cardTint, cardInk, cardInkSecondary, Modifier.weight(1f))
                    ArchiveFact(
                        ProfileIcons.Media,
                        "Total moments",
                        "${summary.momentCount} ${if (summary.momentCount == 1) "Moment" else "Moments"}",
                        colors.accent,
                        cardTint,
                        cardInk,
                        cardInkSecondary,
                        Modifier.weight(1f),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(dims.radii.large),
                    color = cardTint,
                    border = BorderStroke(dims.stroke.hairline, Color(0xFFE9E3DA)),
                ) {
                    Row(Modifier.padding(dims.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        ArchiveIconBubble(ProfileIcons.Lock, colors.accent, Color.White, 44.dp)
                        Spacer(Modifier.width(dims.spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Nothing here can change your live Relive archive.",
                                style = ReliveTheme.typography.tag,
                                color = cardInk,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "This is just a read-only export of your moments.",
                                style = ReliveTheme.typography.tag,
                                color = cardInkSecondary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(dims.spacing.lg))
        Button(
            onClick = onViewTimeline,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.textOnAccent,
            ),
        ) {
            Text("View $timelineName  →", style = ReliveTheme.typography.prominentAction, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(dims.spacing.md))
    }
}

@Composable
private fun ArchiveIconBubble(
    icon: ImageVector,
    tint: Color,
    background: Color,
    size: androidx.compose.ui.unit.Dp = 40.dp,
) {
    Surface(shape = CircleShape, color = background, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ArchiveFact(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    iconBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ArchiveIconBubble(icon, iconTint, iconBackground)
        Text(label, style = ReliveTheme.typography.tag, color = textSecondary, textAlign = TextAlign.Center)
        Text(
            value,
            style = ReliveTheme.typography.tag,
            color = textPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
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
