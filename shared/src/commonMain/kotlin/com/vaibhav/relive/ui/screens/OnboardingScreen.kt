package com.vaibhav.relive.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineHomeSummary
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import com.vaibhav.relive.ui.components.composer.ImageGlyph
import com.vaibhav.relive.ui.components.composer.MicGlyph
import com.vaibhav.relive.ui.components.composer.VideoGlyph
import com.vaibhav.relive.ui.components.timeline.LocalTimelineWallpaperPalette
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.icons.OnboardingIcons
import com.vaibhav.relive.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

internal const val ONBOARDING_DARK_MODE = true
internal val ONBOARDING_CHAPTER_THEME_IDS = listOf(
    ReliveThemeId.TealSaffron,
    ReliveThemeId.InkLilac,
    ReliveThemeId.PlumGold,
    ReliveThemeId.Sunrise,
    ReliveThemeId.Sunset,
)

internal val ONBOARDING_RELIVE_MOMENT_PREVIEWS = listOf(
    MomentPresentation(
        id = MomentId("onboarding-quiet-morning"),
        createdAt = Instant(0),
        updatedAt = null,
        formattedDate = "12 AUG 2026",
        formattedTime = "8:24 AM",
        title = "A quiet morning",
        content = "",
        locationLabel = "Mountain lake",
        location = null,
        isFavorite = false,
        feeling = null,
        tags = emptyList(),
        attachments = emptyList(),
    ),
)

internal val ONBOARDING_TIMELINE_MOMENT_PREVIEWS = listOf(
    ONBOARDING_RELIVE_MOMENT_PREVIEWS.single(),
    MomentPresentation(
        id = MomentId("onboarding-forest-trails"),
        createdAt = Instant(0),
        updatedAt = null,
        formattedDate = "16 AUG 2026",
        formattedTime = "9:08 AM",
        title = "Forest trails",
        content = "",
        locationLabel = "North woods",
        location = null,
        isFavorite = false,
        feeling = null,
        tags = emptyList(),
        attachments = emptyList(),
    ),
    MomentPresentation(
        id = MomentId("onboarding-coastal-escape"),
        createdAt = Instant(0),
        updatedAt = null,
        formattedDate = "18 AUG 2026",
        formattedTime = "6:42 PM",
        title = "A coastal escape",
        content = "",
        locationLabel = "West coast",
        location = null,
        isFavorite = false,
        feeling = null,
        tags = emptyList(),
        attachments = emptyList(),
    ),
    MomentPresentation(
        id = MomentId("onboarding-slow-afternoon"),
        createdAt = Instant(0),
        updatedAt = null,
        formattedDate = "14 AUG 2026",
        formattedTime = "3:17 PM",
        title = "Slow afternoons",
        content = "",
        locationLabel = "At home",
        location = null,
        isFavorite = false,
        feeling = null,
        tags = emptyList(),
        attachments = emptyList(),
    ),
)

internal val ONBOARDING_READY_MOMENT_PREVIEWS = ONBOARDING_TIMELINE_MOMENT_PREVIEWS +
    MomentPresentation(
        id = MomentId("onboarding-evening-glow"),
        createdAt = Instant(0),
        updatedAt = null,
        formattedDate = "21 AUG 2026",
        formattedTime = "7:16 PM",
        title = "Evening glow",
        content = "",
        locationLabel = "Old town",
        location = null,
        isFavorite = false,
        feeling = null,
        tags = emptyList(),
        attachments = emptyList(),
    )

private val CaptureThemeId = ONBOARDING_CHAPTER_THEME_IDS.first()
internal const val ONBOARDING_FOCUSED_MOMENT_SCALE = .76f
internal const val ONBOARDING_RESTING_MOMENT_SCALE = .57f

internal fun onboardingMomentFocus(itemCenterY: Float, focusCenterY: Float, focusHeight: Float): Float {
    val distance = abs(itemCenterY - focusCenterY)
    return (1f - distance / (focusHeight * 1.15f).coerceAtLeast(1f)).coerceIn(0f, 1f)
}

internal fun onboardingMomentScale(focus: Float): Float =
    ONBOARDING_RESTING_MOMENT_SCALE +
        focus.coerceIn(0f, 1f) * (ONBOARDING_FOCUSED_MOMENT_SCALE - ONBOARDING_RESTING_MOMENT_SCALE)

internal data class OnboardingSheetDeformation(
    val retreat: Float,
    val tailWidthFraction: Float,
    val topInsetFraction: Float,
    val finalPull: Float,
)

internal fun onboardingSheetDeformation(progress: Float): OnboardingSheetDeformation {
    val value = progress.coerceIn(0f, 1f)
    // The two pulls overlap. Ending one eased segment before beginning the next
    // creates a visible plateau in the card's bottom-up uncovering.
    val progressivePull = smoothStep(((value - .02f) / .86f).coerceIn(0f, 1f))
    val finalPull = smoothStep(((value - .54f) / .46f).coerceIn(0f, 1f))
    return OnboardingSheetDeformation(
        retreat = progressivePull * .74f + finalPull * .26f,
        tailWidthFraction = lerp(.5f, .11f, progressivePull).let { lerp(it, .006f, finalPull) },
        topInsetFraction = finalPull * .494f,
        finalPull = finalPull,
    )
}

internal fun onboardingCardRevealProgress(suctionProgress: Float): Float =
    onboardingSheetDeformation(suctionProgress).retreat

private fun smoothStep(value: Float): Float = value * value * (3f - 2f * value)

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)

private enum class OnboardingStage {
    Screen1,
    RevealingTimeline,
    TimelineSettled,
    TimelineAutoScroll,
    TimelineIdle,
    ReturningToFocus,
    HidingTimeline,
    SuckingTimeline,
    RevealingThumbnail,
    RevealingScreen3,
    Screen3,
    PrivacyTakeover,
    Screen4,
    RevealingScreen5,
    Screen5,
    PrivacyReturn,
    HidingScreen3,
    RecessingThumbnail,
    RestoringTimeline,
}

@Composable
fun OnboardingScreen(
    mediaStore: MediaStore,
    onContinue: () -> Unit,
    onSkip: suspend () -> Boolean,
) {
    ReliveTheme(CaptureThemeId, ONBOARDING_DARK_MODE) {
        OnboardingFlow(mediaStore, onContinue, onSkip)
    }
}

