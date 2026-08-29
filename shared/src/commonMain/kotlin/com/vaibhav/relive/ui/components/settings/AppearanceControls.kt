package com.vaibhav.relive.ui.components.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.theme.RelivePalette
import com.vaibhav.relive.ui.theme.RelivePaletteOptions
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.paletteFor
import com.vaibhav.relive.ui.theme.previewGradientFor

@Composable
fun AppearanceModeControl(
    selected: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val haptics = rememberReliveHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.radii.pill))
            .background(colors.surfaceFloating)
            .padding(dims.spacing.xs),
    ) {
        AppearanceMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val label = mode.name
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = dims.minTouchTarget)
                    .clip(RoundedCornerShape(dims.radii.pill))
                    .background(if (isSelected) colors.surfaceOverlay else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = {
                            if (!isSelected) {
                                haptics.perform(ReliveHapticCue.Selection)
                                onSelect(mode)
                            }
                        },
                    )
                    .semantics { contentDescription = "$label appearance" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = ReliveTheme.typography.action,
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                )
            }
        }
    }
}

private data class PaletteChoice(
    val theme: ThemeReference?,
    val label: String,
    val palette: RelivePalette,
)

@Composable
fun RelivePalettePicker(
    selectedTheme: ThemeReference?,
    globalTheme: ThemeReference,
    includeUseAppTheme: Boolean,
    onSelect: (ThemeReference?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    val choices = buildList {
        if (includeUseAppTheme) {
            add(PaletteChoice(null, "Use app theme", paletteFor(globalTheme)))
        }
        RelivePaletteOptions.forEach { option ->
            add(PaletteChoice(option.theme, option.label, option))
        }
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        items(choices, key = { it.theme?.name ?: "inherit" }) { choice ->
            val selected = choice.theme == selectedTheme
            PaletteChoiceItem(
                choice = choice,
                selected = selected,
                isDark = ReliveTheme.isDark,
                onClick = {
                    if (!selected) {
                        haptics.perform(ReliveHapticCue.Selection)
                        onSelect(choice.theme)
                    }
                },
            )
        }
    }
}

@Composable
private fun PaletteChoiceItem(
    choice: PaletteChoice,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Column(
        modifier = Modifier
            .width(dims.profile.appearanceItemWidth)
            .heightIn(min = dims.minTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "${choice.label} palette" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(dims.profile.appearanceSelectionSize)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.border(dims.stroke.iconBold, colors.accent, CircleShape)
                    } else {
                        Modifier
                    },
                )
                .padding(dims.spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(dims.profile.appearancePreviewSize)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(previewGradientFor(choice.palette, isDark))),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) PaletteCheck()
            }
        }
        Text(
            text = choice.label,
            style = ReliveTheme.typography.tag,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun PaletteCheck() {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Canvas(Modifier.size(dims.icon.md)) {
        val stroke = dims.stroke.iconBold.toPx()
        drawCircle(colors.accent, radius = size.minDimension * 0.50f)
        drawLine(
            color = colors.textOnAccent,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.20f, size.height * 0.52f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.textOnAccent,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.75f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.27f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
