package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.onboarding.ONBOARDING_PAGE_COUNT
import com.vaibhav.relive.presentation.onboarding.OnboardingCommand
import com.vaibhav.relive.presentation.onboarding.OnboardingPage
import com.vaibhav.relive.presentation.onboarding.OnboardingState
import com.vaibhav.relive.presentation.timeline.MomentPresentation
import com.vaibhav.relive.ui.components.timeline.MomentCard
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.icons.CameraIcons
import com.vaibhav.relive.ui.icons.OnboardingIcons
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.ReliveThemeId
import com.vaibhav.relive.ui.theme.rememberGrainBrush
import com.vaibhav.relive.ui.theme.spec
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import relive.shared.generated.resources.Res
import relive.shared.generated.resources.onboarding_capture_fullscreen_v3
import relive.shared.generated.resources.onboarding_final_fullscreen_v3
import relive.shared.generated.resources.onboarding_organize_fullscreen_v2
import relive.shared.generated.resources.onboarding_organize_coffee
import relive.shared.generated.resources.onboarding_organize_mountains
import relive.shared.generated.resources.onboarding_organize_spring
import relive.shared.generated.resources.onboarding_private_fullscreen_v1
import relive.shared.generated.resources.onboarding_relive_moments_scroll_background_v1
import relive.shared.generated.resources.onboarding_welcome_reference_v2

private const val FEATURE_PAGE_COUNT = ONBOARDING_PAGE_COUNT - 1
internal val ONBOARDING_THEME_ID = ReliveThemeId.Sunset
internal const val ONBOARDING_DARK_MODE = false

/**
 * The supplied onboarding reference uses a quiet ivory paper canvas rather than Sunset's blush
 * canvas. Keep this exception local to onboarding so the selected app palette remains unchanged
 * everywhere else.
 */
internal val ONBOARDING_REFERENCE_CANVAS = Color(0xFFF7F4F1)
private val OnboardingReferenceCanvasBrush = Brush.linearGradient(
    0f to Color(0xFFFBF9F6),
    0.58f to ONBOARDING_REFERENCE_CANVAS,
    1f to Color(0xFFF4F1EE),
)

private data class FeatureCopy(
    val number: Int,
    val title: String,
    val body: String,
)

private val FeatureCopyByPage = mapOf(
    OnboardingPage.Capture to FeatureCopy(
        number = 1,
        title = "Capture\neasily",
        body = "Save photos, videos, notes and\nmore — whenever life happens.",
    ),
    OnboardingPage.Organize to FeatureCopy(
        number = 2,
        title = "Organize\nbeautifully",
        body = "Turn moments into timelines.\nGroup, tag, and relive them your way.",
    ),
    OnboardingPage.ReliveMoments to FeatureCopy(
        number = 3,
        title = "Relive your\nmoments",
        body = "Every memory becomes a warm card\nyou can scroll later to revisit what\nmattered.",
    ),
    OnboardingPage.Private to FeatureCopy(
        number = 4,
        title = "Private\nby design",
        body = "Your memories are stored locally\non your device. You’re always in control.",
    ),
)