@Composable
private fun OnboardingFlow(
    mediaStore: MediaStore,
    onContinue: () -> Unit,
    onSkip: suspend () -> Boolean,
) {
    val motion = ReliveTheme.motion
    val reducedMotion = ReliveTheme.reduceMotion
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf(OnboardingStage.Screen1) }
    var autoScrollHasRun by remember { mutableStateOf(false) }
    var timelinePrepositioned by remember { mutableStateOf(false) }
    var skipEnabled by remember { mutableStateOf(true) }
    var captureCardBounds by remember { mutableStateOf<Rect?>(null) }
    var visualBounds by remember { mutableStateOf<Rect?>(null) }
    var timelineSheetBounds by remember { mutableStateOf<Rect?>(null) }
    var customTimelineHoleBounds by remember { mutableStateOf<Rect?>(null) }
    val timelineCardBounds = remember { mutableStateMapOf<Int, Rect>() }
    var focusedFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var focusedFirstVisibleItemOffset by remember { mutableIntStateOf(0) }
    val transitionProgress = remember { Animatable(0f) }
    val suctionProgress = remember { Animatable(0f) }
    val thumbnailProgress = remember { Animatable(0f) }
    val screen3ContentProgress = remember { Animatable(0f) }
    val privacyTakeoverProgress = remember { Animatable(0f) }
    val screen5TransitionProgress = remember { Animatable(0f) }
    val navigationButtonBrightness = remember { Animatable(.42f) }
    var screen3CardBounds by remember { mutableStateOf<Rect?>(null) }
    val timelineState = rememberLazyListState()
    val userDraggingTimeline by timelineState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(userDraggingTimeline, stage) {
        if (userDraggingTimeline && stage == OnboardingStage.TimelineAutoScroll) {
            stage = OnboardingStage.TimelineIdle
        }
    }

    LaunchedEffect(stage) {
        val navigationReady = stage == OnboardingStage.Screen1 ||
            stage == OnboardingStage.TimelineSettled ||
            stage == OnboardingStage.TimelineAutoScroll ||
            stage == OnboardingStage.TimelineIdle ||
            stage == OnboardingStage.Screen3 ||
            stage == OnboardingStage.Screen4 ||
            stage == OnboardingStage.Screen5
        if (reducedMotion) {
            navigationButtonBrightness.snapTo(if (navigationReady) 1f else .42f)
        } else {
            if (stage == OnboardingStage.Screen1) delay(180)
            navigationButtonBrightness.animateTo(
                if (navigationReady) 1f else .42f,
                tween(
                    durationMillis = if (navigationReady) 820 else 420,
                    easing = motion.easings.emphasizedDecelerate,
                ),
            )
        }
    }

    LaunchedEffect(
        stage,
        captureCardBounds != null,
        timelineCardBounds[0] != null,
        timelinePrepositioned,
        timelineSheetBounds != null,
        customTimelineHoleBounds != null,
    ) {
        when (stage) {
            OnboardingStage.Screen1,
            OnboardingStage.TimelineIdle,
            OnboardingStage.Screen3,
            OnboardingStage.Screen4,
            OnboardingStage.Screen5 -> Unit

            OnboardingStage.RevealingScreen5 -> {
                if (reducedMotion) {
                    screen5TransitionProgress.snapTo(1f)
                } else {
                    screen5TransitionProgress.animateTo(
                        1f,
                        tween(1_900, easing = LinearEasing),
                    )
                }
                stage = OnboardingStage.Screen5
            }

            OnboardingStage.PrivacyTakeover -> {
                if (screen3CardBounds == null) return@LaunchedEffect
                if (reducedMotion) {
                    privacyTakeoverProgress.snapTo(1f)
                } else {
                    privacyTakeoverProgress.animateTo(
                        1f,
                        tween(1_350, easing = motion.easings.emphasizedDecelerate),
                    )
                }
                stage = OnboardingStage.Screen4
            }

            OnboardingStage.PrivacyReturn -> {
                // The panel's contents leave composition before its reveal reverses. This
                // prevents translated bar text from being caught in the shrinking clip.
                if (!reducedMotion) delay(240)
                if (reducedMotion) {
                    privacyTakeoverProgress.snapTo(0f)
                } else {
                    privacyTakeoverProgress.animateTo(
                        0f,
                        tween(900, easing = motion.easings.emphasizedDecelerate),
                    )
                }
                stage = OnboardingStage.Screen3
            }

            OnboardingStage.RevealingTimeline -> {
                val hasMeasuredSharedCardBounds = captureCardBounds != null && timelineCardBounds[0] != null
                if (!hasMeasuredSharedCardBounds) return@LaunchedEffect
                if (!timelinePrepositioned) {
                    val captureBounds = requireNotNull(captureCardBounds)
                    val targetBounds = requireNotNull(timelineCardBounds[0])
                    timelineState.animateScrollBy(
                        value = targetBounds.top - captureBounds.top,
                        animationSpec = tween(1),
                    )
                    focusedFirstVisibleItemIndex = timelineState.firstVisibleItemIndex
                    focusedFirstVisibleItemOffset = timelineState.firstVisibleItemScrollOffset
                    timelinePrepositioned = true
                    return@LaunchedEffect
                }
                if (reducedMotion) {
                    transitionProgress.snapTo(1f)
                } else {
                    transitionProgress.animateTo(
                        1f,
                        tween(850, easing = motion.easings.emphasizedDecelerate),
                    )
                }
                stage = OnboardingStage.TimelineSettled
            }

            OnboardingStage.TimelineSettled -> {
                if (!reducedMotion) delay(320)
                stage = if (reducedMotion) OnboardingStage.TimelineIdle else OnboardingStage.TimelineAutoScroll
            }

            OnboardingStage.TimelineAutoScroll -> {
                if (autoScrollHasRun) {
                    stage = OnboardingStage.TimelineIdle
                } else {
                    autoScrollHasRun = true
                    for (index in 1..ONBOARDING_TIMELINE_MOMENT_PREVIEWS.lastIndex) {
                        var attempts = 0
                        while (timelineCardBounds[index] == null && attempts < 12) {
                            val focusBounds = captureCardBounds ?: break
                            timelineState.animateScrollBy(
                                value = focusBounds.height * .24f,
                                animationSpec = tween(190, easing = motion.easings.standard),
                            )
                            delay(12)
                            attempts++
                        }
                        val nextBounds = timelineCardBounds[index] ?: break
                        val focusBounds = captureCardBounds ?: break
                        timelineState.animateScrollBy(
                            value = nextBounds.top - focusBounds.top,
                            animationSpec = tween(1_180, easing = motion.easings.standard),
                        )
                        delay(90)
                    }
                    stage = OnboardingStage.TimelineIdle
                }
            }

            OnboardingStage.ReturningToFocus -> {
                timelineState.animateScrollToItem(
                    focusedFirstVisibleItemIndex,
                    focusedFirstVisibleItemOffset,
                )
                stage = OnboardingStage.HidingTimeline
            }

            OnboardingStage.HidingTimeline -> {
                if (reducedMotion) {
                    transitionProgress.snapTo(0f)
                } else {
                    transitionProgress.animateTo(
                        0f,
                        tween(720, easing = motion.easings.emphasizedDecelerate),
                    )
                }
                autoScrollHasRun = false
                timelinePrepositioned = false
                timelineCardBounds.clear()
                stage = OnboardingStage.Screen1
            }

            OnboardingStage.SuckingTimeline -> {
                if (timelineSheetBounds == null || customTimelineHoleBounds == null) {
                    return@LaunchedEffect
                }
                if (reducedMotion) {
                    suctionProgress.snapTo(1f)
                } else {
                    suctionProgress.animateTo(
                        1f,
                        tween(1_800, easing = motion.easings.standard),
                    )
                }
                stage = OnboardingStage.RevealingThumbnail
            }

            OnboardingStage.RevealingThumbnail -> {
                if (reducedMotion) {
                    thumbnailProgress.animateTo(
                        1f,
                        tween(motion.durations.short3, easing = motion.easings.standard),
                    )
                } else {
                    thumbnailProgress.animateTo(
                        1f,
                        tween(320, easing = motion.easings.emphasizedDecelerate),
                    )
                }
                stage = OnboardingStage.RevealingScreen3
            }

            OnboardingStage.RevealingScreen3 -> {
                if (reducedMotion) {
                    screen3ContentProgress.animateTo(
                        1f,
                        tween(motion.durations.short3, easing = motion.easings.standard),
                    )
                } else {
                    screen3ContentProgress.animateTo(
                        1f,
                        tween(motion.durations.medium4, easing = motion.easings.standard),
                    )
                }
                stage = OnboardingStage.Screen3
            }

            OnboardingStage.HidingScreen3 -> {
                if (reducedMotion) {
                    screen3ContentProgress.animateTo(
                        0f,
                        tween(motion.durations.short3, easing = motion.easings.standard),
                    )
                } else {
                    screen3ContentProgress.animateTo(
                        0f,
                        tween(motion.durations.medium1, easing = motion.easings.standardAccelerate),
                    )
                }
                stage = OnboardingStage.RecessingThumbnail
            }

            OnboardingStage.RecessingThumbnail -> {
                if (reducedMotion) {
                    thumbnailProgress.animateTo(
                        0f,
                        tween(motion.durations.short3, easing = motion.easings.standard),
                    )
                } else {
                    thumbnailProgress.animateTo(
                        0f,
                        tween(motion.durations.medium1, easing = motion.easings.standardAccelerate),
                    )
                }
                stage = OnboardingStage.RestoringTimeline
            }

            OnboardingStage.RestoringTimeline -> {
                if (reducedMotion) {
                    suctionProgress.snapTo(0f)
                } else {
                    suctionProgress.animateTo(
                        0f,
                        tween(
                            motion.durations.extraLong1 + motion.durations.short2,
                            easing = motion.easings.emphasizedDecelerate,
                        ),
                    )
                }
                stage = OnboardingStage.TimelineIdle
            }
        }
    }

    val readyVisible = stage == OnboardingStage.RevealingScreen5 || stage == OnboardingStage.Screen5
    val privacyVisible = stage == OnboardingStage.PrivacyTakeover ||
        stage == OnboardingStage.Screen4 || stage == OnboardingStage.RevealingScreen5 ||
        stage == OnboardingStage.PrivacyReturn
    val timelineVisible = stage != OnboardingStage.Screen1
    val timelineSheetVisible = stage == OnboardingStage.RevealingTimeline ||
        stage == OnboardingStage.TimelineSettled ||
        stage == OnboardingStage.TimelineAutoScroll ||
        stage == OnboardingStage.TimelineIdle ||
        stage == OnboardingStage.ReturningToFocus ||
        stage == OnboardingStage.SuckingTimeline ||
        stage == OnboardingStage.RestoringTimeline
    val sharedCardVisible = stage == OnboardingStage.Screen1 ||
        stage == OnboardingStage.RevealingTimeline || stage == OnboardingStage.HidingTimeline

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val shellHeight = (maxHeight * .37f).coerceIn(238.dp, 310.dp)
        val momentWidthFraction = if (maxHeight < 780.dp) .56f else .74f
        val focusTopInset = captureCardBounds?.let { captureBounds ->
            visualBounds?.let { bounds ->
                with(density) { (captureBounds.top - bounds.top).coerceAtLeast(0f).toDp() }
            }
        }
        OnboardingAtmosphereCanvas(
            progress = transitionProgress.value + suctionProgress.value + privacyTakeoverProgress.value +
                screen5TransitionProgress.value,
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(shellHeight + 192.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            .28f to ReliveOnboardingColors.darkPlum.copy(alpha = .46f),
                            .52f to ReliveOnboardingColors.darkPlum.copy(alpha = .96f),
                            1f to ReliveOnboardingColors.darkBase,
                        ),
                    ),
                ),
        )
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            CaptureHeader(
                enabled = skipEnabled && !readyVisible,
                departure = screen5TransitionProgress.value,
                timelineBlend = if (timelineSheetVisible) {
                    transitionProgress.value * (1f - suctionProgress.value)
                } else {
                    0f
                },
            ) {
                if (skipEnabled) {
                    skipEnabled = false
                    scope.launch { if (!onSkip()) skipEnabled = true }
                }
            }
            Box(
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onGloballyPositioned { visualBounds = it.boundsInRoot() },
            ) {
                if (timelineVisible && !readyVisible) {
                    Screen3TimelineCard(
                        mediaStore = mediaStore,
                        revealProgress = onboardingCardRevealProgress(suctionProgress.value),
                        thumbnailProgress = thumbnailProgress.value,
                        onHoleBoundsChanged = { customTimelineHoleBounds = it },
                        onCardBoundsChanged = { screen3CardBounds = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = ReliveTheme.dimensions.spacing.xxl)
                            .blur(5.dp * privacyTakeoverProgress.value)
                            .graphicsLayer {
                                alpha = 1f - privacyTakeoverProgress.value * .34f
                            },
                    )
                }
                if (timelineSheetVisible) {
                    Box(
                        Modifier.fillMaxSize()
                            .onGloballyPositioned { timelineSheetBounds = it.boundsInRoot() },
                    ) {
                        TimelineOnboardingVisual(
                            mediaStore = mediaStore,
                            timelineState = timelineState,
                            stage = stage,
                            focusBounds = captureCardBounds,
                            entryProgress = transitionProgress.value,
                            momentWidthFraction = momentWidthFraction,
                            sharedCardWidth = captureCardBounds?.width?.let { with(density) { it.toDp() } },
                            focusTopInset = focusTopInset,
                            onMomentBoundsChanged = { index, bounds -> timelineCardBounds[index] = bounds },
                            modifier = Modifier.timelineSuction(
                                progress = suctionProgress.value,
                                sheetBounds = timelineSheetBounds,
                                holeBounds = customTimelineHoleBounds,
                            ),
                        )
                    }
                }
                CaptureOnboardingVisual(
                    mediaStore = mediaStore,
                    controlsProgress = 1f - transitionProgress.value,
                    showMoment = sharedCardVisible,
                    momentWidthFraction = momentWidthFraction,
                    onMomentBoundsChanged = { captureCardBounds = it },
                )
                val privacyBounds = screen3CardBounds
                val privacyStageBounds = visualBounds
                if (privacyVisible && privacyBounds != null && privacyStageBounds != null) {
                    PrivacyOnboardingVisual(
                        takeoverProgress = privacyTakeoverProgress.value,
                        contentVisible = stage != OnboardingStage.PrivacyReturn,
                        modifier = Modifier
                            .offset(
                                x = with(density) { (privacyBounds.left - privacyStageBounds.left).toDp() },
                                y = with(density) { (privacyBounds.top - privacyStageBounds.top).toDp() },
                            )
                            .size(
                                width = with(density) { privacyBounds.width.toDp() },
                                height = with(density) { privacyBounds.height.toDp() },
                            )
                            .graphicsLayer {
                                val collapse = smoothStep(
                                    (screen5TransitionProgress.value / .34f).coerceIn(0f, 1f),
                                )
                                scaleX = lerp(1f, .001f, collapse)
                                scaleY = scaleX
                            },
                    )
                }
                if (readyVisible) {
                    ReadyOnboardingVisual(
                        transitionProgress = screen5TransitionProgress.value,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (timelineSheetVisible && suctionProgress.value < 1f) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(88.dp)
                            .graphicsLayer {
                                alpha = transitionProgress.value * (1f - suctionProgress.value)
                            }
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        onboardingAtmosphereCurveColor(transitionProgress.value)
                                            .copy(alpha = .82f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                }
                val holeBounds = customTimelineHoleBounds
                val stageBounds = visualBounds
                if (timelineVisible && !privacyVisible && !readyVisible && holeBounds != null && stageBounds != null) {
                    val holeReveal = ((onboardingCardRevealProgress(suctionProgress.value) - .42f) / .54f)
                        .coerceIn(0f, 1f)
                    OnboardingHoleSurface(
                        thumbnailProgress = thumbnailProgress.value,
                        modifier = Modifier
                            .offset(
                                x = with(density) { (holeBounds.left - stageBounds.left).toDp() },
                                y = with(density) { (holeBounds.top - stageBounds.top).toDp() },
                            )
                            .size(
                                width = with(density) { holeBounds.width.toDp() },
                                height = with(density) { holeBounds.height.toDp() },
                            )
                            .graphicsLayer {
                                shape = BottomUpRevealShape(holeReveal)
                                clip = true
                            },
                    )
                }
            }
            // Copy and progress advance during the final pull, leaving the thumbnail
            // as the sole end beat once the sheet has been absorbed.
            val screen3PresentationProgress = maxOf(
                screen3ContentProgress.value,
                ((suctionProgress.value - .52f) / .42f).coerceIn(0f, 1f),
            )
            OnboardingBottomShell(
                captureHeadline = "Keep what matters.",
                captureDescription = "Photos, words, videos, and sounds. A little of life, kept together.",
                timelineHeadline = "Your moments find their place.",
                timelineDescription = "Captured memories are added to your timeline, building your story over time.",
                customTimelineHeadline = "Create custom timelines.",
                customTimelineDescription = "Group your moments into meaningful collections you can revisit anytime.",
                privacyHeadline = "Your memories stay yours.",
                privacyDescription = "A personal archive, stored on your device.",
                readyHeadline = "Your moments, yours to relive.",
                readyDescription = "Keep the little things. Come back to them whenever you want.",
                transition = transitionProgress.value,
                screen3Transition = screen3PresentationProgress,
                screen3CopyTransition = screen3PresentationProgress,
                screen3ControlsProgress = screen3PresentationProgress,
                privacyProgress = smoothStep(
                    ((privacyTakeoverProgress.value - .55f) / .45f).coerceIn(0f, 1f),
                ),
                readyProgress = screen5TransitionProgress.value,
                continueVisualAlpha = navigationButtonBrightness.value,
                onContinue = {
                    if (stage == OnboardingStage.Screen1) stage = OnboardingStage.RevealingTimeline
                    else if (
                        stage == OnboardingStage.TimelineSettled ||
                        stage == OnboardingStage.TimelineAutoScroll ||
                        stage == OnboardingStage.TimelineIdle
                    ) {
                        stage = OnboardingStage.SuckingTimeline
                    } else if (stage == OnboardingStage.Screen3) {
                        stage = OnboardingStage.PrivacyTakeover
                    } else if (stage == OnboardingStage.Screen4) {
                        stage = OnboardingStage.RevealingScreen5
                    } else if (stage == OnboardingStage.Screen5) {
                        onContinue()
                    }
                },
                onBack = {
                    if (stage == OnboardingStage.Screen5) {
                        scope.launch {
                            screen5TransitionProgress.snapTo(0f)
                            stage = OnboardingStage.Screen4
                        }
                    } else {
                        stage = if (stage == OnboardingStage.Screen4) {
                            OnboardingStage.PrivacyReturn
                        } else if (stage == OnboardingStage.Screen3) {
                            OnboardingStage.HidingScreen3
                        } else {
                            OnboardingStage.ReturningToFocus
                        }
                    }
                },
                continueEnabled = stage == OnboardingStage.Screen1 ||
                    stage == OnboardingStage.TimelineSettled ||
                    stage == OnboardingStage.TimelineAutoScroll ||
                    stage == OnboardingStage.TimelineIdle ||
                    stage == OnboardingStage.Screen3 ||
                    stage == OnboardingStage.Screen4 ||
                    stage == OnboardingStage.Screen5,
                showBack = transitionProgress.value >= .999f &&
                    stage != OnboardingStage.SuckingTimeline &&
                    stage != OnboardingStage.RevealingThumbnail &&
                    stage != OnboardingStage.RevealingScreen3 &&
                    stage != OnboardingStage.HidingScreen3 &&
                    stage != OnboardingStage.RecessingThumbnail &&
                    stage != OnboardingStage.RestoringTimeline,
                backEnabled = stage == OnboardingStage.TimelineSettled ||
                    stage == OnboardingStage.TimelineAutoScroll ||
                    stage == OnboardingStage.TimelineIdle ||
                    stage == OnboardingStage.Screen3 ||
                    stage == OnboardingStage.Screen4 ||
                    stage == OnboardingStage.Screen5,
                finalAction = readyVisible,
                modifier = Modifier.height(shellHeight),
            )
        }
    }
}

