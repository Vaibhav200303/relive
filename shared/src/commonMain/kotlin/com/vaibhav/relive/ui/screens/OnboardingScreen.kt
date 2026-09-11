package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.onboarding.ONBOARDING_PAGE_COUNT
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.rememberGrainBrush
import com.vaibhav.relive.ui.theme.spec
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import relive.shared.generated.resources.Res
import relive.shared.generated.resources.onboarding_capture_v2
import relive.shared.generated.resources.onboarding_private
import relive.shared.generated.resources.onboarding_rediscover
import relive.shared.generated.resources.onboarding_timeline_v2

private data class OnboardingPage(
    val hero: DrawableResource,
    val title: String,
    val body: String,
    val action: String,
)

private val OnboardingPages = listOf(
    OnboardingPage(
        hero = Res.drawable.onboarding_capture_v2,
        title = "Capture moments. Relive them later.",
        body = "Keep thoughts, photos, videos, and sounds together in one beautiful archive.",
        action = "Begin",
    ),
    OnboardingPage(
        hero = Res.drawable.onboarding_timeline_v2,
        title = "Your story, in one timeline.",
        body = "Every moment joins All moments. Create timelines for the chapters you want to revisit.",
        action = "Next",
    ),
    OnboardingPage(
        hero = Res.drawable.onboarding_private,
        title = "Private by design.",
        body = "Your archive stays on this device. No account, no social feed. Add App Lock whenever you want.",
        action = "Next",
    ),
    OnboardingPage(
        hero = Res.drawable.onboarding_rediscover,
        title = "The past finds its way back.",
        body = "Favourites, On This Day, and moments from your past make remembering feel effortless.",
        action = "Start your archive",
    ),
)

@Composable
fun OnboardingScreen(
    onFinish: suspend () -> Boolean,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val isDark = ReliveTheme.isDark
    val scope = rememberCoroutineScope()
    var pageIndex by remember { mutableIntStateOf(0) }
    var finishing by remember { mutableStateOf(false) }

    ReliveBackHandler(enabled = pageIndex > 0 && !finishing) {
        pageIndex -= 1
    }

    val finish: () -> Unit = {
        if (!finishing) {
            finishing = true
            scope.launch {
                if (!onFinish()) finishing = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(colors.canvasBrush())) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = rememberGrainBrush(isDark),
                    alpha = if (isDark) 0.07f else 0.05f,
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = dims.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Relive",
                style = type.wordmark,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = dims.spacing.sm, bottom = dims.spacing.xs),
            )
            OnboardingProgress(pageIndex)
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    val exitSpec = motion.spec<Float>(
                        reduceMotion = reduceMotion,
                        full = tween(
                            durationMillis = motion.durations.short4,
                            easing = motion.easings.emphasizedAccelerate,
                        ),
                    )
                    val enterSpec = motion.spec<Float>(
                        reduceMotion = reduceMotion,
                        full = tween(
                            durationMillis = motion.durations.medium2,
                            delayMillis = motion.durations.short4,
                            easing = motion.easings.emphasizedDecelerate,
                        ),
                        reduced = tween(
                            durationMillis = motion.durations.short3,
                            delayMillis = motion.durations.short3,
                            easing = motion.easings.standard,
                        ),
                    )
                    fadeIn(enterSpec) togetherWith fadeOut(exitSpec)
                },
                label = "onboarding page",
                modifier = Modifier.weight(1f),
            ) { targetPage ->
                OnboardingPageContent(OnboardingPages[targetPage])
            }
            OnboardingPrimaryAction(
                label = OnboardingPages[pageIndex].action,
                enabled = !finishing,
                onClick = {
                    if (pageIndex == OnboardingPages.lastIndex) finish() else pageIndex += 1
                },
            )
            Box(
                modifier = Modifier.heightIn(min = dims.minTouchTarget),
                contentAlignment = Alignment.Center,
            ) {
                if (pageIndex < OnboardingPages.lastIndex) {
                    TextButton(
                        onClick = finish,
                        enabled = !finishing,
                        modifier = Modifier.heightIn(min = dims.minTouchTarget),
                    ) {
                        Text(
                            text = "Skip",
                            style = type.action,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(dims.spacing.sm))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val entrance = remember(page.hero) { Animatable(0f) }
    LaunchedEffect(page.hero) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = motion.spec(
                reduceMotion = reduceMotion,
                full = tween(
                    durationMillis = motion.durations.medium4,
                    easing = motion.easings.emphasizedDecelerate,
                ),
            ),
        )
    }
    val idle = if (!reduceMotion) {
        val transition = rememberInfiniteTransition(label = "onboarding hero idle")
        val value by transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = motion.durations.extraLong1 * 2,
                    easing = motion.easings.standard,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "onboarding hero drift",
        )
        value
    } else {
        0f
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heroSize = if (maxHeight < dims.onboarding.compactHeightThreshold) {
            dims.onboarding.heroCompact
        } else {
            dims.onboarding.heroLarge
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(heroSize),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(heroSize * dims.onboarding.heroShadowWidthFraction)
                        .height(dims.onboarding.heroShadowHeight)
                        .blur(
                            radius = dims.onboarding.heroShadowBlur,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded,
                        )
                        .background(colors.shadow.copy(alpha = 0.22f), CircleShape)
                        .graphicsLayer { alpha = entrance.value },
                )
                Image(
                    painter = painterResource(page.hero),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = entrance.value
                            val scale = dims.onboarding.heroInitialScale +
                                (1f - dims.onboarding.heroInitialScale) * entrance.value
                            scaleX = scale
                            scaleY = scale
                            translationY =
                                (1f - entrance.value) * dims.onboarding.heroEntranceTravel.toPx() +
                                    idle * dims.onboarding.heroIdleTravel.toPx()
                            rotationX = if (reduceMotion) {
                                0f
                            } else {
                                (1f - entrance.value) * dims.onboarding.heroEntranceTiltX
                            }
                            rotationY = if (reduceMotion) 0f else idle * dims.onboarding.heroIdleTiltY
                            rotationZ = if (reduceMotion) 0f else idle * dims.onboarding.heroIdleRotationZ
                            cameraDistance = 24f * density
                        },
                )
            }
            Spacer(Modifier.height(dims.spacing.sm))
            Text(
                text = page.title,
                style = ReliveTheme.typography.onboardingTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = dims.onboarding.copyMaxWidth)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(dims.spacing.sm))
            Text(
                text = page.body,
                style = ReliveTheme.typography.onboardingBody,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = dims.onboarding.copyMaxWidth)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(dims.spacing.lg))
        }
    }
}

