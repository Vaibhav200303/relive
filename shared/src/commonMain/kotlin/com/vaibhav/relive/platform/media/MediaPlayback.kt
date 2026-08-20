package com.vaibhav.relive.platform.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaibhav.relive.domain.model.MediaStorageRef

/**
 * Platform playback surfaces. Both are Composables so no platform player
 * object crosses into shared code — implementations render a native surface
 * and manage lifecycle themselves.
 */
@Composable
expect fun RelivedImage(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier)

@Composable
expect fun RelivedVideo(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier)

@Composable
expect fun RelivedAudio(ref: MediaStorageRef, mediaStore: MediaStore, modifier: Modifier)