@Composable
private fun CaptureOnboardingVisual(
    mediaStore: MediaStore,
    controlsProgress: Float,
    showMoment: Boolean,
    momentWidthFraction: Float,
    onMomentBoundsChanged: (Rect) -> Unit,
) {
    val motion = ReliveTheme.motion
    val reduced = ReliveTheme.reduceMotion
    val capabilities = List(4) { remember { Animatable(0f) } }
    val card = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduced) {
            capabilities.forEach { it.snapTo(1f) }
            card.snapTo(1f)
            return@LaunchedEffect
        }
        launch {
            card.animateTo(1f, tween(520, easing = motion.easings.emphasizedDecelerate))
        }
        capabilities.forEachIndexed { index, value ->
            launch {
                delay(120L + index * 85L)
                value.animateTo(1f, tween(680, easing = motion.easings.emphasizedDecelerate))
            }
        }
    }

    Box(
        Modifier.fillMaxSize()
            .padding(horizontal = ReliveTheme.dimensions.onboarding.pageHorizontalPadding),
    ) {
        if (showMoment) {
            CaptureMomentPreview(
                mediaStore = mediaStore,
                entrance = card.value,
                atmosphereProgress = 1f - controlsProgress,
                widthFraction = momentWidthFraction,
                onBoundsChanged = onMomentBoundsChanged,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        CaptureCapabilities(
            values = capabilities.map { it.value },
            visibility = controlsProgress,
            modifier = Modifier.fillMaxSize().departure(1f - controlsProgress, 10.dp),
        )
    }
}

@Composable
private fun ReadyOnboardingVisual(
    transitionProgress: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier.padding(horizontal = 18.dp).semantics {
            contentDescription = "A collection of five abstract Relive moment cards"
        },
        contentAlignment = Alignment.Center,
    ) {
        val compact = maxWidth < 370.dp || maxHeight < 430.dp
        val backWidth = if (compact) 128.dp else 146.dp
        val centerWidth = if (compact) 164.dp else 184.dp
        val horizontalReach = if (compact) 92.dp else 108.dp
        val verticalReach = if (compact) 88.dp else 104.dp

        ReadyMomentCard(
            media = ReadyMomentMedia.Greenery,
            width = backWidth,
            rotation = -9f,
            revealProgress = readyCardRevealProgress(transitionProgress, 0),
            modifier = Modifier.offset(x = -horizontalReach, y = -verticalReach),
        )
        ReadyMomentCard(
            media = ReadyMomentMedia.Beach,
            width = backWidth,
            rotation = 8f,
            revealProgress = readyCardRevealProgress(transitionProgress, 1),
            modifier = Modifier.offset(x = horizontalReach, y = -verticalReach + 8.dp),
        )
        ReadyMomentCard(
            media = ReadyMomentMedia.CityNight,
            width = backWidth,
            rotation = -7f,
            revealProgress = readyCardRevealProgress(transitionProgress, 2),
            modifier = Modifier.offset(x = -horizontalReach, y = verticalReach),
        )
        ReadyMomentCard(
            media = ReadyMomentMedia.Cozy,
            width = backWidth,
            rotation = 8f,
            revealProgress = readyCardRevealProgress(transitionProgress, 3),
            modifier = Modifier.offset(x = horizontalReach, y = verticalReach + 2.dp),
        )
        ReadyMomentCard(
            media = ReadyMomentMedia.Sunset,
            width = centerWidth,
            rotation = 4f,
            revealProgress = readyCardRevealProgress(transitionProgress, 4),
            modifier = Modifier.offset(y = 4.dp),
        )
    }
}

