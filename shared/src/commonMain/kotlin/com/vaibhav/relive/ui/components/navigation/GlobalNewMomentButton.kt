package com.vaibhav.relive.ui.components.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.vaibhav.relive.ui.components.composer.PlusGlyph
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.ReliveTheme

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun GlobalNewMomentButton(
    onClick: () -> Unit,
    expanded: Boolean,
    expandedWidth: Dp,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = rememberReliveHaptics()
    val createMoment = {
        haptics.perform(ReliveHapticCue.Action)
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onClick()
    }
    val toolbarWidth by animateDpAsState(
        targetValue = if (expanded) expandedWidth else dims.floatingToolbar.compactWidth,
        animationSpec = tween(
            durationMillis = ReliveTheme.motion.durations.standardMillis,
            easing = ReliveTheme.motion.easings.standard,
        ),
        label = "new toolbar width",
    )
    HorizontalFloatingToolbar(
        expanded = expanded,
        modifier = Modifier
            .width(toolbarWidth)
            .height(dims.floatingToolbar.height)
            .semantics { contentDescription = "Create new moment" },
        // The primary create action: a filled primary pill, the one workhorse-accent CTA.
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = colors.accent,
            toolbarContentColor = colors.textOnAccent,
        ),
        shape = RoundedCornerShape(dims.radii.pill),
        contentPadding = PaddingValues(),
    ) {
        IconButton(
            onClick = createMoment,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(
                    space = dims.spacing.sm,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlusGlyph(
                    size = dims.icon.md,
                    color = colors.textOnAccent,
                    strokeWidth = dims.stroke.iconBold,
                )
                if (expanded && expandedWidth >= dims.floatingToolbar.newLabelMinimumWidth) {
                    Text(
                        text = "New",
                        style = ReliveTheme.typography.prominentAction,
                        color = colors.textOnAccent,
                    )
                }
            }
        }
    }
}