internal val ONBOARDING_RELIVE_MOMENT_PREVIEWS = listOf(
    MomentPresentation(
        id = MomentId("onboarding-sunday-breakfast"),
        createdAt = Instant(0L),
        updatedAt = null,
        formattedDate = "7 MAY 2022",
        formattedTime = "9:12 AM",
        title = "Sunday breakfast",
        content = "A slow morning, warm coffee, and nowhere to rush.",
        locationLabel = "At home",
        location = null,
        isFavorite = true,
        feeling = MomentFeeling.Great,
        tags = listOf(Tag.of("family"), Tag.of("food")),
        attachments = emptyList(),
    ),
    MomentPresentation(
        id = MomentId("onboarding-weekend-hike"),
        createdAt = Instant(1L),
        updatedAt = null,
        formattedDate = "21 AUG 2023",
        formattedTime = "4:40 PM",
        title = "Weekend hike",
        content = "Fresh air, good company, and views that made everything feel lighter.",
        locationLabel = "Mountain trail",
        location = null,
        isFavorite = false,
        feeling = MomentFeeling.Good,
        tags = listOf(Tag.of("outdoors"), Tag.of("life")),
        attachments = emptyList(),
    ),
    MomentPresentation(
        id = MomentId("onboarding-golden-hour"),
        createdAt = Instant(2L),
        updatedAt = null,
        formattedDate = "2 MAR 2025",
        formattedTime = "6:18 PM",
        title = "Spring light",
        content = "The blossoms arrived all at once, softening the whole afternoon.",
        locationLabel = "In the garden",
        location = null,
        isFavorite = true,
        feeling = MomentFeeling.Great,
        tags = listOf(Tag.of("spring")),
        attachments = emptyList(),
    ),
)

private val OnboardingReliveMomentMedia = listOf(
    Res.drawable.onboarding_organize_coffee,
    Res.drawable.onboarding_organize_mountains,
    Res.drawable.onboarding_organize_spring,
)

@Composable
fun OnboardingScreen(
    mediaStore: MediaStore,
    onFinish: suspend () -> Boolean,
) {
    ReliveTheme(themeId = ONBOARDING_THEME_ID, darkMode = ONBOARDING_DARK_MODE) {
        OnboardingContent(mediaStore, onFinish)
    }
}

@Composable
private fun OnboardingContent(
    mediaStore: MediaStore,
    onFinish: suspend () -> Boolean,
) {
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(OnboardingState()) }
    var finishing by remember { mutableStateOf(false) }

    ReliveBackHandler(enabled = !state.isFirstPage && !finishing) {
        state = state.previous()
    }

    val finish: () -> Unit = {
        if (!finishing) {
            finishing = true
            scope.launch {
                if (!onFinish()) finishing = false
            }
        }
    }
    val primaryAction: () -> Unit = {
        when (state.primaryCommand) {
            OnboardingCommand.Advance -> state = state.next()
            OnboardingCommand.Complete -> finish()
        }
    }

    Box(Modifier.fillMaxSize().background(OnboardingReferenceCanvasBrush)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(brush = rememberGrainBrush(isDark = false), alpha = 0.04f),
        )
        AnimatedContent(
            targetState = state.pageIndex,
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
            modifier = Modifier.fillMaxSize(),
        ) { targetIndex ->
            val targetState = OnboardingState(targetIndex)
            when (targetState.page) {
                OnboardingPage.Welcome -> WelcomePage(
                    enabled = !finishing,
                    onNext = primaryAction,
                )
                OnboardingPage.Capture -> CapturePage(
                    page = targetState,
                    enabled = !finishing,
                    onSkip = finish,
                    onNext = primaryAction,
                )
                OnboardingPage.Organize -> OrganizePage(
                    page = targetState,
                    enabled = !finishing,
                    onSkip = finish,
                    onNext = primaryAction,
                )
                OnboardingPage.ReliveMoments -> ReliveMomentsPage(
                    page = targetState,
                    mediaStore = mediaStore,
                    enabled = !finishing,
                    onSkip = finish,
                    onNext = primaryAction,
                )
                OnboardingPage.Private -> PrivatePage(
                    page = targetState,
                    enabled = !finishing,
                    onSkip = finish,
                    onNext = primaryAction,
                )
                OnboardingPage.Final -> FinalPage(
                    enabled = !finishing,
                    onFinish = primaryAction,
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(
    enabled: Boolean,
    onNext: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val type = ReliveTheme.typography
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.onboarding_welcome_reference_v2),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            alpha = 0.84f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .background(
                    Brush.verticalGradient(
                        listOf(ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.96f), Color.Transparent),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = dims.onboarding.pageHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(128.dp))
                Text(
                    text = "Relive",
                    style = type.onboardingWelcomeTitle,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "S M A L L  M O M E N T S.\nA  F U L L E R  Y O U.",
                    style = type.eyebrow.copy(
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                    ),
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(312.dp))
                Text(
                    text = "Memories\nmake life\nricher  ♡",
                    style = type.onboardingHandwritten.copy(
                        fontSize = 36.sp,
                        lineHeight = 42.sp,
                    ),
                    color = colors.accent.copy(alpha = 0.76f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .offset(x = dims.spacing.lg)
                        .graphicsLayer { rotationZ = -7f },
                )
                Spacer(Modifier.height(dims.spacing.xl))
            }
            OnboardingPrimaryAction(
                label = "Get started  →",
                enabled = enabled,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(0.88f),
            )
            Text(
                text = "Your memories stay yours.",
                style = type.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = dims.spacing.md),
            )
        }
    }
}