private fun readyCardRevealProgress(transitionProgress: Float, index: Int): Float {
    val start = .34f + index * .075f
    return smoothStep(((transitionProgress - start) / .23f).coerceIn(0f, 1f))
}

private enum class ReadyMomentMedia { Greenery, Beach, CityNight, Cozy, Sunset }

@Composable
private fun ReadyMomentCard(
    media: ReadyMomentMedia,
    width: Dp,
    rotation: Float,
    revealProgress: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(11.dp)
    Column(
        modifier = modifier
            .width(width)
            .aspectRatio(.78f)
            .graphicsLayer {
                alpha = revealProgress
                scaleX = revealProgress.coerceAtLeast(.001f)
                scaleY = scaleX
                rotationZ = rotation
                shadowElevation = 11.dp.toPx()
                this.shape = shape
                clip = false
            }
            .blur(
                radius = 6.dp * (1f - revealProgress),
                edgeTreatment = BlurredEdgeTreatment.Unbounded,
            )
            .clip(shape)
            .background(Color(0xFFF8F5F4))
            .padding(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(34.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth(.82f).height(10.dp)
                        .clip(RoundedCornerShape(50)).background(Color(0xFFD4D0D2)),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth(.52f).height(9.dp)
                        .clip(RoundedCornerShape(50)).background(Color(0xFFDEDADD)),
                )
            }
            Box(
                Modifier.padding(start = 6.dp).size(16.dp).clip(CircleShape)
                    .background(readyMomentAccent(media)),
            )
        }
        Spacer(Modifier.height(4.dp))
        ReadyMomentMediaThumbnail(
            media = media,
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(7.dp)),
        )
    }
}

private fun readyMomentAccent(media: ReadyMomentMedia): Color = when (media) {
    ReadyMomentMedia.Greenery -> Color(0xFFB9C9A9)
    ReadyMomentMedia.Beach -> Color(0xFFAEC9EA)
    ReadyMomentMedia.CityNight -> Color(0xFFC0A5D9)
    ReadyMomentMedia.Cozy -> Color(0xFFD6B7AA)
    ReadyMomentMedia.Sunset -> Color(0xFFD6B4D3)
}

