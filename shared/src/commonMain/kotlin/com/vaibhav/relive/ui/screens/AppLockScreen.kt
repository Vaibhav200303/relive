package com.vaibhav.relive.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.platform.system.AuthenticationResult
import com.vaibhav.relive.ui.components.ReliveDoodles
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.rememberGrainBrush
import com.vaibhav.relive.ui.theme.spec
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The veil over a locked archive. It sits on the same atmospheric canvas and film grain as the
 * rest of the app, carries the wordmark, and holds one emotional center — the closed, clasped
 * journal — over a single clear affordance. Authentication itself is the system's sheet
 * (biometric or device PIN, per settings); this surface frames it, raises it once on arrival
 * when biometrics are on, and answers failure with a shake instead of a dialog.
 */
@Composable
fun AppLockScreen(
    /** Biometric unlock is enabled in settings and the device can actually do it. */
    biometricsEnabled: Boolean,
    /** The device has any credential at all — gate for the "use your device PIN" fallback. */
    deviceCredentialAvailable: Boolean,
    /** The settings-respecting unlock: biometrics-only when enabled, device credential otherwise. */
    onUnlock: suspend () -> AuthenticationResult,
    /** Explicit device-credential unlock (PIN/pattern/password), offered beside biometrics. */
    onUnlockWithDeviceCredential: suspend () -> AuthenticationResult,
) {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val isDark = ReliveTheme.isDark
    val haptics = rememberReliveHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var authInFlight by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    val shakeOffset = remember { Animatable(0f) }

    val attempt: (suspend () -> AuthenticationResult) -> Unit = { authenticate ->
        if (!authInFlight) {
            authInFlight = true
            scope.launch {
                try {
                    when (authenticate()) {
                        AuthenticationResult.Authenticated -> haptics.perform(ReliveHapticCue.Confirm)
                        // A dismissed sheet is a choice, not a failure: no shake, no red line.
                        AuthenticationResult.Cancelled -> Unit
                        AuthenticationResult.Failed,
                        AuthenticationResult.Unavailable,
                        -> {
                            failedAttempts += 1
                            haptics.perform(ReliveHapticCue.Reject)
                        }
                    }
                } finally {
                    authInFlight = false
                }
            }
        }
    }

    // One knock for free: with biometrics on, the system prompt is raised as the veil appears,
    // so the common unlock is a glance rather than a tap. Never looped — a cancelled or failed
    // prompt lands the person here, with the button to try again.
    LaunchedEffect(Unit) {
        if (biometricsEnabled) attempt(onUnlock)
    }

    LaunchedEffect(failedAttempts) {
        if (failedAttempts > 0 && !reduceMotion) {
            val amplitude = with(density) { 10.dp.toPx() }
            listOf(-1f, 0.8f, -0.55f, 0.3f, -0.12f, 0f).forEach { fraction ->
                shakeOffset.animateTo(
                    targetValue = amplitude * fraction,
                    animationSpec = tween(
                        durationMillis = motion.durations.short1,
                        easing = motion.easings.standard,
                    ),
                )
            }
        }
    }

    // One entrance progress drives a soft cascade: wordmark, then the journal and greeting,
    // then the affordances, each settling downward-to-still on the emphasized curve. Reduced
    // motion collapses the whole cascade to the short shared fade with no travel.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = motion.spec(
                reduceMotion = reduceMotion,
                full = tween(
                    durationMillis = motion.durations.extraLong1,
                    easing = motion.easings.emphasizedDecelerate,
                ),
            ),
        )
    }
    fun Modifier.cascade(start: Float): Modifier = graphicsLayer {
        val visible = ((entrance.value - start) / 0.4f).coerceIn(0f, 1f)
        alpha = visible
        if (!reduceMotion) translationY = (1f - visible) * 24.dp.toPx()
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
                .padding(horizontal = dims.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Relive",
                style = type.wordmark,
                color = colors.textPrimary,
                modifier = Modifier
                    .cascade(0f)
                    .padding(vertical = dims.spacing.sm + dims.spacing.xs),
            )
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .cascade(0.15f)
                    // The shake answers a failed attempt on the emotional center, not the button.
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            ) {
                ReliveDoodles.KeptSafe()
                Spacer(Modifier.height(dims.spacing.xl))
                Text(
                    text = "Welcome back",
                    style = type.title,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(dims.spacing.xs))
                Text(
                    text = when {
                        failedAttempts > 0 -> "That didn't work — try again"
                        biometricsEnabled -> "Your memories are kept safe"
                        else -> "Unlock with your device PIN to continue"
                    },
                    style = type.subtitle,
                    color = if (failedAttempts > 0) colors.actionDestructive else colors.textSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.cascade(0.35f),
            ) {
                UnlockAction(
                    label = "Unlock",
                    enabled = !authInFlight,
                    onClick = { attempt(onUnlock) },
                )
                if (biometricsEnabled && deviceCredentialAvailable) {
                    Spacer(Modifier.height(dims.spacing.xs))
                    TextButton(
                        onClick = { attempt(onUnlockWithDeviceCredential) },
                        enabled = !authInFlight,
                    ) {
                        Text(
                            text = "Use your device PIN instead",
                            style = type.action,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(dims.spacing.xl))
        }
    }
}

/** The one emphatic control on the veil: a full-round accent pill in the CTA voice. */
@Composable
private fun UnlockAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val haptics = rememberReliveHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.96f else 1f,
        animationSpec = motion.spec(
            reduceMotion = reduceMotion,
            full = tween(
                durationMillis = motion.durations.short2,
                easing = motion.easings.standard,
            ),
        ),
        label = "unlock press scale",
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (enabled) 1f else 0.6f
            }
            .widthIn(min = 200.dp)
            .heightIn(min = ReliveTheme.dimensions.minTouchTarget)
            .clip(RoundedCornerShape(dims.radii.full))
            .background(colors.accent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) {
                haptics.perform(ReliveHapticCue.Action)
                onClick()
            }
            .semantics { contentDescription = "Unlock Relive" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ReliveTheme.typography.prominentAction,
            color = colors.textOnAccent,
        )
    }
}
