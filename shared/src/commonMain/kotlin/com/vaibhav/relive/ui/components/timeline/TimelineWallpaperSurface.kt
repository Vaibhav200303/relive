package com.vaibhav.relive.ui.components.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.TimelineWallpaperPalette
import com.vaibhav.relive.ui.theme.timelineWallpaperPalette
import kotlin.math.min
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import relive.shared.generated.resources.Res
import relive.shared.generated.resources.timeline_wallpaper_blush_pink
import relive.shared.generated.resources.timeline_wallpaper_lavender
import relive.shared.generated.resources.timeline_wallpaper_powder_blue
import relive.shared.generated.resources.timeline_wallpaper_sage_green
import relive.shared.generated.resources.timeline_wallpaper_soft_peach
import relive.shared.generated.resources.timeline_wallpaper_warm_cream

@Immutable
data class TimelineWallpaperVisual(
    val wallpaper: TimelineWallpaper,
    val palette: TimelineWallpaperPalette,
    val pattern: List<MemoryDoodlePlacement> = MemoryDoodlePattern,
)

fun timelineWallpaperVisual(wallpaper: TimelineWallpaper, isDark: Boolean): TimelineWallpaperVisual =
    TimelineWallpaperVisual(wallpaper, timelineWallpaperPalette(wallpaper, isDark))

private val DefaultTimelineWallpaperPalette = timelineWallpaperPalette(TimelineWallpaper.WarmCream, isDark = false)

/** Scoped only to timeline-owned surfaces; it never changes app theme tokens. */
val LocalTimelineWallpaperPalette = staticCompositionLocalOf { DefaultTimelineWallpaperPalette }

@Composable
fun TimelineWallpaperSurface(
    wallpaper: TimelineWallpaper,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    TimelineWallpaperSurface(
        visual = timelineWallpaperVisual(wallpaper, ReliveTheme.isDark),
        modifier = modifier,
        content = content,
    )
}

@Composable
fun TimelineWallpaperSurface(
    visual: TimelineWallpaperVisual,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalTimelineWallpaperPalette provides visual.palette) {
        Box(
            modifier = modifier,
        ) {
            Image(
                painter = painterResource(timelineWallpaperResource(visual.wallpaper)),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            content()
        }
    }
}

private fun timelineWallpaperResource(wallpaper: TimelineWallpaper): DrawableResource = when (wallpaper) {
    TimelineWallpaper.WarmCream -> Res.drawable.timeline_wallpaper_warm_cream
    TimelineWallpaper.BlushPink -> Res.drawable.timeline_wallpaper_blush_pink
    TimelineWallpaper.SageGreen -> Res.drawable.timeline_wallpaper_sage_green
    TimelineWallpaper.Lavender -> Res.drawable.timeline_wallpaper_lavender
    TimelineWallpaper.PowderBlue -> Res.drawable.timeline_wallpaper_powder_blue
    TimelineWallpaper.SoftPeach -> Res.drawable.timeline_wallpaper_soft_peach
}

enum class MemoryDoodleGlyph {
    Camera,
    Photo,
    Heart,
    HeartBurst,
    Star,
    Starburst,
    Sparkle,
    Flower,
    Sprig,
    Pin,
    DashedTrail,
    Calendar,
    Cloud,
    SunCloud,
    Arrow,
    Swirl,
    Dot,
    Dots,
}

@Immutable
data class MemoryDoodlePlacement(
    val glyph: MemoryDoodleGlyph,
    val x: Float,
    val y: Float,
    val size: Float,
    val rotationDegrees: Float = 0f,
)