@Composable
private fun ReadyMomentMediaThumbnail(
    media: ReadyMomentMedia,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        when (media) {
            ReadyMomentMedia.Sunset -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFD8B2CC), Color(0xFFEFB9AC), Color(0xFF8E72AD))))
                drawCircle(Color(0xFFFFE3BF), size.minDimension * .105f, androidx.compose.ui.geometry.Offset(size.width * .73f, size.height * .27f))
                val rear = Path().apply {
                    moveTo(0f, size.height * .74f); lineTo(size.width * .28f, size.height * .54f)
                    lineTo(size.width * .5f, size.height * .75f); lineTo(size.width * .66f, size.height * .65f)
                    lineTo(size.width, size.height * .82f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
                }
                drawPath(rear, Color(0xFF9D7FB0))
                val front = Path().apply {
                    moveTo(0f, size.height * .82f); lineTo(size.width * .3f, size.height * .56f)
                    lineTo(size.width * .58f, size.height * .86f); lineTo(size.width, size.height * .76f)
                    lineTo(size.width, size.height); lineTo(0f, size.height); close()
                }
                drawPath(front, Color(0xFF57477F))
            }
            ReadyMomentMedia.Greenery -> {
                drawRect(Brush.linearGradient(listOf(Color(0xFFE4D8C7), Color(0xFF8C9B78), Color(0xFF435C45))))
                repeat(7) { index ->
                    val x = size.width * (.12f + index * .13f)
                    val y = size.height * (.2f + (index % 3) * .23f)
                    drawOval(Color(0xFF36543B).copy(alpha = .82f), topLeft = androidx.compose.ui.geometry.Offset(x, y), size = Size(size.width * .18f, size.height * .27f))
                }
            }
            ReadyMomentMedia.Beach -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFBBD4E8), Color(0xFFE1E5E3), Color(0xFFB6D4E4), Color(0xFFE5D0B6))))
                drawRect(Color.White.copy(alpha = .68f), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * .62f), size = Size(size.width, size.height * .035f))
                drawRect(Color(0xFF75AAC8).copy(alpha = .48f), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * .49f), size = Size(size.width, size.height * .04f))
            }
            ReadyMomentMedia.CityNight -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF62528E), Color(0xFF24244F), Color(0xFF161B3D))))
                drawCircle(Color(0xFFF2DE9A), size.minDimension * .07f, androidx.compose.ui.geometry.Offset(size.width * .27f, size.height * .2f))
                drawCircle(Color(0xFF62528E), size.minDimension * .07f, androidx.compose.ui.geometry.Offset(size.width * .3f, size.height * .18f))
                repeat(8) { index ->
                    val buildingWidth = size.width / 9f
                    val buildingHeight = size.height * (.22f + (index % 4) * .055f)
                    val left = index * buildingWidth * 1.1f
                    drawRect(Color(0xFF20234B), androidx.compose.ui.geometry.Offset(left, size.height - buildingHeight), Size(buildingWidth, buildingHeight))
                    drawCircle(Color(0xFFE8B26F), 1.4.dp.toPx(), androidx.compose.ui.geometry.Offset(left + buildingWidth * .5f, size.height - buildingHeight * .55f))
                }
            }
            ReadyMomentMedia.Cozy -> {
                drawRect(Brush.linearGradient(listOf(Color(0xFFD7B6A4), Color(0xFFF0D9C5), Color(0xFF9A7569))))
                drawCircle(Color(0xFF574441).copy(alpha = .24f), size.minDimension * .3f, androidx.compose.ui.geometry.Offset(size.width * .86f, size.height * .2f))
                drawOval(Color(0xFF8B6255), topLeft = androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .58f), size = Size(size.width * .34f, size.height * .22f))
                drawArc(Color(0xFF8B6255), -80f, 190f, false, topLeft = androidx.compose.ui.geometry.Offset(size.width * .44f, size.height * .59f), size = Size(size.width * .18f, size.height * .17f), style = Stroke(width = 3.dp.toPx()))
                drawOval(Color(0xFFF4D8C5).copy(alpha = .5f), topLeft = androidx.compose.ui.geometry.Offset(size.width * .15f, size.height * .8f), size = Size(size.width * .52f, size.height * .06f))
            }
        }
    }
}

