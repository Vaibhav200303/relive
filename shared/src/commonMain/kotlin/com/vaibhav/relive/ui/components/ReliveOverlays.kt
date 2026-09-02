package com.vaibhav.relive.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Dialog
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.reliveBottomSheetEnter
import com.vaibhav.relive.ui.theme.reliveBottomSheetExit
import com.vaibhav.relive.ui.theme.reliveInContextVerticalEnter
import com.vaibhav.relive.ui.theme.reliveInContextVerticalExit

/**
 * Tokenized composer sheet. Unlike Material's stock sheet, its transition is explicit: a
 * 400 ms emphasized-decelerate bottom slide in and a 200 ms emphasized-accelerate slide out.
 * Reduced motion uses the shared short fade and never starts a slide.
 */
@Composable
fun ReliveBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrimColor: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val shapes = ReliveTheme.shapes
    val colors = ReliveTheme.colors
    val state = remember { MutableTransitionState(false) }.also { it.targetState = visible }

    if (state.currentState || state.targetState) {
        Popup(
            alignment = Alignment.TopStart,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .clickable(onClick = onDismissRequest),
            ) {
                AnimatedVisibility(
                    visibleState = state,
                    enter = motion.reliveBottomSheetEnter(reduceMotion),
                    exit = motion.reliveBottomSheetExit(reduceMotion),
                    modifier = Modifier.align(Alignment.BottomCenter),
                    label = "relive bottom sheet",
                ) {
                    Surface(
                        modifier = modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {},
                        shape = shapes.sheet,
                        color = colors.surfaceOverlay,
                        contentColor = colors.textPrimary,
                    ) {
                        Box(content = content)
                    }
                }
            }
        }
    }
}

/** Snackbar host whose in-screen content enters from the lower edge rather than scaling. */
@Composable
fun ReliveSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit,
) {
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        AnimatedVisibility(
            visible = true,
            enter = motion.reliveInContextVerticalEnter(reduceMotion),
            exit = motion.reliveInContextVerticalExit(reduceMotion),
            label = "relive snackbar",
        ) {
            snackbar(data)
        }
    }
}

/**
 * Relive's dialog surface with explicit in-context axis expansion. The component is deliberately
 * not a scale transform: dialogs grow along the vertical axis and leave quickly on that axis.
 */
@Composable
fun ReliveAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = ReliveTheme.shapes.dialog,
    containerColor: Color = ReliveTheme.colors.surfaceOverlay,
    iconContentColor: Color = ReliveTheme.colors.textPrimary,
    titleContentColor: Color = ReliveTheme.colors.textPrimary,
    textContentColor: Color = ReliveTheme.colors.textSecondary,
    tonalElevation: androidx.compose.ui.unit.Dp = ReliveTheme.dimensions.spacing.none,
) {
    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    val dims = ReliveTheme.dimensions
    Dialog(onDismissRequest = onDismissRequest) {
        AnimatedVisibility(
            visible = true,
            enter = motion.reliveInContextVerticalEnter(reduceMotion, Alignment.CenterVertically),
            exit = motion.reliveInContextVerticalExit(reduceMotion, Alignment.CenterVertically),
            label = "relive dialog",
        ) {
            Surface(
                modifier = modifier,
                shape = shape,
                color = containerColor,
                contentColor = titleContentColor,
                tonalElevation = tonalElevation,
            ) {
                Column(
                    modifier = Modifier.padding(dims.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
                ) {
                    icon?.let { slot ->
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides iconContentColor,
                            content = slot,
                        )
                    }
                    title?.let { slot ->
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides titleContentColor,
                            content = slot,
                        )
                    }
                    text?.let { slot ->
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides textContentColor,
                            content = slot,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
    }
}