/** One immutable, normalized layout shared by every timeline wallpaper palette. */
val MemoryDoodlePattern: List<MemoryDoodlePlacement> = listOf(
    MemoryDoodlePlacement(MemoryDoodleGlyph.Star, 0.272f, 0.048f, 0.068f, -8f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Swirl, 0.692f, 0.034f, 0.115f, 11f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dot, 0.513f, 0.057f, 0.016f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Starburst, 0.607f, 0.082f, 0.082f, -8f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.870f, 0.076f, 0.046f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Flower, 0.110f, 0.132f, 0.128f, -9f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Cloud, 0.373f, 0.167f, 0.123f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Camera, 0.748f, 0.160f, 0.142f, 11f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Heart, 0.934f, 0.142f, 0.068f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dot, 0.362f, 0.100f, 0.021f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.141f, 0.258f, 0.050f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dot, 0.273f, 0.239f, 0.016f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.473f, 0.231f, 0.035f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dots, 0.853f, 0.245f, 0.040f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Pin, 0.121f, 0.332f, 0.126f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.DashedTrail, 0.131f, 0.360f, 0.150f, 3f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Arrow, 0.480f, 0.322f, 0.120f, -11f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Calendar, 0.759f, 0.350f, 0.142f, 8f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Star, 0.557f, 0.400f, 0.077f, 12f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dots, 0.936f, 0.360f, 0.042f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.331f, 0.425f, 0.048f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dot, 0.097f, 0.451f, 0.018f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Heart, 0.160f, 0.521f, 0.072f, -6f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dots, 0.663f, 0.486f, 0.048f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Flower, 0.858f, 0.490f, 0.132f, 8f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dot, 0.371f, 0.504f, 0.015f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.272f, 0.588f, 0.042f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.SunCloud, 0.112f, 0.638f, 0.125f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.HeartBurst, 0.399f, 0.684f, 0.085f, -12f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dot, 0.651f, 0.713f, 0.016f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.855f, 0.696f, 0.053f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.893f, 0.722f, 0.048f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dot, 0.078f, 0.724f, 0.020f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dots, 0.234f, 0.750f, 0.035f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sprig, 0.155f, 0.823f, 0.095f, -8f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dots, 0.333f, 0.858f, 0.037f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.479f, 0.803f, 0.035f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Swirl, 0.657f, 0.791f, 0.135f, -14f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Photo, 0.780f, 0.900f, 0.130f, 12f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.912f, 0.830f, 0.040f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Swirl, 0.186f, 0.930f, 0.075f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Dot, 0.393f, 0.915f, 0.017f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Sparkle, 0.568f, 0.920f, 0.045f),
    MemoryDoodlePlacement(MemoryDoodleGlyph.Heart, 0.905f, 0.945f, 0.054f),
)

private fun DrawScope.drawMemoryDoodle(
    glyph: MemoryDoodleGlyph,
    center: Offset,
    sizePx: Float,
    rotationDegrees: Float,
    color: Color,
    strokeWidth: Float,
) {
    translate(center.x, center.y) {
        rotate(rotationDegrees) {
            scale(sizePx, sizePx) {
                val stroke = Stroke(width = strokeWidth / sizePx)
                when (glyph) {
                    MemoryDoodleGlyph.Camera -> drawCameraDoodle(color, stroke)
                    MemoryDoodleGlyph.Photo -> drawPhotoDoodle(color, stroke)
                    MemoryDoodleGlyph.Heart -> drawHeartDoodle(color, stroke)
                    MemoryDoodleGlyph.HeartBurst -> drawHeartBurstDoodle(color, stroke)
                    MemoryDoodleGlyph.Star -> drawStarDoodle(color, stroke)
                    MemoryDoodleGlyph.Starburst -> drawStarburstDoodle(color, stroke)
                    MemoryDoodleGlyph.Sparkle -> drawSparkleDoodle(color, stroke)
                    MemoryDoodleGlyph.Flower -> drawFlowerDoodle(color, stroke)
                    MemoryDoodleGlyph.Sprig -> drawSprigDoodle(color, stroke)
                    MemoryDoodleGlyph.Pin -> drawPinDoodle(color, stroke)
                    MemoryDoodleGlyph.DashedTrail -> drawDashedTrailDoodle(color, stroke)
                    MemoryDoodleGlyph.Calendar -> drawCalendarDoodle(color, stroke)
                    MemoryDoodleGlyph.Cloud -> drawCloudDoodle(color, stroke)
                    MemoryDoodleGlyph.SunCloud -> drawSunCloudDoodle(color, stroke)
                    MemoryDoodleGlyph.Arrow -> drawArrowDoodle(color, stroke)
                    MemoryDoodleGlyph.Swirl -> drawSwirlDoodle(color, stroke)
                    MemoryDoodleGlyph.Dot -> drawCircle(color, 0.11f)
                    MemoryDoodleGlyph.Dots -> drawDotsDoodle(color)
                }
            }
        }
    }
}

private fun DrawScope.drawCameraDoodle(color: Color, stroke: Stroke) {
    drawRoundRect(color, Offset(-0.5f, -0.3f), Size(1f, 0.62f), CornerRadius(0.09f, 0.09f), style = stroke)
    drawCircle(color, 0.2f, style = stroke)
    drawRoundRect(color, Offset(-0.25f, -0.43f), Size(0.28f, 0.13f), CornerRadius(0.03f, 0.03f), style = stroke)
}

