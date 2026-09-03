package com.vaibhav.relive.ui.components.mood

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Fixed face palette (ADR-0066): like the print card's white and the audio tile's black,
 * the faces keep one warm gold identity in every palette and appearance mode.
 */
internal object FeelingFaceColors {
    val light = Color(0xFFFFED9A)
    val base = Color(0xFFFFD844)
    val deep = Color(0xFFFFB300)
    val rim = Color(0xFFE08E00)
    val feature = Color(0xFF5B3A00)
    val blush = Color(0xFFEC7850)
    val chartFill = Color(0xFFE59B1F)
    val chartStroke = Color(0xFFCA944A)

    fun split(feeling: MomentFeeling): Color = when (feeling) {
        MomentFeeling.Great -> Color(0xFFE59B1F)
        MomentFeeling.Good -> Color(0xFFC8A45C)
        MomentFeeling.Low -> Color(0xFFC0AF9C)
    }
}

/**
 * Draws one feeling face: a gradient-shaded gold sphere with eyes, a per-feeling mouth,
 * and a blush on the warmer two. Shared by the standalone composable, the mood bar and
 * the chart canvases, so every face in the app is the same face.
 *
 * [eyeOpenFraction] is 1 for open eyes and near zero mid-blink.
 */
internal fun DrawScope.drawFeelingFace(
    feeling: MomentFeeling,
    center: Offset,
    radius: Float,
    eyeOpenFraction: Float = 1f,
) {
    val muted = feeling == MomentFeeling.Low
    fun tone(color: Color): Color = if (muted) desaturate(color, 0.45f) else color

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to tone(FeelingFaceColors.light),
                0.38f to tone(FeelingFaceColors.base),
                0.72f to tone(FeelingFaceColors.deep),
                1.0f to tone(FeelingFaceColors.rim),
            ),
            center = center + Offset(-radius * 0.36f, -radius * 0.44f),
            radius = radius * 1.9f,
        ),
        radius = radius,
        center = center,
    )

    val feature = FeelingFaceColors.feature
    val eyeRadiusY = radius * (if (muted) 0.12f else 0.15f) * eyeOpenFraction.coerceIn(0.07f, 1f)
    for (side in intArrayOf(-1, 1)) {
        drawOval(
            color = feature,
            topLeft = Offset(
                center.x + side * radius * 0.31f - radius * 0.11f,
                center.y - radius * 0.21f - eyeRadiusY,
            ),
            size = Size(radius * 0.22f, eyeRadiusY * 2f),
        )
    }

    if (!muted) {
        val blushAlpha = if (feeling == MomentFeeling.Great) 0.32f else 0.18f
        for (side in intArrayOf(-1, 1)) {
            drawOval(
                color = FeelingFaceColors.blush.copy(alpha = blushAlpha),
                topLeft = Offset(
                    center.x + side * radius * 0.56f - radius * 0.15f,
                    center.y + radius * 0.035f,
                ),
                size = Size(radius * 0.30f, radius * 0.17f),
            )
        }
    }

    when (feeling) {
        MomentFeeling.Great -> {
            // A filled open smile, the one face that reads as delight at small sizes.
            val mouth = Path().apply {
                moveTo(center.x - radius * 0.44f, center.y + radius * 0.08f)
                quadraticTo(
                    center.x, center.y + radius * 0.62f,
                    center.x + radius * 0.44f, center.y + radius * 0.08f,
                )
                quadraticTo(
                    center.x, center.y + radius * 0.31f,
                    center.x - radius * 0.44f, center.y + radius * 0.08f,
                )
                close()
            }
            drawPath(mouth, color = feature)
        }
        MomentFeeling.Good -> drawPath(
            path = Path().apply {
                moveTo(center.x - radius * 0.35f, center.y + radius * 0.15f)
                quadraticTo(
                    center.x, center.y + radius * 0.44f,
                    center.x + radius * 0.35f, center.y + radius * 0.15f,
                )
            },
            color = feature,
            style = mouthStroke(radius),
        )
        MomentFeeling.Low -> drawPath(
            path = Path().apply {
                moveTo(center.x - radius * 0.33f, center.y + radius * 0.30f)
                quadraticTo(
                    center.x, center.y + radius * 0.155f,
                    center.x + radius * 0.33f, center.y + radius * 0.30f,
                )
            },
            color = feature,
            style = mouthStroke(radius),
        )
    }
}

private fun mouthStroke(radius: Float): Stroke = Stroke(
    width = radius * 0.125f,
    cap = StrokeCap.Round,
)

private fun desaturate(color: Color, amount: Float): Color {
    val gray = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f
    return Color(
        red = color.red + (gray - color.red) * amount,
        green = color.green + (gray - color.green) * amount,
        blue = color.blue + (gray - color.blue) * amount,
        alpha = color.alpha,
    )
}

/**
 * A feeling face at [size]. [animated] adds the approved idle bob and occasional blink;
 * both stop entirely under reduced motion. Small inline faces stay static.
 */
@Composable
fun FeelingFace(
    feeling: MomentFeeling,
    size: Dp,
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    /** Staggers the idle animation so two faces side by side do not move in lockstep. */
    animationDelayMillis: Int = 0,
) {
    val animate = animated && !ReliveTheme.reduceMotion
    var bob = 0f
    var eyeOpen = 1f
    if (animate) {
        val transition = rememberInfiniteTransition(label = "feeling face idle")
        val bobValue by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1400,
                    delayMillis = animationDelayMillis,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bob",
        )
        val blinkValue by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 4600
                    1f at 0 using LinearEasing
                    1f at 4180 using LinearEasing
                    0.08f at 4320 using LinearEasing
                    1f at 4460 using LinearEasing
                },
                initialStartOffset = androidx.compose.animation.core.StartOffset(animationDelayMillis),
            ),
            label = "blink",
        )
        bob = bobValue
        eyeOpen = blinkValue
    }
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f * 0.96f
        val lift = bob * radius * 0.09f
        drawFeelingFace(
            feeling = feeling,
            center = Offset(this.size.width / 2f, this.size.height / 2f - lift),
            radius = radius,
            eyeOpenFraction = eyeOpen,
        )
    }
}
