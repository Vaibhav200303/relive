package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import kotlin.random.Random

/**
 * Film-grain texture for screen canvases. A memory app benefits from a faint paper/photographic
 * grain over its flat canvas — it reads as age and warmth rather than a sterile fill. The grain
 * is a small tile of randomly placed dark flecks, generated once and repeated across the surface
 * by an [ImageShader], so it costs a single small bitmap regardless of screen size.
 *
 * Apply it as a very-low-alpha background layer painted over the canvas gradient and beneath
 * content (see the Timeline Home root), never over photos.
 */
private fun buildGrainBitmap(size: Int, seed: Int, fleck: Color): ImageBitmap {
    val bitmap = ImageBitmap(size, size)
    val canvas = Canvas(bitmap)
    val rng = Random(seed)
    val paint = Paint()
    for (y in 0 until size) {
        for (x in 0 until size) {
            val v = rng.nextFloat()
            // Only the upper half of the range becomes a fleck, so roughly half the tile stays
            // clear; fleck opacity scales with the draw so the grain is uneven, like real film.
            if (v <= 0.5f) continue
            paint.color = fleck.copy(alpha = (v - 0.5f) * 1.2f)
            canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
        }
    }
    return bitmap
}

/**
 * A repeating grain [ShaderBrush] built once and remembered for the composition. Paint it with a
 * low `alpha` (about 0.04–0.07) via `Modifier.background(brush, alpha = …)` so it sits just at the
 * edge of perception.
 *
 * The fleck color is themed by [isDark]: dark specks over a light canvas, light specks over a dark
 * canvas. Black flecks are invisible on a near-black ground, so a single fixed color only shows in
 * one mode — the grain must invert with the surface it sits on.
 */
@Composable
fun rememberGrainBrush(isDark: Boolean, tile: Int = 120): ShaderBrush = remember(isDark, tile) {
    val fleck = if (isDark) Color.White else Color.Black
    ShaderBrush(
        ImageShader(
            buildGrainBitmap(tile, seed = 0x5EED, fleck = fleck),
            TileMode.Repeated,
            TileMode.Repeated,
        ),
    )
}