private fun DrawScope.drawPhotoDoodle(color: Color, stroke: Stroke) {
    drawRect(color, Offset(-0.43f, -0.36f), Size(0.86f, 0.72f), style = stroke)
    drawCircle(color, 0.08f, Offset(0.2f, -0.14f))
    drawLine(color, Offset(-0.33f, 0.24f), Offset(-0.05f, -0.05f), stroke.width)
    drawLine(color, Offset(-0.05f, -0.05f), Offset(0.11f, 0.12f), stroke.width)
    drawLine(color, Offset(0.11f, 0.12f), Offset(0.27f, -0.08f), stroke.width)
    drawLine(color, Offset(0.27f, -0.08f), Offset(0.37f, 0.24f), stroke.width)
}

private fun DrawScope.drawHeartDoodle(color: Color, stroke: Stroke) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, 0.42f)
        cubicTo(-0.7f, 0.02f, -0.45f, -0.48f, 0f, -0.15f)
        cubicTo(0.45f, -0.48f, 0.7f, 0.02f, 0f, 0.42f)
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawHeartBurstDoodle(color: Color, stroke: Stroke) {
    drawHeartDoodle(color, stroke)
    drawLine(color, Offset(-0.48f, -0.10f), Offset(-0.64f, -0.20f), stroke.width)
    drawLine(color, Offset(-0.37f, -0.31f), Offset(-0.45f, -0.49f), stroke.width)
    drawLine(color, Offset(0.45f, -0.04f), Offset(0.63f, -0.08f), stroke.width)
    drawLine(color, Offset(0.40f, 0.15f), Offset(0.58f, 0.24f), stroke.width)
}

private fun DrawScope.drawStarDoodle(color: Color, stroke: Stroke) {
    val points = List(10) { index ->
        val angle = -kotlin.math.PI / 2 + index * kotlin.math.PI / 5
        val radius = if (index % 2 == 0) 0.48f else 0.2f
        Offset((kotlin.math.cos(angle) * radius).toFloat(), (kotlin.math.sin(angle) * radius).toFloat())
    }
    drawLinePath(points, color, stroke, close = true)
}

private fun DrawScope.drawStarburstDoodle(color: Color, stroke: Stroke) {
    drawStarDoodle(color, stroke)
    drawLine(color, Offset(0f, -0.68f), Offset(0f, -0.48f), stroke.width)
    drawLine(color, Offset(0.48f, -0.48f), Offset(0.34f, -0.34f), stroke.width)
    drawLine(color, Offset(0.68f, 0f), Offset(0.48f, 0f), stroke.width)
    drawLine(color, Offset(0.48f, 0.48f), Offset(0.34f, 0.34f), stroke.width)
    drawLine(color, Offset(0f, 0.68f), Offset(0f, 0.48f), stroke.width)
    drawLine(color, Offset(-0.48f, 0.48f), Offset(-0.34f, 0.34f), stroke.width)
    drawLine(color, Offset(-0.68f, 0f), Offset(-0.48f, 0f), stroke.width)
}

private fun DrawScope.drawSparkleDoodle(color: Color, stroke: Stroke) {
    drawLine(color, Offset(-0.48f, 0f), Offset(0.48f, 0f), stroke.width)
    drawLine(color, Offset(0f, -0.48f), Offset(0f, 0.48f), stroke.width)
    drawLine(color, Offset(-0.2f, -0.2f), Offset(0.2f, 0.2f), stroke.width * 0.7f)
    drawLine(color, Offset(-0.2f, 0.2f), Offset(0.2f, -0.2f), stroke.width * 0.7f)
}

private fun DrawScope.drawFlowerDoodle(color: Color, stroke: Stroke) {
    listOf(Offset(0f, -0.25f), Offset(0.24f, 0f), Offset(0f, 0.25f), Offset(-0.24f, 0f)).forEach { offset ->
        drawCircle(color, 0.22f, offset, style = stroke)
    }
    drawCircle(color, 0.08f, style = stroke)
    drawLine(color, Offset(0f, 0.3f), Offset(-0.08f, 0.62f), stroke.width)
    drawLine(color, Offset(-0.08f, 0.48f), Offset(-0.29f, 0.36f), stroke.width)
}

private fun DrawScope.drawSprigDoodle(color: Color, stroke: Stroke) {
    drawLine(color, Offset(-0.07f, 0.55f), Offset(0.02f, -0.54f), stroke.width)
    listOf(
        Offset(-0.25f, 0.28f) to Offset(-0.06f, 0.16f),
        Offset(0.23f, 0.11f) to Offset(0.03f, 0.00f),
        Offset(-0.22f, -0.12f) to Offset(0f, -0.20f),
        Offset(0.20f, -0.34f) to Offset(0.01f, -0.39f),
    ).forEach { (leafCenter, stemEnd) ->
        drawLine(color, stemEnd, leafCenter, stroke.width)
        drawOval(color, Offset(leafCenter.x - 0.10f, leafCenter.y - 0.06f), Size(0.20f, 0.12f), style = stroke)
    }
}

