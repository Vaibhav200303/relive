package com.vaibhav.relive.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** UI-only classification for the active inline editor's boundary. */
internal fun isTapOutsideEditor(editorBounds: Rect?, tapPosition: Offset): Boolean =
    editorBounds != null && !editorBounds.contains(tapPosition)