@Composable
private fun PrivacyOnboardingVisual(
    takeoverProgress: Float,
    contentVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val panelShape = RoundedCornerShape(28.dp)
    val surfaceProgress = smoothStep((takeoverProgress / .82f).coerceIn(0f, 1f))
    val reducedMotion = ReliveTheme.reduceMotion
    val motion = ReliveTheme.motion
    val lockProgress = remember { Animatable(0f) }
    val barProgress = List(3) { remember { Animatable(0f) } }
    val entranceReady = contentVisible && takeoverProgress >= .999f

    LaunchedEffect(entranceReady) {
        if (!entranceReady) {
            if (reducedMotion || lockProgress.value == 0f) {
                lockProgress.snapTo(0f)
                barProgress.forEach { it.snapTo(0f) }
                return@LaunchedEffect
            }
            barProgress.forEachIndexed { index, progress ->
                launch {
                    delay(index * 70L)
                    progress.animateTo(
                        0f,
                        spring(
                            dampingRatio = .92f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }
            delay(380)
            lockProgress.animateTo(
                0f,
                tween(270, easing = motion.easings.emphasizedDecelerate),
            )
            return@LaunchedEffect
        }
        if (reducedMotion) {
            lockProgress.snapTo(1f)
            barProgress.forEach { it.snapTo(1f) }
            return@LaunchedEffect
        }
        lockProgress.animateTo(
            1f,
            tween(420, easing = motion.easings.emphasizedDecelerate),
        )
        barProgress.forEachIndexed { index, progress ->
            launch {
                delay(index * 180L)
                progress.animateTo(
                    1f,
                    spring(
                        dampingRatio = .92f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
        }
    }
    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = if (surfaceProgress <= .001f) 0f else 1f
                shape = BottomUpRevealShape(surfaceProgress)
                clip = true
            }
            .shadow(20.dp, panelShape, ambientColor = Color.Black.copy(alpha = .22f))
            .clip(panelShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF35243F), Color(0xFF211828)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = .16f), panelShape)
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .semantics {
                contentDescription =
                    "Privacy: stored on your device, no account needed to begin, and you control what you keep"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (contentVisible) {
            Box(
                Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = .13f))
                    .graphicsLayer {
                        alpha = lockProgress.value
                        scaleX = lerp(.88f, 1f, lockProgress.value)
                        scaleY = scaleX
                        translationY = (1f - lockProgress.value) * 6.dp.toPx()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = OnboardingIcons.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val barTravel = with(LocalDensity.current) { (maxWidth + 18.dp).toPx() }
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrivacyBar(
                        "Stored on your device",
                        "Your archive begins locally.",
                        Color(0xFF6D435E),
                        Modifier.graphicsLayer {
                            translationX = -(1f - barProgress[0].value) * barTravel
                        },
                    )
                    PrivacyBar(
                        "No account needed to begin",
                        "Start without signing in.",
                        Color(0xFF514368),
                        Modifier.graphicsLayer {
                            translationX = (1f - barProgress[1].value) * barTravel
                        },
                    )
                    PrivacyBar(
                        "You control what you keep",
                        "Edit or forget your own moments.",
                        Color(0xFF493A59),
                        Modifier.graphicsLayer {
                            translationX = -(1f - barProgress[2].value) * barTravel
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyBar(
    title: String,
    caption: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(tint.copy(alpha = .72f))
            .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = ReliveTheme.typography.action,
            color = ReliveOnboardingColors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = caption,
            style = ReliveTheme.typography.onboardingBody.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = ReliveOnboardingColors.textSecondary,
        )
    }
}

@Composable
private fun OnboardingAtmosphereCanvas(progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(onboardingAtmosphereBrush(progress))
        val curve = Path().apply {
            moveTo(-size.width * .1f, size.height * .42f)
            cubicTo(size.width * .2f, size.height * .28f, size.width * .62f, size.height * .54f, size.width * 1.1f, size.height * .34f)
        }
        drawPath(
            curve,
            onboardingAtmosphereCurveColor(progress).copy(alpha = .16f),
            style = Stroke(1.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun CaptureHeader(
    enabled: Boolean,
    departure: Float,
    timelineBlend: Float,
    onSkip: () -> Unit,
) {
    val timelineColor = onboardingAtmosphereCurveColor(1f)
    Row(
        Modifier.fillMaxWidth().height(ReliveTheme.dimensions.onboarding.headerHeight)
            .background(
                Brush.verticalGradient(
                    listOf(
                        timelineColor.copy(alpha = 0f),
                        timelineColor.copy(alpha = .82f * timelineBlend),
                    ),
                ),
            )
            .departure(departure, 10.dp)
            .padding(horizontal = ReliveTheme.dimensions.onboarding.pageHorizontalPadding),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onSkip, enabled = enabled, modifier = Modifier.heightIn(min = ReliveTheme.dimensions.minTouchTarget)) {
            Text("Skip", style = ReliveTheme.typography.action, color = ReliveOnboardingColors.textSecondary)
        }
    }
}

@Composable
private fun CaptureCapabilities(
    values: List<Float>,
    visibility: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier.graphicsLayer { alpha = visibility }) {
        Capability(
            label = "Photo",
            value = values[0],
            rotation = -5f,
            driftX = -12.dp,
            surfaceTop = ReliveOnboardingColors.photoControlTop,
            surfaceBottom = ReliveOnboardingColors.photoControlBottom,
            modifier = Modifier.align(Alignment.Center).offset(x = (-115).dp, y = (-190).dp),
        ) {
            ImageGlyph(it, Color.White, ReliveTheme.dimensions.stroke.icon)
        }
        Capability(
            label = "Note",
            value = values[1],
            rotation = 4f,
            driftX = 10.dp,
            surfaceTop = ReliveOnboardingColors.noteControlTop,
            surfaceBottom = ReliveOnboardingColors.noteControlBottom,
            modifier = Modifier.align(Alignment.Center).offset(x = 115.dp, y = (-180).dp),
        ) {
            Icon(OnboardingIcons.Note, null, Modifier.size(it), tint = Color.White)
        }
        Capability(
            label = "Video",
            value = values[2],
            rotation = 3f,
            driftX = -9.dp,
            surfaceTop = ReliveOnboardingColors.videoControlTop,
            surfaceBottom = ReliveOnboardingColors.videoControlBottom,
            modifier = Modifier.align(Alignment.Center).offset(x = (-98).dp, y = 190.dp),
        ) {
            VideoGlyph(it, Color.White, ReliveTheme.dimensions.stroke.icon)
        }
        Capability(
            label = "Audio",
            value = values[3],
            rotation = -4f,
            driftX = 12.dp,
            surfaceTop = ReliveOnboardingColors.audioControlTop,
            surfaceBottom = ReliveOnboardingColors.audioControlBottom,
            modifier = Modifier.align(Alignment.Center).offset(x = 108.dp, y = 205.dp),
        ) {
            MicGlyph(it, Color.White, ReliveTheme.dimensions.stroke.icon)
        }
    }
}

@Composable
private fun Capability(
    label: String,
    value: Float,
    rotation: Float,
    driftX: Dp,
    surfaceTop: Color,
    surfaceBottom: Color,
    modifier: Modifier = Modifier,
    icon: @Composable (Dp) -> Unit,
) {
    val tileShape = RoundedCornerShape(17.dp)
    Box(
        modifier.size(66.dp)
            .graphicsLayer {
                alpha = value
                translationX = (1f - value) * driftX.toPx()
                translationY = (1f - value) * 22.dp.toPx()
                scaleX = .84f + value * .16f
                scaleY = scaleX
                rotationZ = rotation + (1f - value) * -rotation * 1.8f
            }
            .shadow(
                11.dp,
                tileShape,
                ambientColor = ReliveOnboardingColors.controlShadow,
                spotColor = ReliveOnboardingColors.controlShadow,
            )
            .clip(tileShape)
            .background(
                Brush.verticalGradient(
                    listOf(surfaceTop, surfaceBottom),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = .24f), tileShape)
            .semantics { contentDescription = label }
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        icon(ReliveTheme.dimensions.icon.lg)
    }
}

@Composable
private fun CaptureMomentPreview(
    mediaStore: MediaStore,
    entrance: Float,
    atmosphereProgress: Float,
    widthFraction: Float,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(
            LocalTimelineWallpaperPalette provides TimelineWallpaperPalette(
                ReliveTheme.colors.bgCanvas,
                ReliveTheme.colors.borderMuted,
            ),
        ) {
            FocusSizedMoment(
                modifier = Modifier.wrapContentHeight(unbounded = true)
                    .fillMaxWidth(widthFraction)
                    .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
                    .graphicsLayer {
                        alpha = entrance
                        translationY = (1f - entrance) * 12.dp.toPx()
                    },
            ) {
                TimelineMomentCard(
                    moment = ONBOARDING_RELIVE_MOMENT_PREVIEWS.single(),
                    mediaStore = mediaStore,
                    index = 0,
                    showTimelineChrome = false,
                    timelineChromeColor = onboardingAtmosphereChromeColor(atmosphereProgress),
                    modifier = Modifier.semantics {
                    contentDescription = "Example moment: A quiet morning at Mountain lake, 12 August 2026 at 8:24 AM"
                },
                )
            }
        }
    }
}

@Composable
private fun FocusSizedMoment(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val childWidth = (constraints.maxWidth / ONBOARDING_FOCUSED_MOMENT_SCALE).toInt()
        val placeable = measurables.single().measure(
            constraints.copy(minWidth = childWidth, maxWidth = childWidth),
        )
        val width = min(constraints.maxWidth, (placeable.width * ONBOARDING_FOCUSED_MOMENT_SCALE).toInt())
        val height = (placeable.height * ONBOARDING_FOCUSED_MOMENT_SCALE).toInt()
        layout(width, height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = ONBOARDING_FOCUSED_MOMENT_SCALE
                scaleY = ONBOARDING_FOCUSED_MOMENT_SCALE
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }
        }
    }
}

@Composable
private fun TimelineOnboardingVisual(
    mediaStore: MediaStore,
    timelineState: LazyListState,
    stage: OnboardingStage,
    focusBounds: Rect?,
    entryProgress: Float,
    momentWidthFraction: Float,
    sharedCardWidth: Dp?,
    focusTopInset: Dp?,
    onMomentBoundsChanged: (Int, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = timelineState,
        userScrollEnabled = stage == OnboardingStage.TimelineAutoScroll ||
            stage == OnboardingStage.TimelineIdle || stage == OnboardingStage.ReturningToFocus,
        // The entrance alpha has completed before suction begins. Avoid a second full-screen
        // render layer during the path clip so the timeline can stay cached on the GPU.
        modifier = modifier.fillMaxSize().then(
            if (entryProgress < .999f) Modifier.graphicsLayer { alpha = entryProgress } else Modifier,
        ),
        contentPadding = PaddingValues(
            start = ReliveTheme.dimensions.onboarding.pageHorizontalPadding,
            top = focusTopInset ?: ReliveTheme.dimensions.spacing.lg,
            end = ReliveTheme.dimensions.onboarding.pageHorizontalPadding,
            bottom = ReliveTheme.dimensions.spacing.lg,
        ),
    ) {
        itemsIndexed(
            ONBOARDING_TIMELINE_MOMENT_PREVIEWS,
            key = { _, moment -> moment.id.value },
        ) { index, moment ->
            TimelineMomentPreview(
                moment = moment,
                mediaStore = mediaStore,
                index = index,
                focusBounds = focusBounds,
                widthFraction = momentWidthFraction,
                sharedCardWidth = sharedCardWidth,
                // Keep the transferred timeline card present during the handoff so its
                // rail and first dot fade in with the Screen 1 card instead of arriving
                // a beat after it. The shared card remains above it until the handoff ends.
                visibility = 1f,
                timelineChromeColor = onboardingAtmosphereChromeColor(entryProgress),
                modifier = Modifier.onGloballyPositioned {
                    onMomentBoundsChanged(index, it.boundsInRoot())
                },
            )
        }
    }
}

private fun Modifier.timelineSuction(
    progress: Float,
    sheetBounds: Rect?,
    holeBounds: Rect?,
): Modifier = graphicsLayer {
    val source = sheetBounds
    val destination = holeBounds
    val localHole = if (source != null && destination != null) {
        Rect(
            left = destination.left - source.left,
            top = destination.top - source.top,
            right = destination.right - source.left,
            bottom = destination.bottom - source.top,
        )
    } else {
        null
    }
    compositingStrategy = CompositingStrategy.Offscreen
    shape = SuctionSheetShape(progress, localHole)
    clip = true
}

private class SuctionSheetShape(
    private val progress: Float,
    private val holeBounds: Rect?,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (progress <= 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        val deformation = onboardingSheetDeformation(progress)
        val hole = holeBounds ?: Rect(
            left = size.width * .18f,
            top = size.height * .58f,
            right = size.width * .82f,
            bottom = size.height * .84f,
        )
        val holeCenterX = hole.center.x.coerceIn(0f, size.width)
        val holeCenterY = hole.center.y.coerceIn(0f, size.height)
        val topInset = size.width * deformation.topInsetFraction
        val topY = lerp(0f, holeCenterY - hole.height * .12f, deformation.finalPull)
        val retreatY = lerp(size.height, holeCenterY - hole.height * .45f, deformation.retreat)
        val shoulderY = lerp(retreatY, holeCenterY, deformation.finalPull)
        val tailHalfWidth = size.width * deformation.tailWidthFraction
        val tailBottom = lerp(size.height, holeCenterY + hole.height * .18f, deformation.retreat)
        val leftTop = lerp(topInset, holeCenterX - tailHalfWidth, deformation.finalPull)
        val rightTop = lerp(size.width - topInset, holeCenterX + tailHalfWidth, deformation.finalPull)
        val path = Path().apply {
            moveTo(leftTop, topY)
            cubicTo(
                size.width * .28f,
                topY,
                size.width * .72f,
                topY,
                rightTop,
                topY,
            )
            cubicTo(
                lerp(size.width, holeCenterX + tailHalfWidth * 2.1f, deformation.finalPull),
                lerp(size.height * .30f, shoulderY, deformation.finalPull),
                holeCenterX + tailHalfWidth * 1.65f,
                shoulderY - (tailBottom - shoulderY) * .22f,
                holeCenterX + tailHalfWidth,
                tailBottom - (tailBottom - shoulderY) * .10f,
            )
            cubicTo(
                holeCenterX + tailHalfWidth * .72f,
                tailBottom,
                holeCenterX + tailHalfWidth * .30f,
                tailBottom,
                holeCenterX,
                tailBottom,
            )
            cubicTo(
                holeCenterX - tailHalfWidth * .30f,
                tailBottom,
                holeCenterX - tailHalfWidth * .72f,
                tailBottom,
                holeCenterX - tailHalfWidth,
                tailBottom - (tailBottom - shoulderY) * .10f,
            )
            cubicTo(
                holeCenterX - tailHalfWidth * 1.65f,
                shoulderY - (tailBottom - shoulderY) * .22f,
                lerp(0f, holeCenterX - tailHalfWidth * 2.1f, deformation.finalPull),
                lerp(size.height * .30f, shoulderY, deformation.finalPull),
                leftTop,
                topY,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

private class BottomUpRevealShape(private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val revealedHeight = size.height * progress.coerceIn(0f, 1f)
        val radius = with(density) { 20.dp.toPx() }.coerceAtMost(revealedHeight / 2f)
        return Outline.Rounded(
            RoundRect(
                rect = Rect(0f, size.height - revealedHeight, size.width, size.height),
                topLeft = CornerRadius(radius, radius),
                topRight = CornerRadius(radius, radius),
                bottomRight = CornerRadius(radius, radius),
                bottomLeft = CornerRadius(radius, radius),
            ),
        )
    }
}

@Composable
private fun Screen3TimelineCard(
    mediaStore: MediaStore,
    revealProgress: Float,
    thumbnailProgress: Float,
    onHoleBoundsChanged: (Rect) -> Unit,
    onCardBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = remember {
        TimelineHomeSummary(
            timeline = Timeline.Custom(
                id = TimelineId("onboarding-mountain-escapes"),
                name = "Mountain escapes",
            ),
            momentCount = 12,
            previewAttachments = emptyList(),
            createdAt = Instant(1),
        )
    }
    ReliveTheme(ReliveThemeId.PlumGold, ONBOARDING_DARK_MODE) {
        Box(
            modifier
                .fillMaxWidth()
                .padding(horizontal = ReliveTheme.dimensions.onboarding.pageHorizontalPadding)
                .graphicsLayer {
                    shape = BottomUpRevealShape(revealProgress)
                    clip = true
                },
        ) {
            TimelineHomeCard(
                summary = summary,
                mediaStore = mediaStore,
                onClick = {},
                interactive = false,
                previewMediaHeight = 256.dp,
                previewMetadata = "12 moments",
                previewTrailingMetadata = "Aug 2026",
                compactPreviewInfo = true,
                // The expanded Screen 3 card is 360dp tall: 256dp media plus the 104dp
                // bottom title and metadata strip, including compact vertical padding.
                previewInfoHeight = 96.dp,
                previewMediaContent = {
                    OnboardingTimelineHole(
                        thumbnailProgress = thumbnailProgress,
                        onBoundsChanged = onHoleBoundsChanged,
                    )
                },
                modifier = Modifier
                    .onGloballyPositioned { onCardBoundsChanged(it.boundsInRoot()) }
                    .semantics {
                        contentDescription = "Custom timeline: Mountain escapes, 12 moments, August 2026"
                    },
            )
        }
    }
}

@Composable
private fun OnboardingTimelineHole(
    thumbnailProgress: Float,
    onBoundsChanged: (Rect) -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) },
    ) {
        OnboardingHoleSurface(thumbnailProgress, Modifier.matchParentSize())
    }
}

@Composable
private fun OnboardingHoleSurface(thumbnailProgress: Float, modifier: Modifier = Modifier) {
    val holeShape = RoundedCornerShape(15.dp)
    Box(
        modifier
            .clip(holeShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF100B18), Color(0xFF241634)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = .08f), holeShape),
    ) {
        OnboardingTimelineThumbnail(
            progress = thumbnailProgress,
            modifier = Modifier.matchParentSize(),
        )
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = .42f), Color.Transparent),
                    endY = size.height * .36f,
                ),
                cornerRadius = CornerRadius(15.dp.toPx()),
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = .24f),
                cornerRadius = CornerRadius(15.dp.toPx()),
                style = Stroke(3.dp.toPx()),
            )
        }
    }
}