@Composable
private fun OnboardingHeader(
    number: Int,
    showSkip: Boolean,
    enabled: Boolean,
    onSkip: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier.fillMaxWidth().height(dims.onboarding.headerHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$number / $FEATURE_PAGE_COUNT",
            style = ReliveTheme.typography.action,
            color = colors.accent,
        )
        if (showSkip) {
            TextButton(
                onClick = onSkip,
                enabled = enabled,
                modifier = Modifier.heightIn(min = dims.minTouchTarget),
            ) {
                Text("Skip", style = ReliveTheme.typography.action, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun CapturePage(
    page: OnboardingState,
    enabled: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val copy = requireNotNull(FeatureCopyByPage[page.page])
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val type = ReliveTheme.typography
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.onboarding_capture_fullscreen_v3),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.62f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Text(
            text = "Good food.\nBrighter days.  ♡",
            style = type.onboardingHandwritten.copy(fontSize = 13.sp, lineHeight = 16.sp),
            color = colors.textSecondary,
            modifier = Modifier
                .offset(x = maxWidth * 0.25f, y = maxHeight * 0.555f)
                .graphicsLayer { rotationZ = -8f },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = dims.onboarding.pageHorizontalPadding),
        ) {
            Spacer(Modifier.height(dims.onboarding.numberedPageTopPadding))
            OnboardingHeader(
                number = copy.number,
                showSkip = page.canSkip,
                enabled = enabled,
                onSkip = onSkip,
            )
            Text(copy.title, style = type.onboardingTitle, color = colors.textPrimary)
            Spacer(Modifier.height(dims.spacing.xs))
            Text(copy.body, style = type.onboardingBody, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.onboarding.captureFeatureHeight),
                horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
            ) {
                CaptureFeature(ProfileIcons.Media, "Photos", Modifier.weight(1f))
                CaptureFeature(CameraIcons.Videocam, "Videos", Modifier.weight(1f))
                CaptureFeature(OnboardingIcons.Note, "Notes", Modifier.weight(1f))
                CaptureFeature(ProfileIcons.Location, "Location", Modifier.weight(1f))
            }
            Spacer(Modifier.height(dims.spacing.sm))
            OnboardingBottomControls(
                activeIndex = copy.number - 1,
                enabled = enabled,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun CaptureFeature(icon: ImageVector, label: String, modifier: Modifier) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(dims.onboarding.featureTileRadius))
            .background(colors.surfaceCard.copy(alpha = 0.74f))
            .padding(vertical = dims.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(dims.onboarding.captureIconMedallionSize)
                .clip(CircleShape)
                .background(colors.tint.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(dims.onboarding.captureIconSize),
            )
        }
        Text(label, style = ReliveTheme.typography.caption, color = colors.textPrimary)
    }
}

@Composable
private fun OrganizePage(
    page: OnboardingState,
    enabled: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val copy = requireNotNull(FeatureCopyByPage[page.page])
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val type = ReliveTheme.typography
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.onboarding_organize_fullscreen_v2),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.34f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.52f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Text(
            text = "Same\nmoments,\nmore meaning\n♡",
            style = type.onboardingHandwritten.copy(fontSize = 16.sp, lineHeight = 19.sp),
            color = colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(maxWidth * 0.29f)
                .offset(x = maxWidth * 0.68f, y = maxHeight * 0.145f)
                .graphicsLayer { rotationZ = -8f },
        )
        Canvas(
            modifier = Modifier
                .size(width = 42.dp, height = 32.dp)
                .offset(x = maxWidth * 0.73f, y = maxHeight * 0.292f),
        ) {
            val ink = colors.accent.copy(alpha = 0.82f)
            drawLine(ink, start = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 22.dp.toPx()), end = androidx.compose.ui.geometry.Offset(0f, 5.dp.toPx()), strokeWidth = 2.dp.toPx())
            drawLine(ink, start = androidx.compose.ui.geometry.Offset(18.dp.toPx(), 19.dp.toPx()), end = androidx.compose.ui.geometry.Offset(25.dp.toPx(), 2.dp.toPx()), strokeWidth = 2.dp.toPx())
            drawLine(ink, start = androidx.compose.ui.geometry.Offset(28.dp.toPx(), 27.dp.toPx()), end = androidx.compose.ui.geometry.Offset(40.dp.toPx(), 19.dp.toPx()), strokeWidth = 2.dp.toPx())
        }
        Text(
            text = "Growing days",
            style = type.title.copy(fontSize = 22.sp, lineHeight = 26.sp),
            color = colors.textPrimary,
            modifier = Modifier
                .offset(x = maxWidth * 0.105f, y = maxHeight * 0.663f)
                .graphicsLayer { rotationZ = -3.5f },
        )
        Text(
            text = "4 moments",
            style = type.caption.copy(fontSize = 12.sp),
            color = colors.textSecondary,
            modifier = Modifier
                .offset(x = maxWidth * 0.105f, y = maxHeight * 0.704f)
                .graphicsLayer { rotationZ = -3.5f },
        )
        Text(
            text = "4 September 2026",
            style = type.caption.copy(fontSize = 12.sp),
            color = colors.textSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(maxWidth * 0.36f)
                .offset(x = maxWidth * 0.34f, y = maxHeight * 0.704f)
                .graphicsLayer { rotationZ = -3.5f },
        )
        Canvas(
            modifier = Modifier
                .size(width = 66.dp, height = 54.dp)
                .offset(x = maxWidth * 0.255f, y = maxHeight * 0.758f),
        ) {
            val ink = colors.accent.copy(alpha = 0.86f)
            val curve = Path().apply {
                moveTo(size.width * 0.92f, size.height * 0.86f)
                cubicTo(
                    size.width * 0.53f,
                    size.height * 0.82f,
                    size.width * 0.18f,
                    size.height * 0.58f,
                    size.width * 0.16f,
                    size.height * 0.14f,
                )
            }
            drawPath(
                path = curve,
                color = ink,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
            drawLine(
                color = ink,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.14f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.12f, size.height * 0.42f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = ink,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.14f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.37f, size.height * 0.29f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = "Create multiple timelines\nfor different chapters of your life",
            style = type.onboardingHandwritten.copy(fontSize = 14.sp, lineHeight = 17.sp),
            color = colors.textSecondary,
            modifier = Modifier
                .width(maxWidth * 0.57f)
                .offset(x = maxWidth * 0.41f, y = maxHeight * 0.79f)
                .graphicsLayer { rotationZ = -5f },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = dims.onboarding.pageHorizontalPadding),
        ) {
            Spacer(Modifier.height(dims.onboarding.numberedPageTopPadding))
            OnboardingHeader(
                number = copy.number,
                showSkip = page.canSkip,
                enabled = enabled,
                onSkip = onSkip,
            )
            Text(copy.title, style = type.onboardingTitle, color = colors.textPrimary)
            Spacer(Modifier.height(dims.spacing.xs))
            Text(copy.body, style = type.onboardingBody, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            OnboardingBottomControls(
                activeIndex = copy.number - 1,
                enabled = enabled,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun ReliveMomentsPage(
    page: OnboardingState,
    mediaStore: MediaStore,
    enabled: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val copy = requireNotNull(FeatureCopyByPage[page.page])
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val type = ReliveTheme.typography
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.onboarding_relive_moments_scroll_background_v1),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.34f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.58f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Text(
            text = "Your\nmemories,\nall in one\ntimeline  ♡",
            style = type.onboardingHandwritten.copy(fontSize = 15.sp, lineHeight = 18.sp),
            color = colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(maxWidth * 0.22f)
                .offset(x = maxWidth * 0.76f, y = maxHeight * 0.145f)
                .graphicsLayer { rotationZ = -7f },
        )
        OnboardingMomentScrollPreview(
            mediaStore = mediaStore,
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.37f)
                .offset(y = maxHeight * 0.34f),
        )
        Canvas(
            modifier = Modifier
                .size(width = 58.dp, height = 80.dp)
                .offset(x = maxWidth * 0.75f, y = maxHeight * 0.245f),
        ) {
            val ink = colors.accent.copy(alpha = 0.86f)
            val curve = Path().apply {
                moveTo(size.width * 0.82f, size.height * 0.04f)
                cubicTo(
                    size.width * 0.86f,
                    size.height * 0.42f,
                    size.width * 0.58f,
                    size.height * 0.72f,
                    size.width * 0.19f,
                    size.height * 0.91f,
                )
            }
            drawPath(curve, ink, style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round))
            drawLine(
                color = ink,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.19f, size.height * 0.91f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.69f),
                strokeWidth = 1.7.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = ink,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.19f, size.height * 0.91f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.86f),
                strokeWidth = 1.7.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Canvas(
            modifier = Modifier
                .size(width = 48.dp, height = 76.dp)
                .offset(x = maxWidth * 0.73f, y = maxHeight * 0.712f),
        ) {
            val ink = colors.accent.copy(alpha = 0.82f)
            val curve = Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.05f)
                cubicTo(
                    size.width * 0.65f,
                    size.height * 0.28f,
                    size.width * 0.61f,
                    size.height * 0.62f,
                    size.width * 0.35f,
                    size.height * 0.91f,
                )
            }
            drawPath(curve, ink, style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round))
            drawLine(
                color = ink,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.91f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.29f, size.height * 0.72f),
                strokeWidth = 1.7.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = ink,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.91f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.53f, size.height * 0.80f),
                strokeWidth = 1.7.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = "Scroll\nto relive\nmore  ♡",
            style = type.onboardingHandwritten.copy(fontSize = 14.sp, lineHeight = 17.sp),
            color = colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(maxWidth * 0.18f)
                .offset(x = maxWidth * 0.80f, y = maxHeight * 0.755f)
                .graphicsLayer { rotationZ = -6f },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = dims.onboarding.pageHorizontalPadding),
        ) {
            Spacer(Modifier.height(dims.onboarding.numberedPageTopPadding))
            OnboardingHeader(
                number = copy.number,
                showSkip = page.canSkip,
                enabled = enabled,
                onSkip = onSkip,
            )
            Row(
                modifier = Modifier.width(220.dp).height(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(FEATURE_PAGE_COUNT) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (index < copy.number) colors.accent else colors.borderMuted),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("Relive your", style = type.onboardingTitle, color = colors.textPrimary)
            Text(
                "moments",
                style = type.onboardingTitle.copy(fontStyle = FontStyle.Italic),
                color = colors.accent,
                modifier = Modifier.offset(y = (-5).dp),
            )
            Text(
                copy.body,
                style = type.onboardingBody,
                color = colors.textSecondary,
                modifier = Modifier.offset(y = (-4).dp),
            )
            Spacer(Modifier.weight(1f))
            OnboardingBottomControls(
                activeIndex = copy.number - 1,
                enabled = enabled,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun OnboardingMomentScrollPreview(
    mediaStore: MediaStore,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .clipToBounds()
            .semantics { contentDescription = "Scrollable preview of three Relive moments" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(end = dims.onboarding.pageHorizontalPadding),
        ) {
            ONBOARDING_RELIVE_MOMENT_PREVIEWS.forEachIndexed { index, moment ->
                MomentCard(
                    moment = moment,
                    mediaStore = mediaStore,
                    onToggleFavorite = null,
                    onOpenMedia = { _, _ -> },
                    canEditOrForget = false,
                    onEdit = {},
                    onForget = {},
                    hasPreviousMoment = index > 0,
                    hasNextMoment = index < ONBOARDING_RELIVE_MOMENT_PREVIEWS.lastIndex,
                    modifier = Modifier.fillMaxWidth(),
                    previewMediaContent = {
                        OnboardingMomentPhoto(OnboardingReliveMomentMedia[index])
                    },
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.82f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(18.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.9f)),
                    ),
                ),
        )
    }
}

@Composable
private fun OnboardingMomentPhoto(resource: DrawableResource) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Image(
        painter = painterResource(resource),
        contentDescription = "Sample Moment photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(dims.radii.medium))
            .border(
                width = dims.media.collageBorder,
                color = colors.accent,
                shape = RoundedCornerShape(dims.radii.medium),
            ),
    )
}

