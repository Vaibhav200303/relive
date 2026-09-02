package com.vaibhav.relive.ui.components.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.platform.media.CameraCaptureSurface
import com.vaibhav.relive.platform.media.MediaPickerHandle
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.RawMedia
import com.vaibhav.relive.presentation.composer.ComposerOverlay
import com.vaibhav.relive.presentation.composer.PendingMediaAction
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.components.ReliveBottomSheet

/**
 * Presents the composer's active overlay — camera surface or library
 * choice sheet — and delivers results back to the view-model. Consumed as a
 * sibling of the timeline `LazyColumn` in the timeline screen.
 */
@Composable
internal fun ComposerOverlayHost(
    overlay: ComposerOverlay,
    mediaStore: MediaStore,
    onCaptured: (RawMedia) -> Unit,
    onDismiss: () -> Unit,
    onPick: (MediaType) -> Unit,
    onOpenLibraryFromCamera: () -> Unit,
) {
    when (overlay) {
        ComposerOverlay.None -> Unit
        ComposerOverlay.Camera -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                CameraCaptureSurface(
                    mediaStore = mediaStore,
                    onCaptured = { raw ->
                        onCaptured(raw)
                        onDismiss()
                    },
                    onCancel = onDismiss,
                    // In-camera Gallery hands off to the composer's Library
                    // sheet. openLibraryChoice() replaces the Camera overlay
                    // atomically so the user never sees a bare timeline flash.
                    onOpenGallery = onOpenLibraryFromCamera,
                )
            }
        }
        ComposerOverlay.LibraryChoice -> Unit
    }
    LibraryChoiceSheet(
        visible = overlay == ComposerOverlay.LibraryChoice,
        onPickImage = { onPick(MediaType.Image) },
        onPickVideo = { onPick(MediaType.Video) },
        onPickAudio = { onPick(MediaType.Audio) },
        onDismiss = onDismiss,
    )
}

/**
 * Drives pending picker actions: whenever the VM sets a [PendingMediaAction],
 * the handle is invoked once and the result forwarded, then the action is
 * cleared. Empty results (user cancellation) simply clear.
 */
@Composable
internal fun MediaPickerDriver(
    pending: PendingMediaAction?,
    handle: MediaPickerHandle,
    onResult: (List<RawMedia>) -> Unit,
    onClear: () -> Unit,
) {
    LaunchedEffect(pending) {
        val action = pending ?: return@LaunchedEffect
        val result = when (action) {
            PendingMediaAction.PickImage -> handle.pickImage()
            PendingMediaAction.PickVideo -> handle.pickVideo()
            PendingMediaAction.PickAudio -> handle.pickAudio()
        }
        onResult(result)
        onClear()
    }
}

@Composable
private fun LibraryChoiceSheet(
    visible: Boolean,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onPickAudio: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    ReliveBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dims.spacing.lg, end = dims.spacing.lg, bottom = dims.spacing.xl)
                .semantics { contentDescription = "Choose media source" },
            verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
        ) {
            Text("Choose from library", style = type.title, color = colors.textPrimary)
            Spacer(Modifier.height(dims.spacing.xs))
            LibraryOption("Photo", onPickImage)
            LibraryOption("Video", onPickVideo)
            LibraryOption("Audio", onPickAudio)
        }
    }
}

@Composable
private fun LibraryOption(label: String, onClick: () -> Unit) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    TextButton(
        onClick = {
            haptics.perform(ReliveHapticCue.Action)
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.minTouchTarget),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = type.action, color = colors.textPrimary)
        }
    }
}

