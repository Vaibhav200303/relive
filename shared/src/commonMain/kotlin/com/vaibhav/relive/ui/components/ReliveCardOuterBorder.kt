package com.vaibhav.relive.ui.components

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.vaibhav.relive.ui.theme.ReliveTheme

/** Draws Relive's subtle outer definition; the card surface supplies the visual frame. */
@Composable
fun Modifier.reliveCardOuterBorder(
    shape: Shape,
): Modifier {
    val colors = ReliveTheme.colors
    val strokes = ReliveTheme.dimensions.stroke
    return this.border(strokes.cardOuter, colors.border, shape)
}
