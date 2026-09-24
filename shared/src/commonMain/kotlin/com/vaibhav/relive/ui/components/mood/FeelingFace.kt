package com.vaibhav.relive.ui.components.mood

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import relive.shared.generated.resources.*

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
 * The Mood insights surfaces preserve their light editorial paper treatment in every app
 * appearance. These inks travel with that fixed white surface so Dark mode remains legible.
 */
internal object MoodInsightSurfaceColors {
    val surface = Color.White
    val ink = Color(0xFF2F2218)
    val inkMuted = Color(0xFF806957)
    val border = Color(0xFFE1D6CC)
}

/**
 * Draws one feeling face: a gradient-shaded gold sphere with eyes, a per-feeling mouth,
 * and a blush on the warmer two. Shared by the standalone composable, the mood bar and
 * the chart canvases, so every face in the app is the same face.
 *
 * [eyeOpenFraction] is 1 for open eyes and near zero mid-blink. [expressionFraction]
 * travels from the resting expression to the more animated pose used by the large faces.
 */
internal fun DrawScope.drawFeelingFace(
    feeling: MomentFeeling,
    center: Offset,
    radius: Float,
    eyeOpenFraction: Float = 1f,
    expressionFraction: Float = 0f,
) {
    val muted = feeling == MomentFeeling.Low
    val expression = expressionFraction.coerceIn(0f, 1f)
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

    // A soft highlight and lower-edge shade give the face the glossy emoji depth of the
    // reference while keeping it entirely vector-drawn and crisp at every size.
    drawOval(
        color = Color.White.copy(alpha = 0.30f),
        topLeft = Offset(center.x - radius * 0.57f, center.y - radius * 0.66f),
        size = Size(radius * 0.56f, radius * 0.26f),
    )
    drawArc(
        color = tone(FeelingFaceColors.rim).copy(alpha = 0.42f),
        startAngle = 24f,
        sweepAngle = 132f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.89f, center.y - radius * 0.89f),
        size = Size(radius * 1.78f, radius * 1.78f),
        style = Stroke(width = radius * 0.055f, cap = StrokeCap.Round),
    )

    val feature = FeelingFaceColors.feature
    val eyeOpen = eyeOpenFraction.coerceIn(0.07f, 1f)
    val greatRest = if (feeling == MomentFeeling.Great) expression else 0f
    val eyeWidth = radius * (0.22f - 0.07f * greatRest)
    val eyeRadiusY = radius * (if (muted) 0.12f else 0.15f) *
        (1f - 0.42f * greatRest) * eyeOpen
    for (side in intArrayOf(-1, 1)) {
        drawOval(
            color = feature,
            topLeft = Offset(
                center.x + side * radius * 0.31f - eyeWidth / 2f,
                center.y - radius * 0.21f - eyeRadiusY,
            ),
            size = Size(eyeWidth, eyeRadiusY * 2f),
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
            // The reference briefly closes into a tiny white smile, then opens back into
            // a broad purple grin with a single bright upper-teeth band.
            val grinAlpha = 1f - expression
            if (grinAlpha > 0f) {
                val halfWidth = radius * 0.48f
                val mouthTop = center.y + radius * 0.04f
                val mouth = Path().apply {
                    moveTo(center.x - halfWidth, mouthTop)
                    quadraticTo(center.x, center.y + radius * 0.16f, center.x + halfWidth, mouthTop)
                    quadraticTo(center.x, center.y + radius * 0.73f, center.x - halfWidth, mouthTop)
                    close()
                }
                drawPath(mouth, color = feature.copy(alpha = grinAlpha))

                val teeth = Path().apply {
                    moveTo(center.x - halfWidth * 0.88f, mouthTop + radius * 0.04f)
                    quadraticTo(
                        center.x,
                        center.y + radius * 0.20f,
                        center.x + halfWidth * 0.88f,
                        mouthTop + radius * 0.04f,
                    )
                    quadraticTo(
                        center.x,
                        center.y + radius * 0.35f,
                        center.x - halfWidth * 0.88f,
                        mouthTop + radius * 0.04f,
                    )
                    close()
                }
                drawPath(teeth, color = Color.White.copy(alpha = 0.96f * grinAlpha))
            }
            if (expression > 0f) {
                drawArc(
                    color = Color.White.copy(alpha = expression),
                    startAngle = 18f,
                    sweepAngle = 144f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius * 0.20f,
                        center.y + radius * 0.09f,
                    ),
                    size = Size(radius * 0.40f, radius * 0.16f),
                    style = Stroke(width = radius * 0.055f, cap = StrokeCap.Round),
                )
            }
        }
        MomentFeeling.Good -> drawPath(
            path = Path().apply {
                moveTo(center.x - radius * 0.35f, center.y + radius * 0.15f)
                quadraticTo(
                    center.x, center.y + radius * (0.38f + 0.13f * expression),
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
                    center.x, center.y + radius * (0.155f - 0.08f * expression),
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
 * A feeling face at [size]. [animated] plays the supplied emoji motion unless reduced motion is
 * enabled. Small inline faces stay on their first frame.
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
    val telegramFrames = when (feeling) {
        MomentFeeling.Great -> GreatEmojiFrames
        MomentFeeling.Good -> GoodEmojiFrames
        MomentFeeling.Low -> LowEmojiFrames
    }
    TelegramEmoji(
        frames = telegramFrames,
        size = size,
        animated = animated,
        animationDelayMillis = animationDelayMillis,
        modifier = modifier,
    )
}

private const val TelegramEmojiFrameMillis = 66L

private val GreatEmojiFrames: List<DrawableResource> = listOf(
    Res.drawable.great_emoji_00,
    Res.drawable.great_emoji_01,
    Res.drawable.great_emoji_02,
    Res.drawable.great_emoji_03,
    Res.drawable.great_emoji_04,
    Res.drawable.great_emoji_05,
    Res.drawable.great_emoji_06,
    Res.drawable.great_emoji_07,
    Res.drawable.great_emoji_08,
    Res.drawable.great_emoji_09,
    Res.drawable.great_emoji_10,
    Res.drawable.great_emoji_11,
    Res.drawable.great_emoji_12,
    Res.drawable.great_emoji_13,
    Res.drawable.great_emoji_14,
    Res.drawable.great_emoji_15,
    Res.drawable.great_emoji_16,
    Res.drawable.great_emoji_17,
    Res.drawable.great_emoji_18,
    Res.drawable.great_emoji_19,
    Res.drawable.great_emoji_20,
    Res.drawable.great_emoji_21,
    Res.drawable.great_emoji_22,
    Res.drawable.great_emoji_23,
    Res.drawable.great_emoji_24,
    Res.drawable.great_emoji_25,
    Res.drawable.great_emoji_26,
    Res.drawable.great_emoji_27,
    Res.drawable.great_emoji_28,
    Res.drawable.great_emoji_29,
    Res.drawable.great_emoji_30,
    Res.drawable.great_emoji_31,
    Res.drawable.great_emoji_32,
    Res.drawable.great_emoji_33,
    Res.drawable.great_emoji_34,
    Res.drawable.great_emoji_35,
    Res.drawable.great_emoji_36,
    Res.drawable.great_emoji_37,
    Res.drawable.great_emoji_38,
    Res.drawable.great_emoji_39,
    Res.drawable.great_emoji_40,
    Res.drawable.great_emoji_41,
    Res.drawable.great_emoji_42,
    Res.drawable.great_emoji_43,
    Res.drawable.great_emoji_44,
)

private val GoodEmojiFrames: List<DrawableResource> = listOf(
    Res.drawable.good_emoji_00,
    Res.drawable.good_emoji_01,
    Res.drawable.good_emoji_02,
    Res.drawable.good_emoji_03,
    Res.drawable.good_emoji_04,
    Res.drawable.good_emoji_05,
    Res.drawable.good_emoji_06,
    Res.drawable.good_emoji_07,
    Res.drawable.good_emoji_08,
    Res.drawable.good_emoji_09,
    Res.drawable.good_emoji_10,
    Res.drawable.good_emoji_11,
    Res.drawable.good_emoji_12,
    Res.drawable.good_emoji_13,
    Res.drawable.good_emoji_14,
    Res.drawable.good_emoji_15,
    Res.drawable.good_emoji_16,
    Res.drawable.good_emoji_17,
    Res.drawable.good_emoji_18,
    Res.drawable.good_emoji_19,
    Res.drawable.good_emoji_20,
    Res.drawable.good_emoji_21,
    Res.drawable.good_emoji_22,
    Res.drawable.good_emoji_23,
    Res.drawable.good_emoji_24,
    Res.drawable.good_emoji_25,
    Res.drawable.good_emoji_26,
    Res.drawable.good_emoji_27,
    Res.drawable.good_emoji_28,
    Res.drawable.good_emoji_29,
    Res.drawable.good_emoji_30,
    Res.drawable.good_emoji_31,
    Res.drawable.good_emoji_32,
    Res.drawable.good_emoji_33,
    Res.drawable.good_emoji_34,
    Res.drawable.good_emoji_35,
    Res.drawable.good_emoji_36,
    Res.drawable.good_emoji_37,
    Res.drawable.good_emoji_38,
    Res.drawable.good_emoji_39,
    Res.drawable.good_emoji_40,
    Res.drawable.good_emoji_41,
    Res.drawable.good_emoji_42,
    Res.drawable.good_emoji_43,
    Res.drawable.good_emoji_44,
)

private val LowEmojiFrames: List<DrawableResource> = listOf(
    Res.drawable.low_emoji_00,
    Res.drawable.low_emoji_01,
    Res.drawable.low_emoji_02,
    Res.drawable.low_emoji_03,
    Res.drawable.low_emoji_04,
    Res.drawable.low_emoji_05,
    Res.drawable.low_emoji_06,
    Res.drawable.low_emoji_07,
    Res.drawable.low_emoji_08,
    Res.drawable.low_emoji_09,
    Res.drawable.low_emoji_10,
    Res.drawable.low_emoji_11,
    Res.drawable.low_emoji_12,
    Res.drawable.low_emoji_13,
    Res.drawable.low_emoji_14,
    Res.drawable.low_emoji_15,
    Res.drawable.low_emoji_16,
    Res.drawable.low_emoji_17,
    Res.drawable.low_emoji_18,
    Res.drawable.low_emoji_19,
    Res.drawable.low_emoji_20,
    Res.drawable.low_emoji_21,
    Res.drawable.low_emoji_22,
    Res.drawable.low_emoji_23,
    Res.drawable.low_emoji_24,
    Res.drawable.low_emoji_25,
    Res.drawable.low_emoji_26,
    Res.drawable.low_emoji_27,
    Res.drawable.low_emoji_28,
    Res.drawable.low_emoji_29,
    Res.drawable.low_emoji_30,
    Res.drawable.low_emoji_31,
    Res.drawable.low_emoji_32,
    Res.drawable.low_emoji_33,
    Res.drawable.low_emoji_34,
    Res.drawable.low_emoji_35,
    Res.drawable.low_emoji_36,
    Res.drawable.low_emoji_37,
    Res.drawable.low_emoji_38,
    Res.drawable.low_emoji_39,
    Res.drawable.low_emoji_40,
    Res.drawable.low_emoji_41,
    Res.drawable.low_emoji_42,
    Res.drawable.low_emoji_43,
    Res.drawable.low_emoji_44,
)

@Composable
private fun TelegramEmoji(
    frames: List<DrawableResource>,
    size: Dp,
    animated: Boolean,
    animationDelayMillis: Int,
    modifier: Modifier,
) {
    val shouldAnimate = animated && !ReliveTheme.reduceMotion
    var frameIndex by remember(frames) { mutableIntStateOf(0) }

    LaunchedEffect(frames, shouldAnimate, animationDelayMillis) {
        frameIndex = 0
        if (!shouldAnimate) return@LaunchedEffect

        var startedAt = Long.MIN_VALUE
        while (true) {
            withFrameMillis { frameTime ->
                if (startedAt == Long.MIN_VALUE) {
                    startedAt = frameTime + animationDelayMillis
                }
                val elapsed = (frameTime - startedAt).coerceAtLeast(0L)
                frameIndex = ((elapsed / TelegramEmojiFrameMillis) % frames.size).toInt()
            }
        }
    }

    Image(
        painter = painterResource(frames[frameIndex]),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
