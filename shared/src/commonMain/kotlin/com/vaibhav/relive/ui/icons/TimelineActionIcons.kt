package com.vaibhav.relive.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Inlined Material Symbols vectors keep the selection app bar lightweight. */
object TimelineActionIcons {
    val AddToTimeline: ImageVector by lazy {
        ImageVector.Builder("AddToTimeline", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 5f); verticalLineTo(19f); horizontalLineTo(13f); verticalLineTo(17f); horizontalLineTo(5f); verticalLineTo(7f); horizontalLineTo(13f); verticalLineTo(5f); close()
                moveTo(19f, 11f); verticalLineTo(15f); horizontalLineTo(23f); verticalLineTo(17f); horizontalLineTo(19f); verticalLineTo(21f); horizontalLineTo(17f); verticalLineTo(17f); horizontalLineTo(13f); verticalLineTo(15f); horizontalLineTo(17f); verticalLineTo(11f); close()
            }
        }.build()
    }

    val Rename: ImageVector by lazy {
        ImageVector.Builder("Edit", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 17.25f); verticalLineTo(21f); horizontalLineTo(6.75f); lineTo(17.81f, 9.94f); lineTo(14.06f, 6.19f); close()
                moveTo(20.71f, 7.04f); curveTo(21.1f, 6.65f, 21.1f, 6.01f, 20.71f, 5.62f); lineTo(18.38f, 3.29f); curveTo(17.99f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f); lineTo(15.13f, 5.12f); lineTo(18.88f, 8.87f); close()
            }
        }.build()
    }

    val Delete: ImageVector by lazy {
        ImageVector.Builder("Delete", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 19f); curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f); horizontalLineTo(16f); curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f); verticalLineTo(7f); horizontalLineTo(6f); verticalLineTo(19f)
                moveTo(8f, 9f); horizontalLineTo(16f); verticalLineTo(19f); horizontalLineTo(8f); close()
                moveTo(15.5f, 4f); lineTo(14.5f, 3f); horizontalLineTo(9.5f); lineTo(8.5f, 4f); horizontalLineTo(5f); verticalLineTo(6f); horizontalLineTo(19f); verticalLineTo(4f); close()
            }
        }.build()
    }
}