@Composable
private fun OnboardingProgress(pageIndex: Int) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val progress by animateFloatAsState(
        targetValue = pageIndex.toFloat() / (ONBOARDING_PAGE_COUNT - 1),
        animationSpec = motion.spec(
            reduceMotion = ReliveTheme.reduceMotion,
            full = tween(
                durationMillis = motion.durations.medium2,
                easing = motion.easings.standard,
            ),
        ),
        label = "onboarding progress",
    )
    Canvas(
        modifier = Modifier
            .width(dims.onboarding.progressWidth)
            .height(dims.onboarding.progressHeight)
            .semantics {
                contentDescription = "Onboarding page ${pageIndex + 1} of $ONBOARDING_PAGE_COUNT"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = pageIndex.toFloat(),
                    range = 0f..(ONBOARDING_PAGE_COUNT - 1).toFloat(),
                    steps = ONBOARDING_PAGE_COUNT - 2,
                )
            },
    ) {
        val start = dims.onboarding.progressDotActive.toPx() / 2f
        val end = size.width - start
        val y = size.height / 2f
        drawLine(
            color = colors.borderMuted,
            start = androidx.compose.ui.geometry.Offset(start, y),
            end = androidx.compose.ui.geometry.Offset(end, y),
            strokeWidth = dims.stroke.hairline.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.accent,
            start = androidx.compose.ui.geometry.Offset(start, y),
            end = androidx.compose.ui.geometry.Offset(start + (end - start) * progress, y),
            strokeWidth = dims.stroke.icon.toPx(),
            cap = StrokeCap.Round,
        )
        repeat(ONBOARDING_PAGE_COUNT) { index ->
            val active = index <= pageIndex
            val x = start + (end - start) * index / (ONBOARDING_PAGE_COUNT - 1)
            drawCircle(
                color = if (active) colors.accent else colors.bgCanvas,
                radius = (if (active) dims.onboarding.progressDotActive else dims.onboarding.progressDot).toPx() / 2f,
                center = androidx.compose.ui.geometry.Offset(x, y),
            )
            if (!active) {
                drawCircle(
                    color = colors.border,
                    radius = dims.onboarding.progressDot.toPx() / 2f,
                    center = androidx.compose.ui.geometry.Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(dims.stroke.hairline.toPx()),
                )
            }
        }
    }
}

@Composable
private fun OnboardingPrimaryAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val haptics = rememberReliveHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !ReliveTheme.reduceMotion) dims.onboarding.pressedScale else 1f,
        animationSpec = motion.spec(
            reduceMotion = ReliveTheme.reduceMotion,
            full = tween(
                durationMillis = motion.durations.short2,
                easing = motion.easings.standard,
            ),
        ),
        label = "onboarding action press",
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.6f
            }
            .widthIn(min = dims.onboarding.primaryActionWidth)
            .heightIn(min = dims.minTouchTarget)
            .clip(RoundedCornerShape(dims.radii.full))
            .background(colors.accent)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
            ) {
                haptics.perform(ReliveHapticCue.Action)
                onClick()
            }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ReliveTheme.typography.onboardingAction,
            color = colors.textOnAccent,
        )
    }
}