private fun DrawScope.drawPinDoodle(color: Color, stroke: Stroke) {
    drawCircle(color, 0.28f, Offset(0f, -0.15f), style = stroke)
    drawLine(color, Offset(-0.2f, 0.05f), Offset(0f, 0.52f), stroke.width)
    drawLine(color, Offset(0.2f, 0.05f), Offset(0f, 0.52f), stroke.width)
    drawCircle(color, 0.08f, Offset(0f, -0.15f), style = stroke)
}

private fun DrawScope.drawDashedTrailDoodle(color: Color, stroke: Stroke) {
    val points = listOf(
        Offset(-0.52f, -0.08f),
        Offset(-0.24f, -0.11f),
        Offset(0.04f, -0.01f),
        Offset(0.24f, 0.15f),
        Offset(0.19f, 0.35f),
        Offset(0.01f, 0.43f),
    )
    points.zipWithNext().forEachIndexed { index, (start, end) ->
        if (index % 2 == 0) drawLine(color, start, end, stroke.width)
    }
}

private fun DrawScope.drawCalendarDoodle(color: Color, stroke: Stroke) {
    drawRoundRect(color, Offset(-0.4f, -0.34f), Size(0.8f, 0.76f), CornerRadius(0.05f, 0.05f), style = stroke)
    drawLine(color, Offset(-0.4f, -0.08f), Offset(0.4f, -0.08f), stroke.width)
    drawLine(color, Offset(-0.22f, -0.49f), Offset(-0.22f, -0.22f), stroke.width)
    drawLine(color, Offset(0.22f, -0.49f), Offset(0.22f, -0.22f), stroke.width)
    listOf(-0.2f, 0f, 0.2f).forEach { x -> listOf(0.1f, 0.26f).forEach { y -> drawRect(color, Offset(x - 0.045f, y - 0.045f), Size(0.09f, 0.09f), style = stroke) } }
}

private fun DrawScope.drawCloudDoodle(color: Color, stroke: Stroke) {
    drawCircle(color, 0.22f, Offset(-0.22f, 0f), style = stroke)
    drawCircle(color, 0.29f, Offset(0f, -0.09f), style = stroke)
    drawCircle(color, 0.2f, Offset(0.25f, 0.02f), style = stroke)
    drawLine(color, Offset(-0.43f, 0.18f), Offset(0.43f, 0.18f), stroke.width)
}

private fun DrawScope.drawSunCloudDoodle(color: Color, stroke: Stroke) {
    drawCloudDoodle(color, stroke)
    drawCircle(color, 0.22f, Offset(-0.33f, -0.24f), style = stroke)
    drawLine(color, Offset(-0.33f, -0.58f), Offset(-0.33f, -0.46f), stroke.width)
    drawLine(color, Offset(-0.57f, -0.40f), Offset(-0.47f, -0.32f), stroke.width)
    drawLine(color, Offset(-0.10f, -0.40f), Offset(-0.19f, -0.32f), stroke.width)
}

private fun DrawScope.drawArrowDoodle(color: Color, stroke: Stroke) {
    drawLine(color, Offset(-0.47f, 0.26f), Offset(0.34f, -0.14f), stroke.width)
    drawLine(color, Offset(0.34f, -0.14f), Offset(0.1f, -0.17f), stroke.width)
    drawLine(color, Offset(0.34f, -0.14f), Offset(0.22f, 0.08f), stroke.width)
}

private fun DrawScope.drawSwirlDoodle(color: Color, stroke: Stroke) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(-0.45f, 0.14f)
        cubicTo(-0.08f, -0.4f, 0.34f, -0.3f, 0.12f, 0.02f)
        cubicTo(-0.12f, 0.34f, -0.36f, 0.1f, -0.12f, -0.08f)
        cubicTo(0.14f, -0.28f, 0.39f, -0.06f, 0.45f, 0.1f)
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawDotsDoodle(color: Color) {
    listOf(Offset(-0.18f, -0.18f), Offset(0.14f, -0.21f), Offset(-0.24f, 0.12f), Offset(0.06f, 0.08f), Offset(0.25f, 0.24f)).forEach {
        drawCircle(color, 0.055f, it)
    }
}

private fun DrawScope.drawLinePath(points: List<Offset>, color: Color, stroke: Stroke, close: Boolean) {
    points.zipWithNext().forEach { (start, end) -> drawLine(color, start, end, stroke.width) }
    if (close) drawLine(color, points.last(), points.first(), stroke.width)
}