@Composable
private fun OnboardingTimelineThumbnail(progress: Float, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .blur(3.dp * (1f - progress.coerceIn(0f, 1f)))
            .graphicsLayer {
                alpha = progress.coerceIn(0f, 1f)
                scaleX = .94f + progress.coerceIn(0f, 1f) * .06f
                scaleY = scaleX
            },
    ) {
        drawRect(
            Brush.linearGradient(
                listOf(Color(0xFF5C3B78), Color(0xFFB078A7), Color(0xFFF0C264)),
            ),
        )
        drawCircle(
            color = Color(0xFFFFE6A8).copy(alpha = .72f),
            radius = size.minDimension * .12f,
            center = androidx.compose.ui.geometry.Offset(size.width * .78f, size.height * .24f),
        )
        val distantMountain = Path().apply {
            moveTo(0f, size.height * .74f)
            lineTo(size.width * .27f, size.height * .38f)
            lineTo(size.width * .48f, size.height * .67f)
            lineTo(size.width * .66f, size.height * .44f)
            lineTo(size.width, size.height * .76f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(distantMountain, Color(0xFF37234C).copy(alpha = .74f))
        val foreground = Path().apply {
            moveTo(0f, size.height * .82f)
            cubicTo(
                size.width * .24f,
                size.height * .68f,
                size.width * .44f,
                size.height * .96f,
                size.width * .65f,
                size.height * .78f,
            )
            cubicTo(
                size.width * .82f,
                size.height * .65f,
                size.width * .9f,
                size.height * .84f,
                size.width,
                size.height * .72f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(foreground, Color(0xFF21162E).copy(alpha = .9f))
    }
}

@Composable
private fun TimelineMomentPreview(
    moment: MomentPresentation,
    mediaStore: MediaStore,
    index: Int,
    focusBounds: Rect?,
    widthFraction: Float,
    sharedCardWidth: Dp?,
    timelineChromeColor: Color,
    visibility: Float = 1f,
    modifier: Modifier = Modifier,
) {
    var ownBounds by remember { mutableStateOf<Rect?>(null) }
    val naturalFocusTarget = ownBounds?.let { bounds ->
        val focus = focusBounds ?: return@let 0f
        onboardingMomentFocus(bounds.center.y, focus.center.y, focus.height)
    } ?: 0f
    val isQuietMorning = moment.id == ONBOARDING_RELIVE_MOMENT_PREVIEWS.single().id
    val focus = naturalFocusTarget
    CompositionLocalProvider(
        LocalTimelineWallpaperPalette provides TimelineWallpaperPalette(
            ReliveTheme.colors.bgCanvas,
            ReliveTheme.colors.borderMuted,
        ),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FocusSizedMoment(
                modifier = modifier.then(
                    if (sharedCardWidth != null) Modifier.width(sharedCardWidth)
                    else Modifier.fillMaxWidth(widthFraction),
                )
                    .onGloballyPositioned { ownBounds = it.boundsInRoot() }
                    .graphicsLayer { alpha = visibility },
            ) {
                TimelineMomentCard(
                    moment = moment,
                    mediaStore = mediaStore,
                    index = index,
                    modifier = Modifier.semantics {
                        contentDescription = if (isQuietMorning) {
                            "Example timeline moment: A quiet morning at Mountain lake"
                        } else {
                            "Example timeline moment: ${moment.title}"
                        }
                    },
                    focus = focus,
                    timelineChromeColor = timelineChromeColor,
                )
            }
        }
    }
}

@Composable
private fun TimelineMomentCard(
    moment: MomentPresentation,
    mediaStore: MediaStore,
    index: Int,
    modifier: Modifier = Modifier,
    focus: Float = 1f,
    showTimelineChrome: Boolean = true,
    timelineChromeColor: Color,
) {
    MomentCard(
        moment = moment,
        mediaStore = mediaStore,
        onToggleFavorite = null,
        onOpenMedia = { _, _ -> },
        canEditOrForget = false,
        onEdit = {},
        onForget = {},
        hasPreviousMoment = index > 0,
        hasNextMoment = index < ONBOARDING_TIMELINE_MOMENT_PREVIEWS.lastIndex,
        showTags = false,
        previewMediaContent = { OnboardingMomentPlaceholder(moment.id) },
        interactive = false,
        showTimelineChrome = showTimelineChrome,
        timelineChromeColor = timelineChromeColor,
        timelineMetadataColor = timelineChromeColor,
        previewPrintAspectRatio = 1f,
        modifier = modifier,
        printCardModifier = Modifier
            .blur(1.2.dp * (1f - focus))
            .graphicsLayer {
                val scale = onboardingMomentScale(focus) / ONBOARDING_FOCUSED_MOMENT_SCALE
                alpha = .68f + focus * .32f
                scaleX = scale
                scaleY = scale
            },
    )
}

@Composable
private fun OnboardingMomentPlaceholder(momentId: MomentId) {
    val palette = when (momentId.value) {
        "onboarding-quiet-morning" -> listOf(
            Color(0xFFE6C9D8),
            Color(0xFFD9AFC7),
            Color(0xFFC789AE),
            Color(0xFFF1DEE5),
        )
        "onboarding-forest-trails" -> listOf(
            Color(0xFFD7CCEA),
            Color(0xFFB8A5DC),
            Color(0xFF8F79C4),
            Color(0xFFE8E0F4),
        )
        "onboarding-coastal-escape" -> listOf(
            Color(0xFFD7D2F2),
            Color(0xFFB7ADE4),
            Color(0xFF8F82CF),
            Color(0xFFEDEAF9),
        )
        else -> listOf(
            Color(0xFFE5CEDC),
            Color(0xFFCDA8C1),
            Color(0xFFA77A9E),
            Color(0xFFF0DFE9),
        )
    }
    Column {
        listOf(.88f, .64f, .76f).forEachIndexed { index, width ->
            Box(
                Modifier
                    .fillMaxWidth(width)
                    .height(if (index == 0) 12.dp else 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette[1].copy(alpha = if (index == 0) .62f else .48f)),
            )
            if (index < 2) Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(28.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2.05f)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            drawRect(Brush.verticalGradient(listOf(palette[3], palette[0])))
            drawCircle(
                color = Color.White.copy(alpha = .48f),
                radius = size.minDimension * .105f,
                center = androidx.compose.ui.geometry.Offset(size.width * .76f, size.height * .23f),
            )
            val distantHill = Path().apply {
                moveTo(0f, size.height * .66f)
                cubicTo(
                    size.width * .20f,
                    size.height * .47f,
                    size.width * .28f,
                    size.height * .39f,
                    size.width * .47f,
                    size.height * .61f,
                )
                cubicTo(
                    size.width * .67f,
                    size.height * .83f,
                    size.width * .78f,
                    size.height * .45f,
                    size.width,
                    size.height * .62f,
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(distantHill, palette[1].copy(alpha = .72f))
            val foregroundHill = Path().apply {
                moveTo(0f, size.height * .76f)
                cubicTo(
                    size.width * .22f,
                    size.height * .64f,
                    size.width * .38f,
                    size.height * .91f,
                    size.width * .56f,
                    size.height * .82f,
                )
                cubicTo(
                    size.width * .73f,
                    size.height * .72f,
                    size.width * .84f,
                    size.height * .83f,
                    size.width,
                    size.height * .73f,
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(foregroundHill, palette[2].copy(alpha = .62f))
        }
    }
}

@Composable
private fun OnboardingBottomShell(
    captureHeadline: String,
    captureDescription: String,
    timelineHeadline: String,
    timelineDescription: String,
    customTimelineHeadline: String,
    customTimelineDescription: String,
    privacyHeadline: String,
    privacyDescription: String,
    readyHeadline: String,
    readyDescription: String,
    transition: Float,
    screen3Transition: Float,
    screen3CopyTransition: Float,
    screen3ControlsProgress: Float,
    privacyProgress: Float,
    readyProgress: Float,
    continueVisualAlpha: Float,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    continueEnabled: Boolean,
    showBack: Boolean,
    backEnabled: Boolean,
    finalAction: Boolean,
    modifier: Modifier = Modifier,
) {
    val readyCopyReveal = smoothStep(((readyProgress - .76f) / .24f).coerceIn(0f, 1f))
    Column(
        modifier.fillMaxWidth().padding(
            start = ReliveTheme.dimensions.onboarding.pageHorizontalPadding,
            end = ReliveTheme.dimensions.onboarding.pageHorizontalPadding,
            top = ReliveTheme.dimensions.spacing.xs,
            bottom = ReliveTheme.dimensions.spacing.sm,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        SegmentedProgress(transition + screen3Transition + privacyProgress + readyProgress)
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(84.dp)) {
            OnboardingCopy(
                captureHeadline,
                captureDescription,
                Modifier.fillMaxWidth().graphicsLayer {
                    alpha = 1f - transition
                    translationY = -transition * 6.dp.toPx()
                },
            )
            OnboardingCopy(
                timelineHeadline,
                timelineDescription,
                Modifier.fillMaxWidth().graphicsLayer {
                    alpha = transition * (1f - screen3CopyTransition * 2f).coerceIn(0f, 1f)
                    translationY = (1f - transition) * 6.dp.toPx() - screen3CopyTransition * 4.dp.toPx()
                },
            )
            OnboardingCopy(
                customTimelineHeadline,
                customTimelineDescription,
                Modifier.fillMaxWidth().graphicsLayer {
                    val reveal = ((screen3CopyTransition - .48f) / .52f).coerceIn(0f, 1f)
                    alpha = reveal * (1f - privacyProgress)
                    translationY = (1f - reveal) * 8.dp.toPx() - privacyProgress * 6.dp.toPx()
                },
            )
            if (privacyProgress > 0f) {
                OnboardingCopy(
                    privacyHeadline,
                    privacyDescription,
                    Modifier.fillMaxWidth().graphicsLayer {
                        alpha = privacyProgress * (1f - readyCopyReveal)
                        translationY = (1f - privacyProgress) * 8.dp.toPx()
                    },
                )
            }
            if (readyProgress > 0f) {
                OnboardingCopy(
                    readyHeadline,
                    readyDescription,
                    Modifier.fillMaxWidth().graphicsLayer {
                        alpha = readyCopyReveal
                        translationY = (1f - readyCopyReveal) * 8.dp.toPx()
                    },
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        ContinueButton(
            onClick = onContinue,
            enabled = continueEnabled,
            alpha = continueVisualAlpha,
            label = if (finalAction) "Start Reliving" else "Continue",
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
            val backAlpha = if (showBack) 1f else screen3ControlsProgress
            if (backAlpha > 0f) {
                TextButton(
                    onClick = onBack,
                    enabled = backEnabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ReliveOnboardingColors.textSecondary,
                        disabledContentColor = ReliveOnboardingColors.textSecondary,
                    ),
                    modifier = Modifier.graphicsLayer { alpha = backAlpha },
                ) {
                    Text(
                        "Back",
                        style = ReliveTheme.typography.action,
                        color = ReliveOnboardingColors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingCopy(headline: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            headline,
            style = ReliveTheme.typography.onboardingTitle.copy(fontSize = 23.sp, lineHeight = 28.sp),
            color = ReliveOnboardingColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(ReliveTheme.dimensions.spacing.xs))
        Text(
            description,
            style = ReliveTheme.typography.onboardingBody.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = ReliveOnboardingColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SegmentedProgress(transition: Float) {
    val chapterProgress = transition.coerceIn(0f, 4f)
    val activeChapter = chapterProgress.roundToInt()
    Row(
        Modifier.height(6.dp).semantics {
            val progress = (activeChapter + 1) / 5f
            contentDescription = "Onboarding progress ${(progress * 100).toInt()} percent"
            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f, 0)
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            Box(
                Modifier
                    .width(if (index == activeChapter) 30.dp else 6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (index == activeChapter) {
                            ReliveOnboardingColors.softWhiteSurface
                        } else {
                            ReliveOnboardingColors.quietProgress
                        },
                    ),
            )
        }
    }
}

@Composable
private fun ContinueButton(
    onClick: () -> Unit,
    enabled: Boolean,
    alpha: Float = 1f,
    label: String = "Continue",
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = rememberReliveHaptics()
    val scale = if (pressed && !ReliveTheme.reduceMotion) ReliveTheme.dimensions.onboarding.pressedScale else 1f
    Row(
        Modifier.fillMaxWidth().height(56.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }.clip(ReliveTheme.shapes.button).background(ReliveOnboardingColors.softWhiteSurface)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button) {
                haptics.perform(ReliveHapticCue.Action)
                onClick()
            }.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = ReliveTheme.typography.onboardingAction, color = ReliveOnboardingColors.darkBase)
        Spacer(Modifier.width(ReliveTheme.dimensions.spacing.sm))
        Text("→", style = ReliveTheme.typography.onboardingAction, color = ReliveOnboardingColors.darkBase)
    }
}

private fun Modifier.departure(value: Float, travel: Dp) = graphicsLayer {
    alpha = 1f - value
    translationY = -value * travel.toPx()
    scaleX = 1f - value * .015f
    scaleY = scaleX
}
