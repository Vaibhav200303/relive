package com.vaibhav.relive.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.vaibhav.relive.ui.components.ReliveAlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.ForwardGlyph
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics

@Composable
fun ProfileScaffold(title: String, intro: String? = null, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val d = ReliveTheme.dimensions
    ReliveBackHandler(true, onBack)
    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.bgCanvas)) {
        ProfilePageHeader(title, onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = d.spacing.huge)) {
            intro?.let { ProfileSupportingText(it, Modifier.padding(top = d.spacing.md)) }
            content()
        }
    }
}

@Composable
fun ProfilePageHeader(title: String, onBack: () -> Unit, backDescription: String = "Back to Profile", icon: ImageVector? = null) {
    val d = ReliveTheme.dimensions
    Row(Modifier.fillMaxWidth().background(ReliveTheme.colors.bgHeader).windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = d.spacing.md, vertical = d.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack, Modifier.size(d.minTouchTarget).semantics { contentDescription = backDescription }) { BackGlyph(d.icon.lg, ReliveTheme.colors.textSecondary, d.stroke.icon) }
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(d.icon.md), tint = ReliveTheme.colors.accentMuted) }
        Text(title, modifier = Modifier.padding(start = d.spacing.sm).semantics { heading() }, color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.title)
    }
}

@Composable
fun ProfileSectionHeading(text: String) {
    val d = ReliveTheme.dimensions
    Text(text, modifier = Modifier.fillMaxWidth().padding(start = d.spacing.xl, end = d.spacing.xl, top = d.spacing.xxl, bottom = d.spacing.sm).semantics { heading() }, color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.eyebrow)
}

@Composable
fun ProfileSupportingText(text: String, modifier: Modifier = Modifier) {
    val d = ReliveTheme.dimensions
    Text(text, modifier = modifier.fillMaxWidth().padding(horizontal = d.spacing.xl, vertical = d.spacing.sm), color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.body)
}

@Composable
fun ProfileDivider() = HorizontalDivider(Modifier.padding(horizontal = ReliveTheme.dimensions.spacing.xl), color = ReliveTheme.colors.borderMuted, thickness = ReliveTheme.dimensions.stroke.hairline)

@Composable
fun ProfileSettingRow(label: String, supporting: String? = null, enabled: Boolean = true, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val d = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Row(
        Modifier.fillMaxWidth().heightIn(min = d.minTouchTarget)
            .then(if (enabled && onClick != null) Modifier.clickable(role = Role.Button) { haptics.perform(ReliveHapticCue.Selection); onClick() } else Modifier)
            .padding(horizontal = d.spacing.xl, vertical = d.spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = listOfNotNull(label, supporting).joinToString(", "); if (!enabled) disabled() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(d.icon.md), tint = if (enabled) ReliveTheme.colors.accentMuted else ReliveTheme.colors.textMuted)
            Spacer(Modifier.width(d.spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = if (enabled) ReliveTheme.colors.textPrimary else ReliveTheme.colors.textMuted, style = ReliveTheme.typography.body)
            supporting?.let { Text(it, color = ReliveTheme.colors.textMuted, style = ReliveTheme.typography.tag) }
        }
        if (enabled && onClick != null) ForwardGlyph(d.icon.sm, ReliveTheme.colors.textMuted, d.stroke.icon)
    }
}

@Composable
fun ProfileSwitchRow(label: String, supporting: String? = null, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    val d = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Row(
        Modifier.fillMaxWidth().heightIn(min = d.minTouchTarget).toggleable(checked, enabled, Role.Switch) { haptics.perform(ReliveHapticCue.Selection); onCheckedChange(it) }.padding(horizontal = d.spacing.xl, vertical = d.spacing.sm).semantics(mergeDescendants = true) { contentDescription = "$label, ${if (checked) "on" else "off"}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = if (enabled) ReliveTheme.colors.textPrimary else ReliveTheme.colors.textMuted, style = ReliveTheme.typography.body)
            supporting?.let { Text(it, color = ReliveTheme.colors.textMuted, style = ReliveTheme.typography.tag) }
        }
        Switch(checked, null, enabled = enabled)
    }
}

@Composable
fun ProfileSelectionDialog(title: String, values: List<String>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) = ReliveAlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Column { values.forEach { value -> Row(Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = ReliveTheme.dimensions.spacing.sm), verticalAlignment = Alignment.CenterVertically) { RadioButton(value == selected, { onSelect(value) }); Text(value) } } } },
    confirmButton = { TextButton(onDismiss) { Text("Cancel") } },
)