@Composable
private fun PrivatePage(
    page: OnboardingState,
    enabled: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val copy = requireNotNull(FeatureCopyByPage[page.page])
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val type = ReliveTheme.typography
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.onboarding_private_fullscreen_v1),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.33f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.42f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Text(
            text = "Your\nmemories.\nYour control\n♡",
            style = type.onboardingHandwritten.copy(fontSize = 14.sp, lineHeight = 17.sp),
            color = colors.accent.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(maxWidth * 0.19f)
                .offset(x = maxWidth * 0.79f, y = maxHeight * 0.355f)
                .graphicsLayer { rotationZ = -7f },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = dims.onboarding.pageHorizontalPadding),
        ) {
            Spacer(Modifier.height(dims.onboarding.numberedPageTopPadding))
            OnboardingHeader(
                number = copy.number,
                showSkip = page.canSkip,
                enabled = enabled,
                onSkip = onSkip,
            )
            Text(copy.title, style = type.onboardingTitle, color = colors.textPrimary)
            Spacer(Modifier.height(dims.spacing.xs))
            Text(copy.body, style = type.onboardingBody, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(122.dp),
                horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
            ) {
                PrivacyFeature(ProfileIcons.Phone, "Stored\non your device", Modifier.weight(1f))
                PrivacyFeature(
                    OnboardingIcons.VisibilityOff,
                    "You control\nwhat’s visible",
                    Modifier.weight(1f),
                )
                PrivacyFeature(ProfileIcons.CloudOutline, "Optional\ncloud backup", Modifier.weight(1f))
            }
            Spacer(Modifier.height(dims.spacing.md))
            OnboardingBottomControls(
                activeIndex = copy.number - 1,
                enabled = enabled,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun PrivacyFeature(icon: ImageVector, label: String, modifier: Modifier) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(dims.onboarding.featureTileRadius))
            .background(colors.surfaceCard.copy(alpha = 0.82f))
            .padding(horizontal = dims.spacing.xs, vertical = dims.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.tint.copy(alpha = 0.56f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            style = ReliveTheme.typography.title.copy(fontSize = 14.sp, lineHeight = 17.sp),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FinalPage(
    enabled: Boolean,
    onFinish: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val type = ReliveTheme.typography
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.onboarding_final_fullscreen_v3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.7f),
                            ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Text(
            text = "A kinder\ntomorrow,\nwith yesterday  ♡",
            style = type.onboardingHandwritten.copy(fontSize = 17.sp, lineHeight = 22.sp),
            color = colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(maxWidth * 0.32f)
                .offset(x = maxWidth * 0.66f, y = maxHeight * 0.255f)
                .graphicsLayer { rotationZ = -6f },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    start = dims.onboarding.pageHorizontalPadding,
                    end = dims.onboarding.pageHorizontalPadding,
                    bottom = dims.onboarding.finalSheetHeight,
                )
                .verticalScroll(rememberScrollState()),
        ) {
            OnboardingHeader(number = 5, showSkip = false, enabled = enabled, onSkip = {})
            Text("You’re all set!", style = type.onboardingTitle, color = colors.textPrimary)
            Spacer(Modifier.height(dims.spacing.xs))
            Text(
                "Let’s make space for the moments\nthat matter.",
                style = type.onboardingBody,
                color = colors.textSecondary,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(dims.onboarding.finalSheetHeight)
                .clip(
                    RoundedCornerShape(
                        topStart = dims.onboarding.finalSheetRadius,
                        topEnd = dims.onboarding.finalSheetRadius,
                    ),
                )
                .background(ONBOARDING_REFERENCE_CANVAS.copy(alpha = 0.96f))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    horizontal = dims.onboarding.pageHorizontalPadding,
                    vertical = dims.spacing.md,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingPrimaryAction(
                label = "Create my first timeline  →",
                enabled = enabled,
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(dims.radii.full),
                        ambientColor = colors.accent.copy(alpha = 0.28f),
                        spotColor = colors.accent.copy(alpha = 0.28f),
                    ),
            )
            TextButton(
                onClick = onFinish,
                enabled = enabled,
                modifier = Modifier.heightIn(min = dims.minTouchTarget),
            ) {
                Text("Not now", style = type.action, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun OnboardingBottomControls(
    activeIndex: Int,
    enabled: Boolean,
    onNext: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier.fillMaxWidth().height(dims.onboarding.bottomControlsHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingProgress(activeIndex)
        Spacer(Modifier.weight(1f))
        OnboardingArrowAction(enabled = enabled, onClick = onNext)
    }
}

@Composable
private fun OnboardingProgress(pageIndex: Int) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .width(dims.onboarding.progressWidth)
            .height(dims.onboarding.progressHeight)
            .semantics {
                contentDescription = "Onboarding page ${pageIndex + 1} of $FEATURE_PAGE_COUNT"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = pageIndex.toFloat(),
                    range = 0f..(FEATURE_PAGE_COUNT - 1).toFloat(),
                    steps = FEATURE_PAGE_COUNT - 2,
                )
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(FEATURE_PAGE_COUNT) { index ->
            val active = index == pageIndex
            Box(
                Modifier
                    .size(if (active) dims.onboarding.progressDotActive else dims.onboarding.progressDot)
                    .clip(CircleShape)
                    .background(if (active) colors.accent else colors.borderMuted),
            )
        }
    }
}

@Composable
private fun OnboardingPrimaryAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.6f
            }
            .widthIn(min = dims.onboarding.primaryActionWidth)
            .height(dims.onboarding.primaryActionHeight)
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
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = ReliveTheme.typography.onboardingAction, color = colors.textOnAccent)
    }
}

@Composable
private fun OnboardingArrowAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Box(
        modifier = Modifier
            .size(dims.onboarding.arrowActionSize)
            .clip(CircleShape)
            .background(colors.accent)
            .clickable(enabled = enabled) {
                haptics.perform(ReliveHapticCue.Action)
                onClick()
            }
            .semantics {
                contentDescription = "Next"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "→",
            style = ReliveTheme.typography.onboardingAction.copy(fontSize = 24.sp),
            color = colors.textOnAccent,
        )
    }
}
