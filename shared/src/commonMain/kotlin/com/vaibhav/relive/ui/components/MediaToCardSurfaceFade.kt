package com.vaibhav.relive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.vaibhav.relive.ui.theme.ReliveTheme

/** Fades media into the enclosing Relive card surface. */
@Composable
fun MediaToCardSurfaceFade(modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dims.timelineHome.mediaFadeHeight)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, ReliveTheme.colors.surfaceCard),
                ),
            ),
    )
}
